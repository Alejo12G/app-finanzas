'use strict';

const config = require('../config');
const { sendSuccess } = require('../utils/response');
const { nowIsoUtc } = require('../utils/validators');

function handleHealth(req, res) {
  sendSuccess(res, 200, {
    estado: 'OK',
    version_api: config.VERSION_API,
    moneda: config.APP_MONEDA,
    hora_servidor: nowIsoUtc()
  }, null);
}

module.exports = {
  handleHealth
};
