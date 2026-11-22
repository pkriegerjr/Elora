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

    // ------------------------------------------------------------------
    // API real (MOCK_MODE=false): DTO Spring -> formato usado pelas telas
    // ------------------------------------------------------------------

    mapStatusFromApi(status) {
        const map = {
            pendente: CONFIG.CAREGIVER_STATUS.PENDING,
            em_analise: CONFIG.CAREGIVER_STATUS.UNDER_REVIEW,
            aprovado: CONFIG.CAREGIVER_STATUS.APPROVED,
            rejeitado: CONFIG.CAREGIVER_STATUS.REJECTED,
            correcao: CONFIG.CAREGIVER_STATUS.NEEDS_CORRECTION
        };
        return map[String(status || '').toLowerCase()]
            || String(status || '').toUpperCase()
            || CONFIG.CAREGIVER_STATUS.PENDING;
    }

    mapStatusToApi(status) {
        const map = {
            [CONFIG.CAREGIVER_STATUS.PENDING]: 'pendente',
            [CONFIG.CAREGIVER_STATUS.UNDER_REVIEW]: 'em_analise',
            [CONFIG.CAREGIVER_STATUS.APPROVED]: 'aprovado',
            [CONFIG.CAREGIVER_STATUS.REJECTED]: 'reprovado',
            [CONFIG.CAREGIVER_STATUS.NEEDS_CORRECTION]: 'correcao'
        };
        return map[String(status || '').toUpperCase()] || String(status || '').toLowerCase();
    }

    mapDocumentsFromApi(documentos) {
        const out = {};
        (documentos || []).forEach((d, i) => {
            const key = d.tipo || ('doc' + (i + 1));
            out[key] = {
                id: d.id,
                name: d.arquivoUrl ? String(d.arquivoUrl).split('/').pop() : key,
                type: 'application/pdf',
                size: 0,
                uploadedAt: d.criadoEm || null,
                url: d.urlDownload || d.arquivoUrl || null,
                status: d.status === 'aprovado' ? 'approved'
                    : d.status === 'rejeitado' ? 'rejected' : 'pending_review'
            };
        });
        return out;
    }

    mapCaregiverFromApi(p) {
        if (!p) return p;
        const docs = p.documentos || p.documents;
        const status = p.statusVerificacao || p.situacaoCadastro || p.status;
        return {
            ...p,
            name: p.nome ?? p.name,
            phone: p.telefone ?? p.phone,
            photo: p.fotoUrl ?? p.photo,
            gender: p.genero ?? p.gender,
            birthDate: p.dataNascimento ?? p.birthDate,
            bio: p.descricaoPerfil ?? p.bio ?? '',
            hourlyRate: Number(p.precoHora ?? p.hourlyRate ?? 0),
            rating: Number(p.notaMedia ?? p.rating ?? 0),
            reviewCount: Number(p.reviewCount ?? 0),
            specialties: p.especialidades ?? p.specialties ?? [],
            verified: p.documentoVerificado === true || p.verified === true,
            status: this.mapStatusFromApi(status),
            documents: Array.isArray(docs) ? this.mapDocumentsFromApi(docs) : (docs || {}),
            experience: p.experience ?? 0,
            createdAt: p.criadoEm ?? p.createdAt ?? null,
            updatedAt: p.atualizadoEm ?? p.updatedAt ?? null
        };
    }

    mapContractFromApi(c) {
        const clienteId = c.clienteId ?? c.clientId;
        const profissionalId = c.profissionalId ?? c.caregiverId;
        return {
            ...c,
            clientId: clienteId,
            caregiverId: profissionalId,
            client: c.clienteNome ? { id: clienteId, name: c.clienteNome } : (c.client || null),
            caregiver: c.profissionalNome ? { id: profissionalId, name: c.profissionalNome } : (c.caregiver || null),
            startDate: c.dataInicio ?? c.startDate,
            endDate: c.dataFim ?? c.endDate,
            hourlyRate: Number(c.valorHora ?? c.hourlyRate ?? 0),
            totalValue: Number(c.valorTotal ?? c.totalValue ?? 0),
            createdAt: c.criadoEm ?? c.createdAt
        };
    }

    mapReviewFromApi(r) {
        return {
            ...r,
            contractId: r.contratoId ?? r.contractId,
            caregiverId: r.cuidadorId ?? r.caregiverId,
            rating: r.nota ?? r.rating,
            comment: r.comentario ?? r.comment ?? '',
            createdAt: r.criadoEm ?? r.createdAt
        };
    }

    mapNotificationFromApi(n) {
        return {
            ...n,
            userId: n.destinatarioId ?? n.userId,
            title: n.titulo ?? n.title,
            message: n.corpo ?? n.message,
            read: n.lida ?? n.read ?? false,
            readAt: n.lidaEm ?? n.readAt,
            createdAt: n.enviadoEm ?? n.createdAt
        };
    }

    /**
     * Create a new caregiver
     * @param {Object} caregiverData - Caregiver registration data
     * @returns {Promise<Object>} Created caregiver
     */
    async createCaregiver(caregiverData) {
        if (!CONFIG.API.MOCK_MODE) {
            const payload = {
                nome: caregiverData.name || caregiverData.nome,
                email: caregiverData.email,
                cpf: CONFIG.normalizeCPF(caregiverData.cpf),
                telefone: caregiverData.phone || caregiverData.telefone,
                senha: caregiverData.password || caregiverData.senha,
                genero: caregiverData.genero || caregiverData.gender || 'prefiro_nao_informar',
                dataNascimento: caregiverData.birthDate || caregiverData.dataNascimento,
                latitude: caregiverData.latitude ?? caregiverData.address?.latitude ?? null,
                longitude: caregiverData.longitude ?? caregiverData.address?.longitude ?? null,
                descricaoPerfil: caregiverData.bio || caregiverData.descricaoPerfil || '',
                precoHora: caregiverData.hourlyRate ?? caregiverData.precoHora ?? null,
                consentimentoLgpd: caregiverData.consentimentoLgpd !== false
            };
            return this.mapCaregiverFromApi(await apiService.post(CONFIG.ENDPOINTS.caregivers, payload));
        }

        await this.delay(800);
        
        this.validateCaregiverData(caregiverData);
        
        caregiverData.cpfDigits = CONFIG.normalizeCPF(caregiverData.cpf);
        const existingCaregivers = this.getMockCaregivers();
        const duplicate = existingCaregivers.find(c => 
            (c.cpfDigits||c.cpf?.replace(/\D/g,'')) === caregiverData.cpfDigits || c.email === caregiverData.email
        );
        
        if (duplicate) {
            throw new Error("Já existe um cuidador cadastrado com este CPF ou e-mail");
        }
        // V2: disponibilidade periodos + profissional_detalhes
        if(caregiverData.availability?.schedule && !caregiverData.availability.periodos){
            caregiverData.availability.periodos = CONFIG.scheduleToPeriodos(caregiverData.availability.schedule.start, caregiverData.availability.schedule.end);
        }
        const newCaregiver = {
            id: this.generateId(),
            ...caregiverData,
            precoHora: caregiverData.hourlyRate || caregiverData.precoHora || 35,
            descricaoPerfil: caregiverData.bio || caregiverData.descricaoPerfil || '',
            notaMedia: 0,
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
        if (!CONFIG.API.MOCK_MODE) {
            // Perfil público: GET /caregivers/{id} é restrito ao dono/staff,
            // então o cartão público vem do BuscaController.
            return this.mapCaregiverFromApi(
                await apiService.get(`${CONFIG.ENDPOINTS.caregiversSearch}/${caregiverId}`));
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            return this.mapCaregiverFromApi(
                await apiService.get(`${CONFIG.ENDPOINTS.caregivers}/me`));
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            // Cadastro (CPF/e-mail/situação) + cartão público (bio/especialidades).
            const [usuario, publico] = await Promise.all([
                apiService.get(`${CONFIG.ENDPOINTS.caregivers}/${caregiverId}`),
                apiService.get(`${CONFIG.ENDPOINTS.caregiversSearch}/${caregiverId}`).catch(() => null)
            ]);
            return this.mapCaregiverFromApi({ ...(publico || {}), ...usuario });
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            const payload = {};
            const nome = updates.name ?? updates.nome;
            const telefone = updates.phone ?? updates.telefone;
            if (nome != null) payload.nome = nome;
            if (telefone != null) payload.telefone = telefone;
            if (updates.bio != null || updates.descricaoPerfil != null) {
                payload.descricaoPerfil = updates.bio ?? updates.descricaoPerfil;
            }
            if (updates.hourlyRate != null || updates.precoHora != null) {
                payload.precoHora = updates.hourlyRate ?? updates.precoHora;
            }
            if (updates.specialties != null || updates.especialidades != null) {
                payload.especialidades = updates.specialties ?? updates.especialidades;
            }
            const res = await apiService.patch(`${CONFIG.ENDPOINTS.caregivers}/me`, payload);
            const mapped = this.mapCaregiverFromApi(res);
            authService.updateCurrentUser(mapped);
            return mapped;
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            // A API guarda disponibilidade por DATA + período. A tela informa dias
            // da semana + faixa de horário; materializamos as próximas 8 semanas.
            const days = availability.days || [];
            const start = availability.schedule?.start;
            const end = availability.schedule?.end;
            let periodos = (start && end) ? CONFIG.scheduleToPeriodos(start, end) : [];
            if (!periodos.length) periodos = ['matutino', 'vespertino', 'noturno'];

            const body = [];
            if (days.length) {
                const hoje = new Date();
                for (let i = 0; i < 56; i++) {
                    const d = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + i);
                    if (!days.includes(d.getDay())) continue;
                    const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                    periodos.forEach(p => body.push({ data: iso, periodo: p }));
                }
            }
            await apiService.put(`${CONFIG.ENDPOINTS.caregivers}/me/disponibilidade`, body);
            return { ...availability, periodos };
        }

        return this.updateProfile({ availability });
    }

    /**
     * Upload caregiver documents
     * @param {Object} documents - Documents object with file data
     * @returns {Promise<Object>} Updated caregiver with documents
     */
    async uploadDocuments(documents) {
        if (!CONFIG.API.MOCK_MODE) {
            const currentUser = authService.getCurrentUser();
            if (!currentUser || !currentUser.id) throw new Error("Usuário não autenticado");

            const entradas = Object.entries(documents || {}).filter(([, file]) => file);
            if (!entradas.length) throw new Error("Selecione ao menos um arquivo");

            let ultimo = null;
            for (const [tipo, file] of entradas) {
                const formData = new FormData();
                formData.append('tipo', tipo);
                formData.append('arquivo', file);
                ultimo = await apiService.upload(
                    `${CONFIG.ENDPOINTS.caregivers}/${currentUser.id}/documentos/arquivo`, formData);
            }
            // Recarrega o perfil para refletir status/documentos atualizados.
            try { return await this.getProfile(); } catch (_) { return ultimo; }
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            const p = await this.getProfile();
            const documents = {};
            Object.entries(p.documents || {}).forEach(([k, v]) => {
                documents[k] = (v && v.status) ? v.status : 'not_uploaded';
            });
            const valores = Object.values(documents);
            return {
                status: p.status,
                verified: p.verified,
                verificationDate: p.verificationDate || null,
                documents,
                canRequestReview: valores.length > 0
                    && valores.every(v => v === 'pending_review' || v === 'approved'),
                missingDocuments: Object.keys(documents).filter(k => documents[k] === 'not_uploaded')
            };
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            const list = await apiService.get(CONFIG.ENDPOINTS.contracts);
            const mapped = (list || []).map(c => this.mapContractFromApi(c));
            if (!status) return mapped;
            const alvo = CONFIG.toV2ContractStatus(status);
            return mapped.filter(c => String(c.status).toLowerCase() === alvo);
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            const repasses = await apiService.get(`${CONFIG.ENDPOINTS.payments}/repasses`).catch(() => []);
            const payments = window.paymentService
                ? await paymentService.getUserPayments().catch(() => [])
                : [];
            const total = (repasses || []).filter(r => r.status === 'processado')
                .reduce((s, r) => s + Number(r.valor || 0), 0);
            const pending = (repasses || []).filter(r => r.status === 'pendente')
                .reduce((s, r) => s + Number(r.valor || 0), 0);
            return { total, pending, available: total, payments, period };
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            const contratos = await this.getContracts();
            const ativos = contratos.filter(c => ['ativo', 'aguard_assinatura'].includes(String(c.status).toLowerCase()));
            const linhas = [];
            for (const c of ativos) {
                try {
                    const escalas = await apiService.get(CONFIG.ENDPOINTS.escalas, { contratoId: c.id });
                    (escalas || []).forEach(e => linhas.push({
                        id: e.id,
                        contractId: c.id,
                        clientName: c.client?.name || '',
                        data: e.data,
                        periodo: e.periodo,
                        status: e.status
                    }));
                } catch (_) { /* contrato ainda sem escala gerada */ }
            }
            return linhas;
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            const user = authService.getCurrentUser();
            if (!user || !user.id) return [];
            const list = await apiService.get(`${CONFIG.ENDPOINTS.avaliacoes}/cuidador/${user.id}`);
            return (list || []).map(r => this.mapReviewFromApi(r))
                .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            const params = unreadOnly ? { unreadOnly: true } : {};
            const list = await apiService.get(CONFIG.ENDPOINTS.notifications, params);
            return (list || []).map(n => this.mapNotificationFromApi(n));
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            const params = {};
            const nome = filters.name || filters.search || filters.nome;
            if (nome) params.nome = nome;
            if (filters.maxPrice != null) params.precoMax = filters.maxPrice;
            if (filters.minRating != null) params.notaMinima = filters.minRating;
            if (filters.periodo) params.periodo = filters.periodo;
            if (filters.orderBy) params.ordenarPor = filters.orderBy;
            if (filters.limit) params.limit = filters.limit;

            let lista = await apiService.get(CONFIG.ENDPOINTS.caregiversSearch, params);
            lista = (lista || []).map(c => this.mapCaregiverFromApi(c));

            if (filters.specialty) {
                const alvo = String(filters.specialty).toLowerCase();
                lista = lista.filter(c => (c.specialties || [])
                    .some(s => String(s).toLowerCase() === alvo));
            }
            if (filters.verifiedOnly) lista = lista.filter(c => c.verified);
            if (filters.distance && filters.userLocation) {
                lista = lista.map(c => ({
                    ...c,
                    distance: this.calculateDistance(filters.userLocation, c.address)
                })).filter(c => c.distance <= filters.distance);
            }
            return lista;
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            if (!authService.isAdmin()) throw new Error("Acesso negado: apenas administradores");

            const page = Number(params.page || 1);
            const limit = Number(params.limit || 10);
            const query = { page: Math.max(0, page - 1), size: limit };
            if (params.status) query.status = this.mapStatusToApi(params.status);
            // Busca textual é feita no cliente; traz uma página maior para filtrar.
            if (params.search) { query.page = 0; query.size = 100; }

            let lista = await apiService.get(CONFIG.ENDPOINTS.caregivers, query);
            lista = (lista || []).map(c => this.mapCaregiverFromApi(c));

            if (params.search) {
                const termo = String(params.search).toLowerCase();
                lista = lista.filter(c =>
                    String(c.name || '').toLowerCase().includes(termo)
                    || String(c.email || '').toLowerCase().includes(termo)
                    || String(c.cpf || '').includes(String(params.search)));
                const total = lista.length;
                const inicio = (page - 1) * limit;
                return {
                    data: lista.slice(inicio, inicio + limit),
                    pagination: { page, limit, total, totalPages: Math.max(1, Math.ceil(total / limit)) }
                };
            }

            const total = lista.length;
            return {
                data: lista,
                pagination: { page, limit, total, totalPages: Math.max(1, Math.ceil(total / limit)) }
            };
        }

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
        if (!CONFIG.API.MOCK_MODE) {
            if (!authService.isAdmin()) throw new Error("Acesso negado: apenas administradores");
            const resultado = {
                [CONFIG.CAREGIVER_STATUS.APPROVED]: 'aprovado',
                [CONFIG.CAREGIVER_STATUS.REJECTED]: 'reprovado',
                [CONFIG.CAREGIVER_STATUS.NEEDS_CORRECTION]: 'correcao'
            }[String(status || '').toUpperCase()];
            if (!resultado) throw new Error('Status sem equivalente na API: ' + status);
            const res = await apiService.patch(
                `${CONFIG.ENDPOINTS.caregivers}/${caregiverId}/validacao`,
                { resultado, observacao: reason || null });
            return this.mapCaregiverFromApi(res);
        }

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