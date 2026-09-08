/**
 * ELORA Platform - Configuration File
 * Adaptado para integração Spring Boot + Banco (PostgreSQL) + Cloud
 * - Front continua estático mas consome /api (relativo) em produção
 * - MOCK_MODE permite rodar sem backend (demo/local)
 */

const CONFIG = {
    // API Configuration — Spring Boot
    API: {
        // Em produção (Spring servindo static/ ou Nginx + API): use relativo "/api"
        // Em desenvolvimento isolado: "http://localhost:8080/api"
        baseURL: "/api",
        version: "v1",
        timeout: 30000,
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        // Flag para alternar mock vs API real sem quebrar protótipo
        MOCK_MODE: true, // <- altere para false quando backend Spring estiver online
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
        defaultCenter: { lat: -23.5505, lng: -46.6333 },
        defaultZoom: 12,
        // Endpoint Spring que retorna a chave com controle de acesso
        configEndpoint: "/config/maps-key"
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

    PAYMENT_METHODS: { PIX: "pix", CARD: "cartao", BOLETO: "boleto", pix: "pix", card: "cartao", boleto: "boleto", PIX: "pix", CARD: "cartao", BOLETO: "boleto", cartao: "cartao" },

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

    // Spring Boot endpoints (para referência rápida e documentação)
    ENDPOINTS: {
        auth: {
            login: "/auth/login",              // POST {identifier,password} -> {accessToken, refreshToken, user}
            registerClient: "/clients",       // POST
            registerCaregiver: "/caregivers", // POST multipart
            refresh: "/auth/refresh",         // POST {refreshToken}
            me: "/auth/me"                    // GET
        },
        clients: "/clients",
        caregivers: "/caregivers",
        caregiversSearch: "/caregivers/search",
        contracts: "/contracts",
        payments: "/payments",
        notifications: "/notifications",
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
