/**
 * ELORA - Juridico Service (P2)
 * Backend: GET /juridico/painel, POST /juridico/analise (denúncia), POST /juridico/rescisoes.
 * Tabelas v2: denuncia, evidencia_denuncia, disputa; rescisão = contrato.status em rescindido/cancelado/em_disputa.
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
        return await apiService.get(`${CONFIG.ENDPOINTS.juridico}/painel`);
    }

    async listarDenuncias() {
        this.exigirEquipeJuridica();
        if (CONFIG.API.MOCK_MODE) return this.load('elora_mock_denuncias');
        return await apiService.get(`${CONFIG.ENDPOINTS.juridico}/analise`);
    }

    async denunciar({ contratoId, denunciadoId, motivo, descricao }) {
        if (CONFIG.API.MOCK_MODE) {
            await new Promise(r => setTimeout(r, 500));
            if (!motivo) throw new Error('Motivo é obrigatório');
            if (!descricao || descricao.trim().length < 10) throw new Error('Descreva o ocorrido (mínimo 10 caracteres)');
            const user = (window.authService && authService.getCurrentUser && authService.getCurrentUser()) || {};
            const d = {
                id: 'den' + Date.now(), status: 'aberta',
                contratoId: contratoId || null, denunciadoId: denunciadoId || null,
                denuncianteId: user.id || null, motivo, descricao: descricao.trim(),
                createdAt: new Date().toISOString()
            };
            const list = this.load('elora_mock_denuncias'); list.unshift(d); this.save('elora_mock_denuncias', list);
            return d;
        }
        return await apiService.post(`${CONFIG.ENDPOINTS.juridico}/analise`, { contratoId, denunciadoId, motivo, descricao });
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
        return [];
    }
}
const juridicoService = new JuridicoService();
window.juridicoService = juridicoService;
