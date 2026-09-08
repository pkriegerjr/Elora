/**
 * ELORA - DTOs espelhando entidades JPA Spring Boot
 * Use estes objetos como contrato entre front e API /api
 * Todos os campos seguem camelCase (Jackson padrão Spring)
 */

// POST /api/clients  e  POST /api/caregivers
const ClientCreateDTO = {
    name: "string",
    cpf: "123.456.789-00",
    birthDate: "1990-01-01", // LocalDate
    phone: "(11) 99999-9999",
    email: "email@exemplo.com",
    password: "Senha@123",
    address: {
        cep: "01310-100", state: "SP", city: "São Paulo",
        neighborhood: "Jardim Paulista", street: "Av. Paulista", number: "1000", complemento: "Apto 101",
        latitude: -23.5617, longitude: -46.6560
    },
    careNeeds: {
        type: "idoso", description: "...",
        days: [1,2,3,4,5], // 0=Dom
        schedule: { start: "08:00", end: "17:00" },
        startDate: "2024-02-01", duration: "indeterminado"
    }
};

const CaregiverCreateDTO = {
    name: "...", cpf: "...", birthDate: "...", phone: "...", email: "...", password: "...",
    address: { cep: "...", state: "SP", city: "...", neighborhood: "...", street: "...", number: "..." },
    experienceYears: 8,
    education: "Técnico em Enfermagem - Senac",
    certifications: ["Cuidador de Idosos - Cruz Vermelha"],
    specialties: ["idoso","alzheimer"],
    bio: "...",
    hourlyRate: 45.00, // BigDecimal
    availability: { days: [1,2,3,4,5], schedule: { start: "07:00", end: "19:00" }, startDate: "2024-01-15", maxDistanceKm: 15 },
    // Documentos: multipart/form-data com @RequestPart("data") JSON + @RequestPart("files") MultipartFile[]
};

const ContractCreateDTO = {
    clientId: "uuid", caregiverId: "uuid",
    startDate: "2024-02-01", endDate: "2024-07-31", // LocalDate
    days: [1,2,3,4,5],
    schedule: { start: "08:00", end: "17:00" },
    activities: ["Higiene", "Medicação"],
    hourlyRate: 45.00, // snapshot do cuidador
    observations: "...",
    paymentMethod: "PIX" // PIX | CARD | BOLETO (enum PaymentMethod)
};

const PaymentCreateDTO = {
    contractId: "uuid", method: "PIX", amount: 14850.00
};

// Respostas Spring (Page<T> e DTOs)
// GET /api/caregivers/search?lat=&lng=&radius=&specialty=&minRating=&page=&size=
// -> { content: [CaregiverResponseDTO], totalElements, totalPages, number, size }
// GET /api/config/maps-key -> { apiKey: "..." } (autenticado)

window.ELORA_DTO = { ClientCreateDTO, CaregiverCreateDTO, ContractCreateDTO, PaymentCreateDTO };
