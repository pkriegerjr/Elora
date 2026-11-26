/**
 * ELORA - Relatorio Service — Fase 3
 * 6 rotas só-leitura: GET /api/relatorios/resumo|financeiro|usuarios|contratos|avaliacoes|denuncias
 * Todas aceitam ?inicio&fim (inclusive resumo, avaliacoes e denuncias).
 * Financeiro só admin/financeiro, resto só equipe (403) — RelatorioService.java
 * ApiResponse envelope desembalado em apiService.js.
 *
 * Atenção: os nomes dos campos abaixo espelham os DTOs do backend — não renomear
 * sem ajustar Relatorio*.java (ex.: totalBruto, mediaGeral, novosNoPeriodo).
 */
class RelatorioService {
    params(inicio, fim) {
        const p = {};
        if (inicio) p.inicio = inicio;
        if (fim) p.fim = fim;
        return p;
    }

    async resumo(inicio, fim) {
        if (CONFIG.API.MOCK_MODE) return this.mockResumo(inicio, fim);
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/resumo`, this.params(inicio, fim));
    }
    async financeiro(inicio, fim) {
        if (CONFIG.API.MOCK_MODE) return this.mockFinanceiro(inicio, fim);
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/financeiro`, this.params(inicio, fim));
    }
    async usuarios(inicio, fim) {
        if (CONFIG.API.MOCK_MODE) return this.mockUsuarios(inicio, fim);
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/usuarios`, this.params(inicio, fim));
    }
    async contratos(inicio, fim) {
        if (CONFIG.API.MOCK_MODE) return this.mockContratos(inicio, fim);
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/contratos`, this.params(inicio, fim));
    }
    async avaliacoes(inicio, fim) {
        if (CONFIG.API.MOCK_MODE) return this.mockAvaliacoes(inicio, fim);
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/avaliacoes`, this.params(inicio, fim));
    }
    async denuncias(inicio, fim) {
        if (CONFIG.API.MOCK_MODE) return this.mockDenuncias(inicio, fim);
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/denuncias`, this.params(inicio, fim));
    }
    handle403(err) {
        if (err.status === 403) {
            notificationService.showWarning('Acesso negado — relatórios restritos à equipe Elora (admin/jurídico/financeiro). Faça login com perfil autorizado.', '403');
        }
        throw err;
    }

    // ------------------------------------------------------------------
    // MOCK_MODE (mesmos nomes de campos do backend)
    // ------------------------------------------------------------------

    periodo(inicio, fim) {
        return { periodoInicio: inicio || null, periodoFim: fim || null };
    }
    mockResumo(inicio, fim) {
        return {
            totalUsuarios: 12, usuariosAtivos: 10, totalProfissionais: 5, profissionaisPendentes: 2,
            contratosAtivos: 1, contratosEmDisputa: 0, pagamentosPendentes: 1,
            repassesPendentes: 0, valorRepassesPendentes: 0, denunciasAbertas: 0, mediaAvaliacoes: 4.8,
            ...this.periodo(inicio, fim),
            usuariosNovosNoPeriodo: 2, contratosNoPeriodo: 1, pagamentosNoPeriodo: 2,
            denunciasNoPeriodo: 0, avaliacoesNoPeriodo: 2
        };
    }
    mockFinanceiro(inicio, fim) {
        return {
            ...this.periodo(inicio, fim),
            totalPagamentos: 2, totalBruto: 14850, totalTaxa: 1350, totalLiquido: 13500,
            ticketMedio: 6750, porMetodo: { pix: 1, cartao: 0, boleto: 1 },
            porStatus: { pendente: 1, aprovado: 1, recusado: 0, estornado: 0 },
            repassesPendentes: 0, repassesProcessados: 1, valorRepassesPendentes: 0
        };
    }
    mockUsuarios() {
        return {
            total: 12, ativos: 10, novosNoPeriodo: 2,
            porPerfil: { cliente: 2, profissional: 5, admin: 1 },
            porStatus: { ativo: 10, inativo: 1, suspenso: 1 },
            cuidadoresPorVerificacao: { pendente: 1, em_analise: 0, aprovado: 3, rejeitado: 0, correcao: 1 }
        };
    }
    mockContratos() {
        return {
            total: 1, novosNoPeriodo: 1, valorMedioHora: 45,
            porStatus: { rascunho: 0, proposta: 0, negociacao: 0, aguard_assinatura: 0, ativo: 1, concluido: 0, rescindido: 0, cancelado: 0, em_disputa: 0 }
        };
    }
    mockAvaliacoes(inicio, fim) {
        return {
            total: 2, mediaGeral: 4.8, distribuicao: { '5': 2, '4': 0, '3': 0, '2': 0, '1': 0 },
            ...this.periodo(inicio, fim)
        };
    }
    mockDenuncias(inicio, fim) {
        return {
            total: 0, abertas: 0, porStatus: { aberta: 0, em_analise: 0, resolvida: 0, arquivada: 0 },
            ...this.periodo(inicio, fim)
        };
    }
}
const relatorioService = new RelatorioService();
window.relatorioService = relatorioService;
