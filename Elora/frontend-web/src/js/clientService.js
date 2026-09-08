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

    /**
     * Create a new client
     * @param {Object} clientData - Client registration data
     * @returns {Promise<Object>} Created client
     */
    async createClient(clientData) {
        if (!CONFIG.API.MOCK_MODE) return await apiService.post(CONFIG.ENDPOINTS.clients, clientData);
        
        await this.delay(800);
        
        // Validate required fields
        this.validateClientData(clientData);
        
        // Check for duplicates
        const existingClients = this.getMockClients();
        const duplicate = existingClients.find(c => 
            c.cpf === clientData.cpf || c.email === clientData.email
        );
        
        if (duplicate) {
            throw new Error("Já existe um cliente cadastrado com este CPF ou e-mail");
        }
        
        // Create client object
        const newClient = {
            id: this.generateId(),
            ...clientData,
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
        // In production: return await apiService.get(`/clients/${clientId}`);
        
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
        // In production: return await apiService.patch(`/clients/${clientId}`, updates);
        
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
        // In production: return await apiService.post(`/clients/${clientId}/favorites`, { caregiverId });
        
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
        // In production: return await apiService.delete(`/clients/${clientId}/favorites/${caregiverId}`);
        
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
        // In production: return await apiService.get(`/clients/${clientId}/favorites`);
        
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
        // In production: return await apiService.get(`/clients/${clientId}/contracts`, { status });
        
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
        // In production: return await apiService.get(`/clients/${clientId}/payments`);
        
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
        // In production: return await apiService.get(`/clients/${clientId}/notifications`, { unreadOnly });
        
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
        // In production: return await apiService.patch(`/notifications/${notificationId}`, { read: true });
        
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
        // In production: return await apiService.get("/caregivers/search", filters);
        
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
        
        if (!CONFIG.VALIDATION.cpf.test(data.cpf)) {
            throw new Error("CPF inválido");
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