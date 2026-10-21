// @ts-check
const { defineConfig, globalIgnores } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = defineConfig([
  // Los clientes de API son código generado (R5): no se revisan con ESLint.
  globalIgnores([
    'dist/**',
    'coverage/**',
    '.angular/**',
    'node_modules/**',
    'projects/shared/src/lib/api/**',
  ]),
  {
    files: ['**/*.ts'],
    extends: [
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        { type: 'attribute', prefix: 'fs', style: 'camelCase' },
      ],
      '@angular-eslint/component-selector': [
        'error',
        { type: 'element', prefix: 'fs', style: 'kebab-case' },
      ],
    },
  },
  // Un servicio usado como valor desde el espacio de nombres (`catalogApi.TiendaService`) arrastra el cliente
  // entero al bundle inicial. Los servicios se importan desde 'shared/api/<servicio>'; los tipos pueden
  // seguir con el espacio de nombres.
  {
    files: ['**/*.ts'],
    ignores: ['**/*.spec.ts', '**/testing/**', '**/testing.ts'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector:
            'MemberExpression[object.name=/^(auth|catalog|delivery|notification|order|payment|settings)Api$/][property.name=/Service$/]',
          message: "Importa el servicio desde 'shared/api/<servicio>' en lugar de usar el espacio de nombres.",
        },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },
]);
