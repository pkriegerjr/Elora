/**
 * ELORA - Avaliacao Service (P2)
 * Backend: POST /avaliacoes {contratoId, nota 1-5, comentario<=2000} (avaliador=JWT),
 * GET /avaliacoes/cuidador/{id}, GET /avaliacoes/cuidador/{id}/media.
 * Trigger do banco: 1 avaliação por contrato (cliente->profissional).
 * MOCK_MODE: localStorage elora_mock_avaliacoes + espelho elora_mock_reviews (compat perfil-cuidador.html).
 */
class AvaliacaoService {
    keys() { return { aval: 'elora_mock_avaliacoes', rev: 'elora_mock_reviews' }; }

    load() {
        try { return JSON.parse(localStorage.getItem(this.keys().aval) || '[]'); }
        catch (e) { return []; }
    }
    save(list) { localStorage.setItem(this.keys().aval, JSON.stringify(list)); }

    mirrorToReviews(a) {
        try {
            const rev = JSON.parse(localStorage.getItem(this.keys().rev) || '[]');
            if (!rev.some(r => r.contractId === a.contratoId || r.id === a.id)) {
                rev.push({ id: a.id, caregiverId: a.cuidadorId, contractId: a.contratoId, rating: a.nota, comment: a.comentario || '', createdAt: a.createdAt, response: '' });
                localStorage.setItem(this.keys().rev, JSON.stringify(rev));
            }
        } catch (e) { /* ignora */ }
    }

    async criar({ contratoId, nota, comentario }) {
        if (CONFIG.API.MOCK_MODE) {
            await new Promise(r => setTimeout(r, 500));
            const cid = Number(contratoId);
            if (!cid) throw new Error('Contrato não informado');
            const n = Number(nota);
            if (!Number.isInteger(n) || n < 1 || n > 5) throw new Error('Nota deve ser de 1 a 5');
            if ((comentario || '').length > 2000) throw new Error('Comentário deve ter no máximo 2000 caracteres');
            const user = (window.authService && authService.getCurrentUser && authService.getCurrentUser()) || null;
            const list = this.load();
            if (list.some(a => Number(a.contratoId) === cid)) throw new Error('Este contrato já foi avaliado');
            // resolve cuidador do contrato (mock)
            let cuidadorId = null;
            try {
                const c = await contractService.getContract(String(cid));
                cuidadorId = c.caregiverId || c.cuidadorId || (c.caregiver && c.caregiver.id) || null;
            } catch (e) { /* segue sem */ }
            const a = {
                id: 'av' + Date.now(),
                contratoId: cid,
                cuidadorId: cuidadorId ? String(cuidadorId) : null,
                avaliadorId: user ? user.id : null,
                nota: n, rating: n,
                comentario: (comentario || '').trim(), comment: (comentario || '').trim(),
                createdAt: new Date().toISOString()
            };
            list.push(a); this.save(list); this.mirrorToReviews(a);
            return a;
        }
        return await apiService.post(CONFIG.ENDPOINTS.avaliacoes, { contratoId: Number(contratoId), nota: Number(nota), comentario: comentario || null });
    }

    async listarPorCuidador(cuidadorId) {
        if (CONFIG.API.MOCK_MODE) {
            const all = this.load().filter(a => String(a.cuidadorId) === String(cuidadorId));
            let rev = [];
            try { rev = JSON.parse(localStorage.getItem(this.keys().rev) || '[]').filter(r => String(r.caregiverId) === String(cuidadorId)); } catch (e) {}
            const map = new Map();
            all.forEach(a => map.set(String(a.contratoId || a.id), { id: a.id, rating: a.nota, comment: a.comentario, createdAt: a.createdAt, response: '' }));
            rev.forEach(r => { if (!map.has(String(r.contractId || r.id))) map.set(String(r.contractId || r.id), r); });
            return [...map.values()];
        }
        return await apiService.get(`${CONFIG.ENDPOINTS.avaliacoes}/cuidador/${cuidadorId}`);
    }

    async mediaPorCuidador(cuidadorId) {
        if (CONFIG.API.MOCK_MODE) {
            const list = await this.listarPorCuidador(cuidadorId);
            if (!list.length) return { media: 0, total: 0 };
            const media = list.reduce((s, r) => s + (Number(r.rating) || 0), 0) / list.length;
            return { media: Number(media.toFixed(1)), total: list.length };
        }
        return await apiService.get(`${CONFIG.ENDPOINTS.avaliacoes}/cuidador/${cuidadorId}/media`);
    }

    async jaAvaliada(contratoId) {
        if (CONFIG.API.MOCK_MODE) return this.load().some(a => Number(a.contratoId) === Number(contratoId));
        try { return false; } catch (e) { return false; }
    }
}
const avaliacaoService = new AvaliacaoService();
window.avaliacaoService = avaliacaoService;
