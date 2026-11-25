#!/usr/bin/env bash
# =====================================================================
# ELORA — sobe backend (Spring Boot :8080/api) + front (:8000 com proxy /api)
#
# O front é servido por scripts/dev_proxy.py: a página e a API ficam na mesma
# origem (http://localhost:8000), então o navegador só precisa dessa porta.
#
# Uso:
#   ./scripts/dev.sh                 # backend + front
#   ./scripts/dev.sh --backend-only  # só backend
#   ./scripts/dev.sh --front-only    # só front
#   ./scripts/dev.sh --stop          # encerra o que estiver usando as portas
#   ./scripts/dev.sh --help
#
# Portas opcionais:
#   FRONT_PORT=8000 BACKEND_PORT=8080 ./scripts/dev.sh
#
# Ctrl+C encerra os dois processos (inclusive o JVM filho do Maven).
# =====================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/backend"
FRONT="$ROOT/frontend-web"
FRONT_PORT="${FRONT_PORT:-8000}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
API="http://localhost:${BACKEND_PORT}/api"
BACK_LOG="${TMPDIR:-/tmp}/elora-backend.log"
FRONT_LOG="${TMPDIR:-/tmp}/elora-front.log"

STARTED_BACK=0
STARTED_FRONT=0
BACK_PID=""
FRONT_PID=""

usage() {
  cat <<'EOF'
ELORA — sobe backend (Spring Boot :8080/api) + front (:8000 com proxy /api)

O front é servido por scripts/dev_proxy.py na porta 8000 e encaminha /api
para o backend. Acesse apenas http://localhost:8000 (não precisa da 8080).

Uso:
  ./scripts/dev.sh                 # backend + front
  ./scripts/dev.sh --backend-only  # só backend
  ./scripts/dev.sh --front-only    # só front
  ./scripts/dev.sh --stop          # encerra o que estiver usando as portas
  ./scripts/dev.sh --help

Portas opcionais:
  FRONT_PORT=8000 BACKEND_PORT=8080 ./scripts/dev.sh

Ctrl+C encerra os dois processos (inclusive o JVM filho do Maven).
Se já houver um Elora saudável nas portas, o script reaproveita.
EOF
}

MODE="both"
case "${1:-}" in
  "")               MODE="both" ;;
  --backend-only)   MODE="backend" ;;
  --front-only)     MODE="front" ;;
  --stop)           MODE="stop" ;;
  -h|--help)        usage; exit 0 ;;
  *)                echo "Opção inválida: $1"; echo; usage; exit 1 ;;
esac

# ---------------------------------------------------------------- helpers
port_pids() { # $1 = porta -> PIDs escutando (um por linha)
  if command -v ss >/dev/null 2>&1; then
    { ss -ltnp 2>/dev/null | awk -v p=":$1" '$4 ~ p' \
        | grep -o 'pid=[0-9]*' | cut -d= -f2 | sort -u; } || true
  elif command -v lsof >/dev/null 2>&1; then
    lsof -tiTCP:"$1" -sTCP:LISTEN 2>/dev/null | sort -u || true
  fi
}

describe_pid() { # $1 = pid
  ps -p "$1" -o pid=,cmd= 2>/dev/null | sed 's/^ *//' | cut -c1-160
}

backend_healthy() {
  curl -sf --max-time 2 -o /dev/null "${API}/actuator/health"
}

front_serving() {
  # Exige o proxy (dev_proxy.py), não qualquer servidor estático na porta.
  curl -sfI --max-time 2 "http://localhost:${FRONT_PORT}/index.html" 2>/dev/null \
    | grep -qi "^x-elora-proxy: dev"
}

kill_pids() { # mata os PIDs informados, esperando um pouco
  local pid
  for pid in "$@"; do
    [ -n "$pid" ] || continue
    kill "$pid" 2>/dev/null || true
  done
  for _ in $(seq 1 10); do
    local alive=0
    for pid in "$@"; do
      [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null && alive=1
    done
    [ "$alive" -eq 0 ] && return 0
    sleep 1
  done
  for pid in "$@"; do
    [ -n "$pid" ] && kill -9 "$pid" 2>/dev/null || true
  done
}

stop_ports() {
  local port pids
  for port in "$BACKEND_PORT" "$FRONT_PORT"; do
    pids="$(port_pids "$port" | tr '\n' ' ')"
    if [ -n "${pids// /}" ]; then
      echo "Parando processos na porta ${port}: ${pids}"
      kill_pids $pids
    else
      echo "Porta ${port} já está livre."
    fi
  done
}

cleanup() {
  trap - EXIT INT TERM
  if [ "$STARTED_BACK" -eq 0 ] && [ "$STARTED_FRONT" -eq 0 ]; then
    return 0
  fi
  echo
  echo "Encerrando..."
  if [ "$STARTED_BACK" -eq 1 ]; then
    # O Maven gera um JVM filho que é quem escuta a porta.
    kill_pids "${BACK_PID:-}" $(port_pids "$BACKEND_PORT")
  fi
  if [ "$STARTED_FRONT" -eq 1 ]; then
    kill_pids "${FRONT_PID:-}" $(port_pids "$FRONT_PORT")
  fi
  wait 2>/dev/null || true
  echo "Pronto."
}

on_signal() { cleanup; exit 130; }

# ---------------------------------------------------------------- --stop
if [ "$MODE" = "stop" ]; then
  stop_ports
  exit 0
fi

# ---------------------------------------------------------------- pré-checagem
if [ "$MODE" != "front" ]; then
  if [ ! -f "$BACKEND/.env" ]; then
    echo "⚠  $BACKEND/.env não encontrado."
    echo "   Copie de .env.example e preencha DB_HOST/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET."
    echo "   Sem isso o backend tenta localhost:5432 e provavelmente falha."
    exit 1
  fi

  BUSY="$(port_pids "$BACKEND_PORT" | tr '\n' ' ' | sed 's/ *$//')"
  if [ -n "${BUSY// /}" ]; then
    if backend_healthy; then
      echo "✔ Backend Elora já está no ar em ${API} (PID ${BUSY}) — reaproveitando."
      MODE="front"
    else
      echo "✖ A porta ${BACKEND_PORT} está ocupada por outro processo:"
      for pid in $BUSY; do echo "   $(describe_pid "$pid")"; done
      echo
      echo "  Encerre com: ./scripts/dev.sh --stop"
      echo "  Ou use outra porta: BACKEND_PORT=8081 ./scripts/dev.sh"
      exit 1
    fi
  fi
fi

if [ "$MODE" != "backend" ]; then
  BUSY="$(port_pids "$FRONT_PORT" | tr '\n' ' ' | sed 's/ *$//')"
  if [ -n "${BUSY// /}" ]; then
    if front_serving; then
      echo "✔ Front já está no ar em http://localhost:${FRONT_PORT} (PID ${BUSY}) — reaproveitando."
      MODE="none"
    else
      echo "✖ A porta ${FRONT_PORT} está ocupada por outro processo:"
      for pid in $BUSY; do echo "   $(describe_pid "$pid")"; done
      echo
      echo "  Encerre com: ./scripts/dev.sh --stop"
      echo "  Ou use outra porta: FRONT_PORT=8001 ./scripts/dev.sh"
      exit 1
    fi
  fi
fi

trap cleanup EXIT
trap on_signal INT TERM

# ---------------------------------------------------------------- backend
if [ "$MODE" = "both" ] || [ "$MODE" = "backend" ]; then
  echo "▶ Backend  → ${API}  (swagger em /api/swagger-ui.html)"
  echo "  log: ${BACK_LOG}"
  ( cd "$BACKEND" && exec mvn -q spring-boot:run ) >"$BACK_LOG" 2>&1 &
  BACK_PID=$!
  STARTED_BACK=1

  echo -n "  aguardando a API responder..."
  READY=0
  for _ in $(seq 1 90); do
    if backend_healthy; then READY=1; break; fi
    if ! kill -0 "$BACK_PID" 2>/dev/null; then break; fi
    sleep 2
  done
  echo
  if [ "$READY" -ne 1 ]; then
    echo "✖ O backend não subiu. Últimas linhas de ${BACK_LOG}:"
    tail -n 20 "$BACK_LOG" | sed 's/^/  /'
    exit 1
  fi
  echo "  ✔ backend pronto."
fi

# ---------------------------------------------------------------- front
if [ "$MODE" = "both" ] || [ "$MODE" = "front" ]; then
  echo "▶ Front    → http://localhost:${FRONT_PORT}"
  echo "  log: ${FRONT_LOG}"
  if command -v python3 >/dev/null 2>&1; then
    ( exec python3 "$ROOT/scripts/dev_proxy.py" \
        --port "$FRONT_PORT" --bind 0.0.0.0 \
        --root "$FRONT" --backend-port "$BACKEND_PORT" ) >"$FRONT_LOG" 2>&1 &
  else
    echo "  ⚠ python3 não encontrado: servindo estático sem proxy."
    echo "    O front precisará falar direto com http://localhost:${BACKEND_PORT}/api."
    ( cd "$FRONT" && exec npx --yes serve -l "$FRONT_PORT" . ) >"$FRONT_LOG" 2>&1 &
  fi
  FRONT_PID=$!
  STARTED_FRONT=1

  for _ in $(seq 1 15); do
    front_serving && break
    sleep 1
  done
  if ! front_serving; then
    echo "  ✖ O front não respondeu como proxy. Últimas linhas de ${FRONT_LOG}:"
    tail -n 20 "$FRONT_LOG" | sed 's/^/  /'
    exit 1
  fi
  echo "  ✔ front pronto (proxy /api → ${API})."
fi

# ---------------------------------------------------------------- resumo
if [ "$MODE" = "none" ]; then
  echo
  echo "Nada a iniciar: os dois serviços já estavam rodando."
  exit 0
fi

echo
echo "Front (e API via proxy): http://localhost:${FRONT_PORT}"
echo "API direta (opcional, para Swagger): ${API}"
if [ -n "${CODESPACE_NAME:-}" ] && [ -n "${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN:-}" ]; then
  echo
  echo "Codespaces — abra o front pela URL encaminhada:"
  echo "  https://${CODESPACE_NAME}-${FRONT_PORT}.${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN}"
  echo "  A API vai pela mesma origem (/api); a porta ${BACKEND_PORT} pode ficar privada."
fi
echo
echo "Logins demo (senha Senha@123):"
echo "  admin@elora.local | juridico@elora.local | cliente.demo@elora.local | ana.souza@elora.local"
echo "Ctrl+C para encerrar (ou ./scripts/dev.sh --stop em outro terminal)."
wait
