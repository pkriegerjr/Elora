/**
 * ELORA - Conhecimento Service (P2)
 * Backend: GET /conhecimento/artigos, GET /conhecimento/artigos/{id}, POST /conhecimento/artigos.
 * MOCK_MODE: catálogo estático + artigos criados em localStorage elora_mock_artigos.
 */
class ConhecimentoService {
    base() {
        return [
            { id: 'guia-alzheimer', categoria: 'Guias', titulo: 'Guia: cuidados com Alzheimer em casa', resumo: 'Rotina, segurança e comunicação com a pessoa idosa.', corpo: 'Crie uma rotina previsível, reduza estímulos à noite, use etiquetas e iluminação adequada. Registre medicação e horários. Em caso de agitação, mantenha a calma e redirecione a atenção.', autor: 'Equipe Elora', createdAt: '2026-01-10T10:00:00' },
            { id: 'guia-mobilidade', categoria: 'Guias', titulo: 'Mobilidade reduzida: transferências seguras', resumo: 'Técnicas para levantar, sentar e deambular com apoio.', corpo: 'Use cinto de transferência, trave a cadeira de rodas, mantenha os pés da pessoa apoiados. Nunca puxe pelos braços. Ajuste a altura da cama quando possível.', autor: 'Equipe Elora', createdAt: '2026-02-02T10:00:00' },
            { id: 'guia-medicacao', categoria: 'Guias', titulo: 'Medicação: organização sem erros', resumo: 'Caixas semanais, alarmes e registro de doses.', corpo: 'Separe por dia/horário, confira nome e dose antes de administrar, anote imediatamente após dar. Guarde receita e horários no perfil do contrato.', autor: 'Equipe Elora', createdAt: '2026-02-20T10:00:00' },
            { id: 'lgpd-dados', categoria: 'LGPD', titulo: 'Seus dados na Elora (LGPD)', resumo: 'Consentimento, direitos e exclusão de dados.', corpo: 'Coletamos apenas o necessário para o contrato. Você pode solicitar acesso, correção e exclusão pelo perfil. Documentos ficam restritos ao jurídico e à moderação.', autor: 'Jurídico Elora', createdAt: '2026-03-01T10:00:00' },
            { id: 'pagamentos-seguros', categoria: 'Pagamentos', titulo: 'Pagamentos protegidos: como funciona', resumo: 'PIX, cartão e boleto com comprovante e repasse ao cuidador.', corpo: 'O valor fica registrado por contrato com taxa de 10%. O repasse ao cuidador ocorre após confirmação. Guarde o comprovante em PDF no painel.', autor: 'Financeiro Elora', createdAt: '2026-03-10T10:00:00' }
        ];
    }
    custom() {
        try { return JSON.parse(localStorage.getItem('elora_mock_artigos') || '[]'); } catch (e) { return []; }
    }
    async listar() {
        if (CONFIG.API.MOCK_MODE) return [...this.custom(), ...this.base()];
        const r = await apiService.get(`${CONFIG.ENDPOINTS.conhecimento}/artigos`);
        return r.content || r.data || r;
    }
    async obter(id) {
        if (CONFIG.API.MOCK_MODE) {
            const all = await this.listar();
            const a = all.find(x => String(x.id) === String(id));
            if (!a) throw new Error('Artigo não encontrado');
            return a;
        }
        return await apiService.get(`${CONFIG.ENDPOINTS.conhecimento}/artigos/${id}`);
    }
    async criar({ titulo, resumo, corpo, categoria }) {
        if (CONFIG.API.MOCK_MODE) {
            if (!titulo || !corpo) throw new Error('Título e conteúdo são obrigatórios');
            const user = (window.authService && authService.getCurrentUser && authService.getCurrentUser()) || {};
            const a = { id: 'art' + Date.now(), titulo: titulo.trim(), resumo: (resumo || '').trim(), corpo: corpo.trim(), categoria: categoria || 'Guias', autor: user.name || user.nome || 'Equipe', createdAt: new Date().toISOString() };
            const list = this.custom(); list.unshift(a);
            localStorage.setItem('elora_mock_artigos', JSON.stringify(list));
            return a;
        }
        return await apiService.post(`${CONFIG.ENDPOINTS.conhecimento}/artigos`, { titulo, resumo, corpo, categoria });
    }
    faq() {
        return [
            { q: 'Como contrato um cuidador?', a: 'Busque por especialidade e distância, abra o perfil, clique em Contratar, preencha datas/horários e assine digitalmente.' },
            { q: 'O pagamento é seguro?', a: 'Sim. PIX, cartão ou boleto com comprovante. O repasse ao cuidador é registrado por contrato.' },
            { q: 'Como avalio o cuidador?', a: 'Após o contrato, abra a página Avaliar pelo painel ou pelo contrato assinado. Uma avaliação por contrato.' },
            { q: 'Como denuncio um problema?', a: 'Use a página Denunciar (menu Ajuda ou painel jurídico). Descreva o ocorrido e informe o contrato.' },
            { q: 'Meus dados estão protegidos (LGPD)?', a: 'Sim. Consentimento no cadastro, acesso/correção/exclusão pelo perfil e trilha de auditoria.' }
        ];
    }
}
const conhecimentoService = new ConhecimentoService();
window.conhecimentoService = conhecimentoService;
