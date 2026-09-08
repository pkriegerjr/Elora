/**
 * ELORA Platform - Caregiver Service
 * 
 * This module handles all caregiver-related operations including registration,
 * profile management, document upload, availability, and verification status.
 * Prepared for future REST API integration.
 */

class CaregiverService {
    constructor() {
        this.cache = new Map();
        this.cacheTimeout = 5 * 60 * 1000; // 5 minutes
    }

    /**
     * Create a new caregiver
     * @param {Object} caregiverData - Caregiver registration data
     * @returns {Promise<Object>} Created caregiver
     */
    async createCaregiver(caregiverData) {
        // In production: return await apiService.post("/caregivers", caregiverData);
        
        await this.delay(800);
        
        this.validateCaregiverData(caregiverData);
        
        const existingCaregivers = this.getMockCaregivers();
        const duplicate = existingCaregivers.find(c => 
            c.cpf === caregiverData.cpf || c.email === caregiverData.email
        );
        
        if (duplicate) {
            throw new Error("Já existe um cuidador cadastrado com este CPF ou e-mail");
        }
        
        const newCaregiver = {
            id: this.generateId(),
            ...caregiverData,
            password: caregiverData.password, // In production: hashed!
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            status: CONFIG.CAREGIVER_STATUS.PENDING,
            rating: 0,
            reviewCount: 0,
            verified: false,
            verificationDate: null,
            documents: caregiverData.documents || [],
            availability: caregiverData.availability || {},
            contracts: [],
            earnings: 0,
            completedJobs: 0
        };
        
        existingCaregivers.push(newCaregiver);
        localStorage.setItem("elora_mock_caregivers", JSON.stringify(existingCaregivers));
        
        const { password, documents, ...sanitized } = newCaregiver;
        return sanitized;
    }

    /**
     * Get caregiver by ID
     * @param {string} caregiverId - Caregiver ID
     * @returns {Promise<Object>} Caregiver data
     */
    async getCaregiver(caregiverId) {
        // In production: return await apiService.get(`/caregivers/${caregiverId}`);
        
        await this.delay(300);
        
        const caregivers = this.getMockCaregivers();
        const caregiver = caregivers.find(c => c.id === caregiverId);
        
        if (!caregiver) {
            throw new Error("Cuidador não encontrado");
        }
        
        const { password, documents, ...sanitized } = caregiver;
        return sanitized;
    }

    /**
     * Get current authenticated caregiver profile
     * @returns {Promise<Object>} Caregiver profile
     */
    async getProfile() {
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
            throw new Error("Usuário não autenticado ou não é um cuidador");
        }
        
        return this.getCaregiver(currentUser.id);
    }

    /**
     * Get full caregiver profile with documents (for admin)
     * @param {string} caregiverId - Caregiver ID
     * @returns {Promise<Object>} Full caregiver profile
     */
    async getFullProfile(caregiverId) {
        // In production: return await apiService.get(`/caregivers/${caregiverId}/full`);
        
        await this.delay(300);
        
        const caregivers = this.getMockCaregivers();
        const caregiver = caregivers.find(c => c.id === caregiverId);
        
        if (!caregiver) {
            throw new Error("Cuidador não encontrado");
        }
        
        const { password, ...sanitized } = caregiver;
        return sanitized;
    }

    /**
     * Update caregiver profile
     * @param {Object} updates - Fields to update
     * @returns {Promise<Object>} Updated caregiver
     */
    async updateProfile(updates) {
        // In production: return await apiService.patch(`/caregivers/${caregiverId}`, updates);
        
        await this.delay(500);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
            throw new Error("Usuário não autenticado");
        }
        
        const caregivers = this.getMockCaregivers();
        const index = caregivers.findIndex(c => c.id === currentUser.id);
        
        if (index === -1) {
            throw new Error("Cuidador não encontrado");
        }
        
        // Don't allow updating sensitive fields directly
        const { id, password, createdAt, status, verified, verificationDate, rating, reviewCount, ...allowedUpdates } = updates;
        
        caregivers[index] = {
            ...caregivers[index],
            ...allowedUpdates,
            updatedAt: new Date().toISOString()
        };
        
        localStorage.setItem("elora_mock_caregivers", JSON.stringify(caregivers));
        
        const { password: _, documents, ...sanitized } = caregivers[index];
        authService.updateCurrentUser(sanitized);
        
        return sanitized;
    }

    /**
     * Update caregiver availability
     * @param {Object} availability - Availability data
     * @returns {Promise<Object>} Updated caregiver
     */
    async updateAvailability(availability) {
        return this.updateProfile({ availability });
    }

    /**
     * Upload caregiver documents
     * @param {Object} documents - Documents object with file data
     * @returns {Promise<Object>} Updated caregiver with documents
     */
    async uploadDocuments(documents) {
        // In production: 
        // const formData = new FormData();
        // Object.entries(documents).forEach(([key, file]) => formData.append(key, file));
        // return await apiService.upload(`/caregivers/${caregiverId}/documents`, formData);
        
        await this.delay(1000);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
            throw new Error("Usuário não autenticado");
        }
        
        const caregivers = this.getMockCaregivers();
        const index = caregivers.findIndex(c => c.id === currentUser.id);
        
        if (index === -1) {
            throw new Error("Cuidador não encontrado");
        }
        
        // In mock, documents are just metadata objects
        const documentRecords = {};
        for (const [key, file] of Object.entries(documents)) {
            if (file) {
                documentRecords[key] = {
                    name: file.name || `document_${key}`,
                    type: file.type || "application/pdf",
                    size: file.size || 0,
                    uploadedAt: new Date().toISOString(),
                    // In production: URL returned by upload API
                    url: `mock://documents/${currentUser.id}/${key}_${Date.now()}.pdf`,
                    status: "pending_review"
                };
            }
        }
        
        caregivers[index].documents = {
            ...caregivers[index].documents,
            ...documentRecords
        };
        caregivers[index].updatedAt = new Date().toISOString();
        
        // If all required documents uploaded, change status to under review
        const requiredDocs = ["idDocument", "cpf", "addressProof", "certificates"];
        const hasAllRequired = requiredDocs.every(doc => 
            caregivers[index].documents[doc] && caregivers[index].documents[doc].status !== "rejected"
        );
        
        if (hasAllRequired && caregivers[index].status === CONFIG.CAREGIVER_STATUS.PENDING) {
            caregivers[index].status = CONFIG.CAREGIVER_STATUS.UNDER_REVIEW;
        }
        
        localStorage.setItem("elora_mock_caregivers", JSON.stringify(caregivers));
        
        const { password: _, documents: __, ...sanitized } = caregivers[index];
        return sanitized;
    }

    /**
     * Get caregiver verification status
     * @returns {Promise<Object>} Verification status
     */
    async getVerificationStatus() {
        // In production: return await apiService.get(`/caregivers/${caregiverId}/verification`);
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
            throw new Error("Usuário não autenticado");
        }
        
        const caregivers = this.getMockCaregivers();
        const caregiver = caregivers.find(c => c.id === currentUser.id);
        
        if (!caregiver) {
            throw new Error("Cuidador não encontrado");
        }
        
        const requiredDocs = ["idDocument", "cpf", "addressProof", "certificates"];
        const documentStatus = {};
        
        requiredDocs.forEach(doc => {
            const document = caregiver.documents?.[doc];
            documentStatus[doc] = document ? document.status : "not_uploaded";
        });
        
        return {
            status: caregiver.status,
            verified: caregiver.verified,
            verificationDate: caregiver.verificationDate,
            documents: documentStatus,
            canRequestReview: requiredDocs.every(doc => documentStatus[doc] === "pending_review" || documentStatus[doc] === "approved"),
            missingDocuments: requiredDocs.filter(doc => documentStatus[doc] === "not_uploaded")
        };
    }

    /**
     * Get caregiver's contracts
     * @param {string} status - Filter by status (optional)
     * @returns {Promise<Array>} Array of contracts
     */
    async getContracts(status = null) {
        // In production: return await apiService.get(`/caregivers/${caregiverId}/contracts`, { status });
        
        await this.delay(400);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
            throw new Error("Usuário não autenticado");
        }
        
        const contracts = this.getMockContracts();
        let caregiverContracts = contracts.filter(c => c.caregiverId === currentUser.id);
        
        if (status) {
            caregiverContracts = caregiverContracts.filter(c => c.status === status);
        }
        
        // Enrich with client data
        const clients = this.getMockClients();
        return caregiverContracts.map(contract => ({
            ...contract,
            client: clients.find(cl => cl.id === contract.clientId) || null
        }));
    }

    /**
     * Get caregiver's earnings
     * @param {Object} period - Period filter {start, end}
     * @returns {Promise<Object>} Earnings summary
     */
    async getEarnings(period = {}) {
        // In production: return await apiService.get(`/caregivers/${caregiverId}/earnings`, period);
        
        await this.delay(400);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
            throw new Error("Usuário não autenticado");
        }
        
        const payments = this.getMockPayments();
        const contracts = this.getMockContracts();
        
        // Get completed contracts for this caregiver
        const completedContracts = contracts.filter(c => 
            c.caregiverId === currentUser.id && 
            c.status === CONFIG.CONTRACT_STATUS.SIGNED
        );
        
        const contractIds = completedContracts.map(c => c.id);
        const relevantPayments = payments.filter(p => 
            contractIds.includes(p.contractId) && 
            p.status === CONFIG.PAYMENT_STATUS.APPROVED
        );
        
        // Filter by period if provided
        let filteredPayments = relevantPayments;
        if (period.start) {
            filteredPayments = filteredPayments.filter(p => new Date(p.createdAt) >= new Date(period.start));
        }
        if (period.end) {
            filteredPayments = filteredPayments.filter(p => new Date(p.createdAt) <= new Date(period.end));
        }
        
        const total = filteredPayments.reduce((sum, p) => sum + (p.amount || 0), 0);
        const pending = relevantPayments
            .filter(p => p.status === CONFIG.PAYMENT_STATUS.PENDING || p.status === CONFIG.PAYMENT_STATUS.PROCESSING)
            .reduce((sum, p) => sum + (p.amount || 0), 0);
        
        return {
            total,
            pending,
            available: total,
            payments: filteredPayments,
            period
        };
    }

    /**
     * Get caregiver's schedule/agenda
     * @param {Object} period - Period filter {start, end}
     * @returns {Promise<Array>} Scheduled appointments
     */
    async getSchedule(period = {}) {
        // In production: return await apiService.get(`/caregivers/${caregiverId}/schedule`, period);
        
        await this.delay(400);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
            throw new Error("Usuário não autenticado");
        }
        
        const contracts = this.getMockContracts();
        const activeContracts = contracts.filter(c => 
            c.caregiverId === currentUser.id && 
            [CONFIG.CONTRACT_STATUS.SIGNED, CONFIG.CONTRACT_STATUS.AWAITING_CAREGIVER_SIGNATURE].includes(c.status)
        );
        
        const clients = this.getMockClients();
        
        return activeContracts.map(contract => ({
            contractId: contract.id,
            clientName: clients.find(cl => cl.id === contract.clientId)?.name || "Cliente",
            startDate: contract.startDate,
            endDate: contract.endDate,
            days: contract.days,
            schedule: contract.schedule,
            status: contract.status
        }));
    }

    /**
     * Get caregiver's reviews
     * @returns {Promise<Array>} Array of reviews
     */
    async getReviews() {
        // In production: return await apiService.get(`/caregivers/${caregiverId}/reviews`);
        
        await this.delay(400);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
            throw new Error("Usuário não autenticado");
        }
        
        const reviews = this.getMockReviews();
        return reviews.filter(r => r.caregiverId === currentUser.id)
            .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }

    /**
     * Get caregiver's notifications
     * @param {boolean} unreadOnly - Only unread
     * @returns {Promise<Array>} Notifications
     */
    async getNotifications(unreadOnly = false) {
        // In production: return await apiService.get(`/caregivers/${caregiverId}/notifications`, { unreadOnly });
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser || currentUser.type !== CONFIG.USER_TYPES.CAREGIVER) {
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
     * Search caregivers (public)
     * @param {Object} filters - Search filters
     * @returns {Promise<Array>} Matching caregivers
     */
    async searchCaregivers(filters = {}) {
        // In production: return await apiService.get("/caregivers/search", filters);
        
        await this.delay(500);
        
        let caregivers = this.getMockCaregivers()
            .filter(cg => cg.status === CONFIG.CAREGIVER_STATUS.APPROVED);
        
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
        
        if (filters.experience) {
            caregivers = caregivers.filter(cg => cg.experience >= filters.experience);
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

    // Admin methods

    /**
     * Get all caregivers for admin (with pagination)
     * @param {Object} params - Query parameters
     * @returns {Promise<Object>} Paginated caregivers
     */
    async getAllCaregivers(params = {}) {
        // In production: return await apiService.get("/admin/caregivers", params);
        
        await this.delay(500);
        
        if (!authService.isAdmin()) {
            throw new Error("Acesso negado: apenas administradores");
        }
        
        const { page = 1, limit = 10, status, search } = params;
        let caregivers = this.getMockCaregivers();
        
        if (status) {
            caregivers = caregivers.filter(c => c.status === status);
        }
        
        if (search) {
            const searchLower = search.toLowerCase();
            caregivers = caregivers.filter(c => 
                c.name.toLowerCase().includes(searchLower) ||
                c.cpf.includes(search) ||
                c.email.toLowerCase().includes(searchLower)
            );
        }
        
        // Sort by creation date (newest first)
        caregivers.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        
        const total = caregivers.length;
        const start = (page - 1) * limit;
        const end = start + limit;
        const data = caregivers.slice(start, end).map(cg => {
            const { password, documents, ...sanitized } = cg;
            return {
                ...sanitized,
                cpf: this.maskCPF(cg.cpf)
            };
        });
        
        return {
            data,
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.ceil(total / limit)
            }
        };
    }

    /**
     * Update caregiver status (admin)
     * @param {string} caregiverId - Caregiver ID
     * @param {string} status - New status
     * @param {string} reason - Reason for status change
     * @returns {Promise<Object>} Updated caregiver
     */
    async updateCaregiverStatus(caregiverId, status, reason = "") {
        // In production: return await apiService.patch(`/admin/caregivers/${caregiverId}/status`, { status, reason });
        
        await this.delay(500);
        
        if (!authService.isAdmin()) {
            throw new Error("Acesso negado: apenas administradores");
        }
        
        const caregivers = this.getMockCaregivers();
        const index = caregivers.findIndex(c => c.id === caregiverId);
        
        if (index === -1) {
            throw new Error("Cuidador não encontrado");
        }
        
        const oldStatus = caregivers[index].status;
        caregivers[index].status = status;
        caregivers[index].updatedAt = new Date().toISOString();
        
        if (status === CONFIG.CAREGIVER_STATUS.APPROVED) {
            caregivers[index].verified = true;
            caregivers[index].verificationDate = new Date().toISOString();
        } else if (status === CONFIG.CAREGIVER_STATUS.REJECTED || status === CONFIG.CAREGIVER_STATUS.NEEDS_CORRECTION) {
            caregivers[index].verified = false;
            caregivers[index].verificationDate = null;
        }
        
        localStorage.setItem("elora_mock_caregivers", JSON.stringify(caregivers));
        
        // Create notification for caregiver
        await this.createNotificationForUser(caregiverId, {
            type: status === CONFIG.CAREGIVER_STATUS.APPROVED 
                ? CONFIG.NOTIFICATION_TYPES.DOCUMENTS_APPROVED
                : CONFIG.NOTIFICATION_TYPES.REGISTRATION_UNDER_REVIEW,
            title: status === CONFIG.CAREGIVER_STATUS.APPROVED 
                ? "Cadastro Aprovado!" 
                : status === CONFIG.CAREGIVER_STATUS.REJECTED 
                    ? "Cadastro Reprovado" 
                    : "Correção Necessária",
            message: reason || this.getStatusMessage(status),
            relatedId: caregiverId
        });
        
        const { password, documents, ...sanitized } = caregivers[index];
        return sanitized;
    }

    /**
     * Get status message for notification
     * @param {string} status - Caregiver status
     * @returns {string} Message
     */
    getStatusMessage(status) {
        const messages = {
            [CONFIG.CAREGIVER_STATUS.APPROVED]: "Parabéns! Seu cadastro foi aprovado e você já pode receber solicitações.",
            [CONFIG.CAREGIVER_STATUS.REJECTED]: "Infelizmente seu cadastro não foi aprovado. Entre em contato com o suporte para mais informações.",
            [CONFIG.CAREGIVER_STATUS.NEEDS_CORRECTION]: "Seu cadastro precisa de correções. Verifique os documentos pendentes e atualize as informações.",
            [CONFIG.CAREGIVER_STATUS.UNDER_REVIEW]: "Seu cadastro está sendo analisado pela nossa equipe. Você será notificado quando houver atualização."
        };
        return messages[status] || "Status atualizado.";
    }

    /**
     * Create notification for user
     * @param {string} userId - User ID
     * @param {Object} notification - Notification data
     */
    async createNotificationForUser(userId, notification) {
        const notifications = this.getMockNotifications();
        notifications.push({
            id: this.generateId(),
            userId,
            ...notification,
            read: false,
            createdAt: new Date().toISOString()
        });
        localStorage.setItem("elora_mock_notifications", JSON.stringify(notifications));
    }

    // Helper methods

    /**
     * Get mock caregivers from localStorage
     * @returns {Array} Array of caregivers
     */
    getMockCaregivers() {
        return JSON.parse(localStorage.getItem("elora_mock_caregivers") || "[]");
    }

    /**
     * Get mock clients from localStorage
     * @returns {Array} Array of clients
     */
    getMockClients() {
        return JSON.parse(localStorage.getItem("elora_mock_clients") || "[]");
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
     * Get mock reviews from localStorage
     * @returns {Array} Array of reviews
     */
    getMockReviews() {
        return JSON.parse(localStorage.getItem("elora_mock_reviews") || "[]");
    }

    /**
     * Validate caregiver registration data
     * @param {Object} data - Caregiver data
     */
    validateCaregiverData(data) {
        const required = ["name", "cpf", "birthDate", "phone", "email", "password", "experience"];
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
     * Mask CPF for display
     * @param {string} cpf - CPF string
     * @returns {string} Masked CPF
     */
    maskCPF(cpf) {
        if (!cpf) return "";
        const cleaned = cpf.replace(/\D/g, "");
        if (cleaned.length !== 11) return cpf;
        return `***.***.${cleaned.substring(6,9)}-${cleaned.substring(9)}`;
    }

    /**
     * Calculate distance between two locations (mock)
     * @param {Object} loc1 - Location 1 {lat, lng}
     * @param {Object} loc2 - Location 2 {lat, lng}
     * @returns {number} Distance in km
     */
    calculateDistance(loc1, loc2) {
        if (!loc1 || !loc2 || !loc1.lat || !loc2.lat) return 0;
        
        const R = 6371;
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
const caregiverService = new CaregiverService();

// Export for use in other modules
window.CaregiverService = CaregiverService;
window.caregiverService = caregiverService;