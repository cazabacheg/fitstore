import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BannerServicioNoDisponible } from './ui/toast/banner-servicio-no-disponible';
import { Toast } from './ui/toast/toast';

@Component({
  imports: [RouterOutlet, BannerServicioNoDisponible, Toast],
  selector: 'fs-root',
  templateUrl: './app.html',
})
export class App {}
