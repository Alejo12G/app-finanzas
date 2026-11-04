'use strict';

(function (global) {
  const {
    MESES_NOMBRES,
    METODOS_PAGO,
    UNIDADES_ARTICULO,
    NATURALEZAS,
    ESTADOS_PRESUPUESTO,
    PRIORIDADES_IA,
    ICONOS_CATEGORIAS_PERMITIDOS,
    COLORES_CATEGORIAS_PERMITIDOS,
    THEME_STORAGE_KEY,
    DEFAULT_THEME,
    resolveAppTheme,
    getChartThemeConfig,
    containsEmoji,
    stripEmojis,
    calcSubtotalCentavos,
    parseDecimalToCentavos,
    centavosToDecimalInput,
    formatCentavos,
    getTodayIso,
    getCurrentPeriod,
    formatShortMonthYear,
    resolveCategoryIcon,
    escapeHtml
  } = global.AppUtils;

  // Categorias semilla de respaldo visual (API_CONTRACT.md seccion 5)
  const SEED_CATEGORIES = Object.freeze([
    { id: 1, nombre: 'Salario', tipo: 'INGRESO', icono: 'fa-solid fa-briefcase', color: '#2E7D32', activa: true },
    { id: 2, nombre: 'Honorarios', tipo: 'INGRESO', icono: 'fa-solid fa-laptop-code', color: '#2E7D32', activa: true },
    { id: 3, nombre: 'Inversiones', tipo: 'INGRESO', icono: 'fa-solid fa-chart-line', color: '#2E7D32', activa: true },
    { id: 4, nombre: 'Ventas', tipo: 'INGRESO', icono: 'fa-solid fa-hand-holding-dollar', color: '#2E7D32', activa: true },
    { id: 5, nombre: 'Reembolsos', tipo: 'INGRESO', icono: 'fa-solid fa-rotate-left', color: '#2E7D32', activa: true },
    { id: 6, nombre: 'Otros ingresos', tipo: 'INGRESO', icono: 'fa-solid fa-coins', color: '#2E7D32', activa: true },
    { id: 7, nombre: 'Vivienda', tipo: 'GASTO', icono: 'fa-solid fa-house-chimney', color: '#1B2A4A', activa: true },
    { id: 8, nombre: 'Electricidad', tipo: 'GASTO', icono: 'fa-solid fa-bolt', color: '#1B2A4A', activa: true },
    { id: 9, nombre: 'Agua', tipo: 'GASTO', icono: 'fa-solid fa-droplet', color: '#1B2A4A', activa: true },
    { id: 10, nombre: 'Internet y telefonia', tipo: 'GASTO', icono: 'fa-solid fa-wifi', color: '#1B2A4A', activa: true },
    { id: 11, nombre: 'Supermercado', tipo: 'GASTO', icono: 'fa-solid fa-basket-shopping', color: '#4A5568', activa: true },
    { id: 12, nombre: 'Restaurantes', tipo: 'GASTO', icono: 'fa-solid fa-utensils', color: '#4A5568', activa: true },
    { id: 13, nombre: 'Transporte', tipo: 'GASTO', icono: 'fa-solid fa-bus', color: '#4A5568', activa: true },
    { id: 14, nombre: 'Combustible', tipo: 'GASTO', icono: 'fa-solid fa-gas-pump', color: '#4A5568', activa: true },
    { id: 15, nombre: 'Vehiculo', tipo: 'GASTO', icono: 'fa-solid fa-car', color: '#4A5568', activa: true },
    { id: 16, nombre: 'Salud', tipo: 'GASTO', icono: 'fa-solid fa-heart-pulse', color: '#4A5568', activa: true },
    { id: 17, nombre: 'Farmacia', tipo: 'GASTO', icono: 'fa-solid fa-prescription-bottle-medical', color: '#4A5568', activa: true },
    { id: 18, nombre: 'Educacion', tipo: 'GASTO', icono: 'fa-solid fa-graduation-cap', color: '#4A5568', activa: true },
    { id: 19, nombre: 'Entretenimiento', tipo: 'GASTO', icono: 'fa-solid fa-film', color: '#4A5568', activa: true },
    { id: 20, nombre: 'Suscripciones', tipo: 'GASTO', icono: 'fa-solid fa-repeat', color: '#1B2A4A', activa: true },
    { id: 21, nombre: 'Ropa', tipo: 'GASTO', icono: 'fa-solid fa-shirt', color: '#4A5568', activa: true },
    { id: 22, nombre: 'Hogar', tipo: 'GASTO', icono: 'fa-solid fa-couch', color: '#4A5568', activa: true },
    { id: 23, nombre: 'Seguros', tipo: 'GASTO', icono: 'fa-solid fa-shield-halved', color: '#1B2A4A', activa: true },
    { id: 24, nombre: 'Deudas y prestamos', tipo: 'GASTO', icono: 'fa-solid fa-credit-card', color: '#C62828', activa: true },
    { id: 25, nombre: 'Impuestos', tipo: 'GASTO', icono: 'fa-solid fa-landmark', color: '#1B2A4A', activa: true },
    { id: 26, nombre: 'Mascotas', tipo: 'GASTO', icono: 'fa-solid fa-paw', color: '#4A5568', activa: true },
    { id: 27, nombre: 'Regalos', tipo: 'GASTO', icono: 'fa-solid fa-gift', color: '#4A5568', activa: true },
    { id: 28, nombre: 'Viajes', tipo: 'GASTO', icono: 'fa-solid fa-plane', color: '#4A5568', activa: true },
    { id: 29, nombre: 'Cuidado personal', tipo: 'GASTO', icono: 'fa-solid fa-spa', color: '#4A5568', activa: true },
    { id: 30, nombre: 'Ahorro', tipo: 'GASTO', icono: 'fa-solid fa-piggy-bank', color: '#2E7D32', activa: true },
    { id: 31, nombre: 'Otros gastos', tipo: 'GASTO', icono: 'fa-solid fa-ellipsis', color: '#4A5568', activa: true }
  ]);

  const AppState = {
    currentView: 'dashboard',
    period: getCurrentPeriod(),
    presPeriod: getCurrentPeriod(),
    categorias: [...SEED_CATEGORIES],
    transacciones: [],
    transaccionesMeta: { pagina: 1, por_pagina: 50, total_registros: 0, total_paginas: 1 },
    compras: [],
    comprasMeta: { pagina: 1, por_pagina: 50, total_registros: 0, total_paginas: 1 },
    presupuestosData: null,
    chartEvolucion: null,
    chartCategorias: null,
    confirmCallback: null,
    activeTicketCompraId: null,
    dictadoArticulos: [],
    creditCards: [],
    editingCreditCardId: null
  };

  // =========================================================
  // UTILIDADES DE INTERFAZ Y MANEJO DE ERRORES (API_CONTRACT 9.1)
  // =========================================================

  function $(id) {
    return document.getElementById(id);
  }

  // =========================================================
  // SISTEMA DE TEMAS: MODO OSCURO (DEFECTO) Y MODO CLARO
  // =========================================================

  function getActiveTheme() {
    let saved = null;
    try {
      saved = global.localStorage ? global.localStorage.getItem(THEME_STORAGE_KEY) : null;
    } catch (_e) {}
    if (!saved && document.documentElement) {
      saved = document.documentElement.getAttribute('data-theme');
    }
    return resolveAppTheme(saved);
  }

  function updateThemeUi(theme) {
    const isDark = (theme === 'dark');

    // 1. Selector en la barra superior de navegacion
    const topbarIcon = $('topbar-theme-icon');
    const topbarLabel = $('topbar-theme-label');
    const topbarBtn = $('btn-topbar-theme');
    if (topbarIcon) {
      topbarIcon.className = isDark ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
    }
    if (topbarLabel) {
      topbarLabel.textContent = isDark ? 'Modo Claro' : 'Modo Oscuro';
    }
    if (topbarBtn) {
      topbarBtn.title = isDark ? 'Cambiar a Modo Claro' : 'Cambiar a Modo Oscuro';
      topbarBtn.setAttribute('aria-label', isDark ? 'Cambiar a Modo Claro' : 'Cambiar a Modo Oscuro');
    }

    // 2. Controles en la pantalla de Configuracion
    const cfgThemeLabel = $('cfg-theme-current-label');
    if (cfgThemeLabel) {
      cfgThemeLabel.textContent = isDark
        ? 'Tema activo: Modo Oscuro Clásico (Por defecto)'
        : 'Tema activo: Modo Claro Clásico';
    }

    const btnDark = $('btn-cfg-theme-dark');
    const btnLight = $('btn-cfg-theme-light');
    if (btnDark && btnLight) {
      if (isDark) {
        btnDark.className = 'btn btn-primary btn-sm';
        btnLight.className = 'btn btn-secondary btn-sm';
      } else {
        btnDark.className = 'btn btn-secondary btn-sm';
        btnLight.className = 'btn btn-primary btn-sm';
      }
    }

    const diagTema = $('diag-tema');
    if (diagTema) {
      diagTema.textContent = isDark
        ? 'Modo Oscuro (Terminal Contable)'
        : 'Modo Claro Clásico';
    }
  }

  function updateChartTheme(theme) {
    const cfg = getChartThemeConfig(theme);

    // 1. Grafico de Evolucion mensual (6 Meses)
    if (AppState.chartEvolucion) {
      const chart = AppState.chartEvolucion;
      if (chart.options) {
        if (chart.options.plugins && chart.options.plugins.legend && chart.options.plugins.legend.labels) {
          chart.options.plugins.legend.labels.color = cfg.textColor;
        }
        if (chart.options.scales) {
          if (chart.options.scales.x) {
            if (chart.options.scales.x.grid) chart.options.scales.x.grid.color = cfg.gridColor;
            if (chart.options.scales.x.ticks) chart.options.scales.x.ticks.color = cfg.textColor;
          }
          if (chart.options.scales.y) {
            if (chart.options.scales.y.grid) chart.options.scales.y.grid.color = cfg.gridColor;
            if (chart.options.scales.y.ticks) chart.options.scales.y.ticks.color = cfg.textColor;
          }
        }
      }
      if (chart.data && Array.isArray(chart.data.datasets)) {
        if (chart.data.datasets[0]) {
          chart.data.datasets[0].backgroundColor = cfg.ingresoColor;
          chart.data.datasets[0].borderColor = cfg.ingresoColor;
        }
        if (chart.data.datasets[1]) {
          chart.data.datasets[1].backgroundColor = cfg.gastoColor;
          chart.data.datasets[1].borderColor = cfg.gastoColor;
        }
        if (chart.data.datasets[2]) {
          chart.data.datasets[2].backgroundColor = cfg.balanceColor;
          chart.data.datasets[2].borderColor = cfg.balanceColor;
        }
      }
      chart.update();
    }

    // 2. Grafico de Gastos por Categoria
    if (AppState.chartCategorias) {
      const chart = AppState.chartCategorias;
      if (chart.options && chart.options.scales) {
        if (chart.options.scales.x) {
          if (chart.options.scales.x.grid) chart.options.scales.x.grid.color = cfg.gridColor;
          if (chart.options.scales.x.ticks) chart.options.scales.x.ticks.color = cfg.textColor;
        }
        if (chart.options.scales.y) {
          if (chart.options.scales.y.grid) chart.options.scales.y.grid.color = cfg.gridColor;
          if (chart.options.scales.y.ticks) chart.options.scales.y.ticks.color = cfg.textColor;
        }
      }
      if (chart.data && Array.isArray(chart.data.datasets) && chart.data.datasets[0]) {
        const ds = chart.data.datasets[0];
        if (Array.isArray(ds.data) && (ds.data.length === 1 && ds.data[0] === 0)) {
          ds.backgroundColor = [cfg.emptyBarColor];
          ds.borderColor = [cfg.emptyBarColor];
        }
      }
      chart.update();
    }
  }

  function setTheme(targetTheme) {
    const theme = resolveAppTheme(targetTheme);
    if (document.documentElement) {
      document.documentElement.setAttribute('data-theme', theme);
    }
    try {
      if (global.localStorage) {
        global.localStorage.setItem(THEME_STORAGE_KEY, theme);
      }
    } catch (_e) {}

    updateThemeUi(theme);
    updateChartTheme(theme);
    return theme;
  }

  function toggleTheme() {
    const current = getActiveTheme();
    const next = (current === 'dark') ? 'light' : 'dark';
    return setTheme(next);
  }

  function initTheme() {
    let saved = null;
    try {
      saved = global.localStorage ? global.localStorage.getItem(THEME_STORAGE_KEY) : null;
    } catch (_e) {}
    const initialTheme = resolveAppTheme(saved);
    setTheme(initialTheme);
  }

  function showBanner(containerId, tipo, mensaje, retryCallback = null) {
    const el = $(containerId);
    if (!el) return;

    const iconMap = {
      success: 'fa-solid fa-circle-check',
      danger: 'fa-solid fa-circle-xmark',
      warning: 'fa-solid fa-triangle-exclamation',
      info: 'fa-solid fa-circle-info',
      lock: 'fa-solid fa-lock',
      offline: 'fa-solid fa-plug-circle-xmark'
    };

    const classMap = {
      success: 'alert-success',
      danger: 'alert-danger',
      warning: 'alert-warning',
      info: 'alert-info',
      lock: 'alert-danger',
      offline: 'alert-danger'
    };

    const iconClass = iconMap[tipo] || iconMap.info;
    const alertClass = classMap[tipo] || 'alert-info';

    el.style.display = 'block';
    el.innerHTML = `
      <div class="alert-banner ${alertClass}">
        <div class="alert-banner-content">
          <i class="${iconClass}" aria-hidden="true"></i>
          <span>${escapeHtml(mensaje)}</span>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          ${retryCallback ? `<button type="button" class="btn btn-secondary btn-sm btn-banner-retry"><i class="fa-solid fa-rotate-right" aria-hidden="true"></i><span>Reintentar</span></button>` : ''}
          <button type="button" class="btn btn-secondary btn-sm btn-banner-dismiss" aria-label="Cerrar aviso"><i class="fa-solid fa-xmark" aria-hidden="true"></i></button>
        </div>
      </div>
    `;

    const retryBtn = el.querySelector('.btn-banner-retry');
    if (retryBtn && typeof retryCallback === 'function') {
      retryBtn.addEventListener('click', () => {
        el.style.display = 'none';
        retryCallback();
      });
    }

    const dismissBtn = el.querySelector('.btn-banner-dismiss');
    if (dismissBtn) {
      dismissBtn.addEventListener('click', () => {
        el.style.display = 'none';
      });
    }
  }

  function hideBanner(containerId) {
    const el = $(containerId);
    if (el) {
      el.style.display = 'none';
      el.innerHTML = '';
    }
  }

  function clearFormErrors(formEl) {
    if (!formEl) return;
    formEl.querySelectorAll('.is-invalid').forEach((inp) => inp.classList.remove('is-invalid'));
    formEl.querySelectorAll('.field-error').forEach((errEl) => {
      errEl.textContent = '';
      errEl.classList.remove('visible');
    });
  }

  function applyFieldError(formEl, campo, mensaje) {
    if (!formEl) return;
    const errSpan = formEl.querySelector(`.field-error[data-error-for="${campo}"]`);
    if (errSpan) {
      errSpan.textContent = mensaje;
      errSpan.classList.add('visible');
    }
    const input = formEl.querySelector(`[name="${campo}"]`);
    if (input) {
      input.classList.add('is-invalid');
    }
    // Soporte para errores de articulos con notacion de indice: articulos[1].cantidad
    const matchArticulo = /^articulos\[(\d+)\]\.(\w+)$/.exec(campo);
    if (matchArticulo) {
      const idx = Number(matchArticulo[1]);
      const subCampo = matchArticulo[2];
      const rows = formEl.querySelectorAll('#compra-items-tbody tr');
      if (rows[idx]) {
        const targetInput = rows[idx].querySelector(`[data-field="${subCampo}"]`);
        if (targetInput) {
          targetInput.classList.add('is-invalid');
        }
      }
      const artErr = formEl.querySelector('.field-error[data-error-for="articulos"]');
      if (artErr) {
        artErr.textContent = `Fila #${idx + 1} (${subCampo}): ${mensaje}`;
        artErr.classList.add('visible');
      }
    }
  }

  function handleApiError(err, options = {}) {
    const { formEl = null, alertContainerId = 'global-alert-container', retryFn = null } = options;

    if (!err) return;

    if (err.status === 401 || err.codigo === 'TOKEN_INVALIDO') {
      showBanner(
        'global-alert-container',
        'lock',
        'Token de acceso invalido.',
        () => switchView('configuracion')
      );
      setAllMutatingButtonsDisabled(true);
      return;
    }

    if (err.status === 0 || err.codigo === 'SIN_RED' || err.codigo === 'TIMEOUT_RED') {
      showBanner(alertContainerId, 'offline', 'No se pudo conectar con el servidor.', retryFn);
      return;
    }

    if (err.status === 429 || err.codigo === 'LIMITE_IA_EXCEDIDO') {
      showBanner(alertContainerId, 'warning', 'Limite de analisis IA alcanzado. Intente mas tarde.');
      return;
    }

    if (formEl && Array.isArray(err.detalles) && err.detalles.length > 0) {
      for (const d of err.detalles) {
        if (d && d.campo) {
          applyFieldError(formEl, d.campo, d.mensaje || 'Valor invalido.');
        }
      }
    }

    showBanner(alertContainerId, 'danger', err.mensaje || 'Error al procesar la solicitud.', retryFn);
  }

  function setButtonLoading(btn, isLoading, loadingText = 'Procesando...') {
    if (!btn) return;
    if (isLoading) {
      btn.dataset.originalHtml = btn.innerHTML;
      btn.disabled = true;
      btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin" aria-hidden="true"></i><span>${escapeHtml(loadingText)}</span>`;
    } else {
      btn.disabled = global.AppConfig.isTokenBlocked();
      if (btn.dataset.originalHtml) {
        btn.innerHTML = btn.dataset.originalHtml;
      }
    }
  }

  function setAllMutatingButtonsDisabled(disabled) {
    const ids = ['btn-tx-submit', 'btn-compra-submit', 'btn-pres-submit', 'btn-cat-submit', 'btn-ia-submit'];
    for (const id of ids) {
      const btn = $(id);
      if (btn) btn.disabled = Boolean(disabled);
    }
  }

  function renderCategoryChip(cat) {
    if (!cat) {
      return `<span class="category-chip"><span class="category-icon-box" style="background-color: #4A5568;"><i class="fa-solid fa-tag" aria-hidden="true"></i></span><span>Sin categoria</span></span>`;
    }
    const icon = resolveCategoryIcon(cat.icono);
    const color = cat.color || '#4A5568';
    return `
      <span class="category-chip">
        <span class="category-icon-box" style="background-color: ${escapeHtml(color)};">
          <i class="${escapeHtml(icon)}" aria-hidden="true"></i>
        </span>
        <span>${escapeHtml(cat.nombre)}</span>
      </span>
    `;
  }

  function renderMetodoPagoBadge(metodo) {
    const info = METODOS_PAGO[metodo] || METODOS_PAGO.OTRO;
    return `
      <span class="badge badge-neutral">
        <i class="${info.icono}" aria-hidden="true"></i>
        <span>${escapeHtml(info.etiqueta)}</span>
      </span>
    `;
  }

  function renderNaturalezaBadge(nat) {
    const info = NATURALEZAS[nat] || NATURALEZAS.VARIABLE;
    return `
      <span class="badge badge-primary">
        <i class="${info.icono}" aria-hidden="true"></i>
        <span>${escapeHtml(info.etiqueta)}</span>
      </span>
    `;
  }

  function updateCurrencyLabels() {
    const curr = global.AppConfig.getCurrency();
    const topCode = $('topbar-currency-code');
    if (topCode) topCode.textContent = curr;
    document.querySelectorAll('.currency-unit-label').forEach((el) => {
      el.textContent = curr;
    });
  }

  // =========================================================
  // NAVEGACION Y SELECTORES DE FECHA / PERIODO
  // =========================================================

  function populateMonthSelect(selectEl, selectedMonth) {
    if (!selectEl) return;
    selectEl.innerHTML = MESES_NOMBRES.map((nombre, idx) => {
      const m = idx + 1;
      const sel = m === Number(selectedMonth) ? 'selected' : '';
      return `<option value="${m}" ${sel}>${nombre}</option>`;
    }).join('');
  }

  function shiftPeriod(periodObj, deltaMonths) {
    let m = Number(periodObj.mes) + deltaMonths;
    let y = Number(periodObj.anio);
    if (m < 1) {
      m = 12;
      y -= 1;
    } else if (m > 12) {
      m = 1;
      y += 1;
    }
    if (y < 2000) y = 2000;
    if (y > 2100) y = 2100;
    periodObj.anio = y;
    periodObj.mes = m;
  }

  function switchView(viewName) {
    AppState.currentView = viewName;
    document.querySelectorAll('.nav-btn').forEach((btn) => {
      btn.classList.toggle('active', btn.dataset.view === viewName);
    });
    document.querySelectorAll('.view-section').forEach((sec) => {
      sec.classList.toggle('active', sec.id === `view-${viewName}`);
    });

    if (viewName === 'dashboard') {
      loadDashboard();
    } else if (viewName === 'transacciones') {
      loadTransacciones(1);
    } else if (viewName === 'compras') {
      loadCompras(1);
    } else if (viewName === 'presupuestos') {
      loadPresupuestos();
    } else if (viewName === 'categorias') {
      loadCategoriasView();
    } else if (viewName === 'tarjetas') {
      loadCreditCardsView();
    } else if (viewName === 'configuracion') {
      populateConfigForm();
    }
  }

  // =========================================================
  // SALUD DEL SERVIDOR Y CARGA DE CATEGORIAS
  // =========================================================

  async function checkServerHealth(showFeedbackInConfig = false) {
    const pill = $('topbar-connection-pill');
    const icon = $('topbar-connection-icon');
    const text = $('topbar-connection-text');
    const cfg = global.AppConfig.getConfig();

    if ($('sidebar-host-label')) {
      $('sidebar-host-label').textContent = `${cfg.host}:${cfg.puerto}`;
    }

    try {
      const res = await global.ApiClient.getHealth();
      const data = res.data || {};
      if (data.moneda) {
        global.AppConfig.setCurrency(data.moneda);
        updateCurrencyLabels();
      }

      if (pill) {
        pill.classList.remove('disconnected');
        pill.classList.add('connected');
      }
      if (icon) icon.className = 'fa-solid fa-server';
      if (text) text.textContent = `Conectado (${cfg.host}:${cfg.puerto})`;
      if ($('sidebar-api-version')) $('sidebar-api-version').textContent = `v${data.version_api || '1.0.0'}`;

      if ($('diag-estado')) {
        $('diag-estado').innerHTML = `<span class="badge badge-success"><i class="fa-solid fa-circle-check" aria-hidden="true"></i><span>${escapeHtml(data.estado || 'OK')}</span></span>`;
      }
      if ($('diag-version-api')) $('diag-version-api').textContent = data.version_api || '1.0.0';
      if ($('diag-moneda')) $('diag-moneda').textContent = data.moneda || 'COP';
      if ($('diag-hora-servidor')) $('diag-hora-servidor').textContent = data.hora_servidor || '-';

      // Comprobar validez del token privado cargando categorias y tarjetas
      await fetchCategoriasMaster();
      await fetchCreditCardsMaster();
      if ($('diag-auth')) {
        $('diag-auth').innerHTML = `<span class="badge badge-success"><i class="fa-solid fa-key" aria-hidden="true"></i><span>Token Valido</span></span>`;
      }
      setAllMutatingButtonsDisabled(false);
      hideBanner('global-alert-container');

      if (showFeedbackInConfig) {
        showBanner('cfg-alert', 'success', `Conexion verificada correctamente con ${cfg.url_base}.`);
      }
      return true;
    } catch (err) {
      if (pill) {
        pill.classList.remove('connected');
        pill.classList.add('disconnected');
      }
      if (icon) {
        icon.className = err.status === 401 ? 'fa-solid fa-lock' : 'fa-solid fa-plug-circle-xmark';
      }
      if (text) {
        text.textContent = err.status === 401 ? 'Token invalido (401)' : 'Sin conexion al VPS';
      }

      if (err.status === 401) {
        if ($('diag-auth')) {
          $('diag-auth').innerHTML = `<span class="badge badge-danger"><i class="fa-solid fa-lock" aria-hidden="true"></i><span>401 TOKEN_INVALIDO</span></span>`;
        }
        handleApiError(err);
      } else {
        if ($('diag-estado')) {
          $('diag-estado').innerHTML = `<span class="badge badge-danger"><i class="fa-solid fa-plug-circle-xmark" aria-hidden="true"></i><span>Sin conexion</span></span>`;
        }
        if (showFeedbackInConfig) {
          showBanner('cfg-alert', 'offline', 'No se pudo conectar con el servidor.', () => checkServerHealth(true));
        }
      }
      populateAllCategorySelects();
      return false;
    }
  }

  async function fetchCategoriasMaster() {
    const res = await global.ApiClient.getCategorias({ incluir_inactivas: 'true' });
    if (Array.isArray(res.data) && res.data.length > 0) {
      AppState.categorias = res.data;
    }
    populateAllCategorySelects();
  }

  function populateAllCategorySelects() {
    const activeCats = AppState.categorias.filter((c) => c.activa !== false);
    const txTipo = $('tx-tipo') ? $('tx-tipo').value : 'GASTO';
    const txCats = activeCats.filter((c) => c.tipo === txTipo);
    const gastoCats = activeCats.filter((c) => c.tipo === 'GASTO');

    const fillSelect = (selectEl, list, defaultId = null, includeAllOption = false) => {
      if (!selectEl) return;
      const currentVal = selectEl.value;
      const opts = [];
      if (includeAllOption) {
        opts.push('<option value="">Todas las categorias</option>');
      }
      for (const c of list) {
        opts.push(`<option value="${c.id}">${escapeHtml(c.nombre)} (${c.tipo})</option>`);
      }
      selectEl.innerHTML = opts.join('');
      if (currentVal && list.some((c) => String(c.id) === String(currentVal))) {
        selectEl.value = currentVal;
      } else if (defaultId && list.some((c) => c.id === defaultId)) {
        selectEl.value = String(defaultId);
      }
    };

    fillSelect($('tx-categoria'), txCats, txTipo === 'GASTO' ? 11 : 1);
    fillSelect($('filter-tx-categoria'), AppState.categorias, null, true);
    fillSelect($('compra-categoria'), gastoCats, 11);
    fillSelect($('pres-categoria'), gastoCats, 11);
    fillSelect($('dictado-categoria'), gastoCats, 11);
  }

  // =========================================================
  // 1. PANTALLA: DASHBOARD GENERAL
  // =========================================================

  async function loadDashboard() {
    const { anio, mes } = AppState.period;
    if ($('dash-select-mes')) $('dash-select-mes').value = String(mes);
    if ($('dash-input-anio')) $('dash-input-anio').value = String(anio);

    try {
      const res = await global.ApiClient.getDashboard(anio, mes);
      hideBanner('global-alert-container');
      renderDashboardData(res.data);
    } catch (err) {
      renderEmptyDashboard();
      handleApiError(err, {
        alertContainerId: 'global-alert-container',
        retryFn: () => loadDashboard()
      });
    }
  }

  function renderVariationText(val) {
    if (val === null || val === undefined) return 'Sin periodo previo';
    const num = Number(val);
    const sign = num > 0 ? '+' : '';
    return `${sign}${num.toFixed(2)} %`;
  }

  function renderDashboardData(data) {
    if (!data) return;
    const tot = data.totales || {};
    const comp = data.comparativo_mes_anterior || {};
    const pres = data.presupuestos || {};
    const curr = global.AppConfig.getCurrency();

    $('kpi-ingresos').textContent = formatCentavos(tot.ingresos_centavos || 0, 'INGRESO', curr);
    $('kpi-ingresos-var').textContent = renderVariationText(comp.ingresos_variacion_porcentaje);

    $('kpi-gastos').textContent = formatCentavos(tot.gastos_centavos || 0, 'GASTO', curr);
    $('kpi-gastos-desglose').textContent = `Fijos: ${formatCentavos(tot.gastos_fijos_centavos || 0, 'NEUTRO')} | Variables: ${formatCentavos(tot.gastos_variables_centavos || 0, 'NEUTRO')}`;
    $('kpi-gastos-var').textContent = renderVariationText(comp.gastos_variacion_porcentaje);

    const bal = Number(tot.balance_centavos || 0);
    const balEl = $('kpi-balance');
    balEl.textContent = formatCentavos(bal, 'TOTAL_MONEDA', curr);
    balEl.className = `kpi-value ${bal >= 0 ? 'amount-balance-pos' : 'amount-balance-neg'}`;
    $('kpi-tx-count').textContent = String(tot.cantidad_transacciones || 0);

    const ahorroPct = Number(tot.tasa_ahorro_porcentaje || 0);
    $('kpi-ahorro').textContent = `${ahorroPct.toFixed(2)} %`;

    const excedidos = Number(pres.cantidad_excedido || 0);
    const advertencias = Number(pres.cantidad_advertencia || 0);
    const presResumenEl = $('kpi-presupuesto-resumen');
    if (excedidos > 0) {
      presResumenEl.innerHTML = `<span class="amount-gasto"><i class="fa-solid fa-circle-xmark" aria-hidden="true"></i> ${excedidos} excedido(s)</span>`;
    } else if (advertencias > 0) {
      presResumenEl.innerHTML = `<span style="color: var(--color-advertencia);"><i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i> ${advertencias} en alerta</span>`;
    } else {
      presResumenEl.innerHTML = `<span class="amount-ingreso"><i class="fa-solid fa-circle-check" aria-hidden="true"></i> En regla</span>`;
    }

    renderDashboardCharts(data.evolucion_mensual || [], data.gastos_por_categoria || []);
    renderDashboardPresupuestos(pres, curr);
    renderDashboardCategoriasTable(data.gastos_por_categoria || [], data.ingresos_por_categoria || []);
    renderDashboardUltimas(data.ultimas_transacciones || []);
  }

  function renderEmptyDashboard() {
    const curr = global.AppConfig.getCurrency();
    $('kpi-ingresos').textContent = '+ 0.00';
    $('kpi-gastos').textContent = '- 0.00';
    $('kpi-balance').textContent = `${curr} 0.00`;
    $('kpi-ahorro').textContent = '0.00 %';
    renderDashboardCharts([], []);
    renderDashboardPresupuestos({
      total_limite_centavos: 0,
      total_gastado_centavos: 0,
      cantidad_normal: 0,
      cantidad_advertencia: 0,
      cantidad_excedido: 0
    }, curr);
    renderDashboardCategoriasTable([], []);
    renderDashboardUltimas([]);
  }

  function renderDashboardCharts(evolucion, gastosPorCat) {
    if (typeof global.Chart === 'undefined') return;

    const themeCfg = getChartThemeConfig(getActiveTheme());

    // 1. Evolucion 6 meses (Ingresos, Gastos, Balance segun tema contable)
    const ctxEvo = $('chart-evolucion') ? $('chart-evolucion').getContext('2d') : null;
    if (ctxEvo) {
      if (AppState.chartEvolucion) {
        AppState.chartEvolucion.destroy();
      }
      const labels = evolucion.map((item) => formatShortMonthYear(item.anio, item.mes));
      const ingresosData = evolucion.map((item) => Number((item.ingresos_centavos || 0) / 100));
      const gastosData = evolucion.map((item) => Number((item.gastos_centavos || 0) / 100));
      const balanceData = evolucion.map((item) => Number((item.balance_centavos || 0) / 100));

      AppState.chartEvolucion = new global.Chart(ctxEvo, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            {
              label: 'Ingresos',
              data: ingresosData,
              backgroundColor: themeCfg.ingresoColor,
              borderColor: themeCfg.ingresoColor,
              borderWidth: 1
            },
            {
              label: 'Gastos',
              data: gastosData,
              backgroundColor: themeCfg.gastoColor,
              borderColor: themeCfg.gastoColor,
              borderWidth: 1
            },
            {
              label: 'Balance',
              data: balanceData,
              backgroundColor: themeCfg.balanceColor,
              borderColor: themeCfg.balanceColor,
              borderWidth: 1
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: { duration: 200 },
          plugins: {
            legend: {
              labels: { color: themeCfg.textColor, font: { family: '"Segoe UI", Roboto, Arial, sans-serif', size: 12 } }
            }
          },
          scales: {
            x: {
              grid: { color: themeCfg.gridColor },
              ticks: { color: themeCfg.textColor }
            },
            y: {
              grid: { color: themeCfg.gridColor },
              ticks: { color: themeCfg.textColor }
            }
          }
        }
      });
    }

    // 2. Gastos por categoria (Barras horizontales con color de la API)
    const ctxCat = $('chart-categorias') ? $('chart-categorias').getContext('2d') : null;
    if (ctxCat) {
      if (AppState.chartCategorias) {
        AppState.chartCategorias.destroy();
      }
      const labels = gastosPorCat.length > 0
        ? gastosPorCat.map((g) => (g.categoria ? g.categoria.nombre : 'Categoria'))
        : ['Sin gastos en el periodo'];
      const values = gastosPorCat.length > 0
        ? gastosPorCat.map((g) => Number((g.total_centavos || 0) / 100))
        : [0];
      const colors = gastosPorCat.length > 0
        ? gastosPorCat.map((g) => (g.categoria && g.categoria.color ? g.categoria.color : themeCfg.textColor))
        : [themeCfg.emptyBarColor];

      AppState.chartCategorias = new global.Chart(ctxCat, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            {
              label: 'Gasto por Categoria',
              data: values,
              backgroundColor: colors,
              borderColor: colors,
              borderWidth: 1
            }
          ]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          animation: { duration: 200 },
          plugins: {
            legend: { display: false }
          },
          scales: {
            x: {
              grid: { color: themeCfg.gridColor },
              ticks: { color: themeCfg.textColor }
            },
            y: {
              grid: { color: themeCfg.gridColor },
              ticks: { color: themeCfg.textColor }
            }
          }
        }
      });
    }
  }

  function renderDashboardPresupuestos(pres, curr) {
    const box = $('dash-presupuestos-box');
    if (!box) return;

    const lim = Number(pres.total_limite_centavos || 0);
    const gas = Number(pres.total_gastado_centavos || 0);
    const pct = lim > 0 ? Math.min(100, (gas / lim) * 100) : 0;
    const pctReal = lim > 0 ? ((gas / lim) * 100).toFixed(2) : '0.00';
    let estado = 'NORMAL';
    if (lim > 0 && (gas / lim) * 100 >= 100) estado = 'EXCEDIDO';
    else if (lim > 0 && (gas / lim) * 100 >= 80) estado = 'ADVERTENCIA';

    box.innerHTML = `
      <div style="display: flex; flex-direction: column; gap: 12px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>Limite Total Presupuestado: <strong class="tabular-nums">${formatCentavos(lim, 'TOTAL_MONEDA', curr)}</strong></span>
          <span>Ejecutado: <strong class="tabular-nums amount-gasto">${formatCentavos(gas, 'GASTO', curr)}</strong> (${pctReal} %)</span>
        </div>
        <div class="progress-track">
          <div class="progress-fill estado-${estado}" style="width: ${pct.toFixed(2)}%;"></div>
        </div>
        <div class="grid-3" style="margin-top: 4px;">
          <div class="alert-banner alert-success" style="padding: 8px 12px;">
            <span><i class="fa-solid fa-circle-check" aria-hidden="true"></i> Normales</span>
            <strong class="tabular-nums">${Number(pres.cantidad_normal || 0)}</strong>
          </div>
          <div class="alert-banner alert-warning" style="padding: 8px 12px;">
            <span><i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i> Advertencia</span>
            <strong class="tabular-nums">${Number(pres.cantidad_advertencia || 0)}</strong>
          </div>
          <div class="alert-banner alert-danger" style="padding: 8px 12px;">
            <span><i class="fa-solid fa-circle-xmark" aria-hidden="true"></i> Excedidos</span>
            <strong class="tabular-nums">${Number(pres.cantidad_excedido || 0)}</strong>
          </div>
        </div>
      </div>
    `;
  }

  function renderDashboardCategoriasTable(gastosCat, ingresosCat) {
    const tbody = $('dash-categorias-tbody');
    if (!tbody) return;
    const rows = [];

    for (const item of ingresosCat) {
      rows.push(`
        <tr>
          <td>${renderCategoryChip(item.categoria)}</td>
          <td><span class="badge badge-success"><i class="fa-solid fa-arrow-trend-up" aria-hidden="true"></i> INGRESO</span></td>
          <td class="text-right tabular-nums">${Number(item.porcentaje || 0).toFixed(2)} %</td>
          <td class="text-right amount-ingreso">${formatCentavos(item.total_centavos, 'INGRESO')}</td>
        </tr>
      `);
    }

    for (const item of gastosCat) {
      rows.push(`
        <tr>
          <td>${renderCategoryChip(item.categoria)}</td>
          <td><span class="badge badge-danger"><i class="fa-solid fa-arrow-trend-down" aria-hidden="true"></i> GASTO</span></td>
          <td class="text-right tabular-nums">${Number(item.porcentaje || 0).toFixed(2)} %</td>
          <td class="text-right amount-gasto">${formatCentavos(item.total_centavos, 'GASTO')}</td>
        </tr>
      `);
    }

    if (rows.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="4">
            <div class="empty-state" style="padding: 16px;">
              <i class="fa-regular fa-folder-open empty-state-icon" style="font-size: 32px;" aria-hidden="true"></i>
              <span class="empty-state-text">No hay movimientos registrados para este periodo.</span>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = rows.join('');
  }

  function renderDashboardUltimas(ultimas) {
    const tbody = $('dash-ultimas-tbody');
    if (!tbody) return;

    if (!Array.isArray(ultimas) || ultimas.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="7">
            <div class="empty-state" style="padding: 20px;">
              <i class="fa-regular fa-folder-open empty-state-icon" aria-hidden="true"></i>
              <span class="empty-state-text">No hay registros para este periodo.</span>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = ultimas.map((tx) => {
      const isIngreso = tx.tipo === 'INGRESO';
      return `
        <tr>
          <td class="tabular-nums">${escapeHtml(tx.fecha)}</td>
          <td>${renderCategoryChip(tx.categoria)}</td>
          <td>
            ${escapeHtml(tx.descripcion)}
            ${tx.es_compra ? `<span class="badge badge-primary" style="margin-left: 6px;"><i class="fa-solid fa-receipt" aria-hidden="true"></i> ${tx.cantidad_articulos || 0} art.</span>` : ''}
          </td>
          <td>${escapeHtml(tx.comercio || '-')}</td>
          <td>${renderNaturalezaBadge(tx.naturaleza)}</td>
          <td>${renderMetodoPagoBadge(tx.metodo_pago)}</td>
          <td class="text-right ${isIngreso ? 'amount-ingreso' : 'amount-gasto'}">
            ${formatCentavos(tx.monto_centavos, isIngreso ? 'INGRESO' : 'GASTO')}
          </td>
        </tr>
      `;
    }).join('');
  }

  // =========================================================
  // 2. PANTALLA: TRANSACCIONES (INGRESOS Y GASTOS RAPIDOS)
  // =========================================================

  async function loadTransacciones(pagina = 1) {
    const query = {
      desde: $('filter-tx-desde').value || undefined,
      hasta: $('filter-tx-hasta').value || undefined,
      tipo: $('filter-tx-tipo').value || undefined,
      naturaleza: $('filter-tx-naturaleza').value || undefined,
      categoria_id: $('filter-tx-categoria').value || undefined,
      metodo_pago: $('filter-tx-metodo').value || undefined,
      es_compra: $('filter-tx-es-compra').value || undefined,
      buscar: $('filter-tx-buscar').value.trim() || undefined,
      pagina,
      por_pagina: Number($('filter-tx-por-pagina').value || 50)
    };

    try {
      const res = await global.ApiClient.getTransacciones(query);
      AppState.transacciones = Array.isArray(res.data) ? res.data : [];
      AppState.transaccionesMeta = res.meta || {
        pagina: 1,
        por_pagina: query.por_pagina,
        total_registros: AppState.transacciones.length,
        total_paginas: 1
      };
      renderTransaccionesTable();
    } catch (err) {
      AppState.transacciones = [];
      renderTransaccionesTable();
      handleApiError(err, {
        alertContainerId: 'tx-form-alert',
        retryFn: () => loadTransacciones(pagina)
      });
    }
  }

  function renderTransaccionesTable() {
    const tbody = $('tx-table-tbody');
    const meta = AppState.transaccionesMeta;
    if (!tbody) return;

    $('tx-pagination-info').textContent = `Total de registros: ${meta.total_registros || 0}`;
    $('tx-page-indicator').textContent = `Pagina ${meta.pagina || 1} de ${Math.max(1, meta.total_paginas || 1)}`;
    $('btn-tx-prev-page').disabled = (meta.pagina || 1) <= 1;
    $('btn-tx-next-page').disabled = (meta.pagina || 1) >= (meta.total_paginas || 1);

    if (AppState.transacciones.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="9">
            <div class="empty-state">
              <i class="fa-regular fa-folder-open empty-state-icon" aria-hidden="true"></i>
              <span class="empty-state-text">No hay registros para este periodo o criterio de busqueda.</span>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = AppState.transacciones.map((tx) => {
      const isIngreso = tx.tipo === 'INGRESO';
      const isCardPurchase = (tx.metodo_pago === 'CREDITO' || tx.card_id) && tx.installments;
      const cardInstallmentsBadge = isCardPurchase ? `
        <div style="margin-top: 4px; display: inline-flex; align-items: center; gap: 4px; flex-wrap: wrap;">
          <span class="badge badge-primary" title="${tx.installments} cuotas">
            <i class="fa-solid fa-credit-card" aria-hidden="true"></i>
            <span>${tx.installments} cuotas</span>
          </span>
          ${tx.is_interest_exempt
            ? `<span class="badge badge-exempt" title="Compra exenta de intereses (0% interes)"><i class="fa-solid fa-shield-halved" aria-hidden="true"></i> [EXENTA 0%]</span>`
            : `<span class="badge badge-interest" title="Tasa mensual: ${Number(tx.applied_interest_rate || 0).toFixed(2)}% M.V."><i class="fa-solid fa-percent" aria-hidden="true"></i> [CON INTERES]</span>`
          }
        </div>
      ` : '';

      return `
        <tr>
          <td class="tabular-nums">#${tx.id}</td>
          <td class="tabular-nums">${escapeHtml(tx.fecha)}</td>
          <td>${renderCategoryChip(tx.categoria)}</td>
          <td>
            ${escapeHtml(tx.descripcion)}
            ${tx.es_compra ? `<span class="badge badge-primary" style="margin-left: 4px;"><i class="fa-solid fa-receipt" aria-hidden="true"></i> ${tx.cantidad_articulos || 0}</span>` : ''}
          </td>
          <td>${escapeHtml(tx.comercio || '-')}</td>
          <td>${renderNaturalezaBadge(tx.naturaleza)}</td>
          <td>
            ${renderMetodoPagoBadge(tx.metodo_pago)}
            ${cardInstallmentsBadge}
          </td>
          <td class="text-right ${isIngreso ? 'amount-ingreso' : 'amount-gasto'}">
            ${formatCentavos(tx.monto_centavos, isIngreso ? 'INGRESO' : 'GASTO')}
          </td>
          <td class="text-center">
            <div style="display: inline-flex; gap: 4px;">
              ${tx.es_compra ? `
                <button type="button" class="btn btn-secondary btn-sm btn-icon-only" data-action="view-ticket" data-id="${tx.id}" title="Ver desglose de compra" aria-label="Ver desglose de compra">
                  <i class="fa-solid fa-receipt" aria-hidden="true"></i>
                </button>
              ` : ''}
              <button type="button" class="btn btn-secondary btn-sm btn-icon-only" data-action="edit-tx" data-id="${tx.id}" title="Editar registro" aria-label="Editar registro">
                <i class="fa-solid fa-pen" aria-hidden="true"></i>
              </button>
              <button type="button" class="btn btn-danger btn-sm btn-icon-only" data-action="delete-tx" data-id="${tx.id}" title="Eliminar transaccion" aria-label="Eliminar transaccion">
                <i class="fa-solid fa-trash-can" aria-hidden="true"></i>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  function resetTransaccionForm() {
    const form = $('form-transaccion');
    if (!form) return;
    clearFormErrors(form);
    hideBanner('tx-form-alert');
    $('tx-id').value = '';
    $('tx-tipo').value = 'GASTO';
    $('tx-naturaleza').value = 'VARIABLE';
    $('tx-monto').value = '';
    $('tx-fecha').value = getTodayIso();
    $('tx-descripcion').value = '';
    $('tx-metodo-pago').value = 'DEBITO';
    $('tx-comercio').value = '';
    $('tx-notas').value = '';

    // Resetear panel de tarjeta de credito
    if ($('tx-panel-credito')) $('tx-panel-credito').style.display = 'none';
    if ($('tx-card-id')) $('tx-card-id').value = '';
    if ($('tx-installments')) $('tx-installments').value = '1';
    if ($('tx-is-exempt')) $('tx-is-exempt').checked = false;
    if ($('tx-applied-rate')) {
      $('tx-applied-rate').value = '';
      $('tx-applied-rate').disabled = false;
      $('tx-applied-rate').style.cursor = '';
      $('tx-applied-rate').style.opacity = '';
    }

    populateAllCategorySelects();
    $('tx-form-title').innerHTML = `<i class="fa-solid fa-plus" aria-hidden="true"></i><span>Registrar Ingreso o Gasto</span>`;
    $('btn-tx-submit-label').textContent = 'Guardar Transaccion';
    $('btn-tx-cancel-edit').style.display = 'none';
  }

  function startEditTransaccion(tx) {
    if (!tx) return;
    // Regla 7.3.4: Si tiene es_compra = true, editar en el modulo de Compras
    if (tx.es_compra) {
      switchView('compras');
      loadCompraIntoEditor(tx.id);
      return;
    }
    const form = $('form-transaccion');
    clearFormErrors(form);
    hideBanner('tx-form-alert');
    $('tx-id').value = String(tx.id);
    $('tx-tipo').value = tx.tipo;
    populateAllCategorySelects();
    $('tx-categoria').value = String(tx.categoria_id);
    $('tx-naturaleza').value = tx.naturaleza;
    $('tx-monto').value = centavosToDecimalInput(tx.monto_centavos);
    $('tx-fecha').value = tx.fecha;
    $('tx-descripcion').value = tx.descripcion || '';
    $('tx-metodo-pago').value = tx.metodo_pago;
    $('tx-comercio').value = tx.comercio || '';
    $('tx-notas').value = tx.notas || '';

    // Soporte para tarjetas de credito y cuotas
    const isCard = tx.metodo_pago === 'CREDITO' || tx.card_id;
    if (isCard && $('tx-panel-credito')) {
      $('tx-panel-credito').style.display = 'block';
      if ($('tx-card-id')) $('tx-card-id').value = String(tx.card_id || '');
      if ($('tx-installments')) $('tx-installments').value = String(tx.installments || 1);
      const isExempt = Boolean(tx.is_interest_exempt);
      if ($('tx-is-exempt')) $('tx-is-exempt').checked = isExempt;
      if ($('tx-applied-rate')) {
        $('tx-applied-rate').value = isExempt ? '0.00' : String(Number(tx.applied_interest_rate || 0).toFixed(2));
        $('tx-applied-rate').disabled = isExempt;
        $('tx-applied-rate').style.cursor = isExempt ? 'not-allowed' : '';
        $('tx-applied-rate').style.opacity = isExempt ? '0.6' : '';
      }
      runLiveSimulation('tx');
    } else if ($('tx-panel-credito')) {
      $('tx-panel-credito').style.display = 'none';
    }

    $('tx-form-title').innerHTML = `<i class="fa-solid fa-pen" aria-hidden="true"></i><span>Editar Transaccion #${tx.id}</span>`;
    $('btn-tx-submit-label').textContent = 'Guardar Cambios';
    $('btn-tx-cancel-edit').style.display = 'inline-flex';
  }

  async function submitTransaccionForm(ev) {
    ev.preventDefault();
    const form = $('form-transaccion');
    clearFormErrors(form);
    hideBanner('tx-form-alert');

    const id = $('tx-id').value.trim();
    const tipo = $('tx-tipo').value;
    const naturaleza = $('tx-naturaleza').value;
    const montoCentavos = parseDecimalToCentavos($('tx-monto').value);
    const fecha = $('tx-fecha').value.trim();
    const categoriaId = parseInt($('tx-categoria').value, 10);
    const descripcion = $('tx-descripcion').value.trim();
    const metodoPago = $('tx-metodo-pago').value;
    const comercioRaw = $('tx-comercio').value.trim();
    const notasRaw = $('tx-notas').value.trim();

    let hasError = false;
    if (!Number.isInteger(montoCentavos) || montoCentavos < 1 || montoCentavos > 99999999999) {
      applyFieldError(form, 'monto_centavos', 'Debe ingresar un monto mayor que 0.00.');
      hasError = true;
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha)) {
      applyFieldError(form, 'fecha', 'Formato de fecha obligatorio: YYYY-MM-DD.');
      hasError = true;
    }
    if (!descripcion || descripcion.length > 200) {
      applyFieldError(form, 'descripcion', 'La descripcion es obligatoria (1 a 200 caracteres).');
      hasError = true;
    } else if (containsEmoji(descripcion)) {
      applyFieldError(form, 'descripcion', 'El texto no puede contener emojis (TEXTO_CON_EMOJI).');
      hasError = true;
    }
    if (comercioRaw && containsEmoji(comercioRaw)) {
      applyFieldError(form, 'comercio', 'El comercio no puede contener emojis.');
      hasError = true;
    }
    if (notasRaw && containsEmoji(notasRaw)) {
      applyFieldError(form, 'notas', 'Las notas no pueden contener emojis.');
      hasError = true;
    }
    if (!Number.isInteger(categoriaId) || categoriaId <= 0) {
      applyFieldError(form, 'categoria_id', 'Seleccione una categoria valida.');
      hasError = true;
    }

    // Validacion de campos contables de Tarjeta de Credito
    let cardId = null;
    let installments = 1;
    let isExempt = false;
    let appliedRate = 0;

    if (tipo === 'GASTO' && metodoPago === 'CREDITO') {
      const cardVal = $('tx-card-id') ? $('tx-card-id').value : '';
      cardId = parseInt(cardVal, 10);
      if (!Number.isInteger(cardId) || cardId <= 0) {
        applyFieldError(form, 'card_id', 'Seleccione una tarjeta de credito.');
        hasError = true;
      }

      const instVal = $('tx-installments') ? parseInt($('tx-installments').value, 10) : 1;
      if (!Number.isInteger(instVal) || instVal < 1 || instVal > 48) {
        applyFieldError(form, 'installments', 'El numero de cuotas debe estar entre 1 y 48.');
        hasError = true;
      } else {
        installments = instVal;
      }

      isExempt = Boolean($('tx-is-exempt') && $('tx-is-exempt').checked);
      if (isExempt) {
        appliedRate = 0;
      } else {
        const rateVal = parseFloat($('tx-applied-rate') ? $('tx-applied-rate').value : 0);
        if (!Number.isFinite(rateVal) || rateVal < 0 || rateVal > 100) {
          applyFieldError(form, 'applied_interest_rate', 'La tasa de interes debe ser un numero entre 0 y 100.');
          hasError = true;
        } else {
          appliedRate = rateVal;
        }
      }
    }

    if (hasError) return;

    const payload = {
      tipo,
      naturaleza,
      monto_centavos: montoCentavos,
      fecha,
      descripcion,
      categoria_id: categoriaId,
      metodo_pago: metodoPago,
      comercio: comercioRaw ? comercioRaw : null,
      notas: notasRaw ? notasRaw : null
    };

    if (tipo === 'GASTO' && metodoPago === 'CREDITO') {
      payload.payment_method = 'CREDIT_CARD';
      payload.card_id = cardId;
      payload.installments = installments;
      payload.is_interest_exempt = isExempt;
      payload.applied_interest_rate = appliedRate;
    }

    const btn = $('btn-tx-submit');
    setButtonLoading(btn, true, 'Guardando...');
    try {
      if (id) {
        await global.ApiClient.updateTransaccion(id, payload);
        resetTransaccionForm();
        showBanner('tx-form-alert', 'success', 'Transaccion actualizada correctamente.');
      } else {
        await global.ApiClient.createTransaccion(payload);
        resetTransaccionForm();
        showBanner('tx-form-alert', 'success', 'Transaccion registrada correctamente.');
      }
      await loadTransacciones(AppState.transaccionesMeta.pagina || 1);
      if (payload.card_id) {
        await fetchCreditCardsMaster();
      }
    } catch (err) {
      handleApiError(err, { formEl: form, alertContainerId: 'tx-form-alert' });
    } finally {
      setButtonLoading(btn, false);
    }
  }

  function exportTransaccionesCsv() {
    if (!AppState.transacciones || AppState.transacciones.length === 0) {
      showBanner('tx-form-alert', 'warning', 'No hay transacciones cargadas en la tabla para exportar.');
      return;
    }
    const headers = ['id', 'fecha', 'tipo', 'naturaleza', 'categoria', 'descripcion', 'comercio', 'metodo_pago', 'monto_centavos', 'monto_decimal'];
    const lines = [headers.join(',')];
    for (const tx of AppState.transacciones) {
      const catName = tx.categoria ? tx.categoria.nombre : '';
      const row = [
        tx.id,
        tx.fecha,
        tx.tipo,
        tx.naturaleza,
        `"${String(catName).replace(/"/g, '""')}"`,
        `"${String(tx.descripcion || '').replace(/"/g, '""')}"`,
        `"${String(tx.comercio || '').replace(/"/g, '""')}"`,
        tx.metodo_pago,
        tx.monto_centavos,
        (tx.monto_centavos / 100).toFixed(2)
      ];
      lines.push(row.join(','));
    }
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `transacciones_${getTodayIso()}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // =========================================================
  // 3. PANTALLA: COMPRAS DETALLADAS (TABLA DINAMICA DE ARTICULOS)
  // =========================================================

  function createCompraItemRow(item = {}) {
    const tbody = $('compra-items-tbody');
    if (!tbody) return;
    if (tbody.children.length >= 200) {
      showBanner('compra-form-alert', 'warning', 'El limite maximo permitido por compra es de 200 articulos.');
      return;
    }

    const tr = document.createElement('tr');
    const articuloVal = item.articulo || '';
    const cantidadVal = item.cantidad !== undefined ? item.cantidad : 1;
    const unidadVal = item.unidad || 'UNIDAD';
    const precioDecimal = item.precio_unitario_centavos !== undefined
      ? centavosToDecimalInput(item.precio_unitario_centavos)
      : '';

    const unidadesOptions = Object.values(UNIDADES_ARTICULO).map((u) => {
      const sel = u.valor === unidadVal ? 'selected' : '';
      return `<option value="${u.valor}" ${sel}>${u.etiqueta} (${u.valor})</option>`;
    }).join('');

    tr.innerHTML = `
      <td class="row-index tabular-nums">1</td>
      <td>
        <input type="text" class="form-control" data-field="articulo" maxlength="120" placeholder="Ej. Leche entera 1L" value="${escapeHtml(articuloVal)}" required />
      </td>
      <td>
        <input type="number" class="form-control text-right tabular-nums" data-field="cantidad" step="0.001" min="0.001" max="99999" value="${escapeHtml(cantidadVal)}" required />
      </td>
      <td>
        <select class="form-control" data-field="unidad">${unidadesOptions}</select>
      </td>
      <td>
        <input type="number" class="form-control text-right tabular-nums" data-field="precio_unitario_centavos" step="0.01" min="0" placeholder="0.00" value="${escapeHtml(precioDecimal)}" required />
      </td>
      <td class="text-right tabular-nums row-subtotal amount-neutro">0.00</td>
      <td class="text-center">
        <button type="button" class="btn btn-danger btn-sm btn-icon-only btn-remove-item" title="Quitar articulo" aria-label="Quitar articulo">
          <i class="fa-solid fa-square-minus" aria-hidden="true"></i>
        </button>
      </td>
    `;

    tr.querySelectorAll('input, select').forEach((inp) => {
      inp.addEventListener('input', () => {
        inp.classList.remove('is-invalid');
        recalcCompraTotals();
      });
    });

    tr.querySelector('.btn-remove-item').addEventListener('click', () => {
      if (tbody.children.length <= 1) {
        showBanner('compra-form-alert', 'warning', 'La factura debe contener al menos 1 articulo.');
        return;
      }
      tr.remove();
      recalcCompraTotals();
    });

    tbody.appendChild(tr);
    recalcCompraTotals();
  }

  function recalcCompraTotals() {
    const tbody = $('compra-items-tbody');
    if (!tbody) return 0;

    let sumaTotalCentavos = 0;
    const rows = Array.from(tbody.querySelectorAll('tr'));
    rows.forEach((tr, idx) => {
      const idxCell = tr.querySelector('.row-index');
      if (idxCell) idxCell.textContent = String(idx + 1);

      const cantInput = tr.querySelector('[data-field="cantidad"]');
      const precioInput = tr.querySelector('[data-field="precio_unitario_centavos"]');
      const subCell = tr.querySelector('.row-subtotal');

      const cant = Number(cantInput ? cantInput.value : 0);
      const precioCentavos = parseDecimalToCentavos(precioInput ? precioInput.value : '0');
      const subtotalCentavos = calcSubtotalCentavos(
        Number.isFinite(cant) ? cant : 0,
        Number.isInteger(precioCentavos) ? precioCentavos : 0
      );

      sumaTotalCentavos += subtotalCentavos;
      if (subCell) {
        subCell.textContent = formatCentavos(subtotalCentavos, 'NEUTRO');
      }
    });

    if ($('compra-total-items-count')) {
      $('compra-total-items-count').textContent = String(rows.length);
    }
    if ($('compra-total-preview')) {
      $('compra-total-preview').textContent = formatCentavos(sumaTotalCentavos, 'TOTAL_MONEDA');
    }
    if ($('compra-panel-credito') && $('compra-panel-credito').style.display !== 'none') {
      runLiveSimulation('compra');
    }
    return sumaTotalCentavos;
  }

  function resetCompraForm(initialItems = null) {
    const form = $('form-compra');
    if (!form) return;
    clearFormErrors(form);
    hideBanner('compra-form-alert');

    $('compra-id').value = '';
    $('compra-fecha').value = getTodayIso();
    $('compra-comercio').value = '';
    $('compra-metodo-pago').value = 'DEBITO';
    $('compra-naturaleza').value = 'VARIABLE';
    $('compra-descripcion').value = '';
    $('compra-notas').value = '';

    // Resetear panel de tarjeta de credito en compras
    if ($('compra-panel-credito')) $('compra-panel-credito').style.display = 'none';
    if ($('compra-card-id')) $('compra-card-id').value = '';
    if ($('compra-installments')) $('compra-installments').value = '1';
    if ($('compra-is-exempt')) $('compra-is-exempt').checked = false;
    if ($('compra-applied-rate')) {
      $('compra-applied-rate').value = '';
      $('compra-applied-rate').disabled = false;
      $('compra-applied-rate').style.cursor = '';
      $('compra-applied-rate').style.opacity = '';
    }

    populateAllCategorySelects();

    $('compra-form-title').innerHTML = `<i class="fa-solid fa-receipt" aria-hidden="true"></i><span>Nueva Factura / Compra con Desglose de Articulos</span>`;
    $('btn-compra-submit-label').textContent = 'Registrar Compra Detallada';
    $('btn-compra-cancel-edit').style.display = 'none';

    const tbody = $('compra-items-tbody');
    tbody.innerHTML = '';
    if (Array.isArray(initialItems) && initialItems.length > 0) {
      initialItems.forEach((it) => createCompraItemRow(it));
    } else {
      createCompraItemRow();
    }
  }

  async function loadCompraIntoEditor(compraId) {
    try {
      const res = await global.ApiClient.getCompra(compraId);
      const compra = res.data;
      if (!compra) return;

      const form = $('form-compra');
      clearFormErrors(form);
      hideBanner('compra-form-alert');

      $('compra-id').value = String(compra.id);
      $('compra-fecha').value = compra.fecha;
      $('compra-comercio').value = compra.comercio || '';
      populateAllCategorySelects();
      $('compra-categoria').value = String(compra.categoria_id);
      $('compra-metodo-pago').value = compra.metodo_pago;
      $('compra-naturaleza').value = compra.naturaleza || 'VARIABLE';
      $('compra-descripcion').value = compra.descripcion || '';
      $('compra-notas').value = compra.notas || '';

      const isCard = compra.metodo_pago === 'CREDITO' || compra.card_id;
      if (isCard && $('compra-panel-credito')) {
        $('compra-panel-credito').style.display = 'block';
        if ($('compra-card-id')) $('compra-card-id').value = String(compra.card_id || '');
        if ($('compra-installments')) $('compra-installments').value = String(compra.installments || 1);
        const isExempt = Boolean(compra.is_interest_exempt);
        if ($('compra-is-exempt')) $('compra-is-exempt').checked = isExempt;
        if ($('compra-applied-rate')) {
          $('compra-applied-rate').value = isExempt ? '0.00' : String(Number(compra.applied_interest_rate || 0).toFixed(2));
          $('compra-applied-rate').disabled = isExempt;
          $('compra-applied-rate').style.cursor = isExempt ? 'not-allowed' : '';
          $('compra-applied-rate').style.opacity = isExempt ? '0.6' : '';
        }
      } else if ($('compra-panel-credito')) {
        $('compra-panel-credito').style.display = 'none';
      }

      const tbody = $('compra-items-tbody');
      tbody.innerHTML = '';
      if (Array.isArray(compra.articulos) && compra.articulos.length > 0) {
        compra.articulos.forEach((art) => createCompraItemRow(art));
      } else {
        createCompraItemRow();
      }

      if (isCard) {
        runLiveSimulation('compra');
      }

      $('compra-form-title').innerHTML = `<i class="fa-solid fa-pen" aria-hidden="true"></i><span>Editando Compra Detallada #${compra.id}</span>`;
      $('btn-compra-submit-label').textContent = 'Guardar Cambios de Compra';
      $('btn-compra-cancel-edit').style.display = 'inline-flex';
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      handleApiError(err, { alertContainerId: 'compra-form-alert' });
    }
  }

  async function submitCompraForm(ev) {
    ev.preventDefault();
    const form = $('form-compra');
    clearFormErrors(form);
    hideBanner('compra-form-alert');

    const id = $('compra-id').value.trim();
    const fecha = $('compra-fecha').value.trim();
    const comercio = $('compra-comercio').value.trim();
    const categoriaId = parseInt($('compra-categoria').value, 10);
    const metodoPago = $('compra-metodo-pago').value;
    const naturaleza = $('compra-naturaleza').value || 'VARIABLE';
    const descripcionRaw = $('compra-descripcion').value.trim();
    const notasRaw = $('compra-notas').value.trim();

    let hasError = false;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha)) {
      applyFieldError(form, 'fecha', 'La fecha es obligatoria (YYYY-MM-DD).');
      hasError = true;
    }
    if (!comercio || comercio.length > 120) {
      applyFieldError(form, 'comercio', 'El nombre del comercio es obligatorio (1 a 120 caracteres).');
      hasError = true;
    } else if (containsEmoji(comercio)) {
      applyFieldError(form, 'comercio', 'El comercio no puede contener emojis.');
      hasError = true;
    }
    if (descripcionRaw && containsEmoji(descripcionRaw)) {
      applyFieldError(form, 'descripcion', 'La descripcion no puede contener emojis.');
      hasError = true;
    }
    if (notasRaw && containsEmoji(notasRaw)) {
      applyFieldError(form, 'notas', 'Las notas no pueden contener emojis.');
      hasError = true;
    }

    const rows = Array.from($('compra-items-tbody').querySelectorAll('tr'));
    if (rows.length < 1 || rows.length > 200) {
      applyFieldError(form, 'articulos', 'Debe incluir entre 1 y 200 articulos.');
      hasError = true;
    }

    const articulosPayload = [];
    rows.forEach((tr, idx) => {
      const artInput = tr.querySelector('[data-field="articulo"]');
      const cantInput = tr.querySelector('[data-field="cantidad"]');
      const uniInput = tr.querySelector('[data-field="unidad"]');
      const precInput = tr.querySelector('[data-field="precio_unitario_centavos"]');

      const nombreArt = artInput ? artInput.value.trim() : '';
      const cantidadNum = Number(cantInput ? cantInput.value : 0);
      const unidadVal = uniInput ? uniInput.value : 'UNIDAD';
      const precioCentavos = parseDecimalToCentavos(precInput ? precInput.value : '');

      if (!nombreArt || nombreArt.length > 120 || containsEmoji(nombreArt)) {
        if (artInput) artInput.classList.add('is-invalid');
        applyFieldError(form, `articulos[${idx}].articulo`, 'Nombre de articulo obligatorio y sin emojis.');
        hasError = true;
      }
      if (!Number.isFinite(cantidadNum) || cantidadNum <= 0 || cantidadNum > 99999) {
        if (cantInput) cantInput.classList.add('is-invalid');
        applyFieldError(form, `articulos[${idx}].cantidad`, 'Cantidad debe ser mayor que 0.');
        hasError = true;
      }
      if (!Number.isInteger(precioCentavos) || precioCentavos < 0 || precioCentavos > 9999999999) {
        if (precInput) precInput.classList.add('is-invalid');
        applyFieldError(form, `articulos[${idx}].precio_unitario_centavos`, 'Precio unitario invalido.');
        hasError = true;
      }

      articulosPayload.push({
        articulo: nombreArt,
        cantidad: Number(cantidadNum.toFixed(3)),
        unidad: unidadVal,
        precio_unitario_centavos: Number.isInteger(precioCentavos) ? precioCentavos : 0
      });
    });

    const totalCalculado = recalcCompraTotals();
    if (!hasError && totalCalculado <= 0) {
      applyFieldError(form, 'articulos', 'La suma total de los articulos debe ser mayor a 0.00.');
      hasError = true;
    }

    if (hasError) return;

    // Contrato 7.4.3: NO enviar monto_centavos, subtotal_centavos, tipo ni es_compra
    const payload = {
      naturaleza,
      fecha,
      comercio,
      descripcion: descripcionRaw ? descripcionRaw : null,
      categoria_id: categoriaId,
      metodo_pago: metodoPago,
      notas: notasRaw ? notasRaw : null,
      articulos: articulosPayload
    };

    if (metodoPago === 'CREDITO') {
      const cardVal = $('compra-card-id') ? $('compra-card-id').value : '';
      const cardId = parseInt(cardVal, 10);
      if (Number.isInteger(cardId) && cardId > 0) {
        payload.payment_method = 'CREDIT_CARD';
        payload.card_id = cardId;
        payload.installments = Math.max(1, Math.min(48, parseInt($('compra-installments').value, 10) || 1));
        payload.is_interest_exempt = Boolean($('compra-is-exempt') && $('compra-is-exempt').checked);
        payload.applied_interest_rate = payload.is_interest_exempt ? 0 : (parseFloat($('compra-applied-rate').value) || 0);
      }
    }

    const btn = $('btn-compra-submit');
    setButtonLoading(btn, true, 'Procesando compra...');
    try {
      if (id) {
        await global.ApiClient.updateCompra(id, payload);
        resetCompraForm();
        showBanner('compra-form-alert', 'success', 'Compra detallada actualizada correctamente.');
      } else {
        await global.ApiClient.createCompra(payload);
        resetCompraForm();
        showBanner('compra-form-alert', 'success', 'Compra detallada registrada exitosamente.');
      }
      await loadCompras(AppState.comprasMeta.pagina || 1);
      if (payload.card_id) {
        await fetchCreditCardsMaster();
      }
    } catch (err) {
      handleApiError(err, { formEl: form, alertContainerId: 'compra-form-alert' });
    } finally {
      setButtonLoading(btn, false);
    }
  }

  async function loadCompras(pagina = 1) {
    const query = {
      desde: $('filter-compra-desde').value || undefined,
      hasta: $('filter-compra-hasta').value || undefined,
      articulo: $('filter-compra-articulo').value.trim() || undefined,
      buscar: $('filter-compra-buscar').value.trim() || undefined,
      pagina,
      por_pagina: 50
    };

    try {
      const res = await global.ApiClient.getCompras(query);
      AppState.compras = Array.isArray(res.data) ? res.data : [];
      AppState.comprasMeta = res.meta || {
        pagina: 1,
        por_pagina: 50,
        total_registros: AppState.compras.length,
        total_paginas: 1
      };
      renderComprasTable();
    } catch (err) {
      AppState.compras = [];
      renderComprasTable();
      handleApiError(err, {
        alertContainerId: 'compra-form-alert',
        retryFn: () => loadCompras(pagina)
      });
    }
  }

  function renderComprasTable() {
    const tbody = $('compras-table-tbody');
    const meta = AppState.comprasMeta;
    if (!tbody) return;

    $('compras-pagination-info').textContent = `Total de compras: ${meta.total_registros || 0}`;
    $('compras-page-indicator').textContent = `Pagina ${meta.pagina || 1} de ${Math.max(1, meta.total_paginas || 1)}`;
    $('btn-compras-prev-page').disabled = (meta.pagina || 1) <= 1;
    $('btn-compras-next-page').disabled = (meta.pagina || 1) >= (meta.total_paginas || 1);

    if (AppState.compras.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="9">
            <div class="empty-state">
              <i class="fa-regular fa-folder-open empty-state-icon" aria-hidden="true"></i>
              <span class="empty-state-text">No hay compras detalladas registradas para este criterio.</span>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = AppState.compras.map((c) => {
      const isCardPurchase = (c.metodo_pago === 'CREDITO' || c.card_id) && c.installments;
      const cardInstallmentsBadge = isCardPurchase ? `
        <div style="margin-top: 4px; display: inline-flex; align-items: center; gap: 4px; flex-wrap: wrap;">
          <span class="badge badge-primary" title="${c.installments} cuotas">
            <i class="fa-solid fa-credit-card" aria-hidden="true"></i>
            <span>${c.installments} cuotas</span>
          </span>
          ${c.is_interest_exempt
            ? `<span class="badge badge-exempt" title="Compra exenta de intereses (0% interes)"><i class="fa-solid fa-shield-halved" aria-hidden="true"></i> [EXENTA 0%]</span>`
            : `<span class="badge badge-interest" title="Tasa mensual: ${Number(c.applied_interest_rate || 0).toFixed(2)}% M.V."><i class="fa-solid fa-percent" aria-hidden="true"></i> [CON INTERES]</span>`
          }
        </div>
      ` : '';

      return `
        <tr>
          <td class="tabular-nums">#${c.id}</td>
          <td class="tabular-nums">${escapeHtml(c.fecha)}</td>
          <td><strong>${escapeHtml(c.comercio || '-')}</strong></td>
          <td>${renderCategoryChip(c.categoria)}</td>
          <td>${escapeHtml(c.descripcion || '-')}</td>
          <td class="text-center">
            <span class="badge badge-primary tabular-nums">
              <i class="fa-solid fa-receipt" aria-hidden="true"></i>
              <span>${c.cantidad_articulos || 0}</span>
            </span>
          </td>
          <td>
            ${renderMetodoPagoBadge(c.metodo_pago)}
            ${cardInstallmentsBadge}
          </td>
          <td class="text-right amount-gasto">${formatCentavos(c.monto_centavos, 'GASTO')}</td>
          <td class="text-center">
            <div style="display: inline-flex; gap: 4px;">
              <button type="button" class="btn btn-secondary btn-sm btn-icon-only" data-action="view-ticket" data-id="${c.id}" title="Ver ticket detallado" aria-label="Ver ticket detallado">
                <i class="fa-solid fa-eye" aria-hidden="true"></i>
              </button>
              <button type="button" class="btn btn-secondary btn-sm btn-icon-only" data-action="edit-compra" data-id="${c.id}" title="Editar compra" aria-label="Editar compra">
                <i class="fa-solid fa-pen" aria-hidden="true"></i>
              </button>
              <button type="button" class="btn btn-danger btn-sm btn-icon-only" data-action="delete-compra" data-id="${c.id}" title="Eliminar compra" aria-label="Eliminar compra">
                <i class="fa-solid fa-trash-can" aria-hidden="true"></i>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  async function openTicketModal(compraId) {
    try {
      const res = await global.ApiClient.getCompra(compraId);
      const compra = res.data;
      if (!compra) return;
      AppState.activeTicketCompraId = compra.id;

      const isCard = (compra.metodo_pago === 'CREDITO' || compra.card_id) && compra.installments;
      const cardBreakdownHtml = isCard ? `
        <div style="grid-column: 1 / -1; margin-top: 8px; padding: 10px; background-color: var(--color-fondo); border: 1px solid var(--color-borde); border-radius: var(--radio-borde);">
          <div style="font-weight: 700; font-size: 12px; margin-bottom: 6px; display: flex; align-items: center; justify-content: space-between;">
            <span><i class="fa-solid fa-credit-card" aria-hidden="true" style="margin-right: 6px;"></i> Financiacion con Tarjeta de Credito</span>
            ${compra.is_interest_exempt
              ? '<span class="badge badge-exempt"><i class="fa-solid fa-shield-halved" aria-hidden="true"></i> [EXENTA 0%]</span>'
              : '<span class="badge badge-interest"><i class="fa-solid fa-percent" aria-hidden="true"></i> [CON INTERES]</span>'
            }
          </div>
          <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; font-size: 12px;">
            <div>Plazo: <strong>${compra.installments} cuotas</strong></div>
            <div>Tasa Aplicada: <strong>${Number(compra.applied_interest_rate || 0).toFixed(2)}% M.V.</strong></div>
            <div>Cuota Mensual: <strong class="tabular-nums">${formatCentavos(Math.round(Number(compra.monthly_installment_amount || 0) * 100), 'TOTAL_MONEDA')}</strong></div>
            <div>Total Intereses: <strong class="tabular-nums" style="color: ${compra.is_interest_exempt ? 'var(--color-exito)' : 'var(--color-peligro)'};">${formatCentavos(Math.round(Number(compra.total_interest_amount || 0) * 100), 'TOTAL_MONEDA')}</strong></div>
            <div style="grid-column: span 2;">Costo Final con Intereses: <strong class="tabular-nums">${formatCentavos(Math.round(Number(compra.total_with_interest || 0) * 100), 'TOTAL_MONEDA')}</strong></div>
          </div>
        </div>
      ` : '';

      $('modal-ticket-header').innerHTML = `
        <div><strong>Comprobante ID:</strong> <span class="tabular-nums">#${compra.id}</span></div>
        <div><strong>Fecha:</strong> <span class="tabular-nums">${escapeHtml(compra.fecha)}</span></div>
        <div><strong>Comercio:</strong> ${escapeHtml(compra.comercio || '-')}</div>
        <div><strong>Categoria:</strong> ${renderCategoryChip(compra.categoria)}</div>
        <div><strong>Metodo de Pago:</strong> ${renderMetodoPagoBadge(compra.metodo_pago)}</div>
        <div><strong>Naturaleza:</strong> ${renderNaturalezaBadge(compra.naturaleza)}</div>
        <div style="grid-column: 1 / -1;"><strong>Descripcion:</strong> ${escapeHtml(compra.descripcion || '-')}</div>
        ${compra.notas ? `<div style="grid-column: 1 / -1;"><strong>Notas:</strong> ${escapeHtml(compra.notas)}</div>` : ''}
        ${cardBreakdownHtml}
      `;

      const articulos = Array.isArray(compra.articulos) ? compra.articulos : [];
      $('modal-ticket-tbody').innerHTML = articulos.map((art, i) => {
        const uInfo = UNIDADES_ARTICULO[art.unidad] || UNIDADES_ARTICULO.UNIDAD;
        return `
          <tr>
            <td class="tabular-nums">${i + 1}</td>
            <td>${escapeHtml(art.articulo)}</td>
            <td class="text-right tabular-nums">${art.cantidad}</td>
            <td>${escapeHtml(uInfo.etiqueta)}</td>
            <td class="text-right tabular-nums">${formatCentavos(art.precio_unitario_centavos, 'NEUTRO')}</td>
            <td class="text-right tabular-nums">${formatCentavos(art.subtotal_centavos, 'NEUTRO')}</td>
          </tr>
        `;
      }).join('');

      $('modal-ticket-total').textContent = formatCentavos(compra.monto_centavos, 'TOTAL_MONEDA');
      $('modal-ticket').classList.add('open');
    } catch (err) {
      handleApiError(err);
    }
  }

  // =========================================================
  // 4. PANTALLA: GESTOR DE PRESUPUESTOS MENSUALES
  // =========================================================

  async function loadPresupuestos() {
    const { anio, mes } = AppState.presPeriod;
    if ($('pres-select-mes')) $('pres-select-mes').value = String(mes);
    if ($('pres-input-anio')) $('pres-input-anio').value = String(anio);
    if (!$('pres-id').value) {
      if ($('pres-anio')) $('pres-anio').value = String(anio);
      if ($('pres-mes')) $('pres-mes').value = String(mes);
    }

    const estadoFiltro = $('pres-filter-estado') ? $('pres-filter-estado').value : '';
    const query = { anio, mes };
    if (estadoFiltro) query.estado = estadoFiltro;

    try {
      const res = await global.ApiClient.getPresupuestos(query);
      AppState.presupuestosData = res.data;
      renderPresupuestosView(res.data);
    } catch (err) {
      renderPresupuestosView({
        anio,
        mes,
        total_limite_centavos: 0,
        total_gastado_centavos: 0,
        total_disponible_centavos: 0,
        porcentaje_usado_global: 0,
        presupuestos: []
      });
      handleApiError(err, {
        alertContainerId: 'pres-form-alert',
        retryFn: () => loadPresupuestos()
      });
    }
  }

  function renderPresupuestosView(data) {
    if (!data) return;
    const curr = global.AppConfig.getCurrency();
    const disp = Number(data.total_disponible_centavos || 0);
    const pctGlobal = Number(data.porcentaje_usado_global || 0);

    $('pres-kpi-limite').textContent = formatCentavos(data.total_limite_centavos || 0, 'TOTAL_MONEDA', curr);
    $('pres-kpi-gastado').textContent = formatCentavos(data.total_gastado_centavos || 0, 'GASTO', curr);

    const dispEl = $('pres-kpi-disponible');
    dispEl.textContent = formatCentavos(disp, 'BALANCE', curr);
    dispEl.className = `kpi-value ${disp >= 0 ? 'amount-balance-pos' : 'amount-balance-neg'}`;

    $('pres-kpi-porcentaje').textContent = `${pctGlobal.toFixed(2)} %`;
    const globalBar = $('pres-global-bar');
    if (globalBar) {
      let estGlobal = 'NORMAL';
      if (pctGlobal >= 100) estGlobal = 'EXCEDIDO';
      else if (pctGlobal >= 80) estGlobal = 'ADVERTENCIA';
      globalBar.className = `progress-fill estado-${estGlobal}`;
      globalBar.style.width = `${Math.min(100, Math.max(0, pctGlobal)).toFixed(2)}%`;
    }

    const list = Array.isArray(data.presupuestos) ? data.presupuestos : [];
    const tbody = $('pres-table-tbody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="7">
            <div class="empty-state">
              <i class="fa-regular fa-folder-open empty-state-icon" aria-hidden="true"></i>
              <span class="empty-state-text">No hay presupuestos configurados para este periodo.</span>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = list.map((p) => {
      const estInfo = ESTADOS_PRESUPUESTO[p.estado] || ESTADOS_PRESUPUESTO.NORMAL;
      const pctReal = Number(p.porcentaje_usado || 0);
      const pctVisual = Math.min(100, Math.max(0, pctReal));
      const dispCat = Number(p.disponible_centavos || 0);
      const badgeClass = p.estado === 'EXCEDIDO' ? 'badge-danger' : (p.estado === 'ADVERTENCIA' ? 'badge-warning' : 'badge-success');

      return `
        <tr>
          <td>
            ${renderCategoryChip(p.categoria)}
            ${p.notas ? `<div class="form-help">${escapeHtml(p.notas)}</div>` : ''}
          </td>
          <td>
            <span class="badge ${badgeClass}">
              <i class="${estInfo.icono}" aria-hidden="true"></i>
              <span>${p.estado}</span>
            </span>
          </td>
          <td>
            <div style="display: flex; flex-direction: column; gap: 4px;">
              <div class="progress-track">
                <div class="progress-fill estado-${p.estado}" style="width: ${pctVisual.toFixed(2)}%;"></div>
              </div>
              <span class="tabular-nums" style="font-size: 12px; font-weight: 600;">${pctReal.toFixed(2)} % utilizado</span>
            </div>
          </td>
          <td class="text-right tabular-nums">${formatCentavos(p.monto_limite_centavos, 'NEUTRO')}</td>
          <td class="text-right amount-gasto">${formatCentavos(p.gastado_centavos, 'GASTO')}</td>
          <td class="text-right ${dispCat >= 0 ? 'amount-balance-pos' : 'amount-balance-neg'}">${formatCentavos(dispCat, 'BALANCE')}</td>
          <td class="text-center">
            <div style="display: inline-flex; gap: 4px;">
              <button type="button" class="btn btn-secondary btn-sm btn-icon-only" data-action="edit-pres" data-id="${p.id}" title="Editar limite" aria-label="Editar limite">
                <i class="fa-solid fa-pen" aria-hidden="true"></i>
              </button>
              <button type="button" class="btn btn-danger btn-sm btn-icon-only" data-action="delete-pres" data-id="${p.id}" title="Eliminar presupuesto" aria-label="Eliminar presupuesto">
                <i class="fa-solid fa-trash-can" aria-hidden="true"></i>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  function resetPresupuestoForm() {
    const form = $('form-presupuesto');
    if (!form) return;
    clearFormErrors(form);
    hideBanner('pres-form-alert');
    $('pres-id').value = '';
    $('pres-categoria').disabled = false;
    $('pres-anio').disabled = false;
    $('pres-mes').disabled = false;
    $('pres-anio').value = String(AppState.presPeriod.anio);
    $('pres-mes').value = String(AppState.presPeriod.mes);
    $('pres-monto-limite').value = '';
    $('pres-notas').value = '';
    $('pres-form-title').innerHTML = `<i class="fa-solid fa-plus" aria-hidden="true"></i><span>Asignar Presupuesto Mensual</span>`;
    $('btn-pres-submit-label').textContent = 'Guardar Presupuesto';
    $('btn-pres-cancel-edit').style.display = 'none';
  }

  function startEditPresupuesto(pres) {
    if (!pres) return;
    const form = $('form-presupuesto');
    clearFormErrors(form);
    hideBanner('pres-form-alert');
    $('pres-id').value = String(pres.id);
    $('pres-categoria').value = String(pres.categoria_id);
    $('pres-categoria').disabled = true; // Inmutable en PUT segun 7.5.4
    $('pres-anio').value = String(pres.anio);
    $('pres-anio').disabled = true;
    $('pres-mes').value = String(pres.mes);
    $('pres-mes').disabled = true;
    $('pres-monto-limite').value = centavosToDecimalInput(pres.monto_limite_centavos);
    $('pres-notas').value = pres.notas || '';
    $('pres-form-title').innerHTML = `<i class="fa-solid fa-pen" aria-hidden="true"></i><span>Editar Presupuesto #${pres.id}</span>`;
    $('btn-pres-submit-label').textContent = 'Actualizar Presupuesto';
    $('btn-pres-cancel-edit').style.display = 'inline-flex';
  }

  async function submitPresupuestoForm(ev) {
    ev.preventDefault();
    const form = $('form-presupuesto');
    clearFormErrors(form);
    hideBanner('pres-form-alert');

    const id = $('pres-id').value.trim();
    const categoriaId = parseInt($('pres-categoria').value, 10);
    const anio = parseInt($('pres-anio').value, 10);
    const mes = parseInt($('pres-mes').value, 10);
    const montoLimiteCentavos = parseDecimalToCentavos($('pres-monto-limite').value);
    const notasRaw = $('pres-notas').value.trim();

    let hasError = false;
    if (!Number.isInteger(montoLimiteCentavos) || montoLimiteCentavos < 1 || montoLimiteCentavos > 99999999999) {
      applyFieldError(form, 'monto_limite_centavos', 'Ingrese un monto limite mayor a 0.00.');
      hasError = true;
    }
    if (!Number.isInteger(anio) || anio < 2000 || anio > 2100) {
      applyFieldError(form, 'anio', 'El anio debe estar entre 2000 y 2100.');
      hasError = true;
    }
    if (!Number.isInteger(mes) || mes < 1 || mes > 12) {
      applyFieldError(form, 'mes', 'Seleccione un mes valido (1 a 12).');
      hasError = true;
    }
    if (notasRaw && containsEmoji(notasRaw)) {
      applyFieldError(form, 'notas', 'Las notas no pueden contener emojis.');
      hasError = true;
    }
    if (hasError) return;

    const btn = $('btn-pres-submit');
    setButtonLoading(btn, true, 'Guardando...');
    try {
      if (id) {
        // Contrato 7.5.4: solo monto_limite_centavos y notas
        await global.ApiClient.updatePresupuesto(id, {
          monto_limite_centavos: montoLimiteCentavos,
          notas: notasRaw ? notasRaw : null
        });
        resetPresupuestoForm();
        showBanner('pres-form-alert', 'success', 'Presupuesto actualizado correctamente.');
      } else {
        await global.ApiClient.createPresupuesto({
          categoria_id: categoriaId,
          anio,
          mes,
          monto_limite_centavos: montoLimiteCentavos,
          notas: notasRaw ? notasRaw : null
        });
        resetPresupuestoForm();
        showBanner('pres-form-alert', 'success', 'Presupuesto mensual creado correctamente.');
      }
      await loadPresupuestos();
    } catch (err) {
      handleApiError(err, { formEl: form, alertContainerId: 'pres-form-alert' });
    } finally {
      setButtonLoading(btn, false);
    }
  }

  // =========================================================
  // 5. PANTALLA: CATEGORIAS CONTABLES
  // =========================================================

  function initCategoriaIconAndColorSelectors() {
    const colorSel = $('cat-color');
    if (colorSel) {
      colorSel.innerHTML = COLORES_CATEGORIAS_PERMITIDOS.map((c) => (
        `<option value="${c.hex}">${escapeHtml(c.nombre)} (${c.hex})</option>`
      )).join('');
      colorSel.value = '#4A5568';
    }

    const grid = $('cat-icon-selector');
    if (grid) {
      grid.innerHTML = ICONOS_CATEGORIAS_PERMITIDOS.map((iconClass) => `
        <button type="button" class="icon-option-btn ${iconClass === 'fa-solid fa-tag' ? 'selected' : ''}" data-icon="${iconClass}" title="${iconClass}" aria-label="${iconClass}">
          <i class="${iconClass}" aria-hidden="true"></i>
        </button>
      `).join('');

      grid.querySelectorAll('.icon-option-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
          selectCategoriaIcon(btn.dataset.icon);
        });
      });
    }
  }

  function selectCategoriaIcon(iconClass) {
    const clean = resolveCategoryIcon(iconClass);
    $('cat-icono').value = clean;
    $('cat-icono-preview-text').textContent = clean;
    document.querySelectorAll('#cat-icon-selector .icon-option-btn').forEach((b) => {
      b.classList.toggle('selected', b.dataset.icon === clean);
    });
  }

  async function loadCategoriasView() {
    const incluirInactivas = $('cat-filter-inactivas') && $('cat-filter-inactivas').checked ? 'true' : 'false';
    try {
      const res = await global.ApiClient.getCategorias({ incluir_inactivas: incluirInactivas });
      const list = Array.isArray(res.data) ? res.data : [];
      if (incluirInactivas === 'true' && list.length > 0) {
        AppState.categorias = list;
        populateAllCategorySelects();
      }
      renderCategoriasTables(list);
    } catch (err) {
      renderCategoriasTables(AppState.categorias);
      handleApiError(err, {
        alertContainerId: 'cat-form-alert',
        retryFn: () => loadCategoriasView()
      });
    }
  }

  function renderCategoriasTables(list) {
    const ingresos = list.filter((c) => c.tipo === 'INGRESO');
    const gastos = list.filter((c) => c.tipo === 'GASTO');

    const renderRows = (arr) => {
      if (arr.length === 0) {
        return `<tr><td colspan="6" class="text-center" style="color: var(--color-texto-secundario);">Sin categorias para mostrar.</td></tr>`;
      }
      return arr.map((cat) => `
        <tr>
          <td class="tabular-nums">#${cat.id}</td>
          <td>${renderCategoryChip(cat)}</td>
          <td><code>${escapeHtml(cat.icono)}</code></td>
          <td><span class="badge badge-neutral tabular-nums">${escapeHtml(cat.color)}</span></td>
          <td>
            ${cat.activa !== false
              ? `<span class="badge badge-success"><i class="fa-solid fa-circle-check" aria-hidden="true"></i> Activa</span>`
              : `<span class="badge badge-neutral"><i class="fa-solid fa-eye-slash" aria-hidden="true"></i> Inactiva</span>`
            }
          </td>
          <td class="text-center">
            <div style="display: inline-flex; gap: 4px;">
              <button type="button" class="btn btn-secondary btn-sm btn-icon-only" data-action="edit-cat" data-id="${cat.id}" title="Editar categoria" aria-label="Editar categoria">
                <i class="fa-solid fa-pen" aria-hidden="true"></i>
              </button>
              <button type="button" class="btn btn-danger btn-sm btn-icon-only" data-action="delete-cat" data-id="${cat.id}" title="Eliminar categoria" aria-label="Eliminar categoria">
                <i class="fa-solid fa-trash-can" aria-hidden="true"></i>
              </button>
            </div>
          </td>
        </tr>
      `).join('');
    };

    if ($('cat-ingresos-tbody')) $('cat-ingresos-tbody').innerHTML = renderRows(ingresos);
    if ($('cat-gastos-tbody')) $('cat-gastos-tbody').innerHTML = renderRows(gastos);
  }

  function resetCategoriaForm() {
    const form = $('form-categoria');
    if (!form) return;
    clearFormErrors(form);
    hideBanner('cat-form-alert');
    $('cat-id').value = '';
    $('cat-nombre').value = '';
    $('cat-tipo').value = 'GASTO';
    $('cat-tipo').disabled = false;
    $('cat-color').value = '#4A5568';
    $('cat-activa').value = 'true';
    $('cat-activa-group').style.display = 'none';
    selectCategoriaIcon('fa-solid fa-tag');
    $('cat-form-title').innerHTML = `<i class="fa-solid fa-plus" aria-hidden="true"></i><span>Nueva Categoria</span>`;
    $('btn-cat-submit-label').textContent = 'Crear Categoria';
    $('btn-cat-cancel-edit').style.display = 'none';
  }

  function startEditCategoria(cat) {
    if (!cat) return;
    const form = $('form-categoria');
    clearFormErrors(form);
    hideBanner('cat-form-alert');
    $('cat-id').value = String(cat.id);
    $('cat-nombre').value = cat.nombre;
    $('cat-tipo').value = cat.tipo;
    $('cat-tipo').disabled = true; // Inmutable en PUT segun 7.2.3
    $('cat-color').value = cat.color || '#4A5568';
    $('cat-activa').value = cat.activa !== false ? 'true' : 'false';
    $('cat-activa-group').style.display = 'flex';
    selectCategoriaIcon(cat.icono);
    $('cat-form-title').innerHTML = `<i class="fa-solid fa-pen" aria-hidden="true"></i><span>Editar Categoria #${cat.id}</span>`;
    $('btn-cat-submit-label').textContent = 'Guardar Cambios';
    $('btn-cat-cancel-edit').style.display = 'inline-flex';
  }

  async function submitCategoriaForm(ev) {
    ev.preventDefault();
    const form = $('form-categoria');
    clearFormErrors(form);
    hideBanner('cat-form-alert');

    const id = $('cat-id').value.trim();
    const nombre = $('cat-nombre').value.trim();
    const tipo = $('cat-tipo').value;
    const color = $('cat-color').value;
    const icono = $('cat-icono').value.trim();
    const activa = $('cat-activa').value === 'true';

    let hasError = false;
    if (!nombre || nombre.length > 60) {
      applyFieldError(form, 'nombre', 'El nombre es obligatorio (1 a 60 caracteres).');
      hasError = true;
    } else if (containsEmoji(nombre)) {
      applyFieldError(form, 'nombre', 'El nombre no puede contener emojis.');
      hasError = true;
    }
    if (!icono.startsWith('fa-solid fa-') && !icono.startsWith('fa-regular fa-') && !icono.startsWith('fa-brands fa-')) {
      applyFieldError(form, 'icono', 'Seleccione un icono FontAwesome 6 valido.');
      hasError = true;
    }
    if (hasError) return;

    const btn = $('btn-cat-submit');
    setButtonLoading(btn, true, 'Guardando...');
    try {
      if (id) {
        // Contrato 7.2.3: nombre, icono, color, activa
        await global.ApiClient.updateCategoria(id, { nombre, icono, color, activa });
        resetCategoriaForm();
        showBanner('cat-form-alert', 'success', 'Categoria actualizada correctamente.');
      } else {
        await global.ApiClient.createCategoria({ nombre, tipo, icono, color });
        resetCategoriaForm();
        showBanner('cat-form-alert', 'success', 'Categoria creada correctamente.');
      }
      await fetchCategoriasMaster();
      await loadCategoriasView();
    } catch (err) {
      handleApiError(err, { formEl: form, alertContainerId: 'cat-form-alert' });
    } finally {
      setButtonLoading(btn, false);
    }
  }

  async function handleDeleteCategoria(cat) {
    if (!cat) return;
    try {
      await global.ApiClient.deleteCategoria(cat.id);
      showBanner('cat-form-alert', 'success', 'Categoria eliminada correctamente.');
      await fetchCategoriasMaster();
      await loadCategoriasView();
    } catch (err) {
      // Regla 7.2.4: Si 409 CATEGORIA_EN_USO, sugerir desactivarla con PUT activa: false
      if (err.status === 409 || err.codigo === 'CATEGORIA_EN_USO') {
        showBanner(
          'cat-form-alert',
          'warning',
          `${err.mensaje || 'La categoria tiene transacciones o presupuestos asociados.'} Puede desactivarla haciendo clic en Reintentar como Inactiva.`,
          async () => {
            await global.ApiClient.updateCategoria(cat.id, {
              nombre: cat.nombre,
              icono: cat.icono,
              color: cat.color || '#4A5568',
              activa: false
            });
            showBanner('cat-form-alert', 'success', `Categoria "${cat.nombre}" desactivada correctamente.`);
            await fetchCategoriasMaster();
            await loadCategoriasView();
          }
        );
        return;
      }
      handleApiError(err, { alertContainerId: 'cat-form-alert' });
    }
  }

  // =========================================================
  // GESTION DE TARJETAS DE CREDITO Y SIMULADOR DE CUOTAS
  // =========================================================

  const simulationTimers = { tx: null, compra: null };

  async function fetchCreditCardsMaster() {
    try {
      const res = await global.ApiClient.getCreditCards();
      AppState.creditCards = Array.isArray(res.data) ? res.data : [];
      populateCreditCardSelects();
    } catch (_err) {
      AppState.creditCards = [];
      populateCreditCardSelects();
    }
  }

  function populateCreditCardSelects() {
    const selects = [$('tx-card-id'), $('compra-card-id')];
    const cards = AppState.creditCards || [];

    selects.forEach((sel) => {
      if (!sel) return;
      const currentVal = sel.value;
      let html = '<option value="">-- Seleccionar Tarjeta de Credito --</option>';
      cards.forEach((c) => {
        const availCents = Math.round(Number(c.available_limit || 0) * 100);
        const availFmt = formatCentavos(availCents, 'TOTAL_MONEDA');
        html += `<option value="${c.id}" data-rate="${c.default_interest_rate || 0}">${escapeHtml(c.name)} - ${escapeHtml(c.bank)} (Disp: ${availFmt})</option>`;
      });
      sel.innerHTML = html;
      if (currentVal && cards.some((c) => String(c.id) === String(currentVal))) {
        sel.value = currentVal;
      }
    });
  }

  function runLiveSimulation(context) {
    let amount = 0;
    if (context === 'tx') {
      const cents = parseDecimalToCentavos($('tx-monto') ? $('tx-monto').value : 0);
      amount = Number.isFinite(cents) && cents > 0 ? cents / 100 : 0;
    } else {
      const cents = recalcCompraTotals();
      amount = Number.isFinite(cents) && cents > 0 ? cents / 100 : 0;
    }

    const nInput = $(`${context}-installments`);
    const n = Math.max(1, Math.min(48, parseInt(nInput ? nInput.value : 1, 10) || 1));
    const isExempt = Boolean($(`${context}-is-exempt`) && $(`${context}-is-exempt`).checked);
    const rateInput = $(`${context}-applied-rate`);
    const rate = isExempt ? 0 : Math.max(0, Math.min(100, parseFloat(rateInput ? rateInput.value : 0) || 0));

    // Calculo instantaneo con formula local
    const local = global.AppUtils.simulateCreditPurchase(amount, n, rate, isExempt);
    updateSimulationUi(context, local);

    // Validacion y sincronizacion con el endpoint /api/v1/credit-cards/simulate
    if (amount > 0) {
      clearTimeout(simulationTimers[context]);
      simulationTimers[context] = setTimeout(async () => {
        try {
          const res = await global.ApiClient.simulateCreditCard({
            amount,
            installments: n,
            interest_rate: isExempt ? 0 : rate,
            is_interest_exempt: isExempt
          });
          if (res && res.data) {
            updateSimulationUi(context, res.data);
          }
        } catch (_err) {
          // El calculo local ya esta visible
        }
      }, 250);
    }
  }

  function updateSimulationUi(context, data) {
    const cuotaEl = $(`${context}-sim-cuota`);
    const intEl = $(`${context}-sim-intereses`);
    const totEl = $(`${context}-sim-total`);
    const tagEl = $(`${context}-sim-estado-tag`);

    const cuotaCents = Math.round(Number(data.monthly_installment_amount || 0) * 100);
    const intCents = Math.round(Number(data.total_interest_amount || 0) * 100);
    const totCents = Math.round(Number(data.total_with_interest || 0) * 100);

    if (cuotaEl) cuotaEl.textContent = formatCentavos(cuotaCents, 'TOTAL_MONEDA');
    if (totEl) totEl.textContent = formatCentavos(totCents, 'TOTAL_MONEDA');

    const isExempt = data.is_interest_exempt === true || intCents === 0;
    if (intEl) {
      if (isExempt) {
        intEl.style.color = 'var(--color-exito)';
        intEl.textContent = `${formatCentavos(0, 'TOTAL_MONEDA')} (Exenta)`;
      } else {
        intEl.style.color = 'var(--color-peligro)';
        intEl.textContent = `+ ${formatCentavos(intCents, 'TOTAL_MONEDA')}`;
      }
    }
    if (tagEl) {
      if (isExempt) {
        tagEl.className = 'badge badge-success';
        tagEl.innerHTML = '<i class="fa-solid fa-shield-halved" aria-hidden="true"></i> [EXENTA 0%]';
      } else {
        tagEl.className = 'badge badge-warning';
        tagEl.innerHTML = '<i class="fa-solid fa-percent" aria-hidden="true"></i> [CON INTERES]';
      }
    }
  }

  async function loadCreditCardsView() {
    try {
      await fetchCreditCardsMaster();
      renderCreditCardsKpis();
      renderCreditCardsCards();
      renderCreditCardsTable();
    } catch (err) {
      handleApiError(err, { alertContainerId: 'tarjeta-form-alert' });
    }
  }

  function renderCreditCardsKpis() {
    const cards = AppState.creditCards || [];
    let totalLimitCents = 0;
    let totalAvailCents = 0;

    cards.forEach((c) => {
      totalLimitCents += Math.round(Number(c.credit_limit || 0) * 100);
      totalAvailCents += Math.round(Number(c.available_limit || 0) * 100);
    });

    const totalUsedCents = Math.max(0, totalLimitCents - totalAvailCents);
    const usedPct = totalLimitCents > 0 ? (totalUsedCents / totalLimitCents) * 100 : 0;
    const availPct = totalLimitCents > 0 ? (totalAvailCents / totalLimitCents) * 100 : 100;

    if ($('kpi-card-total-limit')) $('kpi-card-total-limit').textContent = formatCentavos(totalLimitCents, 'TOTAL_MONEDA');
    if ($('kpi-card-count')) $('kpi-card-count').textContent = `${cards.length} tarjetas`;

    if ($('kpi-card-used')) $('kpi-card-used').textContent = formatCentavos(totalUsedCents, 'TOTAL_MONEDA');
    if ($('kpi-card-used-pct')) $('kpi-card-used-pct').textContent = `${usedPct.toFixed(2)} %`;

    if ($('kpi-card-available')) $('kpi-card-available').textContent = formatCentavos(totalAvailCents, 'TOTAL_MONEDA');
    if ($('kpi-card-avail-pct')) $('kpi-card-avail-pct').textContent = `${availPct.toFixed(2)} %`;

    if (cards.length > 0) {
      const sorted = [...cards].sort((a, b) => a.payment_day - b.payment_day);
      const nextCard = sorted[0];
      if ($('kpi-card-next-payment')) $('kpi-card-next-payment').textContent = `Dia ${nextCard.payment_day}`;
      if ($('kpi-card-cutoff-info')) $('kpi-card-cutoff-info').textContent = `${escapeHtml(nextCard.name)} (Corte dia ${nextCard.cutoff_day})`;
    } else {
      if ($('kpi-card-next-payment')) $('kpi-card-next-payment').textContent = 'Dia -';
      if ($('kpi-card-cutoff-info')) $('kpi-card-cutoff-info').textContent = 'Sin tarjetas registradas';
    }
  }

  function renderCreditCardsCards() {
    const container = $('tarjetas-cards-container');
    if (!container) return;
    const cards = AppState.creditCards || [];

    if (cards.length === 0) {
      container.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1; background-color: var(--color-superficie); padding: 24px; border: 1px solid var(--color-borde); border-radius: var(--radio-borde); text-align: center;">
          <i class="fa-solid fa-credit-card empty-state-icon" style="font-size: 32px; color: var(--color-texto-secundario); margin-bottom: 8px;" aria-hidden="true"></i>
          <span class="empty-state-text" style="display: block; color: var(--color-texto-secundario);">No hay tarjetas de credito registradas. Utilice el formulario para crear la primera tarjeta.</span>
        </div>
      `;
      return;
    }

    container.innerHTML = cards.map((c) => {
      const limitCents = Math.round(Number(c.credit_limit || 0) * 100);
      const availCents = Math.round(Number(c.available_limit || 0) * 100);
      const usedCents = Math.max(0, limitCents - availCents);
      const usedPct = limitCents > 0 ? (usedCents / limitCents) * 100 : 0;

      let fillClass = 'estado-NORMAL';
      if (usedPct >= 80) fillClass = 'estado-EXCEDIDO';
      else if (usedPct >= 50) fillClass = 'estado-ADVERTENCIA';

      return `
        <div class="credit-card-visual">
          <div class="credit-card-header">
            <div>
              <div class="credit-card-bank">${escapeHtml(c.bank)}</div>
              <div class="credit-card-name">
                <i class="fa-solid fa-credit-card" aria-hidden="true" style="color: var(--color-primario-hover);"></i>
                <span>${escapeHtml(c.name)}</span>
              </div>
            </div>
            <div style="display: flex; gap: 4px;">
              <button type="button" class="btn btn-secondary btn-sm btn-icon-only" data-action="edit-card" data-id="${c.id}" title="Editar tarjeta" aria-label="Editar tarjeta">
                <i class="fa-solid fa-pen" aria-hidden="true"></i>
              </button>
              <button type="button" class="btn btn-danger btn-sm btn-icon-only" data-action="delete-card" data-id="${c.id}" title="Eliminar tarjeta" aria-label="Eliminar tarjeta">
                <i class="fa-solid fa-trash-can" aria-hidden="true"></i>
              </button>
            </div>
          </div>

          <div class="credit-card-metrics">
            <div>
              <div class="credit-card-metric-title">Cupo Total</div>
              <div class="credit-card-metric-num">${formatCentavos(limitCents, 'TOTAL_MONEDA')}</div>
            </div>
            <div>
              <div class="credit-card-metric-title">Cupo Ocupado</div>
              <div class="credit-card-metric-num amount-gasto">${formatCentavos(usedCents, 'TOTAL_MONEDA')}</div>
            </div>
            <div>
              <div class="credit-card-metric-title">Disponible</div>
              <div class="credit-card-metric-num amount-ingreso">${formatCentavos(availCents, 'TOTAL_MONEDA')}</div>
            </div>
          </div>

          <div>
            <div style="display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 4px; color: var(--color-texto-secundario);">
              <span>Utilizacion del Cupo</span>
              <span class="tabular-nums font-bold">${usedPct.toFixed(1)}%</span>
            </div>
            <div class="progress-track">
              <div class="progress-fill ${fillClass}" style="width: ${Math.min(100, Math.max(0, usedPct))}%;"></div>
            </div>
          </div>

          <div class="credit-card-footer">
            <span><i class="fa-solid fa-calendar-days" aria-hidden="true" style="margin-right: 4px;"></i> Corte: <strong>Dia ${c.cutoff_day}</strong> | Pago: <strong>Dia ${c.payment_day}</strong></span>
            <span><i class="fa-solid fa-percent" aria-hidden="true" style="margin-right: 4px;"></i> <strong>${Number(c.default_interest_rate || 0).toFixed(2)}% M.V.</strong></span>
          </div>
        </div>
      `;
    }).join('');
  }

  function renderCreditCardsTable() {
    const tbody = $('tarjetas-table-tbody');
    if (!tbody) return;
    const cards = AppState.creditCards || [];

    if (cards.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="8">
            <div class="empty-state">
              <i class="fa-solid fa-credit-card empty-state-icon" aria-hidden="true"></i>
              <span class="empty-state-text">No hay tarjetas de credito para mostrar.</span>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = cards.map((c) => {
      const limitCents = Math.round(Number(c.credit_limit || 0) * 100);
      const availCents = Math.round(Number(c.available_limit || 0) * 100);
      const usedCents = Math.max(0, limitCents - availCents);

      return `
        <tr>
          <td class="tabular-nums">#${c.id}</td>
          <td>
            <strong>${escapeHtml(c.name)}</strong>
            <div style="font-size: 11px; color: var(--color-texto-secundario);">${escapeHtml(c.bank)}</div>
          </td>
          <td class="text-right tabular-nums">${formatCentavos(limitCents, 'TOTAL_MONEDA')}</td>
          <td class="text-right tabular-nums amount-gasto">${formatCentavos(usedCents, 'TOTAL_MONEDA')}</td>
          <td class="text-right tabular-nums amount-ingreso">${formatCentavos(availCents, 'TOTAL_MONEDA')}</td>
          <td class="text-center">
            <span class="badge badge-neutral">
              <i class="fa-solid fa-calendar-days" aria-hidden="true"></i>
              <span>Corte: ${c.cutoff_day} / Pago: ${c.payment_day}</span>
            </span>
          </td>
          <td class="text-right tabular-nums">
            <span class="badge badge-primary">
              <i class="fa-solid fa-percent" aria-hidden="true"></i>
              <span>${Number(c.default_interest_rate || 0).toFixed(2)}% M.V.</span>
            </span>
          </td>
          <td class="text-center">
            <div style="display: inline-flex; gap: 4px;">
              <button type="button" class="btn btn-secondary btn-sm btn-icon-only" data-action="edit-card" data-id="${c.id}" title="Editar tarjeta" aria-label="Editar tarjeta">
                <i class="fa-solid fa-pen" aria-hidden="true"></i>
              </button>
              <button type="button" class="btn btn-danger btn-sm btn-icon-only" data-action="delete-card" data-id="${c.id}" title="Eliminar tarjeta" aria-label="Eliminar tarjeta">
                <i class="fa-solid fa-trash-can" aria-hidden="true"></i>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  function startEditCreditCard(card) {
    if (!card) return;
    const form = $('form-tarjeta');
    clearFormErrors(form);
    hideBanner('tarjeta-form-alert');
    $('tarjeta-id').value = String(card.id);
    $('tarjeta-name').value = card.name || '';
    $('tarjeta-bank').value = card.bank || '';
    $('tarjeta-limit').value = Number(card.credit_limit || 0).toFixed(2);
    $('tarjeta-rate').value = Number(card.default_interest_rate || 0).toFixed(2);
    $('tarjeta-cutoff').value = String(card.cutoff_day || 15);
    $('tarjeta-payment').value = String(card.payment_day || 5);
    $('tarjeta-form-title').innerHTML = `<i class="fa-solid fa-pen" aria-hidden="true"></i><span>Editar Tarjeta #${card.id}</span>`;
    $('btn-tarjeta-submit-label').textContent = 'Guardar Cambios';
    $('btn-tarjeta-cancel-edit').style.display = 'inline-flex';
    form.scrollIntoView({ behavior: 'smooth' });
  }

  function resetTarjetaForm() {
    const form = $('form-tarjeta');
    if (!form) return;
    clearFormErrors(form);
    hideBanner('tarjeta-form-alert');
    $('tarjeta-id').value = '';
    $('tarjeta-name').value = '';
    $('tarjeta-bank').value = '';
    $('tarjeta-limit').value = '';
    $('tarjeta-rate').value = '';
    $('tarjeta-cutoff').value = '';
    $('tarjeta-payment').value = '';
    $('tarjeta-form-title').innerHTML = `<i class="fa-solid fa-plus" aria-hidden="true"></i><span>Registrar Nueva Tarjeta</span>`;
    $('btn-tarjeta-submit-label').textContent = 'Guardar Tarjeta';
    $('btn-tarjeta-cancel-edit').style.display = 'none';
  }

  async function submitTarjetaForm(ev) {
    ev.preventDefault();
    const form = $('form-tarjeta');
    clearFormErrors(form);
    hideBanner('tarjeta-form-alert');

    const id = $('tarjeta-id').value.trim();
    const name = $('tarjeta-name').value.trim();
    const bank = $('tarjeta-bank').value.trim();
    const limitNum = parseFloat($('tarjeta-limit').value);
    const rateNum = parseFloat($('tarjeta-rate').value);
    const cutoffNum = parseInt($('tarjeta-cutoff').value, 10);
    const paymentNum = parseInt($('tarjeta-payment').value, 10);

    let hasError = false;
    if (!name || name.length > 80) {
      applyFieldError(form, 'name', 'El nombre es obligatorio (1 a 80 caracteres).');
      hasError = true;
    } else if (containsEmoji(name)) {
      applyFieldError(form, 'name', 'El nombre no puede contener emojis.');
      hasError = true;
    }

    if (!bank || bank.length > 80) {
      applyFieldError(form, 'bank', 'El banco es obligatorio (1 a 80 caracteres).');
      hasError = true;
    } else if (containsEmoji(bank)) {
      applyFieldError(form, 'bank', 'El banco no puede contener emojis.');
      hasError = true;
    }

    if (!Number.isFinite(limitNum) || limitNum <= 0 || limitNum > 999999999999) {
      applyFieldError(form, 'credit_limit', 'El cupo debe ser un numero mayor a 0.');
      hasError = true;
    }

    if (!Number.isFinite(rateNum) || rateNum < 0 || rateNum > 100) {
      applyFieldError(form, 'default_interest_rate', 'La tasa debe ser un numero entre 0 y 100.');
      hasError = true;
    }

    if (!Number.isInteger(cutoffNum) || cutoffNum < 1 || cutoffNum > 31) {
      applyFieldError(form, 'cutoff_day', 'El dia de corte debe estar entre 1 y 31.');
      hasError = true;
    }

    if (!Number.isInteger(paymentNum) || paymentNum < 1 || paymentNum > 31) {
      applyFieldError(form, 'payment_day', 'El dia limite de pago debe estar entre 1 y 31.');
      hasError = true;
    }

    if (hasError) return;

    // JSON exacto requerido: { name, bank, credit_limit, default_interest_rate, cutoff_day, payment_day }
    const payload = {
      name,
      bank,
      credit_limit: Number(limitNum.toFixed(2)),
      default_interest_rate: Number(rateNum.toFixed(2)),
      cutoff_day: cutoffNum,
      payment_day: paymentNum
    };

    const btn = $('btn-tarjeta-submit');
    setButtonLoading(btn, true, 'Guardando...');

    try {
      if (id) {
        await global.ApiClient.updateCreditCard(id, payload);
        showBanner('tarjeta-form-alert', 'success', 'Tarjeta de credito actualizada exitosamente.');
      } else {
        await global.ApiClient.createCreditCard(payload);
        showBanner('tarjeta-form-alert', 'success', 'Tarjeta de credito registrada exitosamente.');
      }
      resetTarjetaForm();
      await loadCreditCardsView();
    } catch (err) {
      handleApiError(err, { formEl: form, alertContainerId: 'tarjeta-form-alert' });
    } finally {
      setButtonLoading(btn, false);
    }
  }

  function handleDeleteCreditCard(id) {
    const card = AppState.creditCards.find((c) => c.id === id);
    const cardName = card ? `"${card.name} (${card.bank})"` : `#${id}`;
    openConfirmModal(
      `Esta accion eliminara la tarjeta de credito ${cardName}. Desea continuar?`,
      async () => {
        try {
          await global.ApiClient.deleteCreditCard(id);
          showBanner('tarjeta-form-alert', 'success', 'Tarjeta de credito eliminada exitosamente.');
          await loadCreditCardsView();
        } catch (err) {
          if (err && (err.status === 409 || err.codigo === 'TARJETA_EN_USO')) {
            showBanner('tarjeta-form-alert', 'danger', 'No se puede eliminar la tarjeta porque tiene transacciones contables asociadas.');
          } else {
            handleApiError(err, { alertContainerId: 'tarjeta-form-alert' });
          }
        }
      }
    );
  }

  // =========================================================
  // 6. PANTALLA: ASESOR FINANCIERO IA (GEMINI + DICTADO EN TEXTO LIBRE)
  // =========================================================

  async function submitIaAnalyzeForm(ev) {
    ev.preventDefault();
    const form = $('form-ia-analyze');
    clearFormErrors(form);
    hideBanner('ia-form-alert');

    const tipoAnalisis = $('ia-tipo-analisis').value;
    const anio = parseInt($('ia-anio').value, 10);
    const mes = parseInt($('ia-mes').value, 10);
    const preguntaRaw = $('ia-pregunta').value.trim();

    let hasError = false;
    if (!Number.isInteger(anio) || anio < 2000 || anio > 2100) {
      applyFieldError(form, 'anio', 'Anio invalido (2000 a 2100).');
      hasError = true;
    }
    if (!Number.isInteger(mes) || mes < 1 || mes > 12) {
      applyFieldError(form, 'mes', 'Mes invalido (1 a 12).');
      hasError = true;
    }
    if (tipoAnalisis === 'PREGUNTA_LIBRE') {
      if (preguntaRaw.length < 5 || preguntaRaw.length > 500) {
        applyFieldError(form, 'pregunta', 'La pregunta debe tener entre 5 y 500 caracteres.');
        hasError = true;
      } else if (containsEmoji(preguntaRaw)) {
        applyFieldError(form, 'pregunta', 'La pregunta no puede contener emojis.');
        hasError = true;
      }
    }
    if (hasError) return;

    // Contrato 8.2: pregunta debe ser null cuando no es PREGUNTA_LIBRE
    const payload = {
      tipo_analisis: tipoAnalisis,
      anio,
      mes,
      pregunta: tipoAnalisis === 'PREGUNTA_LIBRE' ? preguntaRaw : null
    };

    const btn = $('btn-ia-submit');
    setButtonLoading(btn, true, 'Consultando a Gemini (hasta 90s)...');
    try {
      const res = await global.ApiClient.analyzeAi(payload);
      renderIaAnalyzeResult(res.data);
    } catch (err) {
      handleApiError(err, { formEl: form, alertContainerId: 'ia-form-alert' });
    } finally {
      setButtonLoading(btn, false);
    }
  }

  function renderIaAnalyzeResult(data) {
    if (!data) return;
    $('ia-result-container').style.display = 'block';

    const score = Math.min(100, Math.max(0, Number(data.puntuacion_salud_financiera || 0)));
    $('ia-score-value').textContent = `${score} / 100`;
    const scoreBar = $('ia-score-bar');
    if (scoreBar) {
      let estado = 'NORMAL';
      if (score < 50) estado = 'EXCEDIDO';
      else if (score < 75) estado = 'ADVERTENCIA';
      scoreBar.className = `progress-fill estado-${estado}`;
      scoreBar.style.width = `${score}%`;
    }

    $('ia-meta-info').textContent = `${data.modelo || 'gemini-2.5-flash'} | ${data.generado_en || ''}`;
    $('ia-resumen-text').textContent = stripEmojis(data.resumen || 'Sin resumen disponible.');

    // Alertas
    const alertas = Array.isArray(data.alertas) ? data.alertas : [];
    const alertasList = $('ia-alertas-list');
    if (alertas.length === 0) {
      alertasList.innerHTML = `<div class="alert-banner alert-success"><span><i class="fa-solid fa-circle-check" aria-hidden="true"></i> No se detectaron alertas criticas en el periodo.</span></div>`;
    } else {
      alertasList.innerHTML = alertas.map((al) => `
        <div class="alert-banner alert-warning">
          <div class="alert-banner-content">
            <i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i>
            <div>
              <strong>${escapeHtml(al.codigo || 'ALERTA')}:</strong>
              <span>${escapeHtml(stripEmojis(al.mensaje || ''))}</span>
            </div>
          </div>
        </div>
      `).join('');
    }

    // Recomendaciones
    const recs = Array.isArray(data.recomendaciones) ? data.recomendaciones : [];
    const recsList = $('ia-recomendaciones-list');
    if (recs.length === 0) {
      recsList.innerHTML = `<div class="form-help">No se emitieron recomendaciones adicionales.</div>`;
    } else {
      recsList.innerHTML = recs.map((r) => {
        const prio = PRIORIDADES_IA[r.prioridad] || PRIORIDADES_IA.MEDIA;
        const ahorro = r.ahorro_estimado_centavos
          ? `<span class="badge badge-success tabular-nums">Ahorro est.: ${formatCentavos(r.ahorro_estimado_centavos, 'TOTAL_MONEDA')}</span>`
          : '';
        return `
          <div class="card" style="padding: 12px;">
            <div style="display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 6px;">
              <strong style="color: var(--color-primario);">
                <i class="fa-solid fa-lightbulb" aria-hidden="true"></i>
                <span>${escapeHtml(stripEmojis(r.titulo || 'Recomendacion'))}</span>
              </strong>
              <div style="display: flex; gap: 6px;">
                ${ahorro}
                <span class="badge ${prio.clase}">
                  <i class="${prio.icono}" aria-hidden="true"></i>
                  <span>${escapeHtml(prio.etiqueta)}</span>
                </span>
              </div>
            </div>
            <p style="font-size: 13px; color: var(--color-texto-secundario);">${escapeHtml(stripEmojis(r.detalle || ''))}</p>
          </div>
        `;
      }).join('');
    }
  }

  // Parser inteligente de Compras Dictadas en Texto Libre
  function parseFreeTextPurchase(rawText) {
    const clean = stripEmojis(String(rawText || '').trim());
    if (!clean) return { comercio: null, metodo: null, articulos: [] };

    // Detectar comercio si dice "en [Comercio] con/:"
    let comercio = null;
    const matchComercio = /(?:compra\s+en|en)\s+([A-Za-z0-9\s.\-_]{2,40}?)(?:\s+con\s+|\s*[:;,]|\s+hoy)/i.exec(clean);
    if (matchComercio && matchComercio[1]) {
      comercio = matchComercio[1].trim();
    }

    // Detectar metodo de pago
    let metodo = null;
    if (/efectivo/i.test(clean)) metodo = 'EFECTIVO';
    else if (/credito/i.test(clean)) metodo = 'CREDITO';
    else if (/transferencia/i.test(clean)) metodo = 'TRANSFERENCIA';
    else if (/debito/i.test(clean)) metodo = 'DEBITO';

    // Extraer segmento de articulos (despues de ':' si existe)
    const bodyPart = clean.includes(':') ? clean.split(':').slice(1).join(':') : clean;
    const segments = bodyPart
      .split(/[,;\n]+/)
      .map((s) => s.trim())
      .filter(Boolean);

    const unidadMap = {
      kg: 'KG', kilo: 'KG', kilos: 'KG',
      g: 'G', gr: 'G', gramos: 'G',
      l: 'L', lt: 'L', litro: 'L', litros: 'L',
      ml: 'ML',
      paq: 'PAQUETE', paquete: 'PAQUETE', paquetes: 'PAQUETE',
      und: 'UNIDAD', unidad: 'UNIDAD', unidades: 'UNIDAD'
    };

    const articulos = [];
    for (const seg of segments) {
      // Patron: [cantidad] [unidad opcional] [nombre articulo] (a | por | $ | COP | USD) [precio]
      const re = /^(?:(\d+(?:\.\d{1,3})?)\s+)?(?:(kg|kilos?|g|gr|gramos|l|lt|litros?|ml|paq|paquetes?|und|unidad(?:es)?)\s+(?:de\s+)?)?(.+?)\s+(?:a|por|precio|@|\$|cop|usd)\s*\$?\s*(\d+(?:\.\d{1,2})?)/i;
      const m = re.exec(seg);
      if (m) {
        const cant = m[1] ? Number(m[1]) : 1;
        const rawUnit = m[2] ? m[2].toLowerCase() : 'und';
        const unidad = unidadMap[rawUnit] || 'UNIDAD';
        const nombre = m[3].trim().replace(/^(?:de\s+)/i, '');
        const precioCentavos = parseDecimalToCentavos(m[4]);

        if (nombre && cant > 0 && Number.isInteger(precioCentavos) && precioCentavos >= 0) {
          articulos.push({
            articulo: nombre.slice(0, 120),
            cantidad: Number(cant.toFixed(3)),
            unidad,
            precio_unitario_centavos: precioCentavos
          });
        }
      }
    }

    return { comercio, metodo, articulos };
  }

  function handleDictadoParse() {
    hideBanner('dictado-alert');
    const txt = $('dictado-texto').value.trim();
    const errEl = $('dictado-texto-error');
    if (errEl) {
      errEl.textContent = '';
      errEl.classList.remove('visible');
    }

    if (!txt) {
      if (errEl) {
        errEl.textContent = 'Escriba el detalle de la compra en texto libre.';
        errEl.classList.add('visible');
      }
      return;
    }
    if (containsEmoji(txt)) {
      if (errEl) {
        errEl.textContent = 'El texto libre no puede contener emojis.';
        errEl.classList.add('visible');
      }
      return;
    }

    const parsed = parseFreeTextPurchase(txt);
    if (parsed.comercio) $('dictado-comercio').value = parsed.comercio;
    if (parsed.metodo) $('dictado-metodo').value = parsed.metodo;
    AppState.dictadoArticulos = parsed.articulos;

    const tbody = $('dictado-preview-tbody');
    if (parsed.articulos.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6" class="text-center" style="color: var(--color-peligro);">
            No se detectaron articulos con formato "[cantidad] [unidad] [articulo] a [precio]". Revise el ejemplo.
          </td>
        </tr>
      `;
      $('dictado-preview-total').textContent = formatCentavos(0, 'TOTAL_MONEDA');
      $('btn-dictado-open-editor').disabled = true;
      $('btn-dictado-save').disabled = true;
      return;
    }

    let totalCentavos = 0;
    tbody.innerHTML = parsed.articulos.map((it, idx) => {
      const sub = calcSubtotalCentavos(it.cantidad, it.precio_unitario_centavos);
      totalCentavos += sub;
      const uInfo = UNIDADES_ARTICULO[it.unidad] || UNIDADES_ARTICULO.UNIDAD;
      return `
        <tr>
          <td class="tabular-nums">${idx + 1}</td>
          <td>${escapeHtml(it.articulo)}</td>
          <td class="text-right tabular-nums">${it.cantidad}</td>
          <td>${escapeHtml(uInfo.etiqueta)}</td>
          <td class="text-right tabular-nums">${formatCentavos(it.precio_unitario_centavos, 'NEUTRO')}</td>
          <td class="text-right tabular-nums">${formatCentavos(sub, 'NEUTRO')}</td>
        </tr>
      `;
    }).join('');

    $('dictado-preview-total').textContent = formatCentavos(totalCentavos, 'TOTAL_MONEDA');
    $('btn-dictado-open-editor').disabled = false;
    $('btn-dictado-save').disabled = global.AppConfig.isTokenBlocked();
  }

  async function handleDictadoSaveDirect() {
    if (!AppState.dictadoArticulos || AppState.dictadoArticulos.length === 0) return;
    hideBanner('dictado-alert');

    const comercio = $('dictado-comercio').value.trim() || 'Comercio General';
    const fecha = $('dictado-fecha').value || getTodayIso();
    const categoriaId = parseInt($('dictado-categoria').value, 10) || 11;
    const metodoPago = $('dictado-metodo').value || 'DEBITO';

    const payload = {
      naturaleza: 'VARIABLE',
      fecha,
      comercio,
      descripcion: `Compra en ${comercio}`.slice(0, 200),
      categoria_id: categoriaId,
      metodo_pago: metodoPago,
      notas: null,
      articulos: AppState.dictadoArticulos
    };

    const btn = $('btn-dictado-save');
    setButtonLoading(btn, true, 'Registrando...');
    try {
      await global.ApiClient.createCompra(payload);
      showBanner('dictado-alert', 'success', 'Compra dictada registrada exitosamente en el servidor.');
      AppState.dictadoArticulos = [];
      $('dictado-texto').value = '';
      $('btn-dictado-open-editor').disabled = true;
      $('btn-dictado-save').disabled = true;
    } catch (err) {
      handleApiError(err, { alertContainerId: 'dictado-alert' });
    } finally {
      setButtonLoading(btn, false);
    }
  }

  // =========================================================
  // 7. PANTALLA: CONFIGURACION Y CONEXION
  // =========================================================

  function populateConfigForm() {
    const cfg = global.AppConfig.getConfig();
    if ($('cfg-host')) $('cfg-host').value = cfg.host;
    if ($('cfg-puerto')) $('cfg-puerto').value = String(cfg.puerto);
    if ($('cfg-token')) $('cfg-token').value = cfg.token_privado;
    updateConfigUrlPreview();
  }

  function updateConfigUrlPreview() {
    const host = ($('cfg-host') ? $('cfg-host').value.trim() : '') || '51.161.47.99';
    const port = ($('cfg-puerto') ? $('cfg-puerto').value.trim() : '') || '54892';
    if ($('cfg-url-preview')) {
      $('cfg-url-preview').value = `http://${host}:${port}/api/v1`;
    }
  }

  async function submitConfigForm(ev) {
    ev.preventDefault();
    hideBanner('cfg-alert');
    const host = $('cfg-host').value.trim();
    const puerto = parseInt($('cfg-puerto').value, 10);
    const tokenPrivado = $('cfg-token').value.trim();

    if (!host || !Number.isInteger(puerto) || puerto < 1 || puerto > 65535 || !tokenPrivado) {
      showBanner('cfg-alert', 'danger', 'Complete correctamente la IP/Host, el puerto TCP y el Token Privado.');
      return;
    }

    await global.AppConfig.updateConfig({
      host,
      puerto,
      token_privado: tokenPrivado
    });

    populateConfigForm();
    showBanner('cfg-alert', 'success', 'Configuracion guardada. Verificando conectividad con el servidor...');
    await checkServerHealth(true);
  }

  // =========================================================
  // MODAL DE CONFIRMACION DESTRUCTIVA (UI_DESIGN.md 7)
  // =========================================================

  function openConfirmModal(message, onConfirm) {
    $('modal-confirm-message').textContent = message;
    AppState.confirmCallback = onConfirm;
    $('modal-confirm').classList.add('open');
  }

  function closeConfirmModal() {
    $('modal-confirm').classList.remove('open');
    AppState.confirmCallback = null;
  }

  // =========================================================
  // INICIALIZACION DE EVENTOS Y ARRANQUE
  // =========================================================

  function bindEvents() {
    // Navegacion lateral
    document.querySelectorAll('.nav-btn').forEach((btn) => {
      btn.addEventListener('click', () => switchView(btn.dataset.view));
    });

    // Botones rapidos con atributo data-goto
    document.querySelectorAll('[data-goto]').forEach((btn) => {
      btn.addEventListener('click', () => switchView(btn.dataset.goto));
    });

    // Sincronizacion superior
    $('btn-topbar-sync').addEventListener('click', async () => {
      await checkServerHealth(false);
      switchView(AppState.currentView);
    });

    // Controles de Dashboard
    populateMonthSelect($('dash-select-mes'), AppState.period.mes);
    $('dash-input-anio').value = String(AppState.period.anio);

    $('dash-prev-month').addEventListener('click', () => {
      shiftPeriod(AppState.period, -1);
      loadDashboard();
    });
    $('dash-next-month').addEventListener('click', () => {
      shiftPeriod(AppState.period, 1);
      loadDashboard();
    });
    $('dash-select-mes').addEventListener('change', (e) => {
      AppState.period.mes = parseInt(e.target.value, 10) || 1;
      loadDashboard();
    });
    $('dash-input-anio').addEventListener('change', (e) => {
      const y = parseInt(e.target.value, 10);
      if (y >= 2000 && y <= 2100) {
        AppState.period.anio = y;
        loadDashboard();
      }
    });
    $('btn-dash-reload').addEventListener('click', () => loadDashboard());
    $('btn-dash-new-tx').addEventListener('click', () => {
      resetTransaccionForm();
      switchView('transacciones');
    });
    $('btn-dash-new-compra').addEventListener('click', () => {
      resetCompraForm();
      switchView('compras');
    });

    // Controles de Transacciones
    $('tx-tipo').addEventListener('change', () => populateAllCategorySelects());
    $('form-transaccion').addEventListener('submit', submitTransaccionForm);
    $('btn-tx-cancel-edit').addEventListener('click', () => resetTransaccionForm());
    $('btn-tx-reload').addEventListener('click', () => loadTransacciones(AppState.transaccionesMeta.pagina || 1));
    $('btn-tx-export-csv').addEventListener('click', exportTransaccionesCsv);
    $('btn-tx-filter-apply').addEventListener('click', () => loadTransacciones(1));
    $('btn-tx-filter-clear').addEventListener('click', () => {
      $('filter-tx-desde').value = '';
      $('filter-tx-hasta').value = '';
      $('filter-tx-tipo').value = '';
      $('filter-tx-naturaleza').value = '';
      $('filter-tx-categoria').value = '';
      $('filter-tx-metodo').value = '';
      $('filter-tx-es-compra').value = '';
      $('filter-tx-buscar').value = '';
      loadTransacciones(1);
    });
    $('filter-tx-por-pagina').addEventListener('change', () => loadTransacciones(1));
    $('btn-tx-prev-page').addEventListener('click', () => {
      if (AppState.transaccionesMeta.pagina > 1) {
        loadTransacciones(AppState.transaccionesMeta.pagina - 1);
      }
    });
    $('btn-tx-next-page').addEventListener('click', () => {
      if (AppState.transaccionesMeta.pagina < AppState.transaccionesMeta.total_paginas) {
        loadTransacciones(AppState.transaccionesMeta.pagina + 1);
      }
    });

    $('tx-table-tbody').addEventListener('click', (e) => {
      const btn = e.target.closest('button[data-action]');
      if (!btn) return;
      const id = Number(btn.dataset.id);
      const action = btn.dataset.action;
      const tx = AppState.transacciones.find((t) => t.id === id);

      if (action === 'view-ticket') {
        openTicketModal(id);
      } else if (action === 'edit-tx' && tx) {
        startEditTransaccion(tx);
      } else if (action === 'delete-tx') {
        openConfirmModal(
          'Esta accion no se puede deshacer. Desea eliminar esta transaccion?',
          async () => {
            await global.ApiClient.deleteTransaccion(id);
            await loadTransacciones(AppState.transaccionesMeta.pagina || 1);
            await fetchCreditCardsMaster();
          }
        );
      }
    });

    // Controles de Compras Detalladas
    $('btn-compra-add-item').addEventListener('click', () => createCompraItemRow());
    $('btn-compra-clear').addEventListener('click', () => resetCompraForm());
    $('btn-compra-cancel-edit').addEventListener('click', () => resetCompraForm());
    $('form-compra').addEventListener('submit', submitCompraForm);
    $('btn-compras-reload').addEventListener('click', () => loadCompras(AppState.comprasMeta.pagina || 1));
    $('btn-compras-filter-apply').addEventListener('click', () => loadCompras(1));
    $('btn-compras-filter-clear').addEventListener('click', () => {
      $('filter-compra-desde').value = '';
      $('filter-compra-hasta').value = '';
      $('filter-compra-articulo').value = '';
      $('filter-compra-buscar').value = '';
      loadCompras(1);
    });
    $('btn-compras-prev-page').addEventListener('click', () => {
      if (AppState.comprasMeta.pagina > 1) loadCompras(AppState.comprasMeta.pagina - 1);
    });
    $('btn-compras-next-page').addEventListener('click', () => {
      if (AppState.comprasMeta.pagina < AppState.comprasMeta.total_paginas) loadCompras(AppState.comprasMeta.pagina + 1);
    });

    $('compras-table-tbody').addEventListener('click', (e) => {
      const btn = e.target.closest('button[data-action]');
      if (!btn) return;
      const id = Number(btn.dataset.id);
      const action = btn.dataset.action;

      if (action === 'view-ticket') {
        openTicketModal(id);
      } else if (action === 'edit-compra') {
        loadCompraIntoEditor(id);
      } else if (action === 'delete-compra') {
        openConfirmModal(
          'Esta accion no se puede deshacer. Desea eliminar esta transaccion?',
          async () => {
            await global.ApiClient.deleteCompra(id);
            await loadCompras(AppState.comprasMeta.pagina || 1);
          }
        );
      }
    });

    // Controles de Presupuestos
    populateMonthSelect($('pres-select-mes'), AppState.presPeriod.mes);
    populateMonthSelect($('pres-mes'), AppState.presPeriod.mes);
    $('pres-input-anio').value = String(AppState.presPeriod.anio);
    $('pres-anio').value = String(AppState.presPeriod.anio);

    $('pres-prev-month').addEventListener('click', () => {
      shiftPeriod(AppState.presPeriod, -1);
      loadPresupuestos();
    });
    $('pres-next-month').addEventListener('click', () => {
      shiftPeriod(AppState.presPeriod, 1);
      loadPresupuestos();
    });
    $('pres-select-mes').addEventListener('change', (e) => {
      AppState.presPeriod.mes = parseInt(e.target.value, 10) || 1;
      loadPresupuestos();
    });
    $('pres-input-anio').addEventListener('change', (e) => {
      const y = parseInt(e.target.value, 10);
      if (y >= 2000 && y <= 2100) {
        AppState.presPeriod.anio = y;
        loadPresupuestos();
      }
    });
    $('pres-filter-estado').addEventListener('change', () => loadPresupuestos());
    $('btn-pres-reload').addEventListener('click', () => loadPresupuestos());
    $('btn-pres-cancel-edit').addEventListener('click', () => resetPresupuestoForm());
    $('form-presupuesto').addEventListener('submit', submitPresupuestoForm);

    $('pres-table-tbody').addEventListener('click', (e) => {
      const btn = e.target.closest('button[data-action]');
      if (!btn || !AppState.presupuestosData) return;
      const id = Number(btn.dataset.id);
      const action = btn.dataset.action;
      const list = AppState.presupuestosData.presupuestos || [];
      const pres = list.find((p) => p.id === id);

      if (action === 'edit-pres' && pres) {
        startEditPresupuesto(pres);
      } else if (action === 'delete-pres') {
        openConfirmModal(
          'Esta accion no se puede deshacer. Desea eliminar este presupuesto mensual?',
          async () => {
            await global.ApiClient.deletePresupuesto(id);
            await loadPresupuestos();
          }
        );
      }
    });

    // Controles de Categorias
    initCategoriaIconAndColorSelectors();
    $('cat-filter-inactivas').addEventListener('change', () => loadCategoriasView());
    $('btn-cat-reload').addEventListener('click', () => loadCategoriasView());
    $('btn-cat-cancel-edit').addEventListener('click', () => resetCategoriaForm());
    $('form-categoria').addEventListener('submit', submitCategoriaForm);

    const handleCatTableClick = (e) => {
      const btn = e.target.closest('button[data-action]');
      if (!btn) return;
      const id = Number(btn.dataset.id);
      const cat = AppState.categorias.find((c) => c.id === id);
      if (!cat) return;

      if (btn.dataset.action === 'edit-cat') {
        startEditCategoria(cat);
      } else if (btn.dataset.action === 'delete-cat') {
        openConfirmModal(
          'Esta accion no se puede deshacer. Desea eliminar esta categoria?',
          async () => handleDeleteCategoria(cat)
        );
      }
    };
    $('cat-ingresos-tbody').addEventListener('click', handleCatTableClick);
    $('cat-gastos-tbody').addEventListener('click', handleCatTableClick);

    // Controles de Asesor IA (Gemini + Dictado de Compras)
    populateMonthSelect($('ia-mes'), AppState.period.mes);
    $('ia-anio').value = String(AppState.period.anio);
    $('dictado-fecha').value = getTodayIso();

    $('ia-tipo-analisis').addEventListener('change', (e) => {
      const isFree = e.target.value === 'PREGUNTA_LIBRE';
      $('ia-pregunta-group').style.display = isFree ? 'flex' : 'none';
    });
    $('form-ia-analyze').addEventListener('submit', submitIaAnalyzeForm);

    $('btn-ia-copy-summary').addEventListener('click', () => {
      const text = $('ia-resumen-text').textContent;
      if (text && navigator.clipboard) {
        navigator.clipboard.writeText(text);
        showBanner('ia-form-alert', 'info', 'Resumen copiado al portapapeles.');
      }
    });

    $('btn-dictado-ejemplo').addEventListener('click', () => {
      $('dictado-texto').value = 'Compra en Supermaxi con debito: 2 und Leche entera 1L a 1.25, 1.5 kg Carne molida a 8.50, 1 paquete Detergente a 30.50';
      handleDictadoParse();
    });
    $('btn-dictado-parse').addEventListener('click', handleDictadoParse);
    $('btn-dictado-open-editor').addEventListener('click', () => {
      if (!AppState.dictadoArticulos || AppState.dictadoArticulos.length === 0) return;
      const comercio = $('dictado-comercio').value.trim() || 'Supermaxi';
      const fecha = $('dictado-fecha').value || getTodayIso();
      const catId = $('dictado-categoria').value || '11';
      const metodo = $('dictado-metodo').value || 'DEBITO';

      switchView('compras');
      resetCompraForm(AppState.dictadoArticulos);
      $('compra-comercio').value = comercio;
      $('compra-fecha').value = fecha;
      $('compra-categoria').value = String(catId);
      $('compra-metodo-pago').value = metodo;
      $('compra-descripcion').value = `Compra en ${comercio}`;
    });
    $('btn-dictado-save').addEventListener('click', handleDictadoSaveDirect);
    $('btn-dictado-auditar-ia').addEventListener('click', () => {
      const txt = $('dictado-texto').value.trim();
      if (!txt) {
        showBanner('dictado-alert', 'warning', 'Ingrese primero el texto libre de la compra para consultar a Gemini.');
        return;
      }
      $('ia-tipo-analisis').value = 'PREGUNTA_LIBRE';
      $('ia-pregunta-group').style.display = 'flex';
      $('ia-pregunta').value = `Evalua el impacto financiero y presupuestario de esta compra: ${stripEmojis(txt)}`.slice(0, 500);
      $('form-ia-analyze').requestSubmit();
    });

    // Controles de Configuracion
    $('cfg-host').addEventListener('input', updateConfigUrlPreview);
    $('cfg-puerto').addEventListener('input', updateConfigUrlPreview);
    $('btn-cfg-toggle-token').addEventListener('click', () => {
      const inp = $('cfg-token');
      const icon = $('icon-cfg-toggle-token');
      if (inp.type === 'password') {
        inp.type = 'text';
        icon.className = 'fa-solid fa-eye-slash';
      } else {
        inp.type = 'password';
        icon.className = 'fa-solid fa-eye';
      }
    });
    $('form-configuracion').addEventListener('submit', submitConfigForm);
    $('btn-cfg-test').addEventListener('click', () => checkServerHealth(true));
    $('btn-cfg-reset').addEventListener('click', async () => {
      await global.AppConfig.resetToDefault();
      populateConfigForm();
      showBanner('cfg-alert', 'info', 'Valores por defecto restaurados (51.161.47.99:54892).');
      await checkServerHealth(true);
    });

    // Selector y alternancia de temas (Barra Superior y Configuracion)
    const topbarThemeBtn = $('btn-topbar-theme');
    if (topbarThemeBtn) {
      topbarThemeBtn.addEventListener('click', () => toggleTheme());
    }

    const cfgThemeDarkBtn = $('btn-cfg-theme-dark');
    if (cfgThemeDarkBtn) {
      cfgThemeDarkBtn.addEventListener('click', () => setTheme('dark'));
    }

    const cfgThemeLightBtn = $('btn-cfg-theme-light');
    if (cfgThemeLightBtn) {
      cfgThemeLightBtn.addEventListener('click', () => setTheme('light'));
    }

    const cfgThemeToggleBtn = $('btn-cfg-theme-toggle');
    if (cfgThemeToggleBtn) {
      cfgThemeToggleBtn.addEventListener('click', () => toggleTheme());
    }

    // Modales
    $('btn-modal-confirm-close').addEventListener('click', closeConfirmModal);
    $('btn-modal-confirm-cancel').addEventListener('click', closeConfirmModal);
    $('btn-modal-confirm-accept').addEventListener('click', async () => {
      const cb = AppState.confirmCallback;
      closeConfirmModal();
      if (typeof cb === 'function') {
        try {
          await cb();
        } catch (err) {
          handleApiError(err);
        }
      }
    });

    const closeTicket = () => $('modal-ticket').classList.remove('open');
    $('btn-modal-ticket-close').addEventListener('click', closeTicket);
    $('btn-modal-ticket-close-footer').addEventListener('click', closeTicket);
    $('btn-modal-ticket-print').addEventListener('click', () => window.print());
    $('btn-modal-ticket-edit').addEventListener('click', () => {
      const id = AppState.activeTicketCompraId;
      closeTicket();
      if (id) {
        switchView('compras');
        loadCompraIntoEditor(id);
      }
    });

    // =========================================================
    // EVENTOS: MODULO DE TARJETAS DE CREDITO
    // =========================================================

    if ($('form-tarjeta')) {
      $('form-tarjeta').addEventListener('submit', submitTarjetaForm);
    }
    if ($('btn-tarjeta-cancel-edit')) {
      $('btn-tarjeta-cancel-edit').addEventListener('click', () => resetTarjetaForm());
    }
    if ($('btn-tarjetas-reload')) {
      $('btn-tarjetas-reload').addEventListener('click', () => loadCreditCardsView());
    }

    // Acciones de tarjetas en vista tipo panel / cards
    if ($('tarjetas-cards-container')) {
      $('tarjetas-cards-container').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        const action = btn.dataset.action;
        const card = (AppState.creditCards || []).find((c) => c.id === id);

        if (action === 'edit-card' && card) {
          startEditCreditCard(card);
        } else if (action === 'delete-card') {
          handleDeleteCreditCard(id);
        }
      });
    }

    // Acciones de tarjetas en vista tipo tabla
    if ($('tarjetas-table-tbody')) {
      $('tarjetas-table-tbody').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        const action = btn.dataset.action;
        const card = (AppState.creditCards || []).find((c) => c.id === id);

        if (action === 'edit-card' && card) {
          startEditCreditCard(card);
        } else if (action === 'delete-card') {
          handleDeleteCreditCard(id);
        }
      });
    }

    // =========================================================
    // EVENTOS: PANEL CONTABLE DE CUOTAS EN FORM-TRANSACCION
    // =========================================================

    function syncTxCreditCardPanel() {
      const isCredit = $('tx-metodo-pago') && $('tx-metodo-pago').value === 'CREDITO' &&
                       $('tx-tipo') && $('tx-tipo').value === 'GASTO';
      if ($('tx-panel-credito')) {
        $('tx-panel-credito').style.display = isCredit ? 'block' : 'none';
      }
      if (isCredit) {
        runLiveSimulation('tx');
      }
    }

    if ($('tx-metodo-pago')) {
      $('tx-metodo-pago').addEventListener('change', syncTxCreditCardPanel);
    }
    if ($('tx-tipo')) {
      $('tx-tipo').addEventListener('change', syncTxCreditCardPanel);
    }

    if ($('tx-card-id')) {
      $('tx-card-id').addEventListener('change', (e) => {
        const cardId = Number(e.target.value);
        const card = (AppState.creditCards || []).find((c) => c.id === cardId);
        if (card && $('tx-applied-rate') && (!$('tx-is-exempt') || !$('tx-is-exempt').checked)) {
          $('tx-applied-rate').value = Number(card.default_interest_rate || 0).toFixed(2);
        }
        runLiveSimulation('tx');
      });
    }

    if ($('tx-is-exempt')) {
      $('tx-is-exempt').addEventListener('change', (e) => {
        const isExempt = e.target.checked;
        if ($('tx-applied-rate')) {
          if (isExempt) {
            $('tx-applied-rate').value = '0.00';
            $('tx-applied-rate').disabled = true;
            $('tx-applied-rate').style.cursor = 'not-allowed';
            $('tx-applied-rate').style.opacity = '0.6';
          } else {
            $('tx-applied-rate').disabled = false;
            $('tx-applied-rate').style.cursor = '';
            $('tx-applied-rate').style.opacity = '';
            const cardId = Number($('tx-card-id') ? $('tx-card-id').value : 0);
            const card = (AppState.creditCards || []).find((c) => c.id === cardId);
            $('tx-applied-rate').value = card ? Number(card.default_interest_rate || 0).toFixed(2) : '0.00';
          }
        }
        runLiveSimulation('tx');
      });
    }

    if ($('tx-installments')) {
      $('tx-installments').addEventListener('input', () => runLiveSimulation('tx'));
      $('tx-installments').addEventListener('change', () => runLiveSimulation('tx'));
    }

    if ($('tx-applied-rate')) {
      $('tx-applied-rate').addEventListener('input', () => runLiveSimulation('tx'));
      $('tx-applied-rate').addEventListener('change', () => runLiveSimulation('tx'));
    }

    if ($('tx-monto')) {
      $('tx-monto').addEventListener('input', () => {
        if ($('tx-panel-credito') && $('tx-panel-credito').style.display !== 'none') {
          runLiveSimulation('tx');
        }
      });
    }

    // =========================================================
    // EVENTOS: PANEL CONTABLE DE CUOTAS EN FORM-COMPRA
    // =========================================================

    function syncCompraCreditCardPanel() {
      const isCredit = $('compra-metodo-pago') && $('compra-metodo-pago').value === 'CREDITO';
      if ($('compra-panel-credito')) {
        $('compra-panel-credito').style.display = isCredit ? 'block' : 'none';
      }
      if (isCredit) {
        runLiveSimulation('compra');
      }
    }

    if ($('compra-metodo-pago')) {
      $('compra-metodo-pago').addEventListener('change', syncCompraCreditCardPanel);
    }

    if ($('compra-card-id')) {
      $('compra-card-id').addEventListener('change', (e) => {
        const cardId = Number(e.target.value);
        const card = (AppState.creditCards || []).find((c) => c.id === cardId);
        if (card && $('compra-applied-rate') && (!$('compra-is-exempt') || !$('compra-is-exempt').checked)) {
          $('compra-applied-rate').value = Number(card.default_interest_rate || 0).toFixed(2);
        }
        runLiveSimulation('compra');
      });
    }

    if ($('compra-is-exempt')) {
      $('compra-is-exempt').addEventListener('change', (e) => {
        const isExempt = e.target.checked;
        if ($('compra-applied-rate')) {
          if (isExempt) {
            $('compra-applied-rate').value = '0.00';
            $('compra-applied-rate').disabled = true;
            $('compra-applied-rate').style.cursor = 'not-allowed';
            $('compra-applied-rate').style.opacity = '0.6';
          } else {
            $('compra-applied-rate').disabled = false;
            $('compra-applied-rate').style.cursor = '';
            $('compra-applied-rate').style.opacity = '';
            const cardId = Number($('compra-card-id') ? $('compra-card-id').value : 0);
            const card = (AppState.creditCards || []).find((c) => c.id === cardId);
            $('compra-applied-rate').value = card ? Number(card.default_interest_rate || 0).toFixed(2) : '0.00';
          }
        }
        runLiveSimulation('compra');
      });
    }

    if ($('compra-installments')) {
      $('compra-installments').addEventListener('input', () => runLiveSimulation('compra'));
      $('compra-installments').addEventListener('change', () => runLiveSimulation('compra'));
    }

    if ($('compra-applied-rate')) {
      $('compra-applied-rate').addEventListener('input', () => runLiveSimulation('compra'));
      $('compra-applied-rate').addEventListener('change', () => runLiveSimulation('compra'));
    }
  }

  async function bootstrap() {
    initTheme();
    await global.AppConfig.initConfig();
    populateConfigForm();
    populateAllCategorySelects();
    resetTransaccionForm();
    resetCompraForm();
    resetPresupuestoForm();
    resetCategoriaForm();
    resetTarjetaForm();
    bindEvents();
    await checkServerHealth(false);
    await loadDashboard();
  }

  global.AppTheme = {
    getActiveTheme,
    setTheme,
    toggleTheme,
    updateThemeUi,
    updateChartTheme,
    initTheme
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bootstrap);
  } else {
    bootstrap();
  }
})(typeof window !== 'undefined' ? window : globalThis);
