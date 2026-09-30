/**
 * ELORA - Escala Service (P2)
 * Junta disponibilidade do cuidador (ProfissionalController: PUT/GET /caregivers/me/disponibilidade,
 * periodo matutino/vespertino/noturno) com contratos ativos para montar a agenda semanal
 * e detectar conflitos. Reaproveita caregiverService + contractService (mock-first).
 */
class EscalaService {
    dias() { return [0, 1, 2, 3, 4, 5, 6]; }
    periodos() { return ['matutino', 'vespertino', 'noturno']; }

    periodosDe(schedule) {
        if (!schedule || !schedule.start || !schedule.end) return [];
        try { return CONFIG.scheduleToPeriodos(schedule.start, schedule.end); } catch (e) { return []; }
    }

    async getDisponibilidade() {
        if (CONFIG.API.MOCK_MODE) {
            const profile = await caregiverService.getProfile();
            return profile.availability || { days: [], schedule: { start: '07:00', end: '19:00' }, maxDistanceKm: 15 };
        }
        return await apiService.get('/caregivers/me/disponibilidade');
    }

    async salvarDisponibilidade({ days, schedule, maxDistanceKm }) {
        if (CONFIG.API.MOCK_MODE) {
            return await caregiverService.updateAvailability({ days, schedule, maxDistanceKm });
        }
        const body = (days || []).map(d => ({ dia: d, periodo: 'matutino' }));
        return await apiService.put('/caregivers/me/disponibilidade', body);
    }

    /**
     * Agenda da semana: para cada dia (0-6) e período, indica se há contrato ativo
     * sobreposto e se a disponibilidade cobre.
     */
    async getAgenda() {
        const [disp, contracts] = await Promise.all([
            this.getDisponibilidade().catch(() => ({ days: [], schedule: null })),
            caregiverService.getContracts().catch(() => [])
        ]);
        const ativos = (contracts || []).filter(c => ['SIGNED', 'ativo'].includes(c.status) || (CONFIG.fromV2ContractStatus && CONFIG.fromV2ContractStatus(c.status) === 'SIGNED'));
        const dispPeriodos = this.periodosDe(disp.schedule);
        const cells = {};
        for (const d of this.dias()) {
            cells[d] = {};
            for (const p of this.periodos()) {
                const coberto = (disp.days || []).includes(d) && (dispPeriodos.length === 0 || dispPeriodos.includes(p));
                const ocup = ativos.filter(c => (c.days || []).includes(d) && this.periodosDe(c.schedule).includes(p));
                cells[d][p] = { disponivel: coberto, contratos: ocup.map(c => ({ id: c.id, cliente: (c.client && c.client.name) || c.clientId || '' })) };
            }
        }
        return { disponibilidade: disp, contratosAtivos: ativos, grade: cells };
    }
}
const escalaService = new EscalaService();
window.escalaService = escalaService;
