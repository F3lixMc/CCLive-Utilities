'use strict';
const assert = require('assert');
const { isAdminUuid } = require('../../../backend_server/website_backend/admin-eligible');

assert.strictEqual(isAdminUuid('47a5c2fc-b317-4180-b96f-ca977c51eb62'), true);
assert.strictEqual(isAdminUuid('47A5C2FCB3174180B96FCA977C51EB62'), true);
assert.strictEqual(isAdminUuid('6a814aa9-acd6-4180-a84c-a816632d7aa1'), true);
assert.strictEqual(isAdminUuid('069a79f4-44e1-4626-a5b0-bac67538bf5a'), false);
assert.strictEqual(isAdminUuid('firestarter03'), false);
assert.strictEqual(isAdminUuid(null), false);
console.log('admin-eligible tests ok');
