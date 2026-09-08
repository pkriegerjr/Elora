/**
 * ELORA Platform - Contract Service
 * 
 * This module handles all contract-related operations including creation,
 * digital signing, status management, and contract lifecycle.
 * Prepared for future REST API integration and digital signature API.
 */

class ContractService {
    constructor() {
        this.cache = new Map();
    }

    /**
     * Create a new contract
     * @param {Object} contractData - Contract data
     * @returns {Promise<Object>} Created contract
     */
    async createContract(contractData) {
        // In production: return await apiService.post("/contracts", contractData);
        
        await this.delay(800);
        
        this.validateContractData(contractData);
        
        // Verify caregiver is approved
        const caregiver = await caregiverService.getCaregiver(contractData.caregiverId);
        if (caregiver.status !== CONFIG.CAREGIVER_STATUS.APPROVED) {
            throw new Error("Cuidador não está aprovado para contratação");
        }
        
        const newContract = {
            id: this.generateId(),
            ...contractData,
            status: CONFIG.CONTRACT_STATUS.DRAFT,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            clientSignature: null,
            caregiverSignature: null,
            signedAt: null,
            totalValue: this.calculateContractValue(contractData),
            documentUrl: null // Will be set when PDF is generated
        };
        
        const contracts = this.getMockContracts();
        contracts.push(newContract);
        localStorage.setItem("elora_mock_contracts", JSON.stringify(contracts));
        
        // Create notifications
        await this.createNotifications(newContract);
        
        return this.sanitizeContract(newContract);
    }

    /**
     * Get contract by ID
     * @param {string} contractId - Contract ID
     * @returns {Promise<Object>} Contract data
     */
    async getContract(contractId) {
        // In production: return await apiService.get(`/contracts/${contractId}`);
        
        await this.delay(300);
        
        const contracts = this.getMockContracts();
        const contract = contracts.find(c => c.id === contractId);
        
        if (!contract) {
            throw new Error("Contrato não encontrado");
        }
        
        return this.enrichContract(contract);
    }

    /**
     * Get contracts for current user
     * @param {Object} filters - Filter options
     * @returns {Promise<Array>} Array of contracts
     */
    async getUserContracts(filters = {}) {
        // In production: return await apiService.get("/contracts", filters);
        
        await this.delay(400);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const contracts = this.getMockContracts();
        let userContracts = [];
        
        if (currentUser.type === CONFIG.USER_TYPES.CLIENT) {
            userContracts = contracts.filter(c => c.clientId === currentUser.id);
        } else if (currentUser.type === CONFIG.USER_TYPES.CAREGIVER) {
            userContracts = contracts.filter(c => c.caregiverId === currentUser.id);
        } else if (currentUser.type === CONFIG.USER_TYPES.ADMIN) {
            userContracts = [...contracts];
        }
        
        if (filters.status) {
            userContracts = userContracts.filter(c => c.status === filters.status);
        }
        
        // Enrich with related data
        return Promise.all(userContracts.map(c => this.enrichContract(c)));
    }

    /**
     * Sign contract (client or caregiver)
     * @param {string} contractId - Contract ID
     * @param {Object} signatureData - Signature data
     * @returns {Promise<Object>} Updated contract
     */
    async signContract(contractId, signatureData) {
        // In production: 
        // return await apiService.post(`/contracts/${contractId}/sign`, signatureData);
        // This would integrate with a digital signature provider (DocuSign, Clicksign, etc.)
        
        await this.delay(1000);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const contracts = this.getMockContracts();
        const index = contracts.findIndex(c => c.id === contractId);
        
        if (index === -1) {
            throw new Error("Contrato não encontrado");
        }
        
        const contract = contracts[index];
        
        // Verify user is part of this contract
        const isClient = contract.clientId === currentUser.id;
        const isCaregiver = contract.caregiverId === currentUser.id;
        
        if (!isClient && !isCaregiver && !authService.isAdmin()) {
            throw new Error("Você não tem permissão para assinar este contrato");
        }
        
        // Verify contract is in signable state
        if (isClient && contract.status !== CONFIG.CONTRACT_STATUS.AWAITING_CLIENT_SIGNATURE) {
            throw new Error("Contrato não está aguardando sua assinatura");
        }
        
        if (isCaregiver && contract.status !== CONFIG.CONTRACT_STATUS.AWAITING_CAREGIVER_SIGNATURE) {
            throw new Error("Contrato não está aguardando assinatura do cuidador");
        }
        
        // Create signature record
        const signature = {
            userId: currentUser.id,
            userType: currentUser.type,
            signedAt: new Date().toISOString(),
            ipAddress: "127.0.0.1", // In production: get from request
            userAgent: navigator.userAgent,
            signatureHash: this.generateSignatureHash(signatureData),
            // In production: digital certificate info
            certificate: signatureData.certificate || null
        };
        
        if (isClient) {
            contract.clientSignature = signature;
            contract.status = CONFIG.CONTRACT_STATUS.AWAITING_CAREGIVER_SIGNATURE;
        } else if (isCaregiver) {
            contract.caregiverSignature = signature;
            contract.status = CONFIG.CONTRACT_STATUS.SIGNED;
            contract.signedAt = new Date().toISOString();
        }
        
        contract.updatedAt = new Date().toISOString();
        localStorage.setItem("elora_mock_contracts", JSON.stringify(contracts));
        
        // Create notifications
        await this.createSignNotifications(contract, isClient);
        
        return this.enrichContract(contract);
    }

    /**
     * Get contract status
     * @param {string} contractId - Contract ID
     * @returns {Promise<Object>} Contract status
     */
    async getContractStatus(contractId) {
        // In production: return await apiService.get(`/contracts/${contractId}/status`);
        
        await this.delay(200);
        
        const contract = await this.getContract(contractId);
        
        return {
            status: contract.status,
            statusLabel: this.getStatusLabel(contract.status),
            canSign: this.canUserSign(contract),
            nextStep: this.getNextStep(contract),
            signedAt: contract.signedAt,
            clientSigned: !!contract.clientSignature,
            caregiverSigned: !!contract.caregiverSignature
        };
    }

    /**
     * Cancel contract
     * @param {string} contractId - Contract ID
     * @param {string} reason - Cancellation reason
     * @returns {Promise<Object>} Updated contract
     */
    async cancelContract(contractId, reason) {
        // In production: return await apiService.patch(`/contracts/${contractId}/cancel`, { reason });
        
        await this.delay(500);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const contracts = this.getMockContracts();
        const index = contracts.findIndex(c => c.id === contractId);
        
        if (index === -1) {
            throw new Error("Contrato não encontrado");
        }
        
        const contract = contracts[index];
        
        // Verify user is part of this contract or admin
        const isParticipant = contract.clientId === currentUser.id || 
                             contract.caregiverId === currentUser.id;
        
        if (!isParticipant && !authService.isAdmin()) {
            throw new Error("Você não tem permissão para cancelar este contrato");
        }
        
        // Can only cancel if not fully signed
        if (contract.status === CONFIG.CONTRACT_STATUS.SIGNED) {
            throw new Error("Contrato já assinado não pode ser cancelado. Entre em contato com o suporte.");
        }
        
        contract.status = CONFIG.CONTRACT_STATUS.CANCELLED;
        contract.cancelledAt = new Date().toISOString();
        contract.cancelledBy = currentUser.id;
        contract.cancellationReason = reason;
        contract.updatedAt = new Date().toISOString();
        
        localStorage.setItem("elora_mock_contracts", JSON.stringify(contracts));
        
        // Notify other party
        const otherPartyId = contract.clientId === currentUser.id ? 
                            contract.caregiverId : contract.clientId;
        
        await this.createNotificationForUser(otherPartyId, {
            type: CONFIG.NOTIFICATION_TYPES.NEW_REQUEST,
            title: "Contrato Cancelado",
            message: `O contrato #${contractId.substring(0,8)} foi cancelado. Motivo: ${reason}`,
            relatedId: contractId
        });
        
        return this.enrichContract(contract);
    }

    /**
     * Generate contract PDF (mock)
     * @param {string} contractId - Contract ID
     * @returns {Promise<Blob>} PDF blob
     */
    async generateContractPDF(contractId) {
        // In production: integrate with PDF generation service
        // return await apiService.download(`/contracts/${contractId}/pdf`);
        
        await this.delay(1000);
        
        const contract = await this.getContract(contractId);
        
        // Create mock PDF content
        const pdfContent = this.generatePDFContent(contract);
        
        // In real implementation, this would return a Blob from the server
        const blob = new Blob([pdfContent], { type: "application/pdf" });
        return blob;
    }

    /**
     * Download contract PDF
     * @param {string} contractId - Contract ID
     */
    async downloadContractPDF(contractId) {
        const blob = await this.generateContractPDF(contractId);
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `contrato_elora_${contractId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    // Private helper methods

    /**
     * Validate contract data
     * @param {Object} data - Contract data
     */
    validateContractData(data) {
        const required = ["clientId", "caregiverId", "startDate", "endDate", "days", "schedule", "hourlyRate"];
        const missing = required.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Campos obrigatórios: ${missing.join(", ")}`);
        }
        
        const start = new Date(data.startDate);
        const end = new Date(data.endDate);
        
        if (start >= end) {
            throw new Error("Data de início deve ser anterior à data de fim");
        }
        
        if (start < new Date()) {
            throw new Error("Data de início não pode ser no passado");
        }
        
        if (!data.days || data.days.length === 0) {
            throw new Error("Selecione pelo menos um dia da semana");
        }
        
        if (!data.schedule || !data.schedule.start || !data.schedule.end) {
            throw new Error("Horário de início e fim são obrigatórios");
        }
        
        if (data.hourlyRate <= 0) {
            throw new Error("Valor hora deve ser maior que zero");
        }
    }

    /**
     * Calculate total contract value
     * @param {Object} data - Contract data
     * @returns {number} Total value
     */
    calculateContractValue(data) {
        const start = new Date(data.startDate);
        const end = new Date(data.endDate);
        const diffTime = Math.abs(end - start);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        // Count weekdays in period
        let weekdaysCount = 0;
        for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
            const day = d.getDay(); // 0 = Sunday, 6 = Saturday
            if (data.days.includes(day)) {
                weekdaysCount++;
            }
        }
        
        const hoursPerDay = this.calculateHours(data.schedule.start, data.schedule.end);
        const totalHours = weekdaysCount * hoursPerDay;
        const subtotal = totalHours * data.hourlyRate;
        
        // Add platform fee (10%)
        const platformFee = subtotal * 0.10;
        const total = subtotal + platformFee;
        
        return {
            subtotal: Math.round(subtotal * 100) / 100,
            platformFee: Math.round(platformFee * 100) / 100,
            total: Math.round(total * 100) / 100,
            totalHours,
            weekdaysCount,
            hoursPerDay
        };
    }

    /**
     * Calculate hours between two times
     * @param {string} start - Start time HH:MM
     * @param {string} end - End time HH:MM
     * @returns {number} Hours
     */
    calculateHours(start, end) {
        const [startH, startM] = start.split(":").map(Number);
        const [endH, endM] = end.split(":").map(Number);
        const startMinutes = startH * 60 + startM;
        const endMinutes = endH * 60 + endM;
        return (endMinutes - startMinutes) / 60;
    }

    /**
     * Enrich contract with related data
     * @param {Object} contract - Contract object
     * @returns {Promise<Object>} Enriched contract
     */
    async enrichContract(contract) {
        const clients = this.getMockClients();
        const caregivers = this.getMockCaregivers();
        
        return {
            ...contract,
            client: clients.find(c => c.id === contract.clientId) || null,
            caregiver: caregivers.find(c => c.id === contract.caregiverId) || null,
            valueBreakdown: this.calculateContractValue(contract),
            statusLabel: this.getStatusLabel(contract.status),
            canSign: this.canUserSign(contract),
            nextStep: this.getNextStep(contract)
        };
    }

    /**
     * Sanitize contract for client
     * @param {Object} contract - Contract object
     * @returns {Object} Sanitized contract
     */
    sanitizeContract(contract) {
        // Remove sensitive internal fields if needed
        return contract;
    }

    /**
     * Get status label in Portuguese
     * @param {string} status - Contract status
     * @returns {string} Status label
     */
    getStatusLabel(status) {
        const labels = {
            [CONFIG.CONTRACT_STATUS.DRAFT]: "Rascunho",
            [CONFIG.CONTRACT_STATUS.AWAITING_CLIENT_SIGNATURE]: "Aguardando Assinatura do Cliente",
            [CONFIG.CONTRACT_STATUS.AWAITING_CAREGIVER_SIGNATURE]: "Aguardando Assinatura do Cuidador",
            [CONFIG.CONTRACT_STATUS.SIGNED]: "Assinado",
            [CONFIG.CONTRACT_STATUS.CANCELLED]: "Cancelado"
        };
        return labels[status] || status;
    }

    /**
     * Check if current user can sign contract
     * @param {Object} contract - Contract object
     * @returns {boolean} Can sign
     */
    canUserSign(contract) {
        const currentUser = authService.getCurrentUser();
        if (!currentUser) return false;
        
        if (currentUser.type === CONFIG.USER_TYPES.CLIENT) {
            return contract.status === CONFIG.CONTRACT_STATUS.AWAITING_CLIENT_SIGNATURE &&
                   contract.clientId === currentUser.id;
        }
        
        if (currentUser.type === CONFIG.USER_TYPES.CAREGIVER) {
            return contract.status === CONFIG.CONTRACT_STATUS.AWAITING_CAREGIVER_SIGNATURE &&
                   contract.caregiverId === currentUser.id;
        }
        
        return false;
    }

    /**
     * Get next step for contract
     * @param {Object} contract - Contract object
     * @returns {string} Next step description
     */
    getNextStep(contract) {
        const currentUser = authService.getCurrentUser();
        if (!currentUser) return "";
        
        switch (contract.status) {
            case CONFIG.CONTRACT_STATUS.DRAFT:
                return "Enviar para assinatura do cliente";
            case CONFIG.CONTRACT_STATUS.AWAITING_CLIENT_SIGNATURE:
                return currentUser.type === CONFIG.USER_TYPES.CLIENT 
                    ? "Assinar contrato" 
                    : "Aguardando assinatura do cliente";
            case CONFIG.CONTRACT_STATUS.AWAITING_CAREGIVER_SIGNATURE:
                return currentUser.type === CONFIG.USER_TYPES.CAREGIVER 
                    ? "Assinar contrato" 
                    : "Aguardando assinatura do cuidador";
            case CONFIG.CONTRACT_STATUS.SIGNED:
                return "Contrato assinado. Próximo passo: pagamento";
            case CONFIG.CONTRACT_STATUS.CANCELLED:
                return "Contrato cancelado";
            default:
                return "";
        }
    }

    /**
     * Generate signature hash (mock)
     * @param {Object} signatureData - Signature data
     * @returns {string} Hash
     */
    generateSignatureHash(signatureData) {
        const data = JSON.stringify(signatureData) + Date.now();
        let hash = 0;
        for (let i = 0; i < data.length; i++) {
            const char = data.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return "sha256_" + Math.abs(hash).toString(16) + Date.now().toString(16);
    }

    /**
     * Create initial notifications for new contract
     * @param {Object} contract - Contract object
     */
    async createNotifications(contract) {
        // Notify caregiver
        await this.createNotificationForUser(contract.caregiverId, {
            type: CONFIG.NOTIFICATION_TYPES.NEW_REQUEST,
            title: "Nova Solicitação de Contrato",
            message: `Você recebeu uma nova solicitação de contrato. Revise os termos e assine.`,
            relatedId: contract.id
        });
        
        // Notify client
        await this.createNotificationForUser(contract.clientId, {
            type: CONFIG.NOTIFICATION_TYPES.NEW_REQUEST,
            title: "Contrato Criado",
            message: `Seu contrato foi criado e enviado para o cuidador. Aguardando assinatura.`,
            relatedId: contract.id
        });
    }

    /**
     * Create notifications after signing
     * @param {Object} contract - Contract object
     * @param {boolean} signedByClient - Whether signed by client
     */
    async createSignNotifications(contract, signedByClient) {
        if (signedByClient) {
            // Notify caregiver
            await this.createNotificationForUser(contract.caregiverId, {
                type: CONFIG.NOTIFICATION_TYPES.CONTRACT_SIGNED,
                title: "Contrato Assinado pelo Cliente",
                message: `O cliente assinou o contrato. Agora é sua vez de assinar.`,
                relatedId: contract.id
            });
        } else {
            // Notify both parties
            await this.createNotificationForUser(contract.clientId, {
                type: CONFIG.NOTIFICATION_TYPES.CONTRACT_SIGNED,
                title: "Contrato Assinado!",
                message: `O cuidador assinou o contrato. O contrato está agora totalmente assinado.`,
                relatedId: contract.id
            });
            
            await this.createNotificationForUser(contract.caregiverId, {
                type: CONFIG.NOTIFICATION_TYPES.CONTRACT_SIGNED,
                title: "Contrato Assinado!",
                message: `Você assinou o contrato. O contrato está agora totalmente assinado.`,
                relatedId: contract.id
            });
        }
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

    /**
     * Get mock contracts from localStorage
     * @returns {Array} Array of contracts
     */
    getMockContracts() {
        return JSON.parse(localStorage.getItem("elora_mock_contracts") || "[]");
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
     * Get mock notifications from localStorage
     * @returns {Array} Array of notifications
     */
    getMockNotifications() {
        return JSON.parse(localStorage.getItem("elora_mock_notifications") || "[]");
    }

    /**
     * Generate mock PDF content
     * @param {Object} contract - Contract object
     * @returns {string} PDF content
     */
    generatePDFContent(contract) {
        const client = contract.client || {};
        const caregiver = contract.caregiver || {};
        const value = contract.valueBreakdown || {};
        
        return `
%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R >>
endobj
4 0 obj
<< /Length 1000 >>
stream
BT
/F1 12 Tf
72 720 Td
(CONTRATO DE PRESTAÇÃO DE SERVIÇOS DE CUIDADO - ELORA) Tj
0 -30 Td
/F1 10 Tf
(Contratante: ${client.name || "[Nome do Cliente]") Tj
0 -20 Td
(CPF: ${client.cpf || "[CPF do Cliente]") Tj
0 -30 Td
(Cuidador: ${caregiver.name || "[Nome do Cuidador]") Tj
0 -20 Td
(CPF: ${caregiver.cpf || "[CPF do Cuidador]") Tj
0 -30 Td
(Período: ${contract.startDate} a ${contract.endDate}) Tj
0 -20 Td
(Dias: ${contract.days?.join(", ") || "[Dias da semana]") Tj
0 -20 Td
(Horário: ${contract.schedule?.start || "[Início]"} - ${contract.schedule?.end || "[Fim]") Tj
0 -20 Td
(Valor hora: R$ ${contract.hourlyRate?.toFixed(2) || "0,00"}) Tj
0 -20 Td
(Total estimado: R$ ${value.total?.toFixed(2) || "0,00"}) Tj
0 -40 Td
(Assinatura do Contratante: _________________________) Tj
0 -20 Td
(Assinatura do Cuidador: _________________________) Tj
ET
endstream
endobj
xref
0 5
0000000000 65535 f
0000000010 00000 n
0000000060 00000 n
0000000117 00000 n
0000000215 00000 n
trailer
<< /Size 5 /Root 1 0 R >>
startxref
350
%%EOF
        `;
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
const contractService = new ContractService();

// Export for use in other modules
window.ContractService = ContractService;
window.contractService = contractService;