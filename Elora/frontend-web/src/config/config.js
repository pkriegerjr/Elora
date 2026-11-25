/**
 * ELORA Platform - Configuration File
 * Adaptado para integração Spring Boot + Banco (PostgreSQL) + Cloud
 * - Front continua estático mas consome /api (relativo) em produção
 * - MOCK_MODE permite rodar sem backend (demo/local)
 */

const CONFIG = {
    // API Configuration — Spring Boot — Fase 1: base http://localhost:8080/api (application.properties:3 /api)
    API: {
        // Backend Spring: context-path /api na porta 8080.
        // - Servido pelo proxy de dev (scripts/dev.sh -> :8000), pelo Live Server com
        //   proxy (.vscode/settings.json -> :5500) ou pelo próprio Spring: relativo "/api".
        // - Servidor estático sem proxy: absoluto para a API
        //   (localhost:8080; no Codespaces, <prefixo>-8080.app.github.dev).
        // - Aberto via file://: absoluto para localhost:8080.
        baseURL: (function resolveApiBaseURL() {
            if (typeof window === "undefined" || !window.location) return "/api";

            // 1) Override explícito (útil para depurar sem editar arquivo):
            //    ?apiBase=https://minha-api.exemplo/api
            //    localStorage.setItem('elora_api_base', 'https://minha-api.exemplo/api')
            try {
                const fromQuery = new URLSearchParams(window.location.search).get("apiBase");
                if (fromQuery) return String(fromQuery).replace(/\/+$/, "");
            } catch (e) { /* URLSearchParams indisponível */ }
            try {
                const saved = window.localStorage && window.localStorage.getItem("elora_api_base");
                if (saved) return String(saved).replace(/\/+$/, "");
            } catch (e) { /* localStorage bloqueado (file://, modo privado) */ }

            const { protocol, hostname, port } = window.location;
            const backendPort = "8080";
            if (protocol === "file:") return `http://localhost:${backendPort}/api`;

            // Codespaces: https://<prefixo>-<porta>.app.github.dev
            // - 8000 (proxy do dev.sh), 8080 (Spring) e 5500 (Live Server com proxy) → mesma origem.
            // - Qualquer outra porta → API na 8080.
            const codespace = hostname.match(/^(.*)-(\d+)\.app\.github\.dev$/i);
            if (codespace) {
                const prefix = codespace[1];
                const forwardedPort = codespace[2];
                if (["8000", "5500", backendPort].includes(forwardedPort)) return "/api";
                return `https://${prefix}-${backendPort}.app.github.dev/api`;
            }

            // Proxy de dev (8000), Live Server com proxy (5500), Spring direto
            // ou produção atrás de reverse proxy: mesma origem => "/api".
            // Inclui 80/443 (porta vazia).
            const sameOriginPorts = ["", "80", "443", "8000", "5500", backendPort];
            if (sameOriginPorts.includes(port)) return "/api";

            // Servidor estático sem proxy (ex.: Live Server em localhost:5500): API absoluta.
            return `${protocol}//${hostname}:${backendPort}/api`;
        })(),
        version: "v1",
        timeout: 30000,
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        // Flag para alternar mock vs API real sem quebrar protótipo
        MOCK_MODE: false, // API real (Spring Boot + Neon); use true para demo 100% localStorage
        // Spring Security: JWT em Authorization: Bearer <token>
        auth: {
            headerName: "Authorization",
            tokenPrefix: "Bearer ",
            refreshEndpoint: "/auth/refresh"
        }
    },

    // Google Maps — em produção a chave vem do backend seguro GET /api/config/maps-key
    GOOGLE_MAPS: {
        apiKey: "YOUR_GOOGLE_MAPS_API_KEY_HERE", // nunca commitar chave real
        libraries: ["places", "geometry"],
        // Sem chave, o mapa usa a localização por IP (LOCATION_IP) — não carrega script do Google
        defaultCenter: { lat: -23.5505, lng: -46.6333 },
        defaultZoom: 12,
        // Endpoint Spring que retorna a chave com controle de acesso
        configEndpoint: "/config/maps-key"
    },

    // Mapa gratuito: Leaflet + tiles do OpenStreetMap (sem API key paga).
    MAPS: {
        provider: "leaflet",
        leafletCss: "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css",
        leafletJs: "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js",
        tileUrl: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        defaultCenter: { lat: -23.5505, lng: -46.6333 },
        defaultZoom: 12
    },

    // Localização usada quando não há chave do Google Maps (caminho sem APIs pagas).
    // Edite latitude/longitude/cidade com a sua localização real, se desejar.
    LOCATION_IP: {
        ip: "3.95.197.167",
        type: "ipv4",
        country_code: "BR",
        country_name: "Brazil",
        region_name: "São Paulo",
        city: "São Paulo",
        zip: "01310-100",
        latitude: -23.5505,
        longitude: -46.6333
    },

    APP: {
        name: "ELORA",
        version: "1.1.0-spring-ready",
        description: "Plataforma de conexão entre clientes e cuidadores qualificados",
        // Quando front for empacotado em src/main/resources/static do Spring
        springStaticPath: "src/main/resources/static"
    },

    USER_TYPES: { CLIENT: "client", CAREGIVER: "caregiver", ADMIN: "admin" },

    CAREGIVER_STATUS: {
        PENDING: "PENDING",               // espelha enum Java CaregiverStatus
        UNDER_REVIEW: "UNDER_REVIEW",
        APPROVED: "APPROVED",
        REJECTED: "REJECTED",
        NEEDS_CORRECTION: "NEEDS_CORRECTION",
        // compat com valores antigos em lowercase para mock
        pending: "PENDING",
        under_review: "UNDER_REVIEW",
        approved: "APPROVED",
        rejected: "REJECTED",
        needs_correction: "NEEDS_CORRECTION"
    },

    // V2: contrato.status ENUM('rascunho','proposta','negociacao','aguard_assinatura','ativo','concluido','rescindido','cancelado','em_disputa')
    // Mantém compatibilidade com mocks em inglês (DRAFT/SIGNED) — mapeamento bidirecional abaixo
    CONTRACT_STATUS: {
        // Inglês (mock / frontend legado)
        DRAFT: "DRAFT",
        AWAITING_CLIENT_SIGNATURE: "AWAITING_CLIENT_SIGNATURE",
        AWAITING_CAREGIVER_SIGNATURE: "AWAITING_CAREGIVER_SIGNATURE",
        SIGNED: "SIGNED",
        CANCELLED: "CANCELLED",
        // V2 PT (DB/API)
        RASCUNHO: "rascunho",
        PROPOSTA: "proposta",
        NEGOCIACAO: "negociacao",
        AGUARD_ASSINATURA: "aguard_assinatura",
        ATIVO: "ativo",
        CONCLUIDO: "concluido",
        RESCINDIDO: "rescindido",
        CANCELADO: "cancelado",
        EM_DISPUTA: "em_disputa",
        // aliases lowercase para compatibilidade direta com DB
        rascunho: "rascunho", proposta: "proposta", negociacao: "negociacao",
        aguard_assinatura: "aguard_assinatura", ativo: "ativo", concluido: "concluido",
        rescindido: "rescindido", cancelado: "cancelado", em_disputa: "em_disputa",
        // legado lowercase inglês
        draft: "DRAFT",
        awaiting_client_signature: "AWAITING_CLIENT_SIGNATURE",
        awaiting_caregiver_signature: "AWAITING_CAREGIVER_SIGNATURE",
        signed: "SIGNED",
        cancelled: "CANCELLED"
    },

    // V2: pagamento.status ENUM('pendente','aprovado','recusado','estornado') + pendente|processando no gateway
    PAYMENT_STATUS: {
        PENDING: "pendente",
        PROCESSING: "pendente", // gateway ainda pendente
        APPROVED: "aprovado",
        REJECTED: "recusado",
        CANCELLED: "estornado",
        // aliases PT direto
        pendente: "pendente",
        aprovado: "aprovado",
        recusado: "recusado",
        estornado: "estornado",
        // legado inglês para mocks
        PENDING_EN: "PENDING", PROCESSING_EN: "PROCESSING", APPROVED_EN: "APPROVED",
        REJECTED_EN: "REJECTED", CANCELLED_EN: "CANCELLED"
    },

    PAYMENT_METHODS: { PIX: "pix", CARD: "cartao", BOLETO: "boleto", pix: "pix", card: "cartao", boleto: "boleto", cartao: "cartao" },

    // V2: disponibilidade/escala periodo ENUM('matutino','vespertino','noturno')
    DISPONIBILIDADE_PERIODO: { MATUTINO: "matutino", VESPERTINO: "vespertino", NOTURNO: "noturno", matutino: "matutino", vespertino: "vespertino", noturno: "noturno" },

    // V2: usuario.genero ENUM('M','F','outro','prefiro_nao_informar') + status
    GENERO: { M: "M", F: "F", OUTRO: "outro", PREFIRO_NAO_INFORMAR: "prefiro_nao_informar" },
    USUARIO_STATUS: { ATIVO: "ativo", INATIVO: "inativo", SUSPENSO: "suspenso" },

    NOTIFICATION_TYPES: {
        CONTRACT_SIGNED: "CONTRACT_SIGNED",
        PAYMENT_APPROVED: "PAYMENT_APPROVED",
        CAREGIVER_CONFIRMED: "CAREGIVER_CONFIRMED",
        REGISTRATION_UNDER_REVIEW: "REGISTRATION_UNDER_REVIEW",
        DOCUMENTS_APPROVED: "DOCUMENTS_APPROVED",
        NEW_REQUEST: "NEW_REQUEST",
        CONTRACT_EXPIRING: "CONTRACT_EXPIRING"
    },

    STORAGE_KEYS: {
        AUTH_TOKEN: "elora_auth_token",
        REFRESH_TOKEN: "elora_refresh_token",
        CURRENT_USER: "elora_current_user",
        USER_PREFERENCES: "elora_user_preferences",
        MOCK_DATA: "elora_mock_data"
    },

    VALIDATION: {
        cpf: /^\d{3}\.?\d{3}\.?\d{3}-?\d{2}$/,
        email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
        phone: /^\(?\d{2}\)?\s?\d{4,5}-?\d{4}$/,
        cep: /^\d{5}-?\d{3}$/,
        password: { minLength: 8, requireUppercase: true, requireLowercase: true, requireNumber: true, requireSpecialChar: true }
    },

    PAGINATION: { defaultPage: 0, defaultLimit: 10, maxLimit: 100 }, // Spring Pageable é 0-based

    MAP_SEARCH_RADIUS: { min: 1, max: 100, default: 10 },

    // Helpers de compatibilidade V1 (inglês) <-> V2 (PT DB) — uso: CONFIG.toV2ContractStatus('SIGNED') => 'ativo'
    // Mantém MOCK_MODE funcionando enquanto API migra
    toV2ContractStatus: null, // preenchido após declaração
    fromV2ContractStatus: null,
    toV2PaymentStatus: null,
    normalizeCPF: null, // remove máscara -> 11 dígitos (CHAR(11) V2)
    // Conversão período disponível <-> matutino/vespertino/noturno (V2)
    scheduleToPeriodos: null,

    // Spring Boot endpoints — Fase 2/3 (NotificacaoController.java:34, RelatorioController)
    ENDPOINTS: {
        auth: {
            login: "/auth/login",
            registerClient: "/clients",
            registerCaregiver: "/caregivers",
            refresh: "/auth/refresh",
            me: "/auth/me"
        },
        clients: "/clients",
        caregivers: "/caregivers",
        // P0: alinhado ao BuscaController (@RequestMapping /busca)
        busca: "/busca",
        caregiversSearch: "/busca/profissionais",
        // P0: alinhado ao ContratoController (@RequestMapping /contratos)
        contracts: "/contratos",
        // EscalaController: GET /escalas?contratoId&inicio&fim, POST /escalas/gerar, PATCH /escalas/{id}
        escalas: "/escalas",
        // PagamentoController (/payments): POST, GET, GET /{id}, POST /{id}/pagar,
        // GET /{id}/status, PATCH /{id}/cancel, POST /{id}/pix/qrcode, POST /{id}/boleto, /repasses
        payments: "/payments",
        notifications: "/notifications", // GET ?unreadOnly&tipo&limit, GET /unread-count, PATCH /read-all, PATCH /{id}, GET/PATCH /preferences, DELETE /{id}, POST /
        relatorios: "/relatorios", // GET /resumo|financeiro|usuarios|contratos|avaliacoes|denuncias (?inicio&fim)
        // P2: AvaliacaoController (/avaliacoes), ConhecimentoController (/conhecimento/artigos), JuridicoController (/juridico)
        avaliacoes: "/avaliacoes",
        conhecimento: "/conhecimento",
        juridico: "/juridico",
        mapsKey: "/config/maps-key", // MapsKeyController: GET -> {apiKey, configured}
        admin: "/admin/caregivers"
    }
};

// Mappers V1<->V2 (definidos fora do literal para usar CONFIG)
CONFIG.normalizeCPF = function(cpf){ return (cpf||'').replace(/\D/g,'').slice(0,11); };
CONFIG.toV2ContractStatus = function(s){
    const m={ DRAFT:'rascunho', AWAITING_CLIENT_SIGNATURE:'aguard_assinatura', AWAITING_CAREGIVER_SIGNATURE:'aguard_assinatura', SIGNED:'ativo', CANCELLED:'cancelado', PENDING:'rascunho', APPROVED:'ativo' };
    if(!s) return 'rascunho';
    const norm=String(s).toLowerCase();
    // se já é PT V2, retorna normalizado
    if(['rascunho','proposta','negociacao','aguard_assinatura','ativo','concluido','rescindido','cancelado','em_disputa'].includes(norm)) return norm;
    return m[String(s).toUpperCase()] || 'rascunho';
};
CONFIG.fromV2ContractStatus = function(s){
    const m={ rascunho:'DRAFT', proposta:'DRAFT', negociacao:'DRAFT', aguard_assinatura:'AWAITING_CLIENT_SIGNATURE', ativo:'SIGNED', concluido:'SIGNED', rescindido:'CANCELLED', cancelado:'CANCELLED', em_disputa:'SIGNED' };
    return m[String(s||'').toLowerCase()] || 'DRAFT';
};
CONFIG.toV2PaymentStatus = function(s){
    const m={ PENDING:'pendente', PROCESSING:'pendente', APPROVED:'aprovado', REJECTED:'recusado', CANCELLED:'estornado', pendente:'pendente', aprovado:'aprovado', recusado:'recusado', estornado:'estornado' };
    return m[String(s||'').toUpperCase()] || m[String(s||'').toLowerCase()] || 'pendente';
};
CONFIG.scheduleToPeriodos = function(start, end){
    // Converte horário HH:MM para periodos V2 matutino (06-12), vespertino (12-18), noturno (18-06)
    const toMin=t=>{ const [h,m]=t.split(':').map(Number); return h*60+m; };
    const s=toMin(start), e=toMin(end);
    const res=new Set();
    // heurística simples por sobreposição
    const mat=[6*60,12*60], ves=[12*60,18*60], not1=[18*60,24*60], not2=[0,6*60];
    const overlap=(a,b,c,d)=> Math.max(a,c) < Math.min(b,d);
    if(overlap(s,e,mat[0],mat[1])) res.add('matutino');
    if(overlap(s,e,ves[0],ves[1])) res.add('vespertino');
    if(overlap(s,e,not1[0],not1[1]) || overlap(s,e,not2[0],not2[1])) res.add('noturno');
    return [...res];
};

window.CONFIG = CONFIG;

// Diagnóstico: mostra no console para qual API a página está apontando.
// Se estiver errado, use ?apiBase=<url completa> ou
// localStorage.setItem('elora_api_base', '<url completa>').
if (typeof window !== "undefined" && window.console && console.info) {
    console.info("[Elora] API base:", CONFIG.API.baseURL, "| MOCK_MODE:", CONFIG.API.MOCK_MODE);
}
