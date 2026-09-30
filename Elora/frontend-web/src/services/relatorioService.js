/**
 * ELORA - Relatorio Service — Fase 3
 * 6 rotas só-leitura: GET /api/relatorios/resumo|financeiro|usuarios|contratos|avaliacoes|denuncias
 * financeiro?inicio&fim, usuarios?inicio&fim, contratos?inicio&fim — RelatorioController.java:37
 * Financeiro só admin/financeiro, resto só equipe (403) — RelatorioService.java:40
 * ApiResponse envelope desembalado em apiService.js:80
 */
class RelatorioService {
    async resumo(){
        if(CONFIG.API.MOCK_MODE){ return this.mockResumo(); }
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/resumo`);
    }
    async financeiro(inicio, fim){
        if(CONFIG.API.MOCK_MODE){ return this.mockFinanceiro(inicio,fim); }
        const p={}; if(inicio) p.inicio=inicio; if(fim) p.fim=fim;
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/financeiro`, p);
    }
    async usuarios(inicio, fim){
        if(CONFIG.API.MOCK_MODE){ return this.mockUsuarios(); }
        const p={}; if(inicio) p.inicio=inicio; if(fim) p.fim=fim;
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/usuarios`, p);
    }
    async contratos(inicio, fim){
        if(CONFIG.API.MOCK_MODE){ return this.mockContratos(); }
        const p={}; if(inicio) p.inicio=inicio; if(fim) p.fim=fim;
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/contratos`, p);
    }
    async avaliacoes(){
        if(CONFIG.API.MOCK_MODE){ return this.mockAvaliacoes(); }
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/avaliacoes`);
    }
    async denuncias(){
        if(CONFIG.API.MOCK_MODE){ return this.mockDenuncias(); }
        return await apiService.get(`${CONFIG.ENDPOINTS.relatorios}/denuncias`);
    }
    handle403(err){
        if(err.status===403){
            notificationService.showWarning('Acesso negado — relatórios restritos à equipe Elora (admin/jurídico/financeiro). Faça login com perfil autorizado.', '403');
        }
        throw err;
    }
    // Mocks para MOCK_MODE=true
    mockResumo(){ return { totalUsuarios: 12, usuariosAtivos: 10, profissionais: 5, profissionaisPendentes: 2, contratosAtivos: 1, contratosEmDisputa:0, pagamentosPendentes:1, repassesPendentes:0, repassesPendentesValor:0, denunciasAbertas:0, mediaAvaliacoes: 4.8 }; }
    mockFinanceiro(){ return { totalPagamentos:2, bruto:14850, taxa:1350, liquido:13500, mediaLiquido:6750, porMetodo:{pix:1,cartao:0,boleto:1}, porStatus:{pendente:1,aprovado:1,recusado:0,estornado:0}, repassesPendentes:0, repassesProcessados:1, repassesPendentesValor:0 }; }
    mockUsuarios(){ return { total:12, ativos:10, novos:2, porPerfil:{cliente:2,profissional:5,admin:1}, porStatus:{ativo:10,inativo:1,suspenso:1}, porVerificacao:{pendente:1,em_analise:0,aprovado:3,rejeitado:0,correcao:1} }; }
    mockContratos(){ return { total:1, novos:1, mediaValorHora:45, porStatus:{rascunho:0,proposta:0,negociacao:0,aguard_assinatura:0,ativo:1,concluido:0,rescindido:0,cancelado:0,em_disputa:0} }; }
    mockAvaliacoes(){ return { total:2, media:4.8, distribuicao:{"5":2,"4":0,"3":0,"2":0,"1":0} }; }
    mockDenuncias(){ return { total:0, abertas:0, porStatus:{aberta:0,em_analise:0,resolvida:0,arquivada:0} }; }
}
const relatorioService = new RelatorioService();
window.relatorioService = relatorioService;
