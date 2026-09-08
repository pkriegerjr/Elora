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

    CONTRACT_STATUS: {
        DRAFT: "DRAFT",
        AWAITING_CLIENT_SIGNATURE: "AWAITING_CLIENT_SIGNATURE",
        AWAITING_CAREGIVER_SIGNATURE: "AWAITING_CAREGIVER_SIGNATURE",
        SIGNED: "SIGNED",
        CANCELLED: "CANCELLED",
        draft: "DRAFT",
        awaiting_client_signature: "AWAITING_CLIENT_SIGNATURE",
        awaiting_caregiver_signature: "AWAITING_CAREGIVER_SIGNATURE",
        signed: "SIGNED",
        cancelled: "CANCELLED"
    },

    PAYMENT_STATUS: {
        PENDING: "PENDING",
        PROCESSING: "PROCESSING",
        APPROVED: "APPROVED",
        REJECTED: "REJECTED",
        CANCELLED: "CANCELLED",
        pending: "PENDING",
        processing: "PROCESSING",
        approved: "APPROVED",
        rejected: "REJECTED",
        cancelled: "CANCELLED"
    },

    PAYMENT_METHODS: { PIX: "PIX", CARD: "CARD", BOLETO: "BOLETO", pix: "PIX", card: "CARD", boleto: "BOLETO" },

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

window.CONFIG = CONFIG;
