'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const folder = path.join(__dirname, 'tests');
for (const name of fs.readdirSync(folder).filter(name => name.endsWith('.test.js')).sort()) {
  const result = spawnSync(process.execPath, [path.join(folder, name)], { stdio: 'inherit', cwd: path.resolve(__dirname, '../../backend_server') });
  if (result.error) { console.error(result.error.message); process.exit(1); }
  if (result.status !== 0) process.exit(result.status || 1);
}
