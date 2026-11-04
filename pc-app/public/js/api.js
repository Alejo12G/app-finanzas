'use strict';

(function (global) {
  function buildQueryString(params) {
    if (!params || typeof params !== 'object') return '';
    const entries = [];
    for (const [key, val] of Object.entries(params)) {
      if (val !== undefined && val !== null && val !== '') {
        entries.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(val))}`);
      }
    }
    return entries.length > 0 ? `?${entries.join('&')}` : '';
  }

  async function request(method, path, options = {}) {
    const cfg = global.AppConfig.getConfig();
    const isAi = path.startsWith('/ai/analyze');
    const timeoutMs = options.timeoutMs || (isAi ? cfg.timeout_ia_ms : cfg.timeout_general_ms);
    const queryStr = buildQueryString(options.query);
    const directUrl = `${cfg.url_base}${path}${queryStr}`;

    const headers = {
      'Accept': 'application/json',
      'Content-Type': 'application/json; charset=utf-8',
      [cfg.header_token]: cfg.token_privado
    };

    const fetchOptions = {
      method,
      headers,
      mode: 'cors',
      cache: 'no-store'
    };

    if (options.body !== undefined && options.body !== null && method !== 'GET' && method !== 'HEAD') {
      fetchOptions.body = JSON.stringify(options.body);
    }

    const controller = new AbortController();
    fetchOptions.signal = controller.signal;
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    let response;
    try {
      response = await fetch(directUrl, fetchOptions);
    } catch (networkErr) {
      clearTimeout(timer);
      const isAbort = networkErr && networkErr.name === 'AbortError';
      if (isAbort) {
        throw createClientError(
          isAi ? 504 : 0,
          isAi ? 'TIEMPO_IA_AGOTADO' : 'TIMEOUT_RED',
          'No se pudo conectar con el servidor.',
          []
        );
      }
      throw createClientError(0, 'SIN_RED', 'No se pudo conectar con el servidor.', []);
    } finally {
      clearTimeout(timer);
    }

    let parsed = null;
    try {
      const text = await response.text();
      parsed = text ? JSON.parse(text) : null;
    } catch (_parseErr) {
      parsed = null;
    }

    if (response.ok && parsed && parsed.ok === true) {
      if (global.AppConfig.isTokenBlocked() && path !== '/health') {
        global.AppConfig.setTokenBlocked(false);
      }
      return {
        status: response.status,
        ok: true,
        data: parsed.data,
        meta: parsed.meta !== undefined ? parsed.meta : null
      };
    }

    const status = response.status;
    const apiErr = parsed && parsed.error ? parsed.error : {};
    const codigo = apiErr.codigo || mapDefaultErrorCode(status);
    const detalles = Array.isArray(apiErr.detalles) ? apiErr.detalles : [];
    let mensaje = apiErr.mensaje || 'Error al procesar la solicitud.';

    if (status === 401 || codigo === 'TOKEN_INVALIDO') {
      mensaje = 'Token de acceso invalido.';
      global.AppConfig.setTokenBlocked(true);
      if (typeof global. dispatchEvent === 'function') {
        global.dispatchEvent(new CustomEvent('app:token-invalido', { detail: { mensaje } }));
      }
    } else if (status === 429 || codigo === 'LIMITE_IA_EXCEDIDO') {
      mensaje = 'Limite de analisis IA alcanzado. Intente mas tarde.';
    } else if (status >= 500) {
      mensaje = apiErr.mensaje || 'Error interno del servidor. Intente mas tarde.';
    }

    throw createClientError(status, codigo, mensaje, detalles);
  }

  function mapDefaultErrorCode(status) {
    switch (status) {
      case 400: return 'JSON_INVALIDO';
      case 401: return 'TOKEN_INVALIDO';
      case 404: return 'RECURSO_NO_ENCONTRADO';
      case 405: return 'METODO_NO_PERMITIDO';
      case 409: return 'CONFLICTO';
      case 413: return 'CUERPO_DEMASIADO_GRANDE';
      case 422: return 'VALIDACION_FALLIDA';
      case 429: return 'LIMITE_IA_EXCEDIDO';
      case 502: return 'ERROR_SERVICIO_IA';
      case 504: return 'TIEMPO_IA_AGOTADO';
      default: return 'ERROR_INTERNO';
    }
  }

  function createClientError(status, codigo, mensaje, detalles) {
    const err = new Error(mensaje);
    err.status = status;
    err.codigo = codigo;
    err.mensaje = mensaje;
    err.detalles = Array.isArray(detalles) ? detalles : [];
    return err;
  }

  const ApiClient = {
    // 7.1 Health
    getHealth() {
      return request('GET', '/health');
    },

    // 7.2 Categorias
    getCategorias(query = {}) {
      return request('GET', '/categorias', { query });
    },
    createCategoria(body) {
      return request('POST', '/categorias', { body });
    },
    updateCategoria(id, body) {
      return request('PUT', `/categorias/${encodeURIComponent(id)}`, { body });
    },
    deleteCategoria(id) {
      return request('DELETE', `/categorias/${encodeURIComponent(id)}`);
    },

    // 7.3 Transacciones
    getTransacciones(query = {}) {
      return request('GET', '/transacciones', { query });
    },
    getTransaccion(id) {
      return request('GET', `/transacciones/${encodeURIComponent(id)}`);
    },
    createTransaccion(body) {
      return request('POST', '/transacciones', { body });
    },
    updateTransaccion(id, body) {
      return request('PUT', `/transacciones/${encodeURIComponent(id)}`, { body });
    },
    deleteTransaccion(id) {
      return request('DELETE', `/transacciones/${encodeURIComponent(id)}`);
    },

    // 7.4 Compras
    getCompras(query = {}) {
      return request('GET', '/compras', { query });
    },
    getCompra(id) {
      return request('GET', `/compras/${encodeURIComponent(id)}`);
    },
    createCompra(body) {
      return request('POST', '/compras', { body });
    },
    updateCompra(id, body) {
      return request('PUT', `/compras/${encodeURIComponent(id)}`, { body });
    },
    deleteCompra(id) {
      return request('DELETE', `/compras/${encodeURIComponent(id)}`);
    },

    // 7.5 Presupuestos
    getPresupuestos(query = {}) {
      return request('GET', '/presupuestos', { query });
    },
    getPresupuesto(id) {
      return request('GET', `/presupuestos/${encodeURIComponent(id)}`);
    },
    createPresupuesto(body) {
      return request('POST', '/presupuestos', { body });
    },
    updatePresupuesto(id, body) {
      return request('PUT', `/presupuestos/${encodeURIComponent(id)}`, { body });
    },
    deletePresupuesto(id) {
      return request('DELETE', `/presupuestos/${encodeURIComponent(id)}`);
    },

    // 7.6 Dashboard
    getDashboard(anio, mes) {
      return request('GET', '/dashboard', { query: { anio, mes } });
    },

    // 7.7 Tarjetas de Credito
    getCreditCards() {
      return request('GET', '/credit-cards');
    },
    getCreditCard(id) {
      return request('GET', `/credit-cards/${encodeURIComponent(id)}`);
    },
    createCreditCard(body) {
      return request('POST', '/credit-cards', { body });
    },
    updateCreditCard(id, body) {
      return request('PUT', `/credit-cards/${encodeURIComponent(id)}`, { body });
    },
    deleteCreditCard(id) {
      return request('DELETE', `/credit-cards/${encodeURIComponent(id)}`);
    },
    simulateCreditCard(body) {
      return request('POST', '/credit-cards/simulate', { body });
    },

    // 8 Modulo IA (Gemini)
    analyzeAi(body) {
      return request('POST', '/ai/analyze', { body });
    }
  };

  global.ApiClient = ApiClient;
})(typeof window !== 'undefined' ? window : globalThis);
