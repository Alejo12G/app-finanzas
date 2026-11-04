'use strict';

const config = require('../config');
const { getDb, formatCategoriaResumen } = require('../db');
const { getPreviousYearMonth, buildDashboardData } = require('./dashboard');
const { getPresupuestosForPeriod } = require('./presupuestos');
const { validateCompraPayload, insertCompraInDb } = require('./compras');
const { AppError, sendSuccess } = require('../utils/response');
const {
  ENUMS,
  stripEmojis,
  nowIsoUtc,
  getMonthRange,
  roundHalfUp,
  checkNoEmojisInFields
} = require('../utils/validators');

// Ventana movil de rate limiting en memoria (10 peticiones / 60 min)
const aiRequestTimestamps = [];

function checkAiRateLimit() {
  const now = Date.now();
  const cutoff = now - config.AI_RATE_LIMIT_WINDOW_MS;
  while (aiRequestTimestamps.length > 0 && aiRequestTimestamps[0] <= cutoff) {
    aiRequestTimestamps.shift();
  }
  if (aiRequestTimestamps.length >= config.AI_RATE_LIMIT_MAX) {
    throw new AppError(
      429,
      'LIMITE_IA_EXCEDIDO',
      'Se ha alcanzado el limite de 10 analisis de IA por cada 60 minutos. Intente mas tarde.'
    );
  }
  aiRequestTimestamps.push(now);
}

function resetAiRateLimitForTests() {
  aiRequestTimestamps.length = 0;
}

async function callGeminiWithFallback(systemInstruction, userPrompt, responseSchema) {
  if (!config.GEMINI_API_KEY) {
    throw new AppError(
      502,
      'ERROR_SERVICIO_IA',
      'La clave GEMINI_API_KEY no esta configurada en el servidor.'
    );
  }

  const candidateModels = [];
  const primary = config.GEMINI_MODEL || 'gemini-2.5-flash';
  candidateModels.push(primary);
  for (const fallback of [
    'gemini-3.6-flash',
    'gemini-3.5-flash',
    'gemini-3.8-flash',
    'gemini-3.1-flash-lite',
    'gemini-flash-lite-latest'
  ]) {
    if (!candidateModels.includes(fallback)) {
      candidateModels.push(fallback);
    }
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), config.AI_TIMEOUT_MS);

  try {
    let lastStatus = 0;
    for (const modelName of candidateModels) {
      const cleanModel = modelName.replace(/^models\//, '');
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(
        cleanModel
      )}:generateContent?key=${encodeURIComponent(config.GEMINI_API_KEY)}`;

      const payload = {
        systemInstruction: {
          parts: [{ text: systemInstruction }]
        },
        contents: [
          {
            role: 'user',
            parts: [{ text: userPrompt }]
          }
        ],
        generationConfig: {
          temperature: 0.2,
          responseMimeType: 'application/json',
          responseSchema
        }
      };

      let response;
      try {
        response = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
          signal: controller.signal
        });
      } catch (err) {
        if (err && err.name === 'AbortError') {
          throw new AppError(
            504,
            'TIEMPO_IA_AGOTADO',
            'El servicio de IA no respondio dentro del tiempo limite de 60 segundos.'
          );
        }
        throw new AppError(
          502,
          'ERROR_SERVICIO_IA',
          'No fue posible establecer conexion con el servicio de IA.'
        );
      }

      lastStatus = response.status;

      // Si el modelo especifico devuelve 404 (deprecado) o 503 (alta demanda), probar siguiente modelo
      if (response.status === 404 || response.status === 503 || response.status === 429) {
        continue;
      }

      if (!response.ok) {
        throw new AppError(
          502,
          'ERROR_SERVICIO_IA',
          `El servicio de IA devolvio un estado inesperado (${response.status}).`
        );
      }

      const data = await response.json();
      const rawText =
        data &&
        data.candidates &&
        data.candidates[0] &&
        data.candidates[0].content &&
        data.candidates[0].content.parts &&
        data.candidates[0].content.parts[0] &&
        data.candidates[0].content.parts[0].text;

      if (typeof rawText !== 'string' || !rawText.trim()) {
        throw new AppError(
          502,
          'ERROR_SERVICIO_IA',
          'El servicio de IA devolvio una respuesta vacia.'
        );
      }

      try {
        return JSON.parse(rawText);
      } catch (_parseErr) {
        throw new AppError(
          502,
          'ERROR_SERVICIO_IA',
          'El servicio de IA devolvio un formato JSON no valido.'
        );
      }
    }

    throw new AppError(
      502,
      'ERROR_SERVICIO_IA',
      `El servicio de IA no estuvo disponible (ultimo estado HTTP ${lastStatus}).`
    );
  } finally {
    clearTimeout(timeoutId);
  }
}

function buildAggregatedContext(db, anio, mes) {
  const dash = buildDashboardData(db, anio, mes);
  const pres = getPresupuestosForPeriod(db, anio, mes, null);

  // 3 meses previos agregados
  const mesesPrevios = [];
  for (let i = 1; i <= 3; i++) {
    const pm = getPreviousYearMonth(anio, mes, i);
    const pmDash = buildDashboardData(db, pm.anio, pm.mes);
    mesesPrevios.push({
      anio: pm.anio,
      mes: pm.mes,
      totales: pmDash.totales,
      gastos_por_categoria: pmDash.gastos_por_categoria.map((g) => ({
        categoria_id: g.categoria.id,
        nombre: g.categoria.nombre,
        total_centavos: g.total_centavos,
        porcentaje: g.porcentaje
      }))
    });
  }

  // Top 20 articulos con mayor gasto acumulado en el periodo y 3 meses previos
  const oldestMonth = getPreviousYearMonth(anio, mes, 3);
  const oldestRange = getMonthRange(oldestMonth.anio, oldestMonth.mes);
  const currentRange = getMonthRange(anio, mes);

  const topArticulos = db
    .prepare(`
      SELECT
        dc.articulo,
        dc.unidad,
        ROUND(SUM(dc.cantidad), 3) AS cantidad_total,
        SUM(dc.subtotal_centavos) AS gasto_acumulado_centavos
      FROM detalle_compras dc
      JOIN transacciones t ON t.id = dc.transaccion_id
      WHERE t.tipo = 'GASTO' AND t.fecha >= ? AND t.fecha <= ?
      GROUP BY LOWER(dc.articulo), dc.unidad
      ORDER BY gasto_acumulado_centavos DESC
      LIMIT 20
    `)
    .all(oldestRange.desde, currentRange.hasta);

  // Categorias disponibles (para que Gemini referencie IDs reales)
  const categoriasActivas = db
    .prepare('SELECT id, nombre, tipo FROM categorias WHERE activa = 1 ORDER BY id ASC')
    .all();

  return {
    moneda: config.APP_MONEDA,
    periodo_actual: {
      anio,
      mes,
      totales: dash.totales,
      comparativo_mes_anterior: dash.comparativo_mes_anterior,
      gastos_por_categoria: dash.gastos_por_categoria.map((g) => ({
        categoria_id: g.categoria.id,
        nombre: g.categoria.nombre,
        total_centavos: g.total_centavos,
        porcentaje: g.porcentaje
      })),
      ingresos_por_categoria: dash.ingresos_por_categoria.map((ing) => ({
        categoria_id: ing.categoria.id,
        nombre: ing.categoria.nombre,
        total_centavos: ing.total_centavos,
        porcentaje: ing.porcentaje
      })),
      presupuestos: pres.presupuestos.map((p) => ({
        categoria_id: p.categoria_id,
        categoria_nombre: p.categoria.nombre,
        monto_limite_centavos: p.monto_limite_centavos,
        gastado_centavos: p.gastado_centavos,
        disponible_centavos: p.disponible_centavos,
        porcentaje_usado: p.porcentaje_usado,
        estado: p.estado
      }))
    },
    tres_meses_previos: mesesPrevios,
    top_20_articulos_comprados: topArticulos.map((a) => ({
      articulo: a.articulo,
      unidad: a.unidad,
      cantidad_total: Number(a.cantidad_total),
      gasto_acumulado_centavos: Number(a.gasto_acumulado_centavos)
    })),
    catalogo_categorias: categoriasActivas.map((c) => ({
      id: Number(c.id),
      nombre: c.nombre,
      tipo: c.tipo
    }))
  };
}

const ANALYZE_RESPONSE_SCHEMA = {
  type: 'OBJECT',
  properties: {
    resumen: {
      type: 'STRING',
      description: 'Resumen ejecutivo sobrio y profesional del periodo en espanol (1 a 2000 caracteres, sin emojis).'
    },
    puntuacion_salud_financiera: {
      type: 'INTEGER',
      description: 'Puntuacion de salud financiera entre 0 y 100.'
    },
    recomendaciones: {
      type: 'ARRAY',
      items: {
        type: 'OBJECT',
        properties: {
          titulo: { type: 'STRING' },
          detalle: { type: 'STRING' },
          prioridad: { type: 'STRING', enum: ['ALTA', 'MEDIA', 'BAJA'] },
          ahorro_estimado_centavos: { type: 'INTEGER', nullable: true },
          categoria_id: { type: 'INTEGER', nullable: true }
        },
        required: ['titulo', 'detalle', 'prioridad', 'ahorro_estimado_centavos', 'categoria_id']
      }
    },
    alertas: {
      type: 'ARRAY',
      items: {
        type: 'OBJECT',
        properties: {
          codigo: {
            type: 'STRING',
            enum: [
              'PRESUPUESTO_EXCEDIDO',
              'PRESUPUESTO_EN_RIESGO',
              'BALANCE_NEGATIVO',
              'GASTO_INUSUAL',
              'AHORRO_BAJO',
              'OTRO'
            ]
          },
          mensaje: { type: 'STRING' },
          categoria_id: { type: 'INTEGER', nullable: true }
        },
        required: ['codigo', 'mensaje', 'categoria_id']
      }
    }
  },
  required: ['resumen', 'puntuacion_salud_financiera', 'recomendaciones', 'alertas']
};

const PARSE_COMPRA_SCHEMA = {
  type: 'OBJECT',
  properties: {
    comercio: {
      type: 'STRING',
      description: 'Nombre del comercio o establecimiento extraido del texto (ej. Exito, Supermaxi). Si no se menciona, usar Comercio general.'
    },
    descripcion: {
      type: 'STRING',
      description: 'Descripcion breve de la compra sin emojis.'
    },
    categoria_id: {
      type: 'INTEGER',
      description: 'ID de la categoria de tipo GASTO mas adecuada del catalogo proporcionado.'
    },
    naturaleza: {
      type: 'STRING',
      enum: ['FIJO', 'VARIABLE']
    },
    metodo_pago: {
      type: 'STRING',
      enum: ['EFECTIVO', 'DEBITO', 'CREDITO', 'TRANSFERENCIA', 'OTRO']
    },
    articulos: {
      type: 'ARRAY',
      items: {
        type: 'OBJECT',
        properties: {
          articulo: { type: 'STRING' },
          cantidad: { type: 'NUMBER' },
          unidad: {
            type: 'STRING',
            enum: ['UNIDAD', 'KG', 'G', 'L', 'ML', 'PAQUETE', 'OTRO']
          },
          precio_unitario_centavos: {
            type: 'INTEGER',
            description: 'Precio unitario entero (si el usuario dice 2 litros de leche a 8000 cada uno o 8000 el litro, indicar el valor unitario entero).'
          }
        },
        required: ['articulo', 'cantidad', 'unidad', 'precio_unitario_centavos']
      }
    }
  },
  required: ['comercio', 'descripcion', 'categoria_id', 'naturaleza', 'metodo_pago', 'articulos']
};

async function handleParseCompraLibre(req, res, body) {
  const db = getDb();
  const rawTexto =
    typeof body.texto === 'string'
      ? body.texto
      : typeof body.texto_compra === 'string'
      ? body.texto_compra
      : typeof body.pregunta === 'string'
      ? body.pregunta
      : '';

  checkNoEmojisInFields({
    texto: rawTexto,
    fecha: body.fecha,
    metodo_pago: body.metodo_pago
  });

  const texto = rawTexto.trim();
  if (texto.length < 5 || texto.length > 1000) {
    throw new AppError(
      422,
      'VALIDACION_FALLIDA',
      'El texto de la compra es obligatorio y debe tener entre 5 y 1000 caracteres.',
      [{ campo: 'texto', mensaje: 'Debe tener entre 5 y 1000 caracteres.' }]
    );
  }

  checkAiRateLimit();

  const categoriasGasto = db
    .prepare("SELECT id, nombre FROM categorias WHERE tipo = 'GASTO' AND activa = 1 ORDER BY id ASC")
    .all();

  const systemInstruction = [
    'Eres un analizador contable estructurado.',
    'Tu tarea es recibir un texto libre en espanol que describe una compra y extraer su estructuracion JSON exacta.',
    'Reglas estrictas:',
    '1. Cero emojis o simbolos decorativos.',
    '2. Selecciona el categoria_id de la lista de categorias de GASTO proporcionada (por ejemplo 11 para Supermercado).',
    '3. Para cada articulo mencionado, identifica nombre, cantidad, unidad (UNIDAD, KG, G, L, ML, PAQUETE, OTRO) y precio_unitario_centavos como entero positivo.',
    '4. Si el usuario menciona un precio como "2 litros de leche a 8000", interpreta 8000 como el valor entero en precio_unitario_centavos (o si indica total, divide por la cantidad para obtener el precio unitario).',
    '5. Devuelve exclusivamente JSON valido.'
  ].join('\n');

  const userPrompt = JSON.stringify({
    texto_compra: texto,
    categorias_gasto_disponibles: categoriasGasto
  });

  const parsed = await callGeminiWithFallback(
    systemInstruction,
    userPrompt,
    PARSE_COMPRA_SCHEMA
  );

  if (!parsed || typeof parsed !== 'object' || !Array.isArray(parsed.articulos) || parsed.articulos.length === 0) {
    throw new AppError(
      502,
      'ERROR_SERVICIO_IA',
      'El servicio de IA no pudo extraer articulos validos del texto proporcionado.'
    );
  }

  const validCatIds = new Set(categoriasGasto.map((c) => Number(c.id)));
  let catId = Number(parsed.categoria_id);
  if (!validCatIds.has(catId)) {
    catId = validCatIds.has(11) ? 11 : Number(categoriasGasto[0].id);
  }

  const todayStr = new Date().toISOString().slice(0, 10);
  const fecha = typeof body.fecha === 'string' && body.fecha.trim() ? body.fecha.trim() : todayStr;
  const metodoPago =
    typeof body.metodo_pago === 'string' && ENUMS.METODO_PAGO.includes(body.metodo_pago.trim())
      ? body.metodo_pago.trim()
      : ENUMS.METODO_PAGO.includes(parsed.metodo_pago)
      ? parsed.metodo_pago
      : 'EFECTIVO';

  const comercio = stripEmojis(String(parsed.comercio || 'Comercio general')).slice(0, 120) || 'Comercio general';
  const descripcion =
    stripEmojis(String(parsed.descripcion || `Compra en ${comercio}`)).slice(0, 200) ||
    `Compra en ${comercio}`;

  const articulosLimpios = parsed.articulos.slice(0, 200).map((art) => {
    const nombreArt = stripEmojis(String(art.articulo || 'Articulo')).slice(0, 120) || 'Articulo';
    const cantidad =
      typeof art.cantidad === 'number' && Number.isFinite(art.cantidad) && art.cantidad > 0
        ? Number(art.cantidad.toFixed(3))
        : 1;
    const unidad = ENUMS.UNIDAD.includes(art.unidad) ? art.unidad : 'UNIDAD';
    const precioUnit =
      typeof art.precio_unitario_centavos === 'number' && art.precio_unitario_centavos >= 0
        ? Math.max(1, Math.round(art.precio_unitario_centavos))
        : 100;
    return {
      articulo: nombreArt,
      cantidad,
      unidad,
      precio_unitario_centavos: precioUnit
    };
  });

  const compraPayload = {
    naturaleza: ENUMS.NATURALEZA.includes(parsed.naturaleza) ? parsed.naturaleza : 'VARIABLE',
    fecha,
    comercio,
    descripcion,
    categoria_id: catId,
    metodo_pago: metodoPago,
    notas: null,
    articulos: articulosLimpios
  };

  const validated = validateCompraPayload(db, compraPayload);
  const autoGuardar = body.guardar !== false && body.auto_guardar !== false;

  if (autoGuardar) {
    const compraGuardada = insertCompraInDb(db, validated);
    sendSuccess(res, 201, {
      ...compraGuardada,
      guardado: true,
      modelo: config.GEMINI_MODEL
    }, null);
    return;
  }

  const catRow = db.prepare('SELECT * FROM categorias WHERE id = ?').get(validated.categoria_id);
  sendSuccess(res, 200, {
    tipo: 'GASTO',
    naturaleza: validated.naturaleza,
    monto_centavos: validated.monto_centavos,
    fecha: validated.fecha,
    comercio: validated.comercio,
    descripcion: validated.descripcion,
    categoria_id: validated.categoria_id,
    categoria: formatCategoriaResumen(catRow),
    metodo_pago: validated.metodo_pago,
    notas: validated.notas,
    es_compra: true,
    cantidad_articulos: validated.articulos.length,
    articulos: validated.articulos,
    guardado: false,
    modelo: config.GEMINI_MODEL
  }, null);
}

async function analyzeFinances(req, res, body) {
  // Soporte dual: si se envia texto libre de compra (texto / texto_compra / tipo_analisis = PARSEAR_COMPRA),
  // procesa y estructura la compra automaticamente.
  if (
    body &&
    (typeof body.texto === 'string' ||
      typeof body.texto_compra === 'string' ||
      body.tipo_analisis === 'PARSEAR_COMPRA' ||
      body.tipo_analisis === 'COMPRA')
  ) {
    return handleParseCompraLibre(req, res, body);
  }

  const db = getDb();
  checkNoEmojisInFields({
    tipo_analisis: body.tipo_analisis,
    pregunta: body.pregunta
  });

  const detalles = [];

  const tipoAnalisis = typeof body.tipo_analisis === 'string' ? body.tipo_analisis.trim() : '';
  if (!ENUMS.TIPO_ANALISIS.includes(tipoAnalisis)) {
    detalles.push({
      campo: 'tipo_analisis',
      mensaje: 'Debe ser RESUMEN_MENSUAL, AHORRO, PRESUPUESTO o PREGUNTA_LIBRE.'
    });
  }

  const anio = body.anio;
  if (typeof anio !== 'number' || !Number.isInteger(anio) || anio < 2000 || anio > 2100) {
    detalles.push({
      campo: 'anio',
      mensaje: 'El anio es obligatorio y debe estar entre 2000 y 2100.'
    });
  }

  const mes = body.mes;
  if (typeof mes !== 'number' || !Number.isInteger(mes) || mes < 1 || mes > 12) {
    detalles.push({
      campo: 'mes',
      mensaje: 'El mes es obligatorio y debe estar entre 1 y 12.'
    });
  }

  let pregunta = null;
  if (tipoAnalisis === 'PREGUNTA_LIBRE') {
    const trimmedPreg = typeof body.pregunta === 'string' ? body.pregunta.trim() : '';
    if (typeof body.pregunta !== 'string' || trimmedPreg.length < 5 || trimmedPreg.length > 500) {
      detalles.push({
        campo: 'pregunta',
        mensaje: 'La pregunta es obligatoria para PREGUNTA_LIBRE y debe tener entre 5 y 500 caracteres.'
      });
    } else {
      pregunta = trimmedPreg;
    }
  } else {
    if (body.pregunta !== undefined && body.pregunta !== null) {
      detalles.push({
        campo: 'pregunta',
        mensaje: 'El campo pregunta debe ser null cuando tipo_analisis no es PREGUNTA_LIBRE.'
      });
    }
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }

  checkAiRateLimit();

  const contextData = buildAggregatedContext(db, anio, mes);

  const systemInstruction = [
    'Eres un asesor financiero personal senior, analitico y sobrio.',
    'Reglas obligatorias:',
    '1. Responde siempre en espanol con tono profesional, bancario y objetivo.',
    '2. PROHIBIDO usar emojis, emoticonos o simbolos Unicode decorativos.',
    '3. Todos los montos en el contexto estan expresados en centavos enteros (divide entre 100 para expresar cifras en la moneda en textos legibles, pero devuelve ahorro_estimado_centavos siempre en centavos enteros).',
    '4. Basa tus conclusiones estrictamente en los datos agregados proporcionados del periodo consultado y los 3 meses previos.',
    '5. Devuelve unicamente un objeto JSON que cumpla el esquema solicitado.'
  ].join('\n');

  const userPrompt = JSON.stringify({
    tipo_analisis: tipoAnalisis,
    periodo_solicitado: { anio, mes },
    pregunta_usuario: pregunta,
    datos_financieros_agregados: contextData
  });

  const rawAi = await callGeminiWithFallback(
    systemInstruction,
    userPrompt,
    ANALYZE_RESPONSE_SCHEMA
  );

  if (
    !rawAi ||
    typeof rawAi !== 'object' ||
    typeof rawAi.resumen !== 'string' ||
    typeof rawAi.puntuacion_salud_financiera !== 'number' ||
    !Array.isArray(rawAi.recomendaciones) ||
    !Array.isArray(rawAi.alertas)
  ) {
    throw new AppError(
      502,
      'ERROR_SERVICIO_IA',
      'La respuesta del modelo de IA no cumple con el esquema requerido.'
    );
  }

  const resumenLimpio = stripEmojis(rawAi.resumen).slice(0, 2000);
  if (resumenLimpio.length < 1) {
    throw new AppError(
      502,
      'ERROR_SERVICIO_IA',
      'El resumen generado por la IA es invalido.'
    );
  }

  const puntuacion = Math.max(0, Math.min(100, Math.round(rawAi.puntuacion_salud_financiera)));

  const allCatRows = db.prepare('SELECT id FROM categorias').all();
  const existingCatIds = new Set(allCatRows.map((r) => Number(r.id)));

  const recomendaciones = rawAi.recomendaciones.slice(0, 8).map((rec) => {
    const titulo = stripEmojis(String(rec.titulo || 'Recomendacion financiera')).slice(0, 200) || 'Recomendacion financiera';
    const detalle = stripEmojis(String(rec.detalle || 'Sin detalle adicional.')).slice(0, 1000) || 'Sin detalle adicional.';
    const prioridad = ENUMS.PRIORIDAD_IA.includes(rec.prioridad) ? rec.prioridad : 'MEDIA';
    const ahorro =
      typeof rec.ahorro_estimado_centavos === 'number' &&
      Number.isFinite(rec.ahorro_estimado_centavos) &&
      rec.ahorro_estimado_centavos >= 0
        ? roundHalfUp(rec.ahorro_estimado_centavos)
        : null;
    const catId =
      typeof rec.categoria_id === 'number' &&
      Number.isInteger(rec.categoria_id) &&
      existingCatIds.has(rec.categoria_id)
        ? rec.categoria_id
        : null;

    return {
      titulo,
      detalle,
      prioridad,
      ahorro_estimado_centavos: ahorro,
      categoria_id: catId
    };
  });

  const alertas = rawAi.alertas.slice(0, 10).map((al) => {
    const codigo = ENUMS.CODIGO_ALERTA_IA.includes(al.codigo) ? al.codigo : 'OTRO';
    const mensaje = stripEmojis(String(al.mensaje || 'Alerta financiera.')).slice(0, 500) || 'Alerta financiera.';
    const catId =
      typeof al.categoria_id === 'number' &&
      Number.isInteger(al.categoria_id) &&
      existingCatIds.has(al.categoria_id)
        ? al.categoria_id
        : null;

    return {
      codigo,
      mensaje,
      categoria_id: catId
    };
  });

  sendSuccess(
    res,
    200,
    {
      tipo_analisis: tipoAnalisis,
      periodo: { anio, mes },
      pregunta,
      resumen: resumenLimpio,
      puntuacion_salud_financiera: puntuacion,
      recomendaciones,
      alertas,
      modelo: config.GEMINI_MODEL || 'gemini-2.5-flash',
      generado_en: nowIsoUtc()
    },
    null
  );
}

module.exports = {
  analyzeFinances,
  handleParseCompraLibre,
  resetAiRateLimitForTests
};
