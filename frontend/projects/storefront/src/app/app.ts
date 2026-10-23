import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CarritoService } from './features/carrito/carrito.service';

@Component({
  imports: [RouterOutlet],
  selector: 'fs-root',
  templateUrl: './app.html',
})
export class App {
  constructor() {
    // CA-4-61: el contador de la bolsa se carga al arrancar (la sesión ya se recuperó en el inicializador)
    inject(CarritoService).iniciarApp();
  }
}
