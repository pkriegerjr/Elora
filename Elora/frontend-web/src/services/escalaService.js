/**
 * ELORA - Escala Service (P2)
 * Junta disponibilidade do cuidador (ProfissionalController: PUT/GET /caregivers/me/disponibilidade,
 * periodo matutino/vespertino/noturno) com contratos ativos para montar a agenda semanal
 * e detectar conflitos. Em modo real também lê/gera escalas (EscalaController: /escalas).
 *
 * Observação: a disponibilidade no backend é por DATA concreta ({data, periodo}),
 * não por dia da semana. O service converte data <-> dia-da-semana para a UI.
 */
class EscalaService {
    dias() { return [0, 1, 2, 3, 4, 5, 6]; }
    periodos() { return ['matutino', 'vespertino', 'noturno']; }

    // ---------- helpers de data ----------
    iso(dt) {
        const y = dt.getFullYear();
        const m = String(dt.getMonth() + 1).padStart(2, '0');
        const d = String(dt.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }
    dayFromIso(data) {
        if (!data) return null;
        const dt = new Date(`${data}T00:00:00`);
        return isNaN(dt.getTime()) ? null : dt.getDay();
    }
    /** Semana corrente (segunda a domingo) em ISO yyyy-MM-dd. */
    semanaAtual(base = new Date()) {
        const dow = base.getDay(); // 0=Dom
        const diffToMonday = (dow + 6) % 7;
        const monday = new Date(base.getFullYear(), base.getMonth(), base.getDate() - diffToMonday);
        const sunday = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 6);
        return { inicio: this.iso(monday), fim: this.iso(sunday) };
    }
    /** Próximas N semanas de datas (a partir de amanhã) nos dias da semana escolhidos. */
    proximasDatas(days, semanas = 4) {
        const alvo = new Set(days || []);
        const out = [];
        const hoje = new Date();
        hoje.setHours(0, 0, 0, 0);
        // Começa em amanhã para evitar fuso: o backend rejeita data < hoje (LocalDate do servidor).
        for (let i = 1; i <= 7 * semanas; i++) {
            const dt = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + i);
            if (alvo.has(dt.getDay())) out.push(this.iso(dt));
        }
        return out;
    }
    /** Aproxima um horário a partir dos períodos salvos (para preencher o formulário). */
    scheduleFromPeriodos(periodos) {
        const janelas = { matutino: ['06:00', '12:00'], vespertino: ['12:00', '18:00'], noturno: ['18:00', '23:59'] };
        const escolhidos = (periodos || []).filter(p => janelas[p]);
        if (!escolhidos.length) return { start: '07:00', end: '19:00' };
        const inicio = escolhidos.map(p => janelas[p][0]).sort()[0];
        const fim = escolhidos.map(p => janelas[p][1]).sort().slice(-1)[0];
        return { start: inicio, end: fim };
    }
    periodosDe(schedule) {
        if (!schedule || !schedule.start || !schedule.end) return [];
        try { return CONFIG.scheduleToPeriodos(schedule.start, schedule.end); } catch (e) { return []; }
    }
    statusLabel(status) {
        const map = { prevista: 'Previsto', executada: 'Executado', faltou: 'Falta', cancelada: 'Cancelado' };
        return map[String(status || '').toLowerCase()] || status;
    }

    // ---------- disponibilidade ----------
    async getDisponibilidade() {
        if (CONFIG.API.MOCK_MODE) {
            const profile = await caregiverService.getProfile();
            return profile.availability || { days: [], schedule: { start: '07:00', end: '19:00' }, maxDistanceKm: 15 };
        }

        // GET /caregivers/me/disponibilidade -> List<{id, data, periodo, status}>
        const lista = await apiService.get(`${CONFIG.ENDPOINTS.caregivers}/me/disponibilidade`);
        const periodos = [...new Set((lista || []).map(d => String(d.periodo || '').toLowerCase()).filter(Boolean))];
        const days = [...new Set((lista || []).map(d => this.dayFromIso(d.data)).filter(d => d !== null))].sort();
        return {
            days,
            periodos,
            schedule: this.scheduleFromPeriodos(periodos),
            maxDistanceKm: 15,
            datas: (lista || []).map(d => ({ id: d.id, data: d.data, periodo: d.periodo, status: d.status }))
        };
    }

    async salvarDisponibilidade({ days, schedule, maxDistanceKm }) {
        if (CONFIG.API.MOCK_MODE) {
            return await caregiverService.updateAvailability({ days, schedule, maxDistanceKm });
        }

        // Backend grava por data concreta: expande os dias escolhidos nas
        // próximas 4 semanas × períodos derivados do horário.
        const periodos = this.periodosDe(schedule);
        const listaPeriodos = periodos.length ? periodos : ['matutino'];
        const body = this.proximasDatas(days, 4)
            .flatMap(data => listaPeriodos.map(periodo => ({ data, periodo })));
        return await apiService.put(`${CONFIG.ENDPOINTS.caregivers}/me/disponibilidade`, body);
    }

    // ---------- escalas (EscalaController) ----------
    async getEscalas(contratoId, inicio, fim) {
        const params = { contratoId };
        if (inicio) params.inicio = inicio;
        if (fim) params.fim = fim;
        const lista = await apiService.get(CONFIG.ENDPOINTS.escalas, params);
        return (lista || []).map(e => ({ ...e, day: this.dayFromIso(e.data) }));
    }

    async gerarEscala({ contratoId, dataInicio, dataFim, periodos }) {
        return await apiService.post(`${CONFIG.ENDPOINTS.escalas}/gerar`, {
            contratoId: Number(contratoId),
            dataInicio,
            dataFim,
            periodos: periodos && periodos.length ? periodos : ['matutino']
        });
    }

    async atualizarStatusEscala(id, status) {
        return await apiService.patch(`${CONFIG.ENDPOINTS.escalas}/${id}`, { status });
    }

    /**
     * Agenda da semana: para cada dia (0-6) e período, indica se há contrato ativo
     * sobreposto, escalas reais e se a disponibilidade cobre.
     */
    async getAgenda() {
        const [disp, contracts] = await Promise.all([
            this.getDisponibilidade().catch(() => ({ days: [], schedule: null })),
            caregiverService.getContracts().catch(() => [])
        ]);
        const ativos = (contracts || []).filter(c =>
            ['SIGNED', 'ativo'].includes(c.status)
            || (CONFIG.fromV2ContractStatus && CONFIG.fromV2ContractStatus(c.status) === 'SIGNED'));
        const dispPeriodos = this.periodosDe(disp.schedule);

        const cells = {};
        for (const d of this.dias()) {
            cells[d] = {};
            for (const p of this.periodos()) {
                const coberto = (disp.days || []).includes(d) && (dispPeriodos.length === 0 || dispPeriodos.includes(p));
                cells[d][p] = { disponivel: coberto, contratos: [], escalas: [] };
            }
        }

        // Contratos (quando o contrato traz days/schedule, ex.: mock)
        for (const c of ativos) {
            for (const d of (c.days || [])) {
                for (const p of this.periodosDe(c.schedule)) {
                    if (cells[d] && cells[d][p]) {
                        cells[d][p].contratos.push({
                            id: c.id,
                            cliente: (c.client && c.client.name) || c.clientId || ''
                        });
                    }
                }
            }
        }

        // Escalas reais da semana corrente, por contrato ativo
        let escalas = [];
        if (!CONFIG.API.MOCK_MODE && ativos.length) {
            const { inicio, fim } = this.semanaAtual();
            const listas = await Promise.all(
                ativos.map(c => this.getEscalas(c.id, inicio, fim).catch(() => [])));
            escalas = listas.flat();
            for (const e of escalas) {
                const dia = e.day;
                const periodo = String(e.periodo || '').toLowerCase();
                if (dia !== null && cells[dia] && cells[dia][periodo]) {
                    cells[dia][periodo].escalas.push({
                        id: e.id, contratoId: e.contratoId, status: e.status, data: e.data
                    });
                }
            }
        }

        return { disponibilidade: disp, contratosAtivos: ativos, escalas, grade: cells };
    }
}
const escalaService = new EscalaService();
window.escalaService = escalaService;
