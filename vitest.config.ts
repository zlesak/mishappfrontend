import {defineConfig} from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    // e2e/*.test.ts are unit tests of the Playwright helpers, not browser specs; the specs
    // themselves are *.spec.ts and stay with Playwright.
    include: ['src/main/frontend/**/*.test.ts', 'src/main/frontend/**/*.test.tsx', 'e2e/**/*.test.ts'],
    exclude: ['src/main/frontend/generated/**', 'node_modules/**'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json-summary'],
      all: true,
      include: ['src/main/frontend/js/**/*.ts', 'src/main/frontend/js/**/*.tsx'],
      exclude: [
        'src/main/frontend/**/*.test.ts',
        'src/main/frontend/**/*.test.tsx',
        'src/main/frontend/**/*.d.ts',
        'src/main/frontend/js/editorjs/editor-js.ts',
        'src/main/frontend/js/editorjs/editorjs-init.ts',
        'src/main/frontend/js/threejs/types/interfaces.ts',
      ],
    },
  },
});
