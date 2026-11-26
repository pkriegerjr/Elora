/**
 * ELORA - Calendário mensal de contratos do cuidador.
 *
 * Componente compartilhado por dashboard-cuidador.html e escalas/agenda.html.
 * Reutiliza as classes .elora-calendar/.calendar-day de dashboard.css e as
 * funções globais formatDate()/escapeHtml() de app.js — carregar depois dele.
 *
 * Nenhuma requisição própria: recebe os contratos já carregados pela página.
 * O detalhe do dia pode receber também escalas indexadas por data.
 */
class CalendarioContratosService {
  constructor() {
    this.diasSemana = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
    this.config = null;
    this.contratos = [];
    this.escalasPorData = {};
    const hoje = new Date();
    this.ano = hoje.getFullYear();
    this.mes = hoje.getMonth();
    this.selecionado = null;
    this._gridId = null;
    this._bindingsId = null;
  }

  /** Filtro único de "contrato ativo assinado" para as duas páginas. */
  filtrarContratos(lista, usuarioId) {
    const uid = String(usuarioId ?? '');
    return (lista || []).filter(c =>
      ['ativo', 'signed'].includes(String(c.status || '').toLowerCase()) &&
      String(c.caregiverId ?? c.profissionalId ?? '') === uid
    );
  }

  /** Indexa escalas por YYYY-MM-DD, descartando canceladas. */
  indexarEscalas(escalas) {
    const mapa = {};
    (escalas || []).forEach(e => {
      if (String(e.status || '').toLowerCase() === 'cancelada') return;
      const iso = this.isoDe(e.data);
      if (!iso) return;
      if (!mapa[iso]) mapa[iso] = [];
      mapa[iso].push(e);
    });
    return mapa;
  }

  iso(ano, mes, dia) {
    return `${ano}-${String(mes + 1).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;
  }

  isoDe(valor) {
    if (!valor) return null;
    if (typeof valor === 'string') {
      const m = valor.match(/^(\d{4})-(\d{2})-(\d{2})/);
      return m ? `${m[1]}-${m[2]}-${m[3]}` : null;
    }
    const dt = new Date(valor);
    return isNaN(dt.getTime()) ? null : this.iso(dt.getFullYear(), dt.getMonth(), dt.getDate());
  }

  dataDeIso(iso) {
    if (!iso) return null;
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  contratosNoDia(iso) {
    return this.contratos.filter(c => {
      const ini = this.isoDe(c.startDate);
      if (!ini) return false;
      const fim = this.isoDe(c.endDate) || ini;
      return iso >= ini && iso <= fim;
    });
  }

  periodoLabel(periodo) {
    return String(periodo || '').replace(/^./, c => c.toUpperCase());
  }

  statusLabel(status) {
    if (typeof escalaService !== 'undefined' && typeof escalaService.statusLabel === 'function') {
      return escalaService.statusLabel(status);
    }
    return status || '';
  }

  /** Monta o card. Chamadas repetidas na mesma página preservam mês/dia. */
  montar(config) {
    const cfg = Object.assign({
      gridId: 'calendarGrid',
      titleId: 'calTitle',
      prevId: 'calPrev',
      nextId: 'calNext',
      todayId: 'calToday',
      detailId: 'calDayDetail'
    }, config || {});
    this.config = cfg;
    if (this._gridId !== cfg.gridId) {
      this._gridId = cfg.gridId;
      const hoje = new Date();
      this.ano = hoje.getFullYear();
      this.mes = hoje.getMonth();
      this.selecionado = null;
      this._bindingsId = null;
    }
    if (cfg.ano != null) this.ano = Number(cfg.ano);
    if (cfg.mes != null) this.mes = Number(cfg.mes);
    if (Object.prototype.hasOwnProperty.call(cfg, 'selecionado')) this.selecionado = cfg.selecionado;
    this.contratos = Array.isArray(cfg.contratos) ? cfg.contratos : [];
    this.escalasPorData = cfg.escalasPorData || {};
    this.bindNavegacao();
    this.render();
  }

  bindNavegacao() {
    if (this._bindingsId === this.config.gridId) return;
    document.getElementById(this.config.prevId)?.addEventListener('click', () => this.mudarMes(-1));
    document.getElementById(this.config.nextId)?.addEventListener('click', () => this.mudarMes(1));
    document.getElementById(this.config.todayId)?.addEventListener('click', () => this.irParaHoje());
    this._bindingsId = this.config.gridId;
  }

  mudarMes(delta) {
    this.mes += delta;
    if (this.mes < 0) { this.mes = 11; this.ano--; }
    if (this.mes > 11) { this.mes = 0; this.ano++; }
    this.selecionado = null;
    this.render();
  }

  irParaHoje() {
    const hoje = new Date();
    this.ano = hoje.getFullYear();
    this.mes = hoje.getMonth();
    this.selecionado = this.iso(this.ano, this.mes, hoje.getDate());
    this.render();
  }

  render() {
    const grid = document.getElementById(this.config.gridId);
    if (!grid) return;
    const titulo = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
      .format(new Date(this.ano, this.mes, 1)).replace(/^./, c => c.toUpperCase());
    const elTitulo = document.getElementById(this.config.titleId);
    if (elTitulo) elTitulo.textContent = titulo;

    const hoje = new Date();
    const hojeIso = this.iso(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());
    const desloc = (new Date(this.ano, this.mes, 1).getDay() + 6) % 7; // Seg = 0
    const total = new Date(this.ano, this.mes + 1, 0).getDate();

    let html = '<div class="calendar-weekdays">' +
      this.diasSemana.map(d => `<div>${d}</div>`).join('') + '</div><div class="calendar-days">';
    for (let i = 0; i < desloc; i++) html += '<div class="calendar-empty" aria-hidden="true"></div>';
    for (let d = 1; d <= total; d++) {
      const iso = this.iso(this.ano, this.mes, d);
      const contratos = this.contratosNoDia(iso);
      const classes = ['calendar-day'];
      if (contratos.length) classes.push('has-contract');
      if (iso === hojeIso) classes.push('is-today');
      if (iso === this.selecionado) classes.push('is-selected');
      const rotulo = formatDate(this.dataDeIso(iso));
      html += `<button type="button" class="${classes.join(' ')}" data-date="${iso}"` +
        `${iso === hojeIso ? ' aria-current="date"' : ''}` +
        ` aria-label="${escapeHtml(rotulo)}${contratos.length ? ', ' + contratos.length + ' contrato(s) ativo(s)' : ''}">` +
        `<span class="calendar-day-number">${d}</span>` +
        `${contratos.length ? '<span class="calendar-day-mark" aria-hidden="true"></span>' : ''}</button>`;
    }
    html += '</div>';
    grid.innerHTML = html;
    grid.querySelectorAll('[data-date]').forEach(b =>
      b.addEventListener('click', () => { this.selecionado = b.dataset.date; this.render(); }));
    this.renderDetalhe();
  }

  renderDetalhe() {
    const box = document.getElementById(this.config.detailId);
    if (!box) return;
    if (!this.selecionado) {
      box.innerHTML = '<span class="text-muted">Selecione um dia para ver os contratos.</span>';
      return;
    }
    const contratos = this.contratosNoDia(this.selecionado);
    let html = `<div class="fw-semibold mb-2">${formatDate(this.dataDeIso(this.selecionado))}</div>`;
    if (!contratos.length) {
      html += '<span class="text-muted">Nenhum contrato ativo neste dia.</span>';
    } else {
      html += contratos.map(c => {
        const nome = c.client?.name || c.clientId || 'Cliente';
        const fim = c.endDate || c.startDate;
        return `<div class="d-flex justify-content-between align-items-center gap-2 border-bottom py-2">` +
          `<div><strong>${escapeHtml(nome)}</strong>` +
          `<div class="text-muted">${formatDate(this.dataDeIso(c.startDate))} – ${formatDate(this.dataDeIso(fim))}</div></div>` +
          `<span class="d-inline-flex align-items-center gap-2">` +
          `<span class="badge bg-success">Ativo</span>` +
          `<a href="../contratos/contrato.html?id=${encodeURIComponent(c.id)}" class="btn btn-sm btn-elora-outline">Ver</a>` +
          `</span></div>`;
      }).join('');
    }

    if (Object.keys(this.escalasPorData).length) {
      const escalas = this.escalasPorData[this.selecionado] || [];
      html += '<div class="mt-2 pt-2 border-top"><div class="fw-semibold mb-1">Escalas neste dia</div>';
      html += escalas.length
        ? '<div class="d-flex flex-wrap gap-2">' + escalas.map(e =>
            `<span class="badge bg-light text-dark border">${escapeHtml(this.periodoLabel(e.periodo))} · ${escapeHtml(this.statusLabel(e.status))}</span>`
          ).join('') + '</div>'
        : '<span class="text-muted">Sem escalas registradas neste dia.</span>';
      html += '</div>';
    }
    box.innerHTML = html;
  }
}

const calendarioService = new CalendarioContratosService();
window.calendarioService = calendarioService;
