/**
 * ELORA Platform - Payment Service
 * 
 * This module handles all payment-related operations including payment creation,
 * processing simulation, status management, and integration preparation for
 * payment gateways (PIX, credit card, boleto).
 * 
 * IMPORTANT: This is a SIMULATION layer. Real payment processing must be
 * handled by certified payment gateways (Mercado Pago, Stripe, PagSeguro, etc.)
 * Never process real payments in frontend code!
 */

class PaymentService {
    constructor() {
        this.cache = new Map();
    }

    /**
     * Create a new payment
     * @param {Object} paymentData - Payment data
     * @returns {Promise<Object>} Created payment
     */
    async createPayment(paymentData) {
        // In production: return await apiService.post("/payments", paymentData);
        // This would integrate with payment gateway APIs
        
        await this.delay(800);
        
        this.validatePaymentData(paymentData);
        
        // Verify contract exists and is signed
        const contract = await contractService.getContract(paymentData.contractId);
        if (contract.status !== CONFIG.CONTRACT_STATUS.SIGNED) {
            throw new Error("Contrato deve estar assinado para gerar pagamento");
        }
        
        // Check if payment already exists for this contract
        const existingPayments = this.getMockPayments();
        const existing = existingPayments.find(p => 
            p.contractId === paymentData.contractId && 
            [CONFIG.PAYMENT_STATUS.PENDING, CONFIG.PAYMENT_STATUS.PROCESSING, CONFIG.PAYMENT_STATUS.APPROVED].includes(p.status)
        );
        
        if (existing) {
            throw new Error("Já existe um pagamento pendente ou aprovado para este contrato");
        }
        
        const idempotencyKey = paymentData.idempotencyKey || paymentData.idempotency_key || `idem-${paymentData.contractId||paymentData.contratoId}-${Date.now()}`;
        const vMethod = (paymentData.metodo||paymentData.method||'pix').toLowerCase();
        const normalizedMethod = vMethod==='pix'?'pix': vMethod==='cartao'?'cartao':'boleto';
        const newPayment = {
            id: this.generateId(),
            contratoId: paymentData.contratoId||paymentData.contractId,
            contractId: paymentData.contractId||paymentData.contratoId,
            pagadorId: paymentData.pagadorId||paymentData.clientId,
            clientId: paymentData.clientId||paymentData.pagadorId,
            caregiverId: paymentData.caregiverId,
            amount: paymentData.amount || paymentData.valorBruto,
            valorBruto: paymentData.valorBruto || paymentData.amount,
            valorTaxa: paymentData.valorTaxa || Math.round((paymentData.amount||0)*0.10*100)/100,
            valorLiquido: paymentData.valorLiquido || Math.round((paymentData.amount||0)*0.90*100)/100,
            idempotencyKey, idempotency_key: idempotencyKey,
            method: normalizedMethod, metodo: normalizedMethod,
            ...paymentData,
            contratoId: paymentData.contratoId||paymentData.contractId,
            contractId: paymentData.contractId||paymentData.contratoId,
            status: CONFIG.PAYMENT_STATUS.PENDING,
            statusV2: 'pendente',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            approvedAt: null,
            rejectedAt: null,
            rejectedReason: null,
            transactionId: null,
            gatewayResponse: null
        };
        
        existingPayments.push(newPayment);
        localStorage.setItem("elora_mock_payments", JSON.stringify(existingPayments));
        
        // Create notification for client
        await this.createNotificationForUser(paymentData.clientId, {
            type: CONFIG.NOTIFICATION_TYPES.PAYMENT_APPROVED,
            title: "Pagamento Gerado",
            message: `Um novo pagamento foi gerado para o contrato #${paymentData.contractId.substring(0,8)}.`,
            relatedId: newPayment.id
        });
        
        return this.sanitizePayment(newPayment);
    }

    /**
     * Get payment by ID
     * @param {string} paymentId - Payment ID
     * @returns {Promise<Object>} Payment data
     */
    async getPayment(paymentId) {
        // In production: return await apiService.get(`/payments/${paymentId}`);
        
        await this.delay(300);
        
        const payments = this.getMockPayments();
        const payment = payments.find(p => p.id === paymentId);
        
        if (!payment) {
            throw new Error("Pagamento não encontrado");
        }
        
        return this.enrichPayment(payment);
    }

    /**
     * Get payments for current user
     * @param {Object} filters - Filter options
     * @returns {Promise<Array>} Array of payments
     */
    async getUserPayments(filters = {}) {
        // In production: return await apiService.get("/payments", filters);
        
        await this.delay(400);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const payments = this.getMockPayments();
        let userPayments = [];
        
        if (currentUser.type === CONFIG.USER_TYPES.CLIENT) {
            userPayments = payments.filter(p => p.clientId === currentUser.id);
        } else if (currentUser.type === CONFIG.USER_TYPES.CAREGIVER) {
            // Get payments for caregiver's contracts
            const contracts = this.getMockContracts();
            const caregiverContractIds = contracts
                .filter(c => c.caregiverId === currentUser.id)
                .map(c => c.id);
            userPayments = payments.filter(p => caregiverContractIds.includes(p.contractId));
        } else if (currentUser.type === CONFIG.USER_TYPES.ADMIN) {
            userPayments = [...payments];
        }
        
        if (filters.status) {
            userPayments = userPayments.filter(p => p.status === filters.status);
        }
        
        return Promise.all(userPayments.map(p => this.enrichPayment(p)));
    }

    /**
     * Get payment status
     * @param {string} paymentId - Payment ID
     * @returns {Promise<Object>} Payment status
     */
    async getPaymentStatus(paymentId) {
        // In production: return await apiService.get(`/payments/${paymentId}/status`);
        
        await this.delay(200);
        
        const payment = await this.getPayment(paymentId);
        
        return {
            status: payment.status,
            statusLabel: this.getStatusLabel(payment.status),
            canPay: payment.status === CONFIG.PAYMENT_STATUS.PENDING,
            canCancel: [CONFIG.PAYMENT_STATUS.PENDING, CONFIG.PAYMENT_STATUS.PROCESSING].includes(payment.status),
            approvedAt: payment.approvedAt,
            rejectedAt: payment.rejectedAt
        };
    }

    /**
     * Process payment (simulate gateway processing)
     * @param {string} paymentId - Payment ID
     * @param {Object} paymentDetails - Payment method details
     * @returns {Promise<Object>} Updated payment
     */
    async processPayment(paymentId, paymentDetails) {
        // In production: 
        // 1. Validate payment details
        // 2. Call payment gateway API (Mercado Pago, Stripe, etc.)
        // 3. Handle webhook/callback for status updates
        // 4. Never handle sensitive card data in frontend!
        
        await this.delay(2000); // Simulate gateway processing
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const payments = this.getMockPayments();
        const index = payments.findIndex(p => p.id === paymentId);
        
        if (index === -1) {
            throw new Error("Pagamento não encontrado");
        }
        
        const payment = payments[index];
        
        // Verify user is the payer (client)
        if (payment.clientId !== currentUser.id && !authService.isAdmin()) {
            throw new Error("Você não tem permissão para processar este pagamento");
        }
        
        if (payment.status !== CONFIG.PAYMENT_STATUS.PENDING) {
            throw new Error("Pagamento não está em status pendente");
        }
        
        // Update to processing
        payment.status = CONFIG.PAYMENT_STATUS.PROCESSING;
        payment.updatedAt = new Date().toISOString();
        payment.paymentMethod = paymentDetails.method;
        payment.paymentDetails = this.sanitizePaymentDetails(paymentDetails);
        localStorage.setItem("elora_mock_payments", JSON.stringify(payments));
        
        // Simulate gateway response
        // In production, this would be handled by webhook
        const success = this.simulateGatewayResponse(paymentDetails);
        
        if (success) {
            payment.status = CONFIG.PAYMENT_STATUS.APPROVED;
            payment.approvedAt = new Date().toISOString();
            payment.transactionId = this.generateTransactionId();
            payment.gatewayResponse = { success: true, code: "00" };
        } else {
            payment.status = CONFIG.PAYMENT_STATUS.REJECTED;
            payment.rejectedAt = new Date().toISOString();
            payment.rejectedReason = "Pagamento recusado pela instituição financeira";
            payment.gatewayResponse = { success: false, code: "05" };
        }
        
        payment.updatedAt = new Date().toISOString();
        localStorage.setItem("elora_mock_payments", JSON.stringify(payments));
        
        // Create notifications
        await this.createPaymentNotifications(payment);
        
        return this.enrichPayment(payment);
    }

    /**
     * Cancel payment
     * @param {string} paymentId - Payment ID
     * @param {string} reason - Cancellation reason
     * @returns {Promise<Object>} Updated payment
     */
    async cancelPayment(paymentId, reason) {
        // In production: return await apiService.patch(`/payments/${paymentId}/cancel`, { reason });
        
        await this.delay(500);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const payments = this.getMockPayments();
        const index = payments.findIndex(p => p.id === paymentId);
        
        if (index === -1) {
            throw new Error("Pagamento não encontrado");
        }
        
        const payment = payments[index];
        
        // Verify user is the payer or admin
        if (payment.clientId !== currentUser.id && !authService.isAdmin()) {
            throw new Error("Você não tem permissão para cancelar este pagamento");
        }
        
        // Can only cancel pending or processing payments
        if (![CONFIG.PAYMENT_STATUS.PENDING, CONFIG.PAYMENT_STATUS.PROCESSING].includes(payment.status)) {
            throw new Error("Pagamento não pode ser cancelado no status atual");
        }
        
        payment.status = CONFIG.PAYMENT_STATUS.CANCELLED;
        payment.cancelledAt = new Date().toISOString();
        payment.cancelledBy = currentUser.id;
        payment.cancellationReason = reason;
        payment.updatedAt = new Date().toISOString();
        
        localStorage.setItem("elora_mock_payments", JSON.stringify(payments));
        
        // Notify caregiver
        const contract = await contractService.getContract(payment.contractId);
        if (contract.caregiverId) {
            await this.createNotificationForUser(contract.caregiverId, {
                type: CONFIG.NOTIFICATION_TYPES.PAYMENT_APPROVED,
                title: "Pagamento Cancelado",
                message: `O pagamento do contrato #${payment.contractId.substring(0,8)} foi cancelado.`,
                relatedId: paymentId
            });
        }
        
        return this.enrichPayment(payment);
    }

    /**
     * Generate PIX QR Code (mock)
     * @param {string} paymentId - Payment ID
     * @returns {Promise<Object>} PIX data with QR code
     */
    async generatePixQrCode(paymentId) {
        // In production: integrate with PIX API (Banco Central / payment gateway)
        // return await apiService.post(`/payments/${paymentId}/pix/qrcode`);
        
        await this.delay(500);
        
        const payment = await this.getPayment(paymentId);
        
        // Mock PIX data
        const pixData = {
            qrCode: this.generateMockPixQrCode(payment),
            qrCodeBase64: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...", // Would be real QR code
            copiaEcola: this.generatePixCopiaECola(payment),
            expiresAt: new Date(Date.now() + 30 * 60 * 1000).toISOString() // 30 minutes
        };
        
        return pixData;
    }

    /**
     * Generate boleto (mock)
     * @param {string} paymentId - Payment ID
     * @returns {Promise<Object>} Boleto data
     */
    async generateBoleto(paymentId) {
        // In production: integrate with boleto API (payment gateway)
        // return await apiService.post(`/payments/${paymentId}/boleto`);
        
        await this.delay(500);
        
        const payment = await this.getPayment(paymentId);
        
        // Mock boleto data
        const boletoData = {
            barcode: this.generateMockBarcode(payment),
            linhaDigitavel: this.generateMockLinhaDigitavel(payment),
            pdfUrl: `mock://boletos/${paymentId}.pdf`,
            expiresAt: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString() // 3 days
        };
        
        return boletoData;
    }

    /**
     * Simulate card payment (mock)
     * @param {string} paymentId - Payment ID
     * @param {Object} cardData - Card data (tokenized in production!)
     * @returns {Promise<Object>} Processing result
     */
    async processCardPayment(paymentId, cardData) {
        // IMPORTANT: In production, NEVER handle raw card data in frontend!
        // Use payment gateway's client-side SDK to tokenize card data
        // Send only token to your backend
        
        await this.delay(2000);
        
        const payment = await this.getPayment(paymentId);
        
        // Validate card data (basic)
        if (!cardData.token) {
            throw new Error("Dados do cartão inválidos. Use tokenização do gateway de pagamento.");
        }
        
        // Simulate processing
        return this.processPayment(paymentId, {
            method: CONFIG.PAYMENT_METHODS.CARD,
            token: cardData.token,
            installments: cardData.installments || 1
        });
    }

    // Private helper methods

    /**
     * Validate payment data
     * @param {Object} data - Payment data
     */
    validatePaymentData(data) {
        const required = ["contractId", "clientId", "caregiverId", "amount", "method"];
        const missing = required.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Campos obrigatórios: ${missing.join(", ")}`);
        }
        
        if (!Object.values(CONFIG.PAYMENT_METHODS).includes(data.method)) {
            throw new Error("Método de pagamento inválido");
        }
        
        if (data.amount <= 0) {
            throw new Error("Valor deve ser maior que zero");
        }
    }

    /**
     * Sanitize payment details (remove sensitive data)
     * @param {Object} details - Payment details
     * @returns {Object} Sanitized details
     */
    sanitizePaymentDetails(details) {
        const { cardNumber, cvv, expiry, token, ...safe } = details;
        return {
            ...safe,
            // Mask card if present
            cardLast4: cardNumber ? cardNumber.slice(-4) : null,
            cardBrand: cardNumber ? this.detectCardBrand(cardNumber) : null
        };
    }

    /**
     * Detect card brand from number
     * @param {string} number - Card number
     * @returns {string} Brand
     */
    detectCardBrand(number) {
        const cleaned = number.replace(/\D/g, "");
        if (/^4/.test(cleaned)) return "Visa";
        if (/^5[1-5]/.test(cleaned)) return "Mastercard";
        if (/^3[47]/.test(cleaned)) return "Amex";
        if (/^6011|^65/.test(cleaned)) return "Discover";
        if (/^50/.test(cleaned)) return "Aura";
        if (/^6062/.test(cleaned)) return "Hipercard";
        if (/^4011|^4312|^4389|^4514|^4576|^5041|^5066|^5067|^5090|^6277|^6362|^6363/.test(cleaned.substring(0,4))) return "Elo";
        return "Desconhecido";
    }

    /**
     * Simulate gateway response (mock)
     * @param {Object} paymentDetails - Payment details
     * @returns {boolean} Success
     */
    simulateGatewayResponse(paymentDetails) {
        // In production, this is handled by actual gateway
        // For mock, simulate 90% success rate
        return Math.random() > 0.1;
    }

    /**
     * Generate mock transaction ID
     * @returns {string} Transaction ID
     */
    generateTransactionId() {
        return "ELR" + Date.now().toString(36).toUpperCase() + Math.random().toString(36).substring(2, 8).toUpperCase();
    }

    /**
     * Generate mock PIX QR code data
     * @param {Object} payment - Payment object
     * @returns {string} QR code data
     */
    generateMockPixQrCode(payment) {
        // This is a mock BR Code (PIX QR Code format)
        return `00020126580014br.gov.bcb.pix0136${this.generateId()}520400005303986540${payment.amount.toFixed(2)}5802BR5925ELORA PLATAFORMA6009SAO PAULO62070503***6304`;
    }

    /**
     * Generate PIX copia e cola
     * @param {Object} payment - Payment object
     * @returns {string} Copia e cola string
     */
    generatePixCopiaECola(payment) {
        return this.generateMockPixQrCode(payment);
    }

    /**
     * Generate mock barcode for boleto
     * @param {Object} payment - Payment object
     * @returns {string} Barcode
     */
    generateMockBarcode(payment) {
        // Mock boleto barcode (44 digits)
        return "2379" + Date.now().toString().slice(-10) + payment.amount.toFixed(2).replace(".", "").padStart(10, "0") + "00000000000";
    }

    /**
     * Generate mock linha digitavel for boleto
     * @param {Object} payment - Payment object
     * @returns {string} Linha digitável
     */
    generateMockLinhaDigitavel(payment) {
        const barcode = this.generateMockBarcode(payment);
        // Format as linha digitável (5 blocks)
        return `${barcode.slice(0,5)}.${barcode.slice(5,10)} ${barcode.slice(10,15)}.${barcode.slice(15,21)} ${barcode.slice(21,26)}.${barcode.slice(26,32)} ${barcode.slice(32)} ${barcode.slice(33)}`;
    }

    /**
     * Enrich payment with related data
     * @param {Object} payment - Payment object
     * @returns {Promise<Object>} Enriched payment
     */
    async enrichPayment(payment) {
        const contracts = this.getMockContracts();
        const contract = contracts.find(c => c.id === payment.contractId);
        
        let client = null, caregiver = null;
        if (contract) {
            const clients = this.getMockClients();
            const caregivers = this.getMockCaregivers();
            client = clients.find(c => c.id === contract.clientId) || null;
            caregiver = caregivers.find(c => c.id === contract.caregiverId) || null;
        }
        
        return {
            ...payment,
            contract,
            client,
            caregiver,
            statusLabel: this.getStatusLabel(payment.status),
            methodLabel: this.getMethodLabel(payment.method)
        };
    }

    /**
     * Sanitize payment for client
     * @param {Object} payment - Payment object
     * @returns {Object} Sanitized payment
     */
    sanitizePayment(payment) {
        // Remove sensitive internal fields
        return payment;
    }

    /**
     * Get status label in Portuguese
     * @param {string} status - Payment status
     * @returns {string} Status label
     */
    getStatusLabel(status) {
        const labels = {
            [CONFIG.PAYMENT_STATUS.PENDING]: "Aguardando Pagamento",
            [CONFIG.PAYMENT_STATUS.PROCESSING]: "Processando",
            [CONFIG.PAYMENT_STATUS.APPROVED]: "Aprovado",
            [CONFIG.PAYMENT_STATUS.REJECTED]: "Recusado",
            [CONFIG.PAYMENT_STATUS.CANCELLED]: "Cancelado",
            pendente: "Pendente", aprovado: "Aprovado", recusado: "Recusado", estornado: "Estornado"
        };
        return labels[status] || labels[String(status||'').toLowerCase()] || status;
    }

    /**
     * Get method label in Portuguese
     * @param {string} method - Payment method
     * @returns {string} Method label
     */
    getMethodLabel(method) {
        const labels = {
            [CONFIG.PAYMENT_METHODS.PIX]: "PIX",
            [CONFIG.PAYMENT_METHODS.CARD]: "Cartão de Crédito/Débito",
            [CONFIG.PAYMENT_METHODS.BOLETO]: "Boleto Bancário"
        };
        return labels[method] || method;
    }

    /**
     * Create payment notifications
     * @param {Object} payment - Payment object
     */
    async createPaymentNotifications(payment) {
        const contract = await contractService.getContract(payment.contractId);
        
        if (payment.status === CONFIG.PAYMENT_STATUS.APPROVED) {
            // Notify client
            await this.createNotificationForUser(payment.clientId, {
                type: CONFIG.NOTIFICATION_TYPES.PAYMENT_APPROVED,
                title: "Pagamento Aprovado!",
                message: `Seu pagamento de R$ ${payment.amount.toFixed(2)} foi aprovado. O cuidador foi notificado.`,
                relatedId: payment.id
            });
            
            // Notify caregiver
            if (contract.caregiverId) {
                await this.createNotificationForUser(contract.caregiverId, {
                    type: CONFIG.NOTIFICATION_TYPES.PAYMENT_APPROVED,
                    title: "Pagamento Recebido!",
                    message: `O pagamento do contrato #${payment.contractId.substring(0,8)} foi aprovado. Valor: R$ ${payment.amount.toFixed(2)}.`,
                    relatedId: payment.id
                });
            }
        } else if (payment.status === CONFIG.PAYMENT_STATUS.REJECTED) {
            // Notify client
            await this.createNotificationForUser(payment.clientId, {
                type: CONFIG.NOTIFICATION_TYPES.PAYMENT_APPROVED,
                title: "Pagamento Recusado",
                message: `Seu pagamento de R$ ${payment.amount.toFixed(2)} foi recusado. Tente outro método ou entre em contato com seu banco.`,
                relatedId: payment.id
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
     * Get mock payments from localStorage
     * @returns {Array} Array of payments
     */
    getMockPayments() {
        return JSON.parse(localStorage.getItem("elora_mock_payments") || "[]");
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
const paymentService = new PaymentService();

// Export for use in other modules
window.PaymentService = PaymentService;
window.paymentService = paymentService;