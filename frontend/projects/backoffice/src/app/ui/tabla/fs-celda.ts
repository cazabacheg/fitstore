import { Directive, TemplateRef, inject, input } from '@angular/core';

/** Plantilla de celda de `FsTabla` para una columna: `<ng-template fsCelda="estado" let-fila>…</ng-template>`. */
@Directive({ selector: 'ng-template[fsCelda]' })
export class FsCelda {
  readonly fsCelda = input.required<string>();
  readonly plantilla = inject<TemplateRef<{ $implicit: unknown }>>(TemplateRef);
}
