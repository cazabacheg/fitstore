import { HttpClient, HttpContext, HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EstadoServicio } from './estado-servicio';
import { camposConError } from './api-error';
import { provideFitstoreHttp } from './proveedores';
import { SIN_TOAST } from './interceptores';
import { ToastService } from './toast.service';

function configurar(opciones: { avisarErroresDeCampos?: boolean } = {}) {
  TestBed.configureTestingModule({
    providers: [provideFitstoreHttp({ carrito: false, ...opciones }), provideHttpClientTesting()],
  });
  return {
    http: TestBed.inject(HttpClient),
    ctrl: TestBed.inject(HttpTestingController),
    toasts: TestBed.inject(ToastService),
    estado: TestBed.inject(EstadoServicio),
  };
}

describe('Interceptor de errores', () => {
  it('CA-0b-13 un error con formato ApiError muestra su mensaje en un toast y se propaga', () => {
    const { http, ctrl, toasts } = configurar();
    let error: HttpErrorResponse | undefined;

    http.post('/api/v1/checkout', {}).subscribe({ error: (e) => (error = e) });
    ctrl.expectOne('/api/v1/checkout').flush(
      {
        success: false,
        mensaje: 'Los precios cambiaron, revisa tu pedido',
        codigo: 'PRECIO_CAMBIADO',
      },
      { status: 409, statusText: 'Conflict' },
    );

    expect(toasts.toasts().map((t) => [t.tipo, t.mensaje])).toEqual([
      ['error', 'Los precios cambiaron, revisa tu pedido'],
    ]);
    expect(error?.status).toBe(409);
  });

  it('CA-0b-13 un 400 con campos no muestra toast: los errores quedan para el formulario', () => {
    const { http, ctrl, toasts } = configurar();
    let error: HttpErrorResponse | undefined;

    http.post('/api/v1/auth/registro', {}).subscribe({ error: (e) => (error = e) });
    ctrl.expectOne('/api/v1/auth/registro').flush(
      {
        success: false,
        mensaje: 'Revisa los datos ingresados',
        codigo: 'VALIDACION',
        errores: [{ campo: 'dni', mensaje: 'El DNI debe tener 8 dígitos' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(toasts.toasts()).toHaveLength(0);
    expect(camposConError(error)).toEqual([
      { campo: 'dni', mensaje: 'El DNI debe tener 8 dígitos' },
    ]);
  });

  const rechazoDeCampos = {
    success: false,
    mensaje: 'Revisa los datos ingresados',
    codigo: 'VALIDACION',
    errores: [{ campo: 'codigo', mensaje: 'El código usa de 2 a 20 letras mayúsculas o números' }],
  };

  it('CA-0b-34 con avisarErroresDeCampos, un 400 con campos de un formulario que no los muestra sale en un toast con el detalle de cada campo', () => {
    const { http, ctrl, toasts } = configurar({ avisarErroresDeCampos: true });
    let error: HttpErrorResponse | undefined;

    http.post('/api/v1/admin/productos', {}).subscribe({ error: (e) => (error = e) });
    ctrl
      .expectOne('/api/v1/admin/productos')
      .flush(rechazoDeCampos, { status: 400, statusText: 'Bad Request' });

    expect(toasts.toasts().map((t) => [t.tipo, t.mensaje])).toEqual([
      ['error', 'El código usa de 2 a 20 letras mayúsculas o números'],
    ]);
    expect(camposConError(error)).toHaveLength(1);
  });

  it('CA-0b-34 con avisarErroresDeCampos, un formulario que ya muestra sus campos (SIN_TOAST) no repite el aviso', () => {
    const { http, ctrl, toasts } = configurar({ avisarErroresDeCampos: true });

    http
      .post('/api/v1/admin/productos', {}, { context: new HttpContext().set(SIN_TOAST, true) })
      .subscribe({ error: () => undefined });
    ctrl
      .expectOne('/api/v1/admin/productos')
      .flush(rechazoDeCampos, { status: 400, statusText: 'Bad Request' });

    expect(toasts.toasts()).toHaveLength(0);
  });

  it('CA-0b-34 con varios campos inválidos el toast los junta', () => {
    const con = configurar({ avisarErroresDeCampos: true });
    con.http.post('/api/v1/x', {}).subscribe({ error: () => undefined });
    con.ctrl.expectOne('/api/v1/x').flush(
      {
        ...rechazoDeCampos,
        errores: [
          { campo: 'codigo', mensaje: 'El código es obligatorio' },
          { campo: 'nombre', mensaje: 'El nombre debe tener de 3 a 120 caracteres' },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(con.toasts.toasts().map((t) => t.mensaje)).toEqual([
      'El código es obligatorio. El nombre debe tener de 3 a 120 caracteres',
    ]);
  });

  it('CA-0b-13 un 400 sin campos sí muestra su mensaje en un toast', () => {
    const { http, ctrl, toasts } = configurar();

    http.get('/api/v1/productos').subscribe({ error: () => undefined });
    ctrl
      .expectOne('/api/v1/productos')
      .flush(
        { success: false, mensaje: 'Parámetro de orden inválido', codigo: 'VALIDACION' },
        { status: 400, statusText: 'Bad Request' },
      );

    expect(toasts.toasts().map((t) => t.mensaje)).toEqual(['Parámetro de orden inválido']);
  });

  it('CA-0b-13 un 503 muestra el banner "Servicio no disponible" y no un toast; se oculta al responder un servicio', () => {
    const { http, ctrl, toasts, estado } = configurar();

    http.get('/api/v1/pedidos').subscribe({ error: () => undefined });
    ctrl.expectOne('/api/v1/pedidos').flush(
      {
        success: false,
        mensaje: 'Servicio no disponible, inténtalo en un momento',
        codigo: 'SERVICIO_NO_DISPONIBLE',
      },
      { status: 503, statusText: 'Service Unavailable' },
    );
    expect(estado.noDisponible()).toBe(true);
    expect(toasts.toasts()).toHaveLength(0);

    http.get('/api/v1/productos').subscribe();
    ctrl.expectOne('/api/v1/productos').flush({ success: true, mensaje: 'ok', data: [] });
    expect(estado.noDisponible()).toBe(false);
  });

  it('CA-0b-13 una respuesta de error sin formato ApiError no rompe la app ni muestra un toast vacío', () => {
    const { http, ctrl, toasts } = configurar();
    let error: HttpErrorResponse | undefined;

    http.get('/api/v1/productos').subscribe({ error: (e) => (error = e) });
    ctrl
      .expectOne('/api/v1/productos')
      .flush('<html>Bad gateway</html>', { status: 502, statusText: 'Bad Gateway' });

    expect(error?.status).toBe(502);
    expect(toasts.toasts()).toHaveLength(0);
  });

  it('CA-0b-13 camposConError devuelve una lista vacía si el error no trae campos', () => {
    expect(camposConError(new Error('x'))).toEqual([]);
    expect(camposConError(undefined)).toEqual([]);
  });

  it('CA-0b-13 el toast se puede descartar', () => {
    const { toasts } = configurar();
    toasts.mostrar('exito', 'Pedido creado');
    const [toast] = toasts.toasts();
    toasts.descartar(toast.id);
    expect(toasts.toasts()).toHaveLength(0);
  });
});
