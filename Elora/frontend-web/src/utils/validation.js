// ELORA utils - ajudante de validação (referencia V2 CHAR(11))
function validateCPF(cpf){ if(!cpf) return false; const c=cpf.replace(/\D/g,''); return c.length===11; }
function validateCEP(cep){ return /^\d{5}-?\d{3}$/.test(cep); }

