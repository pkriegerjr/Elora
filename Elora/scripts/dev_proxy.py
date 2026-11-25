#!/usr/bin/env python3
"""ELORA — servidor de desenvolvimento com front estático + proxy da API.

Serve `frontend-web/` na porta do front (padrão 8000) e encaminha tudo em
`/api/*` para o backend Spring (padrão 127.0.0.1:8080). Assim o navegador
fala com uma única origem: sem CORS, sem precisar encaminhar a 8080 e sem
divergência de host entre a página e a API.

Uso:
    python3 scripts/dev_proxy.py [--port 8000] [--root frontend-web]
                                 [--backend-port 8080] [--bind 0.0.0.0]

Somente biblioteca padrão — não instala nada.
"""

from __future__ import annotations

import argparse
import http.client
import os
import posixpath
import socket
import sys
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlsplit

PROXY_HEADER = "X-Elora-Proxy"
PROXY_VALUE = "dev"

# Cabeçalhos de conexão que não devem ser repassados de um lado para o outro.
HOP_BY_HOP = {
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailers",
    "transfer-encoding",
    "upgrade",
    "host",
}

NO_STORE_SUFFIXES = (".html", ".htm", ".js", ".css", ".json", ".map")
NO_STORE_TYPES = ("text/html", "text/css", "application/javascript", "text/javascript")


class DevProxyHandler(SimpleHTTPRequestHandler):
    backend_host = "127.0.0.1"
    backend_port = 8080

    # ------------------------------------------------------------------ utils
    def _is_api(self) -> bool:
        return urlsplit(self.path).path == "/api" or urlsplit(self.path).path.startswith("/api/")

    def _is_fresh_path(self) -> bool:
        path = unquote(urlsplit(self.path).path).lower()
        if path.endswith(NO_STORE_SUFFIXES):
            return True
        last = posixpath.basename(path)
        # "/" ou diretório sem extensão -> normalmente index.html
        return last == "" or "." not in last

    def end_headers(self):
        if not getattr(self, "_skip_auto_headers", False):
            if self._is_fresh_path():
                self.send_header("Cache-Control", "no-store, must-revalidate")
                self.send_header("Pragma", "no-cache")
            self.send_header(PROXY_HEADER, PROXY_VALUE)
        super().end_headers()

    def log_message(self, fmt, *args):  # log mais enxuto
        sys.stderr.write("[proxy] %s - %s\n" % (self.address_string(), fmt % args))

    # ---------------------------------------------------------------- dispatch
    def do_GET(self):
        self._dispatch("GET")

    def do_HEAD(self):
        self._dispatch("HEAD")

    def do_POST(self):
        self._dispatch("POST")

    def do_PUT(self):
        self._dispatch("PUT")

    def do_PATCH(self):
        self._dispatch("PATCH")

    def do_DELETE(self):
        self._dispatch("DELETE")

    def do_OPTIONS(self):
        self._dispatch("OPTIONS")

    def _dispatch(self, method: str):
        if self._is_api():
            self._proxy(method)
            return
        # Caminhos estáticos: apenas leitura.
        if method in ("GET", "HEAD"):
            return super().do_GET() if method == "GET" else super().do_HEAD()
        if method == "OPTIONS":
            self._skip_auto_headers = True
            self.send_response(204)
            self.send_header("Allow", "GET, HEAD, OPTIONS")
            self.end_headers()
            return
        self._json_error(405, "Método não permitido em arquivo estático.")

    # ------------------------------------------------------------------- proxy
    def _read_request_body(self) -> bytes:
        transfer = (self.headers.get("Transfer-Encoding") or "").lower()
        if "chunked" in transfer:
            chunks = []
            while True:
                line = self.rfile.readline(65536).strip()
                if not line:
                    continue
                try:
                    size = int(line.split(b";")[0], 16)
                except ValueError:
                    break
                if size == 0:
                    self.rfile.readline(65536)  # CRLF final
                    break
                chunks.append(self.rfile.read(size))
                self.rfile.readline(65536)  # CRLF do chunk
            return b"".join(chunks)

        try:
            length = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            length = 0
        return self.rfile.read(length) if length > 0 else b""

    def _proxy(self, method: str):
        body = self._read_request_body()

        headers = {
            key: value
            for key, value in self.headers.items()
            if key.lower() not in HOP_BY_HOP
        }
        headers["Host"] = f"{self.backend_host}:{self.backend_port}"

        # Requisição same-origin chega sem CORS; removemos Origin/Referer para o
        # backend não precisar autorizar o host do proxy.
        headers.pop("Origin", None)
        headers.pop("Referer", None)

        conn = http.client.HTTPConnection(self.backend_host, self.backend_port, timeout=120)
        try:
            conn.request(method, self.path, body=body if body else None, headers=headers)
            upstream = conn.getresponse()
        except (ConnectionError, socket.timeout, OSError) as exc:
            conn.close()
            self._json_error(
                502,
                "Backend indisponível em "
                f"http://{self.backend_host}:{self.backend_port}/api ({exc}).",
            )
            return

        try:
            self._skip_auto_headers = True
            self.send_response(upstream.status, upstream.reason)
            for key, value in upstream.getheaders():
                if key.lower() in HOP_BY_HOP or key.lower() == "content-length":
                    continue
                self.send_header(key, value)

            data = upstream.read()
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            if method != "HEAD":
                self.wfile.write(data)
        finally:
            conn.close()

    def _json_error(self, status: int, message: str):
        payload = (
            '{"success":false,"message":'
            + __import__("json").dumps(message)
            + ',"data":null}\n'
        ).encode("utf-8")
        self._skip_auto_headers = True
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header(PROXY_HEADER, PROXY_VALUE)
        self.end_headers()
        self.wfile.write(payload)


def main() -> int:
    parser = argparse.ArgumentParser(description="ELORA dev proxy (front + /api).")
    parser.add_argument("--port", type=int, default=int(os.environ.get("FRONT_PORT", "8000")))
    parser.add_argument("--bind", default=os.environ.get("FRONT_BIND", "0.0.0.0"))
    parser.add_argument(
        "--root",
        default=os.environ.get("FRONT_ROOT", os.path.join(os.path.dirname(__file__), "..", "frontend-web")),
    )
    parser.add_argument(
        "--backend-port", type=int, default=int(os.environ.get("BACKEND_PORT", "8080"))
    )
    parser.add_argument(
        "--backend-host", default=os.environ.get("BACKEND_HOST", "127.0.0.1")
    )
    args = parser.parse_args()

    root = os.path.abspath(args.root)
    if not os.path.isdir(root):
        print(f"✖ Diretório do front não encontrado: {root}", file=sys.stderr)
        return 1

    DevProxyHandler.backend_host = args.backend_host
    DevProxyHandler.backend_port = args.backend_port

    handler = partial(DevProxyHandler, directory=root)
    server = ThreadingHTTPServer((args.bind, args.port), handler)
    server.daemon_threads = True

    print(
        f"ELORA dev proxy em http://localhost:{args.port} "
        f"(front: {root}) -> API http://{args.backend_host}:{args.backend_port}/api",
        flush=True,
    )
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
