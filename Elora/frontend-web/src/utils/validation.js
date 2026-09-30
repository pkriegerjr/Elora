// ELORA utils - ajudante de validação (referencia V2 CHAR(11))
// Não redefine validateCPF: a versão com checksum de app.js tem precedência.
// Só instala o fallback simples se nenhuma versão existir (evita clobber por ordem de <script>).
if (typeof window !== 'undefined' && typeof window.validateCPF !== 'function') {
    window.validateCPF = function(cpf){ if(!cpf) return false; const c=String(cpf).replace(/\D/g,''); return c.length===11; };
}
function validateCEP(cep){ return /^\d{5}-?\d{3}$/.test(cep); }

