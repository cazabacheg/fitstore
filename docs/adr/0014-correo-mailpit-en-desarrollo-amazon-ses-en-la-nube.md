# 14. Correo: Mailpit en desarrollo, Amazon SES en la nube

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** **Mailpit** (bandeja falsa) en local; **Amazon SES** en la nube, usado desde la VM (funciona igual en Azure o AWS) vía SMTP o API.
- **Costo:** la cuenta AWS es antigua, así que no aplica el saldo gratuito inicial de SES; el costo es de centavos (~US$0.10 por 1 000 correos en tarifa por uso; verificar el plan vigente).
- **Sandbox de SES:** al inicio solo envía a direcciones verificadas (máx. 200/día), suficiente para la demo verificando los correos de prueba. Para clientes reales se solicita salida del sandbox.
- **Plantillas** ✅: **Thymeleaf genera el HTML completo de cada correo** en `notification-service` (con logo, colores y textos de `settings-service`) y SES solo lo **envía** (no se usan plantillas de SES). El envío pasa por un puerto `EmailSender` con dos adaptadores: SMTP (Mailpit en local) y SES (en la nube); la plantilla es la misma en ambos. Así los correos son white-label, editables desde backoffice y portables. Se descartan las plantillas nativas de SES (más acopladas a AWS y más difíciles de adaptar por cliente).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
