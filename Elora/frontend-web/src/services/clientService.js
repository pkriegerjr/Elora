/**
 * ELORA Platform - Client Service
 * 
 * This module handles all client-related operations including registration,
 * profile management, care needs, and favorites.
 * Prepared for future REST API integration.
 */

class ClientService {
    constructor() {
        this.cache = new Map();
        this.cacheTimeout = 5 * 60 * 1000; // 5 minutes
    }

    /** DTO Spring (UsuarioResponse) -> formato usado pelas telas. */
    mapClientFromApi(p) {
        if (!p) return p;
        return {
            ...p,
            name: p.nome ?? p.name,
            phone: p.telefone ?? p.phone,
            photo: p.fotoUrl ?? p.photo,
            gender: p.genero ?? p.gender,
            birthDate: p.dataNascimento ?? p.birthDate,
            careNeeds: p.observacoesCuidado != null
                ? { ...(p.careNeeds || {}), description: p.observacoesCuidado }
                : p.careNeeds,
            createdAt: p.criadoEm ?? p.createdAt
        };
    }

    /**
     * Create a new client
     * @param {Object} clientData - Client registration data
     * @returns {Promise<Object>} Created client
     */
    async createClient(clientData) {
        if (!CONFIG.API.MOCK_MODE) {
            return this.mapClientFromApi(await apiService.post(CONFIG.ENDPOINTS.clients, clientData));
        }
        
        await this.delay(800);
        
        // Validate required fields
        this.validateClientData(clientData);
        
        // V2: normaliza CPF para CHAR(11) dígitos (elora_schema_v2.sql:36)
        clientData.cpfDigits = CONFIG.normalizeCPF(clientData.cpf);
        clientData.cpf = clientData.cpfDigits ? clientData.cpf : clientData.cpf; // mantém máscara para exibição mas guarda cpfDigits
        // Check for duplicates (por cpfDigits ou email)
        const existingClients = this.getMockClients();
        const duplicate = existingClients.find(c => 
            (c.cpfDigits||c.cpf?.replace(/\D/g,'')) === clientData.cpfDigits || c.email === clientData.email
        );
        
        if (duplicate) {
            throw new Error("Já existe um cliente cadastrado com este CPF ou e-mail");
        }
        
        // Create client object (V2: genero, latitude/longitude, consentimento)
        const newClient = {
            id: this.generateId(),
            ...clientData,
            genero: clientData.genero||'prefiro_nao_informar',
            latitude: clientData.address?.latitude ?? clientData.latitude ?? null,
            longitude: clientData.address?.longitude ?? clientData.longitude ?? null,
            consentimentoLgpd: true,
            password: clientData.password, // In production: hashed!
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            status: "active",
            favorites: [],
            contracts: [],
            notifications: []
        };
        
        // Save to mock storage
        existingClients.push(newClient);
        localStorage.setItem("elora_mock_clients", JSON.stringify(existingClients));
        
        // Return sanitized client (without password)
        const { password, ...sanitized } = newClient;
        return sanitized;
    }

    /**
     * Get client by ID
     * @param {string} clientId - Client ID
     * @returns {Promise<Object>} Client data
     */
    async getClient(clientId) {
        if (!CONFIG.API.MOCK_MODE) {
            return this.mapClientFromApi(await apiService.get(`${CONFIG.ENDPOINTS.clients}/${clientId}`));
        }
        
        await this.delay(300);
        
        const clients = this.getMockClients();
        const client = clients.find(c => c.id === clientId);
        
        if (!client) {
            throw new Error("Cliente não encontrado");
        }
        
        const { password, ...sanitized } = client;
        return sanitized;
    }

    /**
     * Get current authenticated client profile
     * @returns {Promise<Object>} Client profile
     */
    async getProfile() {
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CLIENT) {
            throw new Error("Usuário não autenticado ou não é um cliente");
        }
        
        return this.getClient(currentUser.id);
    }

    /**
     * Update client profile
     * @param {Object} updates - Fields to update
     * @returns {Promise<Object>} Updated client
     */
    async updateProfile(updates) {
        if (!CONFIG.API.MOCK_MODE) {
            const payload = {};
            const nome = updates.name ?? updates.nome;
            const telefone = updates.phone ?? updates.telefone;
            if (nome != null) payload.nome = nome;
            if (telefone != null) payload.telefone = telefone;
            if (updates.careNeeds?.description != null) {
                payload.observacoesCuidado = updates.careNeeds.description;
            }
            if (updates.address?.latitude != null) payload.latitude = updates.address.latitude;
            if (updates.address?.longitude != null) payload.longitude = updates.address.longitude;
            const res = await apiService.patch(`${CONFIG.ENDPOINTS.clients}/me`, payload);
            const mapped = this.mapClientFromApi(res);
            authService.updateCurrentUser(mapped);
            return mapped;
        }

        await this.delay(500);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CLIENT) {
            throw new Error("Usuário não autenticado");
        }
        
        const clients = this.getMockClients();
        const index = clients.findIndex(c => c.id === currentUser.id);
        
        if (index === -1) {
            throw new Error("Cliente não encontrado");
        }
        
        // Don't allow updating sensitive fields directly
        const { id, password, createdAt, ...allowedUpdates } = updates;
        
        clients[index] = {
            ...clients[index],
            ...allowedUpdates,
            updatedAt: new Date().toISOString()
        };
        
        localStorage.setItem("elora_mock_clients", JSON.stringify(clients));
        
        const { password: _, ...sanitized } = clients[index];
        authService.updateCurrentUser(sanitized);
        
        return sanitized;
    }

    /**
     * Update client address
     * @param {Object} addressData - Address data
     * @returns {Promise<Object>} Updated client
     */
    async updateAddress(addressData) {
        return this.updateProfile({ address: addressData });
    }

    /**
     * Update care needs
     * @param {Object} careNeeds - Care needs data
     * @returns {Promise<Object>} Updated client
     */
    async updateCareNeeds(careNeeds) {
        return this.updateProfile({ careNeeds });
    }

    /**
     * Add caregiver to favorites
     * @param {string} caregiverId - Caregiver ID
     * @returns {Promise<Object>} Updated client
     */
    async addFavorite(caregiverId) {
        if (!CONFIG.API.MOCK_MODE) {
            // BuscaController: POST /busca/favoritos { profissionalId: Integer }
            return await apiService.post(`${CONFIG.ENDPOINTS.busca}/favoritos`, {
                profissionalId: Number(caregiverId)
            });
        }
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CLIENT) {
            throw new Error("Usuário não autenticado");
        }
        
        const clients = this.getMockClients();
        const index = clients.findIndex(c => c.id === currentUser.id);
        
        if (index === -1) {
            throw new Error("Cliente não encontrado");
        }
        
        if (!clients[index].favorites) {
            clients[index].favorites = [];
        }
        
        if (!clients[index].favorites.includes(caregiverId)) {
            clients[index].favorites.push(caregiverId);
            clients[index].updatedAt = new Date().toISOString();
            localStorage.setItem("elora_mock_clients", JSON.stringify(clients));
            
            const { password, ...sanitized } = clients[index];
            authService.updateCurrentUser(sanitized);
        }
        
        const { password: _, ...sanitized } = clients[index];
        return sanitized;
    }

    /**
     * Remove caregiver from favorites
     * @param {string} caregiverId - Caregiver ID
     * @returns {Promise<Object>} Updated client
     */
    async removeFavorite(caregiverId) {
        if (!CONFIG.API.MOCK_MODE) {
            return await apiService.delete(`${CONFIG.ENDPOINTS.busca}/favoritos/${caregiverId}`);
        }
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CLIENT) {
            throw new Error("Usuário não autenticado");
        }
        
        const clients = this.getMockClients();
        const index = clients.findIndex(c => c.id === currentUser.id);
        
        if (index === -1) {
            throw new Error("Cliente não encontrado");
        }
        
        if (clients[index].favorites) {
            clients[index].favorites = clients[index].favorites.filter(id => id !== caregiverId);
            clients[index].updatedAt = new Date().toISOString();
            localStorage.setItem("elora_mock_clients", JSON.stringify(clients));
            
            const { password, ...sanitized } = clients[index];
            authService.updateCurrentUser(sanitized);
        }
        
        const { password: _, ...sanitized } = clients[index];
        return sanitized;
    }

    /**
     * Get client's favorite caregivers
     * @returns {Promise<Array>} Array of caregiver objects
     */
    async getFavorites() {
        if (!CONFIG.API.MOCK_MODE) {
            // BuscaController: GET /busca/favoritos -> List<ProfissionalResponse>
            const lista = await apiService.get(`${CONFIG.ENDPOINTS.busca}/favoritos`);
            const svc = window.caregiverService;
            return (lista || []).map(c => svc ? svc.mapCaregiverFromApi(c) : c);
        }
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CLIENT) {
            throw new Error("Usuário não autenticado");
        }
        
        const clients = this.getMockClients();
        const client = clients.find(c => c.id === currentUser.id);
        
        if (!client || !client.favorites) {
            return [];
        }
        
        const caregivers = this.getMockCaregivers();
        return client.favorites
            .map(favId => caregivers.find(cg => cg.id === favId))
            .filter(Boolean)
            .map(cg => {
                const { password, documents, ...sanitized } = cg;
                return sanitized;
            });
    }

    /**
     * Get client's contracts
     * @param {string} status - Filter by status (optional)
     * @returns {Promise<Array>} Array of contracts
     */
    async getContracts(status = null) {
        if (!CONFIG.API.MOCK_MODE) {
            const list = await apiService.get(CONFIG.ENDPOINTS.contracts, { papel: 'cliente' });
            const svc = window.contractService;
            const mapped = (list || []).map(c => svc?.mapContractFromApi ? svc.mapContractFromApi(c) : c);
            if (!status) return mapped;
            const alvo = CONFIG.toV2ContractStatus(status);
            return mapped.filter(c => String(c.status).toLowerCase() === alvo);
        }
        
        await this.delay(400);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CLIENT) {
            throw new Error("Usuário não autenticado");
        }
        
        const contracts = this.getMockContracts();
        let clientContracts = contracts.filter(c => c.clientId === currentUser.id);
        
        if (status) {
            clientContracts = clientContracts.filter(c => c.status === status);
        }
        
        // Enrich with caregiver data
        const caregivers = this.getMockCaregivers();
        return clientContracts.map(contract => ({
            ...contract,
            caregiver: caregivers.find(cg => cg.id === contract.caregiverId) || null
        }));
    }

    /**
     * Get client's payment history
     * @returns {Promise<Array>} Array of payments
     */
    async getPayments() {
        if (!CONFIG.API.MOCK_MODE) {
            const list = await apiService.get(CONFIG.ENDPOINTS.payments);
            const svc = window.paymentService;
            return (list || []).map(p => svc ? svc.mapPaymentFromApi(p) : p);
        }
        
        await this.delay(400);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CLIENT) {
            throw new Error("Usuário não autenticado");
        }
        
        const payments = this.getMockPayments();
        return payments.filter(p => p.clientId === currentUser.id);
    }

    /**
     * Get client's notifications
     * @param {boolean} unreadOnly - Only unread notifications
     * @returns {Promise<Array>} Array of notifications
     */
    async getNotifications(unreadOnly = false) {
        if (!CONFIG.API.MOCK_MODE) {
            const params = {};
            if (unreadOnly) params.unreadOnly = true;
            const list = await apiService.get(CONFIG.ENDPOINTS.notifications, params);
            return (list || []).map(n => ({
                ...n,
                userId: n.destinatarioId ?? n.userId,
                title: n.titulo ?? n.title,
                message: n.corpo ?? n.message,
                read: n.lida ?? n.read ?? false,
                readAt: n.lidaEm ?? n.readAt,
                createdAt: n.enviadoEm ?? n.createdAt
            }));
        }
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CLIENT) {
            throw new Error("Usuário não autenticado");
        }
        
        const notifications = this.getMockNotifications();
        let userNotifications = notifications.filter(n => n.userId === currentUser.id);
        
        if (unreadOnly) {
            userNotifications = userNotifications.filter(n => !n.read);
        }
        
        return userNotifications.sort((a, b) => 
            new Date(b.createdAt) - new Date(a.createdAt)
        );
    }

    /**
     * Mark notification as read
     * @param {string} notificationId - Notification ID
     * @returns {Promise<Object>} Updated notification
     */
    async markNotificationRead(notificationId) {
        if (!CONFIG.API.MOCK_MODE) {
            const res = await apiService.patch(
                `${CONFIG.ENDPOINTS.notifications}/${notificationId}`, { lida: true });
            return {
                ...res,
                userId: res.destinatarioId ?? res.userId,
                title: res.titulo ?? res.title,
                message: res.corpo ?? res.message,
                read: res.lida ?? true,
                readAt: res.lidaEm ?? res.readAt,
                createdAt: res.enviadoEm ?? res.createdAt
            };
        }

        await this.delay(200);
        
        const notifications = this.getMockNotifications();
        const index = notifications.findIndex(n => n.id === notificationId);
        
        if (index !== -1) {
            notifications[index].read = true;
            notifications[index].readAt = new Date().toISOString();
            localStorage.setItem("elora_mock_notifications", JSON.stringify(notifications));
            return notifications[index];
        }
        
        throw new Error("Notificação não encontrada");
    }

    /**
     * Search for caregivers (client side)
     * @param {Object} filters - Search filters
     * @returns {Promise<Array>} Matching caregivers
     */
    async searchCaregivers(filters = {}) {
        if (!CONFIG.API.MOCK_MODE) {
            // Busca pública vive em /busca/profissionais (BuscaController).
            if (window.caregiverService) return caregiverService.searchCaregivers(filters);

            const params = {};
            if (filters.name || filters.search) params.nome = filters.name || filters.search;
            if (filters.maxPrice != null) params.precoMax = filters.maxPrice;
            if (filters.minRating != null) params.notaMinima = filters.minRating;
            const lista = await apiService.get(CONFIG.ENDPOINTS.caregiversSearch, params);
            return lista || [];
        }

        await this.delay(500);
        
        let caregivers = this.getMockCaregivers()
            .filter(cg => cg.status === CONFIG.CAREGIVER_STATUS.APPROVED);
        
        // Apply filters
        if (filters.specialty) {
            caregivers = caregivers.filter(cg => 
                cg.specialties && cg.specialties.includes(filters.specialty)
            );
        }
        
        if (filters.minRating) {
            caregivers = caregivers.filter(cg => cg.rating >= filters.minRating);
        }
        
        if (filters.maxPrice) {
            caregivers = caregivers.filter(cg => cg.hourlyRate <= filters.maxPrice);
        }
        
        if (filters.availability) {
            caregivers = caregivers.filter(cg => 
                cg.availability && this.checkAvailabilityMatch(cg.availability, filters.availability)
            );
        }
        
        if (filters.verifiedOnly) {
            caregivers = caregivers.filter(cg => cg.verified);
        }
        
        if (filters.distance && filters.userLocation) {
            caregivers = caregivers.map(cg => ({
                ...cg,
                distance: this.calculateDistance(filters.userLocation, cg.address)
            })).filter(cg => cg.distance <= filters.distance);
        }
        
        // Sanitize
        return caregivers.map(cg => {
            const { password, documents, ...sanitized } = cg;
            return sanitized;
        });
    }

    /**
     * Get mock clients from localStorage
     * @returns {Array} Array of clients
     */
    getMockClients() {
        return JSON.parse(localStorage.getItem("elora_mock_clients") || "[]");
    }

    /**
     * Get mock caregivers from localStorage
     * @returns {Array} Array of caregivers
     */
    getMockCaregivers() {
        return JSON.parse(localStorage.getItem("elora_mock_caregivers") || "[]");
    }

    /**
     * Get mock contracts from localStorage
     * @returns {Array} Array of contracts
     */
    getMockContracts() {
        return JSON.parse(localStorage.getItem("elora_mock_contracts") || "[]");
    }

    /**
     * Get mock payments from localStorage
     * @returns {Array} Array of payments
     */
    getMockPayments() {
        return JSON.parse(localStorage.getItem("elora_mock_payments") || "[]");
    }

    /**
     * Get mock notifications from localStorage
     * @returns {Array} Array of notifications
     */
    getMockNotifications() {
        return JSON.parse(localStorage.getItem("elora_mock_notifications") || "[]");
    }

    /**
     * Validate client registration data
     * @param {Object} data - Client data
     */
    validateClientData(data) {
        const required = ["name", "cpf", "birthDate", "phone", "email", "password"];
        const missing = required.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Campos obrigatórios: ${missing.join(", ")}`);
        }
        
        if (!CONFIG.VALIDATION.email.test(data.email)) {
            throw new Error("E-mail inválido");
        }
        
        // CPF: 11 dígitos + dígitos verificadores (validateCPF global de app.js; fallback só-dígitos)
        if (data.cpf) {
            const digits = CONFIG.normalizeCPF(data.cpf);
            const ok = (typeof validateCPF === "function") ? validateCPF(digits) : /^\d{11}$/.test(digits);
            if (!ok) throw new Error("CPF inválido");
        }
        
        if (!CONFIG.VALIDATION.phone.test(data.phone)) {
            throw new Error("Telefone inválido");
        }
    }

    /**
     * Check if caregiver availability matches client needs
     * @param {Object} caregiverAvailability - Caregiver availability
     * @param {Object} clientNeeds - Client needs
     * @returns {boolean} True if matches
     */
    checkAvailabilityMatch(caregiverAvailability, clientNeeds) {
        if (!caregiverAvailability.days || !clientNeeds.days) return true;
        
        const clientDays = clientNeeds.days;
        const caregiverDays = caregiverAvailability.days;
        
        return clientDays.some(day => caregiverDays.includes(day));
    }

    /**
     * Calculate distance between two locations (mock)
     * @param {Object} loc1 - Location 1 {lat, lng}
     * @param {Object} loc2 - Location 2 {lat, lng}
     * @returns {number} Distance in km
     */
    calculateDistance(loc1, loc2) {
        // Mock implementation - in production use Google Maps Distance Matrix API
        if (!loc1 || !loc2 || !loc1.lat || !loc2.lat) return 0;
        
        const R = 6371; // Earth radius in km
        const dLat = (loc2.lat - loc1.lat) * Math.PI / 180;
        const dLon = (loc2.lng - loc1.lng) * Math.PI / 180;
        const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(loc1.lat * Math.PI / 180) * Math.cos(loc2.lat * Math.PI / 180) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.round(R * c * 10) / 10;
    }

    /**
     * Generate unique ID
     * @returns {string} Unique ID
     */
    generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substring(2, 9);
    }

    /**
     * Delay helper
     * @param {number} ms - Milliseconds
     * @returns {Promise<void>}
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Create singleton instance
const clientService = new ClientService();

// Export for use in other modules
window.ClientService = ClientService;
window.clientService = clientService;