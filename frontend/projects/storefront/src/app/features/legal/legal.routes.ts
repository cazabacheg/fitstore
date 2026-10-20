import { Routes } from '@angular/router';
import { LibroReclamacionesPage } from './libro-reclamaciones.page';
import { PoliticaPage } from './politica.page';

/** Libro de Reclamaciones (P-3-03) y políticas de la tienda (P-3-02). */
export const LEGAL_ROUTES: Routes = [
  { path: 'libro-de-reclamaciones', component: LibroReclamacionesPage },
  { path: 'politicas/:tipo', component: PoliticaPage },
];
