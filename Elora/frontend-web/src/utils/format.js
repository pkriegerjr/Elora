// ELORA utils - ajudante de formatação (extraído de app.js - mantido em app.js para MOCK_MODE)
function formatCurrency(value){ return new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(value); }
function escapeHtml(v){ return String(v ?? '').replace(/[&<>"'`=]/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','`':'&#96;','=':'&#61;'}[s])); }
if (typeof window !== 'undefined' && !window.escapeHtml) window.escapeHtml = escapeHtml;

