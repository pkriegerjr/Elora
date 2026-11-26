/**
 * ELORA - Juridico Service (P2)
 * Backend: GET /juridico/painel, POST /juridico/denuncias (alias /analise),
 * GET /juridico/denuncias[/minhas|/{id}], PATCH /juridico/denuncias/{id}/status,
 * POST /juridico/rescisoes.
 * Denúncias agora persistem na tabela v2 `denuncia` (não são mais stub em memória).
 * MOCK_MODE: localStorage elora_mock_denuncias + elora_mock_rescisoes.
 */
class JuridicoService {
    load(k) { try { return JSON.parse(localStorage.getItem(k) || '[]'); } catch (e) { return []; } }
    save(k, v) { localStorage.setItem(k, JSON.stringify(v)); }

    /** Painel e filas restritos à equipe jurídica/administração (admin cobre ambos no mock; perfis juridico->ADMIN na API). */
    exigirEquipeJuridica() {
        const staff = !!(window.authService && authService.isAdmin && authService.isAdmin());
        if (!staff) throw new Error('Acesso negado: restrito à equipe jurídica/administração');
    }

    /** Normaliza o DTO real da API (e aceita o formato antigo do mock). */
    normalizar(d) {
        return {
            ...d,
            id: d.id ?? d.idAnalise,
            status: d.status ?? d.statusAnalise ?? 'aberta',
            motivo: d.motivo || d.parecer || '—',
            descricao: d.descricao || '',
            contratoId: d.contratoId ?? null,
            denuncianteId: d.denuncianteId ?? null,
            denunciadoId: d.denunciadoId ?? null,
            parecer: d.parecer || '',
            analisadoPor: d.analisadoPor ?? null,
            createdAt: d.criadoEm ?? d.createdAt ?? null
        };
    }

    async painel() {
        this.exigirEquipeJuridica();
        if (CONFIG.API.MOCK_MODE) {
            const den = this.load('elora_mock_denuncias');
            const res = this.load('elora_mock_rescisoes');
            let pendentes = 0, emAnalise = 0;
            try { pendentes = (await caregiverService.getAllCaregivers({ limit: 100 }).then(r => r.data || r)).filter(c => c.status === 'PENDING').length; } catch (e) {}
            try { emAnalise = (await caregiverService.getAllCaregivers({ limit: 100 }).then(r => r.data || r)).filter(c => c.status === 'UNDER_REVIEW').length; } catch (e) {}
            return {
                validacaoPendentes: pendentes, validacaoEmAnalise: emAnalise,
                denunciasAbertas: den.filter(d => d.status === 'aberta').length,
                denunciasEmAnalise: den.filter(d => d.status === 'em_analise').length,
                rescisoesPendentes: res.filter(r => r.status === 'solicitada').length,
                emDisputa: res.filter(r => r.status === 'em_disputa').length
            };
        }
        const p = await apiService.get(`${CONFIG.ENDPOINTS.juridico}/painel`);
        return {
            validacaoPendentes: p.validacaoPendentes ?? 0,
            validacaoEmAnalise: p.validacaoEmAnalise ?? 0,
            denunciasAbertas: p.denunciasAbertas ?? p.analisesPendentes ?? 0,
            denunciasEmAnalise: p.denunciasEmAnalise ?? 0,
            rescisoesPendentes: p.rescisoesPendentes ?? p.rescisoesTotal ?? 0,
            emDisputa: p.emDisputa ?? 0
        };
    }

    /** Fila completa — somente equipe (401/403 no backend se outro perfil). */
    async listarDenuncias() {
        this.exigirEquipeJuridica();
        if (CONFIG.API.MOCK_MODE) return this.load('elora_mock_denuncias').map(d => this.normalizar(d));
        const list = await apiService.get(`${CONFIG.ENDPOINTS.juridico}/denuncias`);
        return (list || []).map(d => this.normalizar(d));
    }

    /** Denúncias feitas pelo usuário logado (qualquer autenticado). */
    async minhasDenuncias() {
        if (CONFIG.API.MOCK_MODE) {
            const id = window.authService?.getCurrentUser?.()?.id;
            return this.load('elora_mock_denuncias')
                .filter(d => !id || String(d.denuncianteId) === String(id))
                .map(d => this.normalizar(d));
        }
        const list = await apiService.get(`${CONFIG.ENDPOINTS.juridico}/denuncias/minhas`);
        return (list || []).map(d => this.normalizar(d));
    }

    async detalharDenuncia(id) {
        if (CONFIG.API.MOCK_MODE) {
            const d = this.load('elora_mock_denuncias').find(x => String(x.id) === String(id));
            if (!d) throw new Error('Denúncia não encontrada');
            return this.normalizar(d);
        }
        const d = await apiService.get(`${CONFIG.ENDPOINTS.juridico}/denuncias/${encodeURIComponent(id)}`);
        return this.normalizar(d);
    }

    /** Transição de status com parecer — somente equipe. */
    async atualizarStatusDenuncia(id, status, parecer) {
        this.exigirEquipeJuridica();
        if (CONFIG.API.MOCK_MODE) {
            const list = this.load('elora_mock_denuncias');
            const i = list.findIndex(x => String(x.id) === String(id));
            if (i < 0) throw new Error('Denúncia não encontrada');
            list[i] = { ...list[i], status, parecer: parecer || list[i].parecer, atualizadoEm: new Date().toISOString() };
            this.save('elora_mock_denuncias', list);
            return this.normalizar(list[i]);
        }
        const d = await apiService.patch(`${CONFIG.ENDPOINTS.juridico}/denuncias/${encodeURIComponent(id)}/status`,
            { status, parecer: parecer || null });
        return this.normalizar(d);
    }

    async denunciar({ contratoId, denunciadoId, motivo, descricao }) {
        if (CONFIG.API.MOCK_MODE) {
            await new Promise(r => setTimeout(r, 500));
            if (!motivo) throw new Error('Motivo é obrigatório');
            if (!denunciadoId) throw new Error('Informe o ID do denunciado');
            if (!descricao || descricao.trim().length < 10) throw new Error('Descreva o ocorrido (mínimo 10 caracteres)');
            const user = (window.authService && authService.getCurrentUser && authService.getCurrentUser()) || {};
            const d = {
                id: 'den' + Date.now(), status: 'aberta',
                contratoId: contratoId || null, denunciadoId: denunciadoId || null,
                denuncianteId: user.id || null, motivo, descricao: descricao.trim(),
                createdAt: new Date().toISOString()
            };
            const list = this.load('elora_mock_denuncias'); list.unshift(d); this.save('elora_mock_denuncias', list);
            return this.normalizar(d);
        }
        return await apiService.post(`${CONFIG.ENDPOINTS.juridico}/denuncias`,
            { contratoId: contratoId || null, denunciadoId, motivo, descricao });
    }

    async solicitarRescisao({ contratoId, motivo }) {
        this.exigirEquipeJuridica();
        if (CONFIG.API.MOCK_MODE) {
            await new Promise(r => setTimeout(r, 500));
            if (!contratoId) throw new Error('Contrato não informado');
            if (!motivo || motivo.trim().length < 5) throw new Error('Informe o motivo da rescisão');
            try { await contractService.cancelContract(String(contratoId), motivo); } catch (e) { /* segue */ }
            const r = { id: 'res' + Date.now(), contratoId: String(contratoId), motivo: motivo.trim(), status: 'solicitada', createdAt: new Date().toISOString() };
            const list = this.load('elora_mock_rescisoes'); list.unshift(r); this.save('elora_mock_rescisoes', list);
            return r;
        }
        return await apiService.post(`${CONFIG.ENDPOINTS.juridico}/rescisoes`, { contratoId, motivo });
    }

    async listarRescisoes() {
        this.exigirEquipeJuridica();
        if (CONFIG.API.MOCK_MODE) return this.load('elora_mock_rescisoes');
        const list = await apiService.get(`${CONFIG.ENDPOINTS.juridico}/rescisoes`);
        return (list || []).map(r => ({
            ...r,
            id: r.idAnalise ?? r.id,
            status: r.status ?? r.statusAnalise ?? 'solicitada',
            motivo: r.motivo || r.parecer || '—',
            contratoId: r.contratoId ?? null,
            createdAt: r.criadoEm ?? r.createdAt
        }));
    }
}
const juridicoService = new JuridicoService();
window.juridicoService = juridicoService;
