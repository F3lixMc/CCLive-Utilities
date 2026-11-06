'use strict';
const assert = require('assert');
const fs = require('fs');
const path = require('path');

const css = fs.readFileSync(path.join(__dirname, '../../../backend_server/public/admin-style.css'), 'utf8');
const html = fs.readFileSync(path.join(__dirname, '../../../backend_server/admin_ui/admin.html'), 'utf8');

assert.match(html, /Spielername \*/);
assert.match(html, /id="staff-add-form"/);
assert.match(html, /id="staff-add-error"/);
assert.match(html, /id="staff-add-clear"/);
assert.match(html, /class="staff-suggest"/);
assert.match(css, /\.staff-field\s*\{[^}]*flex-direction:column[^}]*gap:10px/);
assert.match(css, /\.staff-input-wrap\s*\{[^}]*padding-top:8px[^}]*padding-bottom:8px/);
assert.match(css, /#staff-add-name\s*\{[^}]*width:100%/);
assert.match(css, /\.staff-clear\s*\{/);
assert.match(css, /\.staff-suggest\s*\{/);
assert.match(css, /#staff-add-form\s*\{[^}]*flex-direction:column/);

console.log('staff-form-spacing.test.js ok');
