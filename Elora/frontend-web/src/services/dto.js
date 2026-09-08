/**
 * ELORA - DTOs espelhando entidades JPA Spring Boot + elora_schema_v2.sql
 * Todos os campos seguem camelCase (Jackson padrão Spring). No envio, CPF => dígitos (CHAR(11) V2)
 * e status mapeados via CONFIG.toV2* quando MOCK_MODE=false.
 */

// POST /api/clients
const ClientCreateDTO = {
    nome: "Maria Silva Santos", // V2: usuario.nome
    cpf: "12345678900", // V2: CHAR(11) dígitos — CONFIG.normalizeCPF() antes de enviar
    dataNascimento: "1990-01-01", // V2: usuario.data_nascimento
    genero: "F", // V2: ENUM M/F/outro/prefiro_nao_informar
    telefone: "(11) 98765-4321", // usuario.telefone
    email: "email@exemplo.com",
    senha: "Senha@123", // -> senha_hash bcrypt na API
    origemLogin: "senha", // V2: senha|google
    endereco: {
        cep: "01310-100", state: "SP", city: "São Paulo",
        neighborhood: "Jardim Paulista", street: "Av. Paulista", number: "1000", complemento: "Apto 101",
        latitude: -23.5617, longitude: -46.6560 // V2: usuario.latitude/longitude -> geo_ponto
    },
    consentimentoLgpd: true, // V2: usuario.consentimento_lgpd + consentimento_lgpd_at
    detalhesContratante: { observacoesCuidado: "Mobilidade reduzida, precisa de companhia" }, // contratante_detalhes
    cuidado: {
        tipo: "idoso", descricao: "...",
        dias: [1,2,3,4,5],
        periodos: ["matutino","vespertino"], // V2: disponibilidade.periodo por data
        schedule: { start: "08:00", end: "17:00" }, // convertido via CONFIG.scheduleToPeriodos()
        startDate: "2024-02-01", duration: "indeterminado"
    }
};

const CaregiverCreateDTO = {
    nome: "...", cpf: "11122233344", dataNascimento: "1985-03-22", genero: "F",
    telefone: "...", email: "...", senha: "...",
    endereco: { cep: "...", state: "SP", city: "...", neighborhood: "...", street: "...", number: "...", latitude: -23.5, longitude: -46.6 },
    consentimentoLgpd: true,
    profissionalDetalhes: {
        descricaoPerfil: "...", // V2: profissional_detalhes.descricao_perfil
        precoHora: 45.00, // V2: preco_hora BigDecimal
        especialidades: ["idoso","alzheimer"], // V2: usuario_especialidade N:N
        certificacoes: ["Cuidador de Idosos - Cruz Vermelha"],
        bio: "..."
    },
    disponibilidade: { dias: [1,2,3,4,5], schedule: { start: "07:00", end: "19:00" }, startDate: "2024-01-15", maxDistanceKm: 15, periodos: ["matutino","vespertino"] },
    // Documentos: multipart/form-data com @RequestPart("data") JSON + @RequestPart("files") MultipartFile[] -> documento_profissional
    hourlyRate: 45.00 // alias legado
};

const ContractCreateDTO = {
    codigo: "ELO-2026-000123", // V2: contrato.codigo UNIQUE (gerado no backend se omitido)
    clienteId: "uuid", profissionalId: "uuid",
    titulo: "Cuidado idoso - mobilidade reduzida", // V2: contrato.titulo
    descricaoNecessidade: "...", // V2: descricao_necessidade
    valorHora: 45.00, // V2: valor_hora
    enderecoAtendimento: "Av. Paulista, 1000 - SP",
    dataInicio: "2024-02-01", dataFim: "2024-07-31", // V2: data_inicio/data_fim
    status: "rascunho", // V2: rascunho|proposta|negociacao|aguard_assinatura|ativo|...
    // Legado (compat): startDate/endDate/days/schedule/activities/hourlyRate -> mapeados no service
    startDate: "2024-02-01", endDate: "2024-07-31",
    days: [1,2,3,4,5],
    schedule: { start: "08:00", end: "17:00" },
    periodos: ["matutino","vespertino"], // derivado de schedule
    atividades: ["Higiene", "Medicação"],
    observacoes: "...",
    metodoPagamento: "pix" // V2: pagamento.metodo pix|cartao|boleto
};

const PaymentCreateDTO = {
    contratoId: "uuid", // V2: pagamento.contrato_id
    pagadorId: "uuid", // V2: pagador_id (cliente)
    taxaId: 1, // V2: taxa_servico.id_taxa
    valorBruto: 14850.00, // V2: valor_bruto
    valorTaxa: 1350.00, // V2: valor_taxa
    valorLiquido: 13500.00, // V2: valor_liquido (repasse)
    metodo: "pix", // V2: pix|cartao|boleto
    idempotencyKey: "idem-uuid-v4", // V2: UNIQUE para retry seguro
    // Legado
    contractId: "uuid", method: "pix", amount: 14850.00
};

// Mappers para V2 (uso nos services quando MOCK_MODE=false)
const toV2ClientPayload = (form) => ({
    nome: form.name, cpf: CONFIG.normalizeCPF(form.cpf), dataNascimento: form.birthDate, genero: form.genero||'prefiro_nao_informar',
    telefone: form.phone, email: form.email, senha: form.password, origemLogin: 'senha',
    endereco: form.address, consentimentoLgpd: !!form.consentimentoLgpd,
    latitude: form.address?.latitude, longitude: form.address?.longitude
});

window.ELORA_DTO = { ClientCreateDTO, CaregiverCreateDTO, ContractCreateDTO, PaymentCreateDTO, toV2ClientPayload };
