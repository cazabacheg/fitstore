/**
 * Bloques de maquetación de las pantallas de la tienda (diseño aprobado: `docs/diseno/pantallas/marca.css`).
 *
 * Las clases de página del diseño (`fs-tarjeta`, `fs-dos-columnas`, `fs-titulo-pagina`…) no están en la hoja global
 * (`styles.css` solo trae las de `ui/` y las de la landing y el listado), así que las pantallas de carrito, checkout,
 * pedidos, pago y detalle las traen en sus propios `styles`. Solo usan tokens (`--fs-*`), de modo que cambian con la
 * marca. Se agregan a `styles: [ESTILOS_PANTALLA, …]` de cada componente.
 */
export const ESTILOS_PANTALLA = `
  :host {
    display: block;
  }
  .fs-tarjeta {
    background: var(--fs-color-superficie);
    border: 1px solid var(--fs-color-linea);
    border-radius: var(--fs-radio-lg);
    padding: 24px;
    box-sizing: border-box;
    min-width: 0;
  }
  .fs-titulo-pagina {
    margin: 0;
    font-size: 28px;
    line-height: 34px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }
  .fs-titulo-seccion {
    margin: 0;
    font-size: 17px;
    line-height: 24px;
    font-weight: 600;
  }
  .fs-eyebrow {
    font-size: 12px;
    line-height: 16px;
    font-weight: 600;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }
  .fs-migas {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 13px;
    line-height: 20px;
    color: var(--fs-color-texto-suave);
  }
  .fs-migas a {
    color: var(--fs-color-texto-suave);
    text-decoration: none;
  }
  .fs-entre {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }
  hr.fs-linea {
    border: 0;
    border-top: 1px solid var(--fs-color-linea);
    margin: 0;
  }
  .fs-miniatura {
    width: 72px;
    flex: none;
    aspect-ratio: 4 / 5;
    border-radius: var(--fs-radio-sm);
    background: var(--fs-color-superficie-alterna);
    display: grid;
    place-items: center;
    color: var(--fs-color-texto-suave);
    overflow: hidden;
  }
  .fs-miniatura img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  .fs-dos-columnas {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 380px;
    gap: 48px;
    align-items: start;
  }
  .fs-lateral {
    position: sticky;
    top: 24px;
  }
  .fs-fila-monto {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-variant-numeric: tabular-nums;
  }
  .fs-fila-monto--exito {
    color: var(--fs-color-exito);
  }
  .fs-fila-monto--total {
    font-size: 18px;
    font-weight: 700;
  }
  @media (max-width: 720px) {
    .fs-dos-columnas {
      grid-template-columns: minmax(0, 1fr);
      gap: 24px;
    }
    .fs-lateral {
      position: static;
    }
    .fs-titulo-pagina {
      font-size: 24px;
      line-height: 30px;
    }
    .fs-tarjeta {
      padding: 16px;
    }
  }
`;
