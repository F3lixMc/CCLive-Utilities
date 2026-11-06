'use strict';
const path = require('node:path');
module.exports = require('node:module').createRequire(path.resolve(__dirname, '../../../backend_server/package.json'));
