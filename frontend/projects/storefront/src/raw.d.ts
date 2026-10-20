// Solo para las pruebas: lectura de styles.css con el módulo de Node que ya trae Vitest.
declare module 'node:fs' {
  export function readFileSync(ruta: string, codificacion: 'utf8'): string;
}
declare const process: { cwd(): string };
