/**
 * ELORA Platform - Mock Data
 * 
 * This file contains mock data for demonstration purposes.
 * ALL DATA HERE IS FICTITIOUS AND FOR PROTOTYPE ONLY.
 * In production, this data would come from the backend API.
 * 
 * DO NOT USE REAL PERSONAL DATA IN MOCK DATA.
 */

// Mock Clients
const mockClients = [
    {
        id: "client_001",
        name: "Maria Silva Santos",
        cpf: "123.456.789-00",
        birthDate: "1950-05-15",
        phone: "(11) 98765-4321",
        email: "maria.santos@email.com",
        password: "Senha@123",
        createdAt: "2024-01-15T10:30:00Z",
        updatedAt: "2024-01-15T10:30:00Z",
        status: "active",
        address: {
            cep: "01310-100",
            state: "SP",
            city: "São Paulo",
            neighborhood: "Jardim Paulista",
            street: "Av. Paulista",
            number: "1000",
            complemento: "Apto 101",
            lat: -23.5617,
            lng: -46.6560
        },
        careNeeds: {
            type: "idoso",
            description: "Necessito de acompanhante para minha mãe de 85 anos com mobilidade reduzida. Precisa de ajuda com higiene, alimentação e medicação.",
            days: [1, 2, 3, 4, 5], // Mon-Fri
            schedule: { start: "08:00", end: "17:00" },
            startDate: "2024-02-01",
            duration: "indeterminado"
        },
        favorites: ["caregiver_001", "caregiver_003"],
        contracts: ["contract_001"],
        notifications: []
    },
    {
        id: "client_002",
        name: "João Pedro Oliveira",
        cpf: "987.654.321-00",
        birthDate: "1945-12-20",
        phone: "(11) 91234-5678",
        email: "joao.oliveira@email.com",
        password: "Senha@123",
        createdAt: "2024-01-20T14:00:00Z",
        updatedAt: "2024-01-20T14:00:00Z",
        status: "active",
        address: {
            cep: "04543-011",
            state: "SP",
            city: "São Paulo",
            neighborhood: "Vila Olímpia",
            street: "Rua Olimpíadas",
            number: "200",
            complemento: "Casa",
            lat: -23.5983,
            lng: -46.6799
        },
        careNeeds: {
            type: "pos_operatorio",
            description: "Recuperação de cirurgia de quadril. Necessita ajuda com mobilidade, curativos e fisioterapia domiciliar.",
            days: [0, 1, 2, 3, 4, 5, 6], // All days
            schedule: { start: "07:00", end: "19:00" },
            startDate: "2024-02-05",
            duration: "60 dias"
        },
        favorites: [],
        contracts: [],
        notifications: []
    }
];

// Mock Caregivers
const mockCaregivers = [
    {
        id: "caregiver_001",
        name: "Ana Paula Ferreira",
        cpf: "111.222.333-44",
        birthDate: "1985-03-22",
        phone: "(11) 99999-1111",
        email: "ana.ferreira@email.com",
        password: "Senha@123",
        createdAt: "2023-11-10T09:00:00Z",
        updatedAt: "2024-01-10T15:30:00Z",
        status: CONFIG.CAREGIVER_STATUS.APPROVED,
        verified: true,
        verificationDate: "2023-12-01T10:00:00Z",
        address: {
            cep: "01452-000",
            state: "SP",
            city: "São Paulo",
            neighborhood: "Pinheiros",
            street: "Rua dos Pinheiros",
            number: "500",
            complemento: "Apto 50",
            lat: -23.5589,
            lng: -46.6924
        },
        experience: 8,
        education: "Técnico em Enfermagem - Senac",
        certifications: [
            "Cuidador de Idosos - Cruz Vermelha (2020)",
            "Primeiros Socorros - Bombeiros (2021)",
            "Cuidados Paliativos - Hospital Albert Einstein (2022)"
        ],
        specialties: ["idoso", "alzheimer", "parkinson", "mobilidade_reduzida", "medicacao"],
        bio: "Cuidadora dedicada com 8 anos de experiência no cuidado de idosos com doenças neurodegenerativas. Paciente, atenciosa e comprometida com o bem-estar e dignidade dos assistidos.",
        availability: {
            days: [1, 2, 3, 4, 5, 6], // Mon-Sat
            schedule: { start: "07:00", end: "19:00" },
            startDate: "2024-01-15",
            maxDistance: 15
        },
        documents: {
            idDocument: { name: "rg_ana.pdf", status: "approved", uploadedAt: "2023-11-10T09:00:00Z" },
            cpf: { name: "cpf_ana.pdf", status: "approved", uploadedAt: "2023-11-10T09:00:00Z" },
            addressProof: { name: "comprovante_ana.pdf", status: "approved", uploadedAt: "2023-11-10T09:00:00Z" },
            certificates: { name: "certificados_ana.pdf", status: "approved", uploadedAt: "2023-11-10T09:00:00Z" }
        },
        rating: 4.9,
        reviewCount: 47,
        hourlyRate: 45.00,
        contracts: ["contract_001"],
        earnings: 12500.00,
        completedJobs: 28
    },
    {
        id: "caregiver_002",
        name: "Carlos Eduardo Lima",
        cpf: "222.333.444-55",
        birthDate: "1990-07-15",
        phone: "(11) 98888-2222",
        email: "carlos.lima@email.com",
        password: "Senha@123",
        createdAt: "2023-12-05T11:00:00Z",
        updatedAt: "2024-01-05T10:00:00Z",
        status: CONFIG.CAREGIVER_STATUS.APPROVED,
        verified: true,
        verificationDate: "2024-01-03T14:00:00Z",
        address: {
            cep: "05407-000",
            state: "SP",
            city: "São Paulo",
            neighborhood: "Perdizes",
            street: "Rua Apiacás",
            number: "300",
            complemento: "",
            lat: -23.5352,
            lng: -46.6792
        },
        experience: 5,
        education: "Enfermagem - USP (cursando 3º ano)",
        certifications: [
            "Cuidador Hospitalar - SENAC (2021)",
            "Suporte Básico de Vida - AHA (2022)",
            "Cuidados com Feridas - SBEnfer (2023)"
        ],
        specialties: ["pos_operatorio", "curativos", "fisioterapia", "medicacao", "monitoramento"],
        bio: "Estudante de enfermagem com experiência em home care e pós-operatório. Foco em reabilitação e recuperação humanizada. Disponível para plantões noturnos.",
        availability: {
            days: [1, 2, 3, 4, 5], // Mon-Fri
            schedule: { start: "19:00", end: "07:00" }, // Night shift
            startDate: "2024-01-10",
            maxDistance: 20
        },
        documents: {
            idDocument: { name: "rg_carlos.pdf", status: "approved", uploadedAt: "2023-12-05T11:00:00Z" },
            cpf: { name: "cpf_carlos.pdf", status: "approved", uploadedAt: "2023-12-05T11:00:00Z" },
            addressProof: { name: "comprovante_carlos.pdf", status: "approved", uploadedAt: "2023-12-05T11:00:00Z" },
            certificates: { name: "certificados_carlos.pdf", status: "approved", uploadedAt: "2023-12-05T11:00:00Z" }
        },
        rating: 4.8,
        reviewCount: 32,
        hourlyRate: 50.00,
        contracts: [],
        earnings: 8200.00,
        completedJobs: 18
    },
    {
        id: "caregiver_003",
        name: "Luciana Costa Ribeiro",
        cpf: "333.444.555-66",
        birthDate: "1988-11-30",
        phone: "(11) 97777-3333",
        email: "luciana.ribeiro@email.com",
        password: "Senha@123",
        createdAt: "2024-01-08T16:00:00Z",
        updatedAt: "2024-01-15T09:00:00Z",
        status: CONFIG.CAREGIVER_STATUS.APPROVED,
        verified: true,
        verificationDate: "2024-01-12T11:00:00Z",
        address: {
            cep: "03178-200",
            state: "SP",
            city: "São Paulo",
            neighborhood: "Tatuapé",
            street: "Rua Tuiuti",
            number: "1200",
            complemento: "Bloco B, Apto 45",
            lat: -23.5389,
            lng: -46.5678
        },
        experience: 6,
        education: "Técnico em Cuidador de Idosos - ETEC",
        certifications: [
            "Cuidadora de Idosos - ETEC (2018)",
            "Demência e Alzheimer - ABRAz (2020)",
            "Nutrição para Idosos - CRN (2021)"
        ],
        specialties: ["idoso", "alzheimer", "demencia", "nutricao", "companhia"],
        bio: "Especialista em cuidados com demência e Alzheimer. Abordagem humanizada focada na manutenção da autonomia e qualidade de vida. Experiência em estimulação cognitiva.",
        availability: {
            days: [0, 1, 2, 3, 4, 5, 6], // All days
            schedule: { start: "08:00", end: "18:00" },
            startDate: "2024-01-20",
            maxDistance: 10
        },
        documents: {
            idDocument: { name: "rg_luciana.pdf", status: "approved", uploadedAt: "2024-01-08T16:00:00Z" },
            cpf: { name: "cpf_luciana.pdf", status: "approved", uploadedAt: "2024-01-08T16:00:00Z" },
            addressProof: { name: "comprovante_luciana.pdf", status: "approved", uploadedAt: "2024-01-08T16:00:00Z" },
            certificates: { name: "certificados_luciana.pdf", status: "approved", uploadedAt: "2024-01-08T16:00:00Z" }
        },
        rating: 4.95,
        reviewCount: 23,
        hourlyRate: 48.00,
        contracts: [],
        earnings: 5600.00,
        completedJobs: 12
    },
    {
        id: "caregiver_004",
        name: "Roberto Alves Machado",
        cpf: "444.555.666-77",
        birthDate: "1982-09-18",
        phone: "(11) 96666-4444",
        email: "roberto.machado@email.com",
        password: "Senha@123",
        createdAt: "2024-01-12T10:00:00Z",
        updatedAt: "2024-01-12T10:00:00Z",
        status: CONFIG.CAREGIVER_STATUS.UNDER_REVIEW,
        verified: false,
        verificationDate: null,
        address: {
            cep: "02030-000",
            state: "SP",
            city: "São Paulo",
            neighborhood: "Santana",
            street: "Av. Luiz Dumont Villares",
            number: "800",
            complemento: "",
            lat: -23.4935,
            lng: -46.6123
        },
        experience: 10,
        education: "Enfermeiro - UNIP",
        certifications: [
            "Enfermagem Geral - COREN ativo",
            "UTI Móvel - SAMU (2015)",
            "Home Care Avançado - Hospital Sírio-Libanês (2019)"
        ],
        specialties: ["idoso", "pos_operatorio", "ventilacao_mecanica", "traqueostomia", "sonda"],
        bio: "Enfermeiro com 10 anos de experiência em UTI e Home Care. Especialista em cuidados complexos: ventilação mecânica, traqueostomia, sonda enteral. Disponível para plantões 12x36.",
        availability: {
            days: [1, 3, 5], // Mon, Wed, Fri
            schedule: { start: "07:00", end: "19:00" },
            startDate: "2024-02-01",
            maxDistance: 25
        },
        documents: {
            idDocument: { name: "rg_roberto.pdf", status: "pending_review", uploadedAt: "2024-01-12T10:00:00Z" },
            cpf: { name: "cpf_roberto.pdf", status: "pending_review", uploadedAt: "2024-01-12T10:00:00Z" },
            addressProof: { name: "comprovante_roberto.pdf", status: "pending_review", uploadedAt: "2024-01-12T10:00:00Z" },
            certificates: { name: "certificados_roberto.pdf", status: "pending_review", uploadedAt: "2024-01-12T10:00:00Z" }
        },
        rating: 0,
        reviewCount: 0,
        hourlyRate: 80.00,
        contracts: [],
        earnings: 0,
        completedJobs: 0
    },
    {
        id: "caregiver_005",
        name: "Fernanda Santos Oliveira",
        cpf: "555.666.777-88",
        birthDate: "1992-02-25",
        phone: "(11) 95555-5555",
        email: "fernanda.oliveira@email.com",
        password: "Senha@123",
        createdAt: "2024-01-18T14:00:00Z",
        updatedAt: "2024-01-18T14:00:00Z",
        status: CONFIG.CAREGIVER_STATUS.PENDING,
        verified: false,
        verificationDate: null,
        address: {
            cep: "04094-050",
            state: "SP",
            city: "São Paulo",
            neighborhood: "Ipiranga",
            street: "Rua dos Patriotas",
            number: "450",
            complemento: "Casa 2",
            lat: -23.5958,
            lng: -46.6107
        },
        experience: 3,
        education: "Curso de Cuidador de Idosos - Senac",
        certifications: [
            "Cuidador de Idosos - Senac (2022)",
            "Primeiros Socorros - Cruz Vermelha (2023)"
        ],
        specialties: ["idoso", "companhia", "medicacao", "higiene", "alimentacao"],
        bio: "Cuidadora recém-formada, motivada e carinhosa. Experiência em acompanhamento de idosos lúcidos para consultas, caminhadas e atividades de lazer. Disponível para período integral.",
        availability: {
            days: [1, 2, 3, 4, 5], // Mon-Fri
            schedule: { start: "08:00", end: "17:00" },
            startDate: "2024-02-01",
            maxDistance: 12
        },
        documents: {
            idDocument: { name: "rg_fernanda.pdf", status: "not_uploaded" },
            cpf: { name: "cpf_fernanda.pdf", status: "not_uploaded" },
            addressProof: { name: "comprovante_fernanda.pdf", status: "not_uploaded" },
            certificates: { name: "certificados_fernanda.pdf", status: "not_uploaded" }
        },
        rating: 0,
        reviewCount: 0,
        hourlyRate: 35.00,
        contracts: [],
        earnings: 0,
        completedJobs: 0
    }
];

// Mock Admins
const mockAdmins = [
    {
        id: "admin_001",
        name: "Administrador ELORA",
        email: "admin@elora.com.br",
        username: "admin",
        password: "Admin@123",
        createdAt: "2023-10-01T08:00:00Z",
        updatedAt: "2023-10-01T08:00:00Z",
        role: "super_admin",
        permissions: ["all"]
    },
    {
        id: "admin_002",
        name: "Carla Mendes - Jurídico",
        email: "juridico@elora.com.br",
        username: "carla.juridico",
        password: "Juridico@123",
        createdAt: "2023-10-15T09:00:00Z",
        updatedAt: "2023-10-15T09:00:00Z",
        role: "legal",
        permissions: ["view_users", "review_documents", "approve_caregivers", "reject_caregivers"]
    }
];

// Mock Contracts — V2: contrato.codigo, valor_hora, status PT (elora_schema_v2.sql:209)
const mockContracts = [
    {
        id: "contract_001",
        codigo: "ELO-2026-000123", // V2: contrato.codigo UNIQUE
        titulo: "Cuidado idoso - mobilidade reduzida", // V2
        descricaoNecessidade: "Acompanhante para mãe 85 anos com mobilidade reduzida",
        clientId: "client_001",
        clienteId: "client_001", // alias V2
        caregiverId: "caregiver_001",
        profissionalId: "caregiver_001", // V2
        startDate: "2024-02-01",
        dataInicio: "2024-02-01", // V2
        endDate: "2024-07-31",
        dataFim: "2024-07-31", // V2
        days: [1, 2, 3, 4, 5],
        periodos: ["matutino","vespertino"], // V2: disponibilidade periodos derivado de schedule
        schedule: { start: "08:00", end: "17:00" },
        atividades: ["Higiene pessoal e banho","Administração de medicação"],
        activities: [
            "Higiene pessoal e banho",
            "Administração de medicação",
            "Preparo e auxílio na alimentação",
            "Mobilização e exercícios leves",
            "Acompanhamento em consultas médicas",
            "Companhia e conversação"
        ],
        hourlyRate: 45.00,
        valorHora: 45.00, // V2
        precoHora: 45.00,
        valorTotal: 14850.00, // V2: contrato.valor_total
        enderecoAtendimento: "Av. Paulista, 1000 - Jardim Paulista, São Paulo/SP",
        paymentMethod: CONFIG.PAYMENT_METHODS.PIX,
        metodoPagamento: "pix", // V2
        status: CONFIG.CONTRACT_STATUS.SIGNED,
        statusV2: "ativo", // V2: ativo (mapeado de SIGNED)
        createdAt: "2024-01-20T10:00:00Z",
        updatedAt: "2024-01-25T14:30:00Z",
        signedAt: "2024-01-25T14:30:00Z",
        clientSignature: {
            userId: "client_001",
            userType: "client",
            signedAt: "2024-01-25T10:00:00Z",
            signatureHash: "sha256_abc123..."
        },
        caregiverSignature: {
            userId: "caregiver_001",
            userType: "caregiver",
            signedAt: "2024-01-25T14:30:00Z",
            signatureHash: "sha256_def456..."
        },
        totalValue: {
            subtotal: 13500.00,
            platformFee: 1350.00,
            total: 14850.00,
            totalHours: 300,
            weekdaysCount: 60,
            hoursPerDay: 9
        }
    }
];

// Mock Payments — V2: pagamento.valor_bruto/valor_taxa/valor_liquido, idempotency_key (elora_schema_v2.sql:311)
const mockPayments = [
    {
        id: "payment_001",
        contratoId: "contract_001", // V2
        contractId: "contract_001",
        clientId: "client_001",
        pagadorId: "client_001", // V2
        caregiverId: "caregiver_001",
        amount: 14850.00,
        valorBruto: 14850.00, // V2
        valorTaxa: 1350.00,
        valorLiquido: 13500.00,
        idempotencyKey: "idem-contract_001-001", // V2 UNIQUE
        method: CONFIG.PAYMENT_METHODS.PIX,
        metodo: "pix", // V2
        status: CONFIG.PAYMENT_STATUS.APPROVED,
        statusV2: "aprovado",
        createdAt: "2024-01-26T09:00:00Z",
        updatedAt: "2024-01-26T09:05:00Z",
        approvedAt: "2024-01-26T09:05:00Z",
        transactionId: "ELR1A2B3C4D5E6",
        gatewayResponse: { success: true, code: "00" },
        paymentDetails: { method: "pix" }
    }
];

// Mock Reviews
const mockReviews = [
    {
        id: "review_001",
        contractId: "contract_001",
        clientId: "client_001",
        caregiverId: "caregiver_001",
        rating: 5,
        comment: "A Ana é uma cuidadora excepcional. Minha mãe adora ela, tem muita paciência e carinho. Recomendo de olhos fechados!",
        createdAt: "2024-03-15T10:00:00Z",
        response: null
    },
    {
        id: "review_002",
        contractId: "contract_001",
        clientId: "client_002",
        caregiverId: "caregiver_001",
        rating: 5,
        comment: "Profissional muito competente e humana. Cuidou do meu pai pós-operatório com excelência.",
        createdAt: "2023-11-20T14:00:00Z",
        response: "Muito obrigada! Foi um prazer cuidar do seu pai."
    }
];

// Mock Notifications
const mockNotifications = [
    {
        id: "notif_001",
        userId: "client_001",
        type: CONFIG.NOTIFICATION_TYPES.CONTRACT_SIGNED,
        title: "Contrato Assinado!",
        message: "O cuidador Ana Paula Ferreira assinou o contrato. O contrato está agora totalmente assinado.",
        read: false,
        createdAt: "2024-01-25T14:30:00Z",
        relatedId: "contract_001"
    },
    {
        id: "notif_002",
        userId: "client_001",
        type: CONFIG.NOTIFICATION_TYPES.PAYMENT_APPROVED,
        title: "Pagamento Aprovado!",
        message: "Seu pagamento de R$ 14.850,00 foi aprovado. A cuidadora foi notificada.",
        read: false,
        createdAt: "2024-01-26T09:05:00Z",
        relatedId: "payment_001"
    },
    {
        id: "notif_003",
        userId: "caregiver_001",
        type: CONFIG.NOTIFICATION_TYPES.CONTRACT_SIGNED,
        title: "Contrato Assinado!",
        message: "Você assinou o contrato. O contrato está agora totalmente assinado.",
        read: true,
        createdAt: "2024-01-25T14:30:00Z",
        readAt: "2024-01-25T15:00:00Z",
        relatedId: "contract_001"
    },
    {
        id: "notif_004",
        userId: "caregiver_001",
        type: CONFIG.NOTIFICATION_TYPES.PAYMENT_APPROVED,
        title: "Pagamento Recebido!",
        message: "O pagamento do contrato #contract_001 foi aprovado. Valor: R$ 14.850,00.",
        read: false,
        createdAt: "2024-01-26T09:05:00Z",
        relatedId: "payment_001"
    },
    {
        id: "notif_005",
        userId: "caregiver_004",
        type: CONFIG.NOTIFICATION_TYPES.REGISTRATION_UNDER_REVIEW,
        title: "Cadastro em Análise",
        message: "Seu cadastro está sendo analisado pela nossa equipe. Você será notificado quando houver atualização.",
        read: false,
        createdAt: "2024-01-12T10:00:00Z",
        relatedId: "caregiver_004"
    }
];

// Enriquecimento V2: adiciona campos do elora_schema_v2.sql mantendo compatibilidade V1
function enrichV2(){
    // CPF -> CHAR(11) dígitos em cpfDigits (V2) + geolocalização + genero/consentimento
    mockClients.forEach(c=>{ c.cpfDigits = (c.cpf||'').replace(/\D/g,''); c.genero = c.genero||'prefiro_nao_informar'; c.consentimentoLgpd = true; c.latitude = c.address?.lat; c.longitude = c.address?.lng; });
    mockCaregivers.forEach(cg=>{
        cg.cpfDigits = (cg.cpf||'').replace(/\D/g,''); cg.genero = cg.genero||'prefiro_nao_informar'; cg.consentimentoLgpd = true;
        cg.latitude = cg.address?.lat; cg.longitude = cg.address?.lng;
        cg.precoHora = cg.hourlyRate; cg.descricaoPerfil = cg.bio; cg.notaMedia = cg.rating;
        cg.periodos = cg.availability? (CONFIG.scheduleToPeriodos? CONFIG.scheduleToPeriodos(cg.availability.schedule.start, cg.availability.schedule.end) : ['matutino','vespertino']) : [];
        // especialidades V2: usuario_especialidade N:N (mantém array specialties)
        // documento_profissional status já existe; garante pendente/aprovado lowercase PT
        Object.values(cg.documents||{}).forEach(d=>{ if(d.status==='approved') d.status='aprovado'; if(d.status==='pending_review') d.status='pendente'; });
    });
}
enrichV2();

// Initialize mock data in localStorage if not exists (com migração V2 se já existir dados antigos)
function initializeMockData() {
    // se já existe, migra para V2 sem apagar
    function migrate(key, source){
        const existing = localStorage.getItem(key);
        if(!existing){ localStorage.setItem(key, JSON.stringify(source)); return; }
        try{
            const arr = JSON.parse(existing);
            // adiciona campos V2 faltantes
            if(key==='elora_mock_clients' || key==='elora_mock_caregivers'){
                arr.forEach(o=>{ if(!o.cpfDigits) o.cpfDigits=(o.cpf||'').replace(/\D/g,''); if(!o.genero) o.genero='prefiro_nao_informar'; });
                localStorage.setItem(key, JSON.stringify(arr));
            } else if(key==='elora_mock_contracts'){
                arr.forEach(o=>{ if(!o.codigo) o.codigo='ELO-2026-'+o.id.slice(-6).toUpperCase(); if(!o.statusV2) o.statusV2=CONFIG.toV2ContractStatus(o.status); });
                localStorage.setItem(key, JSON.stringify(arr));
            } else if(key==='elora_mock_payments'){
                arr.forEach(o=>{ if(!o.valorBruto) o.valorBruto=o.amount; if(!o.idempotencyKey) o.idempotencyKey='idem-'+o.id; });
                localStorage.setItem(key, JSON.stringify(arr));
            }
        }catch(e){ console.warn('Migração mock falhou', e); }
    }
    migrate("elora_mock_clients", mockClients);
    migrate("elora_mock_caregivers", mockCaregivers);
    if (!localStorage.getItem("elora_mock_admins")) localStorage.setItem("elora_mock_admins", JSON.stringify(mockAdmins));
    migrate("elora_mock_contracts", mockContracts);
    migrate("elora_mock_payments", mockPayments);
    if (!localStorage.getItem("elora_mock_reviews")) localStorage.setItem("elora_mock_reviews", JSON.stringify(mockReviews));
    if (!localStorage.getItem("elora_mock_notifications")) localStorage.setItem("elora_mock_notifications", JSON.stringify(mockNotifications));
}

// Initialize on load
initializeMockData();

// Export for use in other modules (read-only access to mock data)
window.MockData = {
    clients: mockClients,
    caregivers: mockCaregivers,
    admins: mockAdmins,
    contracts: mockContracts,
    payments: mockPayments,
    reviews: mockReviews,
    notifications: mockNotifications,
    initializeMockData
};

// Log warning that this is mock data
console.warn("%c⚠️ ELORA: Usando dados MOCK (fictícios) para demonstração.", "color: orange; font-weight: bold;");
console.warn("%c⚠️ NUNCA use dados reais em localStorage em produção!", "color: red; font-weight: bold;");