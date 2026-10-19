/*
 * FitStore — modelo C4 (Structurizr DSL)
 * Fuente: docs/analisis/01-analisis-macro.md (v16) y docs/analisis/micro/*.md
 * Vistas: C1 (contexto), C2 (contenedores: general, síncrono, eventos, plataforma) y despliegue.
 * Los C3 (componentes) se agregan al completar la sección 10 de cada micro.
 */
workspace "FitStore" "Plantilla white-label de e-commerce de ropa y moda para MYPEs (primer caso: ropa deportiva de Gamarra)." {

    !identifiers flat

    model {

        # ---------------------------------------------------------------- Personas
        visitante = person "Visitante" "Navega la tienda sin sesión; puede registrar un reclamo." "Cliente"
        cliente = person "Cliente" "Compra, paga y sigue sus pedidos. Requiere cuenta con DNI." "Cliente"
        operador = person "Operador" "Staff: prepara pedidos, gestiona stock, entregas y repartidores." "Staff"
        administrador = person "Administrador" "Staff: todo lo del operador + catálogo, configuración de tienda, pagos, usuarios, plantillas." "Staff"

        # ---------------------------------------------------------------- Sistemas externos
        culqi = softwareSystem "Culqi" "Pasarela de pagos peruana (sandbox): tarjetas y Yape." "Externo"
        mercadoPago = softwareSystem "Mercado Pago" "Pasarela de pagos (sandbox): tarjetas y saldo MP con Payment Brick." "Externo"
        ses = softwareSystem "Amazon SES" "Envío de correos transaccionales por SMTP (en la nube)." "Externo"
        letsEncrypt = softwareSystem "Let's Encrypt" "Autoridad certificadora gratuita (HTTPS automático)." "Externo"
        duckDns = softwareSystem "DuckDNS" "DNS dinámico gratuito: subdominios tienda. y admin." "Externo"

        # ---------------------------------------------------------------- FitStore
        fitstore = softwareSystem "FitStore" "Tienda virtual + backoffice tipo Shopify. Una instalación por cliente (single-tenant), marca configurable." {
            !docs docs
            !adrs ../adr


            # Frontends
            storefront = container "Storefront" "Tienda pública: landing, catálogo, carrito, checkout, pago, mis pedidos, libro de reclamaciones." "Angular + Tailwind CSS (SPA)" "Web"
            backoffice = container "Backoffice" "Panel de staff: dashboard, catálogo, inventario, pedidos, pagos, entregas, configuración de tienda, plantillas." "Angular + Angular Material (SPA)" "Web"

            # Borde e infraestructura
            caddy = container "Caddy" "Borde de internet: HTTPS automático, sirve los frontends y enruta /api y /media." "Caddy 2" "Borde"
            gateway = container "api-gateway" "Único punto de entrada al backend: rutas, validación de JWT, rate limiting, CORS, bloqueo de /internal, Swagger agregado." "Spring Cloud Gateway (WebFlux)" "Infra" {
                gwRutas = component "Rutas" "Definidas en api-gateway.yml (config-server): prefijo → lb://servicio." "RouteLocator (YAML)" "Servicio"
                gwSeguridad = component "SeguridadJwt" "Valida firma y expiración del JWT con el JWKS de auth; rutas públicas sin token." "SecurityWebFilterChain · resource server" "Seguridad"
                gwBloqueo = component "BloqueoInternoFilter" "Rechaza /internal/** con 404." "GlobalFilter" "Seguridad"
                gwRate = component "RateLimiting" "RequestRateLimiter con Redis; KeyResolver por IP (X-Forwarded-For de Caddy) y por usuario (sub)." "RedisRateLimiter" "Seguridad"
                gwCorrelacion = component "CorrelacionFilter" "Genera o propaga X-Request-Id." "GlobalFilter" "Servicio"
                gwCors = component "CorsConfig" "Orígenes permitidos de storefront y backoffice." "CorsWebFilter" "Seguridad"
                gwFallback = component "FallbackController" "503 con formato ApiResponse cuando un servicio no responde." "@RestController" "Servicio"
                gwDocs = component "OpenApiAgregado" "Swagger UI con un selector por servicio (protegido en cloud)." "springdoc webflux" "Servicio"
            }
            configServer = container "config-server" "Configuración técnica por servicio y entorno (dev, cloud). Sin secretos." "Spring Cloud Config (backend nativo)" "Infra"
            discovery = container "discovery-server" "Registro y descubrimiento de servicios." "Spring Cloud Netflix Eureka" "Infra"

            # Servicios de negocio
            auth = container "auth-service" "Clientes y staff (tablas separadas), login, JWT RS256 + JWKS, perfil y direcciones." "Spring Boot 4 · Spring MVC" "Servicio" {
                authAuthController = component "AuthController" "Registro, login de cliente y de staff, refresh, logout, recuperación y restablecimiento de contraseña." "@RestController /api/v1/auth" "Controlador"
                authJwksController = component "JwksController" "Publica la clave pública RSA (JWKS)." "@RestController /.well-known/jwks.json" "Controlador"
                authClienteController = component "ClienteController" "Perfil propio, cambio de contraseña y direcciones del cliente; consulta y bloqueo de clientes para staff." "@RestController /api/v1/clientes" "Controlador"
                authStaffController = component "StaffController" "Perfil propio del staff y CRUD de staff (ADMIN)." "@RestController /api/v1/staff" "Controlador"

                authRegistroService = component "RegistroService" "Registra clientes con consentimientos y publica ClienteRegistrado." "@Service" "Servicio"
                authAutenticacionService = component "AutenticacionService" "Login genérico sobre Cuenta (cliente o staff): BCrypt, hash ficticio, bloqueo temporal." "@Service" "Servicio"
                authTokenService = component "TokenService" "Emite access y refresh (RS256), rota y revoca refresh, invalida sesiones por credenciales_actualizadas_en." "@Service" "Servicio"
                authPasswordService = component "PasswordService" "Cambio, recuperación e invitación: tokens de un solo uso (hash SHA-256)." "@Service" "Servicio"
                authClienteService = component "ClienteService" "Perfil, direcciones (máx. 10, una predeterminada), consulta y bloqueo de clientes." "@Service" "Servicio"
                authStaffService = component "StaffService" "Alta con invitación, edición, activación; invariante del último ADMIN." "@Service" "Servicio"

                authClaves = component "ProveedorClavesRsa" "Carga el par RSA desde PEM (o lo genera en dev) y expone JwtEncoder y JWKSet." "@Component · Nimbus JOSE" "Seguridad"
                authIntentos = component "RegistroIntentosLogin" "Contador de fallos y bloqueo por tipo y correo." "@Component · Spring Data Redis" "Seguridad"
                authRevocados = component "RegistroRefreshRevocados" "jti revocados con TTL igual a la vida restante del token." "@Component · Spring Data Redis" "Seguridad"
                authBootstrap = component "AdminInicial" "Crea el primer ADMIN desde variables de entorno si no existe ninguno." "ApplicationRunner" "Seguridad"

                authClienteRepo = component "ClienteRepository" "Clientes." "Spring Data JPA" "Repositorio"
                authStaffRepo = component "StaffRepository" "Staff." "Spring Data JPA" "Repositorio"
                authDireccionRepo = component "DireccionRepository" "Direcciones." "Spring Data JPA" "Repositorio"
                authConsentimientoRepo = component "ConsentimientoRepository" "Consentimientos (Ley 29733)." "Spring Data JPA" "Repositorio"
                authTokenPasswordRepo = component "TokenPasswordRepository" "Tokens de recuperación e invitación." "Spring Data JPA" "Repositorio"
                authOutbox = component "OutboxPublisher" "Guarda eventos en outbox_evento y los publica en RabbitMQ con confirmación (fitstore-common)." "fitstore-common" "Comun"
            }
            catalog = container "catalog-service" "Catálogo (categorías, productos, variantes, imágenes, descuentos por volumen) + inventario (stock, reservas, kardex)." "Spring Boot 4 · Spring MVC" "Servicio" {
                catPublico = component "CatalogoPublicoController" "Categorías, listado con filtros, detalle, destacados, tallas, colores, guía de tallas." "@RestController /api/v1/{categorias,productos,tallas,colores}" "Controlador"
                catCotizacion = component "CotizacionController" "Cotiza líneas: precio vigente, descuento por volumen, disponibilidad." "@RestController /api/v1/precios" "Controlador"
                catAdmin = component "Controladores admin de catálogo" "Categorías, productos, variantes (matriz), imágenes, tallas, colores, descuentos." "@RestController /api/v1/admin/..." "Controlador"
                catInventarioCtl = component "InventarioController" "Stock, kardex, entradas, ajustes, umbrales y configuración de inventario." "@RestController /api/v1/admin/inventario" "Controlador"
                catReservaCtl = component "ReservaInternaController" "Reserva síncrona para order (no expuesto por el gateway)." "@RestController /internal/v1/inventario/reservas" "Controlador"
                catCategoriaSvc = component "CategoriaService" "Árbol de 2 niveles, orden, guía de tallas heredable." "@Service" "Servicio"
                catProductoSvc = component "ProductoService" "Productos, variantes, publicación (invariante 4), precioDesde, búsqueda con EXISTS y unaccent." "@Service" "Servicio"
                catImagenSvc = component "ImagenService" "Valida tipo real, redimensiona (1200 y 400 px) y guarda." "@Service · Thumbnailator" "Servicio"
                catDescuentoSvc = component "DescuentoService" "Política global y excepciones por producto (hasta 10 tramos)." "@Service" "Servicio"
                catCalculadora = component "CalculadoraDescuentos" "Función pura: agrupa por producto, aplica el tramo más alto, redondeo half-up." "Clase de dominio" "Servicio"
                catCotizacionSvc = component "CotizacionService" "Arma la cotización usando precios (caché) y stock (BD)." "@Service" "Servicio"
                catInventarioSvc = component "InventarioService" "Entradas, ajustes con motivo, kardex, alerta de stock bajo al cruzar el umbral." "@Service" "Servicio"
                catReservaSvc = component "ReservaService" "Reserva atómica todo-o-nada e idempotente; confirma, libera y reingresa." "@Service" "Servicio"
                catLiberador = component "LiberadorReservasVencidas" "Red de seguridad: cada minuto libera reservas vencidas que quedaron huérfanas o cuyo evento PedidoExpirado no llegó." "@Scheduled" "Servicio"
                catConsumidor = component "ConsumidorPedidos" "PedidoPagado, PedidoCancelado, PedidoExpirado (idempotente)." "@RabbitListener" "Servicio"
                catCatalogoRepos = component "Repositorios de catálogo" "Categoría, producto, variante, imagen, talla, color, guía, tramo." "Spring Data JPA" "Repositorio"
                catInventarioRepos = component "Repositorios de inventario" "Stock (UPDATE atómico), movimiento, reserva, configuración." "Spring Data JPA" "Repositorio"
                catStorage = component "FileStorage" "Puerto de archivos con adaptador S3 (Garage)." "fitstore-common" "Comun"
                catOutbox = component "OutboxPublisher" "Outbox y publicación confirmada." "fitstore-common" "Comun"
            }
            order = container "order-service" "Carrito + pedidos: checkout, snapshot, máquina de estados, expiración, cancelación. Estado de la saga." "Spring Boot 4 · Spring MVC" "Servicio" {
                ordCarritoCtl = component "CarritoController" "Carrito anónimo (X-Carrito-Id) o de cliente; fusión al iniciar sesión." "@RestController /api/v1/carrito" "Controlador"
                ordCheckoutCtl = component "CheckoutController" "Resumen del checkout y confirmación con Idempotency-Key." "@RestController /api/v1/{checkout,pedidos}" "Controlador"
                ordPedidoCtl = component "PedidoController" "Mis pedidos, detalle con línea de tiempo, cancelación." "@RestController /api/v1/pedidos" "Controlador"
                ordAdminCtl = component "AdminPedidoController" "Listado (filtro por cliente), avance de estados, cancelación, reembolso postventa, resumen de ventas." "@RestController /api/v1/admin/pedidos" "Controlador"
                ordCarritoSvc = component "CarritoService" "Líneas, límites, última cotización, fusión." "@Service" "Servicio"
                ordCheckoutSvc = component "CheckoutService" "Orquesta el checkout: llamadas en paralelo, total esperado, reserva y guardado." "@Service · CompletableFuture" "Servicio"
                ordPedidoSvc = component "PedidoService" "Transiciones condicionales, historial, cancelación con compensación, reembolso postventa." "@Service" "Servicio"
                ordMaquina = component "MaquinaEstadosPedido" "Función pura: transiciones permitidas por estado, actor y modalidad." "Clase de dominio" "Servicio"
                ordExpirador = component "ExpiradorPedidos" "Expira pedidos PENDIENTE_PAGO vencidos cada minuto." "@Scheduled" "Servicio"
                ordConsumidor = component "ConsumidorEventos" "PagoConfirmado, PagoRechazado, BoletaEmitida, PagoReembolsado, EntregaActualizada." "@RabbitListener" "Servicio"
                ordResumenSvc = component "ResumenVentasService" "Consultas del dashboard (ventas, estados, top productos)." "@Service" "Servicio"
                ordCatalogCli = component "CatalogClient" "Cotizar y reservar (timeout 2–3 s, circuit breaker)." "@FeignClient + Resilience4j" "ClienteHttp"
                ordAuthCli = component "AuthClient" "Cliente y dirección con el JWT propagado." "@FeignClient + Resilience4j" "ClienteHttp"
                ordDeliveryCli = component "DeliveryClient" "Cotizar envío." "@FeignClient + Resilience4j" "ClienteHttp"
                ordSettingsCli = component "SettingsClient" "Parámetros (caché 5 min) y nombres de ubigeo." "@FeignClient + Resilience4j" "ClienteHttp"
                ordPedidoRepo = component "PedidoRepository" "Pedido con ítems e historial; secuencia del código." "Spring Data JPA" "Repositorio"
                ordCarritoRepo = component "CarritoRepository" "Hashes order:carrito:* con TTL." "Spring Data Redis" "Repositorio"
                ordOutbox = component "OutboxPublisher" "Outbox y publicación confirmada." "fitstore-common" "Comun"
            }
            payment = container "payment-service" "Intenciones de pago, cargos (Strategy: Culqi, Mercado Pago, simulado), webhooks, reembolsos, boleta simulada en PDF." "Spring Boot 4 · Spring MVC" "Servicio" {
                payPagoCtl = component "PagoController" "Página de pago, cobro con Idempotency-Key, descarga de boleta." "@RestController /api/v1/pagos" "Controlador"
                payWebhookCtl = component "WebhookController" "Webhooks de Culqi y Mercado Pago." "@RestController /api/v1/pagos/webhooks" "Controlador"
                payAdminCtl = component "AdminPagoController" "Pagos, reembolsos fallidos, reintento y resolución manual." "@RestController /api/v1/admin/{pagos,reembolsos}" "Controlador"
                payBoletaCtl = component "BoletaInternaController" "PDF de la boleta para notification." "@RestController /internal/v1/boletas" "Controlador"
                payIntencionSvc = component "IntencionPagoService" "Crea, anula y marca intenciones según eventos del pedido." "@Service" "Servicio"
                payCobroSvc = component "CobroService" "Bloquea la intención, valida, crea transacción y cobra con la estrategia elegida." "@Service" "Servicio"
                payRegistro = component "ProveedorRegistry" "Elige la estrategia: habilitada en settings ∩ con credenciales." "@Component" "Servicio"
                payCulqi = component "CulqiProvider" "Tarjeta y Yape: cargos, reembolsos, consulta (céntimos)." "PaymentProvider · RestClient" "ClienteHttp"
                payMp = component "MercadoPagoProvider" "Payment Brick: cargos, reembolsos, consulta, firma de webhook." "PaymentProvider · SDK MP" "ClienteHttp"
                paySimulado = component "SimuladoProvider" "Aprobar o rechazar para la demo (solo por configuración técnica)." "PaymentProvider" "ClienteHttp"
                payWebhookSvc = component "WebhookService" "Deduplica, valida y confirma consultando la API." "@Service" "Servicio"
                payVerificador = component "VerificadorPendientes" "Consulta transacciones PROCESANDO de más de 2 min." "@Scheduled" "Servicio"
                payReembolsoSvc = component "ReembolsoService" "Reembolsos totales y parciales (suma ≤ cobrado) con reintentos 1, 5, 15, 60 min." "@Service · @Scheduled" "Servicio"
                payBoletaSvc = component "BoletaService" "Correlativo B001, HTML con Thymeleaf, PDF con OpenHTMLtoPDF, bucket privado." "@Service" "Servicio"
                payConsumidor = component "ConsumidorEventos" "PedidoCreado, PedidoCancelado, PedidoExpirado, PagoNoAplicable, ReembolsoSolicitado." "@RabbitListener" "Servicio"
                paySettingsCli = component "SettingsClient" "Proveedores habilitados y datos del emisor (caché 5 min)." "@FeignClient + Resilience4j" "ClienteHttp"
                payRepos = component "Repositorios de pagos" "Intención, transacción, reembolso, boleta, correlativo, webhook." "Spring Data JPA" "Repositorio"
                payStorage = component "FileStorage" "Puerto de archivos (bucket privado)." "fitstore-common" "Comun"
                payOutbox = component "OutboxPublisher" "Outbox y publicación confirmada." "fitstore-common" "Comun"
            }
            delivery = container "delivery-service" "Tarifas por ubigeo, cotización, agencias, repartidores, entregas locales y por agencia." "Spring Boot 4 · Spring MVC" "Servicio" {
                delEnvioCtl = component "EnvioController" "Opciones por distrito, cotización y agencias activas." "@RestController /api/v1/{envios,agencias}" "Controlador"
                delEntregaCtl = component "AdminEntregaController" "Entregas, avances, hoja de ruta del día, resumen." "@RestController /api/v1/admin/entregas" "Controlador"
                delMaestrosCtl = component "Controladores admin de maestros" "Repartidores, tarifas y agencias." "@RestController /api/v1/admin/{repartidores,tarifas,agencias}" "Controlador"
                delCotizacionSvc = component "CotizacionEnvioService" "Tarifa más específica: distrito → provincia → departamento (una consulta)." "@Service" "Servicio"
                delEntregaSvc = component "EntregaService" "Crea entregas, aplica avances y publica EntregaActualizada." "@Service" "Servicio"
                delMaquina = component "MaquinaEstadosEntrega" "Función pura: transiciones local y agencia, validaciones (código de seguimiento, observación)." "Clase de dominio" "Servicio"
                delMaestrosSvc = component "Servicios de maestros" "RepartidorService, TarifaService (CHECK Lima/Callao), AgenciaService." "@Service" "Servicio"
                delConsumidor = component "ConsumidorPedidos" "PedidoListoDespacho (idempotente)." "@RabbitListener" "Servicio"
                delRepos = component "Repositorios" "Tarifa, agencia, repartidor, entrega y eventos de seguimiento." "Spring Data JPA" "Repositorio"
                delOutbox = component "OutboxPublisher" "Outbox y publicación confirmada." "fitstore-common" "Comun"
            }
            notification = container "notification-service" "Correos transaccionales con la marca (Thymeleaf), editor de textos, registro de envíos y reintentos." "Spring Boot 4 · Spring MVC" "Servicio" {
                notEnviosCtl = component "AdminNotificacionController" "Registro de envíos y reenvío." "@RestController /api/v1/admin/notificaciones" "Controlador"
                notPlantillaCtl = component "AdminPlantillaController" "Editor de textos, activar/desactivar, restaurar, vista previa." "@RestController /api/v1/admin/notificaciones/plantillas" "Controlador"
                notEnrutador = component "EnrutadorEventos" "Evento → plantilla(s) y destinatarios; guarda envíos PENDIENTE; copia marca y resumen de pedido." "@RabbitListener" "Servicio"
                notCatalogo = component "CatalogoPlantillas" "13 plantillas al cliente + 4 a la tienda: variables permitidas y si son obligatorias." "Enum de dominio" "Servicio"
                notPlantillaSvc = component "PlantillaService" "Valida variables, escapa texto, restaura predeterminados." "@Service" "Servicio"
                notRenderizador = component "RenderizadorCorreo" "Layout Thymeleaf + textos editables + marca → HTML y texto plano." "@Component · Thymeleaf" "Servicio"
                notDespachador = component "DespachadorCorreos" "Toma pendientes a 1 correo/s, reintentos 1, 5, 15, 60 min." "@Scheduled" "Servicio"
                notTemporizador = component "TemporizadorConfirmacion" "Respaldo: confirma sin boleta si no llegó BoletaEmitida en 10 min." "@Scheduled" "Servicio"
                notLimpieza = component "LimpiezaRetencion" "Purga envíos y resúmenes de más de 180 días." "@Scheduled" "Servicio"
                notSender = component "SmtpEmailSender" "Implementa EmailSender con JavaMailSender (Mailpit o SES)." "EmailSender · Spring Mail" "ClienteHttp"
                notPaymentCli = component "PaymentClient" "Descarga la boleta PDF." "@FeignClient + Resilience4j" "ClienteHttp"
                notSettingsCli = component "SettingsClient" "Marca inicial si no hay copia." "@FeignClient" "ClienteHttp"
                notRepos = component "Repositorios" "Envío, plantilla, marca, resumen de pedido, evento procesado." "Spring Data JPA" "Repositorio"
            }
            settings = container "settings-service" "Configuración de tienda (marca, tema, políticas versionadas, parámetros), ubigeo INEI, Libro de Reclamaciones." "Spring Boot 4 · Spring MVC" "Servicio" {
                setPublico = component "SettingsPublicoController" "Configuración pública y políticas vigentes." "@RestController /api/v1/settings" "Controlador"
                setUbigeoCtl = component "UbigeoController" "Departamentos, provincias, distritos y buscador." "@RestController /api/v1/ubigeo" "Controlador"
                setReclamoCtl = component "ReclamoController" "Registro público de reclamos y quejas." "@RestController /api/v1/reclamos" "Controlador"
                setAdmin = component "AdminSettingsController" "Secciones, imágenes, políticas versionadas." "@RestController /api/v1/admin/settings" "Controlador"
                setAdminReclamo = component "AdminReclamoController" "Bandeja de reclamos, atención y respuesta." "@RestController /api/v1/admin/reclamos" "Controlador"
                setConfigSvc = component "ConfiguracionService" "Guarda secciones con versión optimista, invalida caché y publica ConfiguracionActualizada." "@Service" "Servicio"
                setContraste = component "ContrasteWcag" "Función pura: razón de contraste y advertencia AA." "Clase de dominio" "Servicio"
                setImagenSvc = component "ImagenTiendaService" "Logos, favicon y landing (sin SVG, redimensiona)." "@Service · Thumbnailator" "Servicio"
                setPoliticaSvc = component "PoliticaService" "Versiones inmutables de políticas." "@Service" "Servicio"
                setUbigeoSvc = component "UbigeoService" "Consultas del maestro INEI (solo lectura)." "@Service" "Servicio"
                setReclamoSvc = component "ReclamoService" "Correlativo atómico por año, fecha límite, respuesta." "@Service" "Servicio"
                setDiasHabiles = component "CalculadoraDiasHabiles" "Función pura: 15 días hábiles (sin sábados ni domingos)." "Clase de dominio" "Servicio"
                setRepos = component "Repositorios de configuración" "Configuración, imagen de tienda, política." "Spring Data JPA" "Repositorio"
                setUbigeoRepos = component "Repositorios de ubigeo" "Departamento, provincia, distrito." "Spring Data JPA" "Repositorio"
                setReclamoRepos = component "Repositorios de reclamos" "Reclamo, correlativo (UPDATE … RETURNING)." "Spring Data JPA" "Repositorio"
                setStorage = component "FileStorage" "Puerto de archivos con adaptador S3." "fitstore-common" "Comun"
                setOutbox = component "OutboxPublisher" "Outbox y publicación confirmada." "fitstore-common" "Comun"
            }

            # Datos (una instancia de PostgreSQL con una base por servicio)
            authDb = container "auth_db" "Clientes, staff, direcciones, consentimientos, tokens de contraseña, outbox." "PostgreSQL 17" "BaseDeDatos"
            catalogDb = container "catalog_db" "Catálogo, stock, movimientos, reservas, outbox." "PostgreSQL 17" "BaseDeDatos"
            orderDb = container "order_db" "Pedidos, ítems, historial, outbox." "PostgreSQL 17" "BaseDeDatos"
            paymentDb = container "payment_db" "Intenciones, transacciones, reembolsos, boletas, webhooks, outbox." "PostgreSQL 17" "BaseDeDatos"
            deliveryDb = container "delivery_db" "Tarifas, agencias, repartidores, entregas, outbox." "PostgreSQL 17" "BaseDeDatos"
            notificationDb = container "notification_db" "Envíos, plantillas, copia de marca y de pedidos." "PostgreSQL 17" "BaseDeDatos"
            settingsDb = container "settings_db" "Configuración, políticas, ubigeo, reclamos, outbox." "PostgreSQL 17" "BaseDeDatos"

            redis = container "Redis" "Rate limiting (gateway), carritos, intentos fallidos y refresh revocados, cachés de lectura." "Redis 8" "BaseDeDatos"
            rabbitmq = container "RabbitMQ" "Exchange topic fitstore.eventos; una cola por servicio consumidor + DLQ." "RabbitMQ 4" "Cola"
            garage = container "Garage" "Almacenamiento compatible con S3: bucket público (imágenes) y privado (boletas)." "Garage (S3 API)" "Almacenamiento"
            jaeger = container "Jaeger" "Trazas distribuidas (OTLP) de punta a punta." "Jaeger all-in-one" "Infra"
            mailpit = container "Mailpit" "Bandeja SMTP falsa para ver los correos en desarrollo (solo entorno dev)." "Mailpit" "Infra"
        }

        # ================================================================ Relaciones
        # ---- Personas → frontends
        visitante -> storefront "Navega y registra reclamos" "HTTPS"
        cliente -> storefront "Compra, paga y sigue sus pedidos" "HTTPS"
        operador -> backoffice "Gestiona pedidos, stock y entregas" "HTTPS"
        administrador -> backoffice "Administra catálogo, tienda, pagos y usuarios" "HTTPS"

        # ---- Frontends ↔ borde
        caddy -> storefront "Entrega los archivos estáticos" "HTTPS"
        caddy -> backoffice "Entrega los archivos estáticos" "HTTPS"
        storefront -> caddy "Llama a /api y carga /media" "JSON / HTTPS"
        backoffice -> caddy "Llama a /api y carga /media" "JSON / HTTPS"
        storefront -> culqi "Tokeniza tarjeta o Yape (Culqi Checkout)" "HTTPS"
        storefront -> mercadoPago "Tokeniza tarjeta (Payment Brick)" "HTTPS"

        # ---- Solo en desarrollo: ng serve reenvía /api directo al gateway (sin Caddy)
        storefront -> gateway "Llama a /api (proxy de ng serve, solo en desarrollo)" "JSON / HTTP" "SoloLocal"
        backoffice -> gateway "Llama a /api (proxy de ng serve, solo en desarrollo)" "JSON / HTTP" "SoloLocal"

        # ---- Borde → backend
        caddy -> gateway "Reenvía /api" "HTTP"
        caddy -> garage "Reenvía /media (bucket público, solo lectura)" "HTTP"
        caddy -> letsEncrypt "Obtiene y renueva certificados" "ACME"
        duckDns -> caddy "Resuelve tienda. y admin. hacia la VM" "DNS"
        culqi -> caddy "Notifica pagos (webhook)" "HTTPS"
        mercadoPago -> caddy "Notifica pagos (webhook firmado)" "HTTPS"

        # ---- Gateway → servicios
        gateway -> auth "Enruta /auth, /clientes, /staff, JWKS" "HTTP · Eureka"
        gateway -> catalog "Enruta /categorias, /productos, /precios, /admin/catálogo e inventario" "HTTP · Eureka"
        gateway -> order "Enruta /carrito, /checkout, /pedidos" "HTTP · Eureka"
        gateway -> payment "Enruta /pagos (incl. webhooks)" "HTTP · Eureka"
        gateway -> delivery "Enruta /envios, /agencias, /admin/entregas" "HTTP · Eureka"
        gateway -> notification "Enruta /admin/notificaciones" "HTTP · Eureka"
        gateway -> settings "Enruta /settings, /ubigeo, /reclamos" "HTTP · Eureka"
        gateway -> redis "Rate limiting por IP y usuario" "RESP"

        # ---- Llamadas síncronas entre servicios (OpenFeign + Resilience4j)
        order -> catalog "Cotiza precios y descuentos; reserva stock" "Feign · HTTP" "Sincrono"
        order -> auth "Obtiene destinatario y dirección (JWT del cliente)" "Feign · HTTP" "Sincrono"
        order -> delivery "Cotiza envío" "Feign · HTTP" "Sincrono"
        order -> settings "Parámetros y nombres de ubigeo (caché 5 min)" "Feign · HTTP" "Sincrono"
        payment -> settings "Proveedores habilitados y datos del emisor (caché 5 min)" "Feign · HTTP" "Sincrono"
        notification -> payment "Descarga la boleta para adjuntarla (/internal)" "Feign · HTTP" "Sincrono"
        notification -> settings "Marca inicial si no hay copia (solo al arrancar)" "Feign · HTTP" "Sincrono"
        payment -> culqi "Cargos, reembolsos y consulta de estado" "REST · HTTPS" "Sincrono"
        payment -> mercadoPago "Cargos, reembolsos y consulta de estado" "REST · HTTPS" "Sincrono"

        # ---- Datos
        auth -> authDb "Lee y escribe" "JDBC" "Datos"
        catalog -> catalogDb "Lee y escribe" "JDBC" "Datos"
        order -> orderDb "Lee y escribe" "JDBC" "Datos"
        payment -> paymentDb "Lee y escribe" "JDBC" "Datos"
        delivery -> deliveryDb "Lee y escribe" "JDBC" "Datos"
        notification -> notificationDb "Lee y escribe" "JDBC" "Datos"
        settings -> settingsDb "Lee y escribe" "JDBC" "Datos"
        auth -> redis "Intentos fallidos, refresh revocados" "RESP" "Datos"
        catalog -> redis "Caché de categorías, productos y descuentos" "RESP" "Datos"
        order -> redis "Carritos y caché de settings" "RESP" "Datos"
        payment -> redis "Caché de settings" "RESP" "Datos"
        settings -> redis "Caché de configuración pública y ubigeo" "RESP" "Datos"
        catalog -> garage "Guarda imágenes de productos" "S3 API" "Datos"
        settings -> garage "Guarda logos e imágenes de la landing" "S3 API" "Datos"
        payment -> garage "Guarda boletas PDF (bucket privado)" "S3 API" "Datos"
        notification -> mailpit "Envía correos (dev)" "SMTP" "Datos"
        notification -> ses "Envía correos (cloud)" "SMTP" "Datos"

        # ---- Mensajería (vista general)
        auth -> rabbitmq "Publica eventos (outbox)" "AMQP"
        catalog -> rabbitmq "Publica y consume eventos" "AMQP"
        order -> rabbitmq "Publica y consume eventos" "AMQP"
        payment -> rabbitmq "Publica y consume eventos" "AMQP"
        delivery -> rabbitmq "Publica y consume eventos" "AMQP"
        settings -> rabbitmq "Publica eventos (outbox)" "AMQP"
        notification -> rabbitmq "Consume eventos" "AMQP"

        # ---- Eventos de dominio (vista de eventos; pasan por RabbitMQ)
        order -> payment "PedidoCreado, PedidoCancelado, PedidoExpirado, PagoNoAplicable, ReembolsoSolicitado" "RabbitMQ" "Evento"
        payment -> order "PagoConfirmado, PagoRechazado, BoletaEmitida, PagoReembolsado" "RabbitMQ" "Evento"
        order -> catalog "PedidoPagado, PedidoCancelado, PedidoExpirado" "RabbitMQ" "Evento"
        order -> delivery "PedidoListoDespacho" "RabbitMQ" "Evento"
        delivery -> order "EntregaActualizada" "RabbitMQ" "Evento"
        order -> notification "PedidoCreado, PedidoPagado, PedidoCancelado, PedidoListoRecojo, PedidoRecogido" "RabbitMQ" "Evento"
        payment -> notification "BoletaEmitida, PagoReembolsado, ReembolsoFallido" "RabbitMQ" "Evento"
        delivery -> notification "EntregaActualizada" "RabbitMQ" "Evento"
        catalog -> notification "StockBajo, StockNoConfirmado" "RabbitMQ" "Evento"
        auth -> notification "ClienteRegistrado, PasswordResetSolicitado, PasswordCambiado" "RabbitMQ" "Evento"
        settings -> notification "ConfiguracionActualizada, ReclamoRegistrado, ReclamoRespondido" "RabbitMQ" "Evento"

        # ---- Plataforma (configuración, descubrimiento, trazas)
        gateway -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        gateway -> discovery "Resuelve servicios" "HTTP" "Plataforma"
        gateway -> auth "Descarga el JWKS para validar JWT" "HTTP" "Plataforma"
        discovery -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        auth -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        catalog -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        order -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        payment -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        delivery -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        notification -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        settings -> configServer "Obtiene su configuración" "HTTP" "Plataforma"
        auth -> discovery "Se registra" "HTTP" "Plataforma"
        catalog -> discovery "Se registra" "HTTP" "Plataforma"
        order -> discovery "Se registra y resuelve servicios" "HTTP" "Plataforma"
        payment -> discovery "Se registra y resuelve servicios" "HTTP" "Plataforma"
        delivery -> discovery "Se registra" "HTTP" "Plataforma"
        notification -> discovery "Se registra y resuelve servicios" "HTTP" "Plataforma"
        settings -> discovery "Se registra" "HTTP" "Plataforma"
        gateway -> jaeger "Envía trazas" "OTLP" "Plataforma"
        auth -> jaeger "Envía trazas" "OTLP" "Plataforma"
        catalog -> jaeger "Envía trazas" "OTLP" "Plataforma"
        order -> jaeger "Envía trazas" "OTLP" "Plataforma"
        payment -> jaeger "Envía trazas" "OTLP" "Plataforma"
        delivery -> jaeger "Envía trazas" "OTLP" "Plataforma"
        notification -> jaeger "Envía trazas" "OTLP" "Plataforma"
        settings -> jaeger "Envía trazas" "OTLP" "Plataforma"

        # ================================================================ C3 · auth-service
        gateway -> authAuthController "Enruta /api/v1/auth/**" "HTTP"
        gateway -> authJwksController "Descarga el JWKS" "HTTP"
        gateway -> authClienteController "Enruta /api/v1/clientes/**" "HTTP"
        gateway -> authStaffController "Enruta /api/v1/staff/**" "HTTP"
        order -> authClienteController "GET /clientes/me y /clientes/me/direcciones/{id} (JWT del cliente)" "Feign" "Sincrono"
        authAuthController -> authRegistroService "Registra"
        authAuthController -> authAutenticacionService "Autentica"
        authAuthController -> authTokenService "Refresca y cierra sesión"
        authAuthController -> authPasswordService "Recupera y restablece"
        authJwksController -> authClaves "Obtiene la clave pública"
        authClienteController -> authClienteService "Perfil y direcciones"
        authClienteController -> authPasswordService "Cambia contraseña"
        authStaffController -> authStaffService "Gestiona staff"
        authStaffController -> authPasswordService "Cambia contraseña"
        authRegistroService -> authClienteRepo "Guarda"
        authRegistroService -> authConsentimientoRepo "Guarda consentimientos"
        authRegistroService -> authTokenService "Inicia sesión tras el registro"
        authRegistroService -> authOutbox "ClienteRegistrado"
        authAutenticacionService -> authClienteRepo "Busca por correo"
        authAutenticacionService -> authStaffRepo "Busca por correo"
        authAutenticacionService -> authIntentos "Consulta y registra fallos"
        authAutenticacionService -> authTokenService "Emite tokens"
        authTokenService -> authClaves "Firma con la clave privada"
        authTokenService -> authRevocados "Revoca y verifica jti"
        authTokenService -> authClienteRepo "Verifica estado y credenciales"
        authTokenService -> authStaffRepo "Verifica estado y credenciales"
        authPasswordService -> authTokenPasswordRepo "Crea y consume tokens"
        authPasswordService -> authClienteRepo "Actualiza contraseña"
        authPasswordService -> authStaffRepo "Actualiza contraseña"
        authPasswordService -> authOutbox "PasswordResetSolicitado, PasswordCambiado"
        authClienteService -> authClienteRepo "Lee y actualiza"
        authClienteService -> authDireccionRepo "CRUD de direcciones"
        authStaffService -> authStaffRepo "CRUD"
        authStaffService -> authPasswordService "Envía invitación"
        authBootstrap -> authStaffRepo "Crea el ADMIN inicial"
        authClienteRepo -> authDb "SQL" "JDBC"
        authStaffRepo -> authDb "SQL" "JDBC"
        authDireccionRepo -> authDb "SQL" "JDBC"
        authConsentimientoRepo -> authDb "SQL" "JDBC"
        authTokenPasswordRepo -> authDb "SQL" "JDBC"
        authOutbox -> authDb "outbox_evento" "JDBC"
        authOutbox -> rabbitmq "Publica eventos" "AMQP"
        authIntentos -> redis "auth:login:*" "RESP"
        authRevocados -> redis "auth:refresh:revocado:*" "RESP"

        # ================================================================ C3 · catalog
        gateway -> catPublico "Enruta /categorias, /productos, /tallas, /colores" "HTTP"
        gateway -> catCotizacion "Enruta /precios" "HTTP"
        gateway -> catAdmin "Enruta /admin/catálogo" "HTTP"
        gateway -> catInventarioCtl "Enruta /admin/inventario" "HTTP"
        order -> catCotizacion "Cotiza carrito y checkout" "Feign"
        order -> catReservaCtl "Reserva stock (T1)" "Feign"
        catPublico -> catCategoriaSvc "Árbol"
        catPublico -> catProductoSvc "Búsqueda y detalle"
        catAdmin -> catCategoriaSvc "CRUD"
        catAdmin -> catProductoSvc "CRUD y publicación"
        catAdmin -> catImagenSvc "Sube imágenes"
        catAdmin -> catDescuentoSvc "Tramos"
        catCotizacion -> catCotizacionSvc "Cotiza"
        catCotizacionSvc -> catCalculadora "Calcula descuentos"
        catCotizacionSvc -> catDescuentoSvc "Tramos efectivos"
        catCotizacionSvc -> catInventarioRepos "Disponibilidad"
        catInventarioCtl -> catInventarioSvc "Entradas y ajustes"
        catReservaCtl -> catReservaSvc "Reserva"
        catConsumidor -> catReservaSvc "Confirma, libera, reingresa"
        catLiberador -> catReservaSvc "Libera vencidas (respaldo)"
        catReservaSvc -> catInventarioRepos "UPDATE stock atómico"
        catReservaSvc -> catInventarioSvc "Evalúa stock bajo"
        catInventarioSvc -> catInventarioRepos "Stock y kardex"
        catInventarioSvc -> catOutbox "StockBajo"
        catReservaSvc -> catOutbox "StockNoConfirmado"
        catCategoriaSvc -> catCatalogoRepos "Lee y escribe"
        catProductoSvc -> catCatalogoRepos "Lee y escribe"
        catDescuentoSvc -> catCatalogoRepos "Tramos"
        catImagenSvc -> catCatalogoRepos "Metadatos"
        catImagenSvc -> catStorage "Guarda archivos"
        catProductoSvc -> redis "Caché de detalle y destacados" "RESP"
        catCategoriaSvc -> redis "Caché del árbol" "RESP"
        catCatalogoRepos -> catalogDb "SQL" "JDBC"
        catInventarioRepos -> catalogDb "SQL" "JDBC"
        catOutbox -> catalogDb "outbox_evento" "JDBC"
        catOutbox -> rabbitmq "Publica eventos" "AMQP"
        catConsumidor -> rabbitmq "Consume de catalog.eventos" "AMQP"
        catStorage -> garage "Bucket público" "S3 API"

        # ================================================================ C3 · settings
        gateway -> setPublico "Enruta /settings" "HTTP"
        gateway -> setUbigeoCtl "Enruta /ubigeo" "HTTP"
        gateway -> setReclamoCtl "Enruta /reclamos" "HTTP"
        gateway -> setAdmin "Enruta /admin/settings" "HTTP"
        gateway -> setAdminReclamo "Enruta /admin/reclamos" "HTTP"
        order -> setPublico "Parámetros (caché 5 min)" "Feign"
        order -> setUbigeoCtl "Nombres de ubigeo" "Feign"
        payment -> setPublico "Proveedores y datos del emisor" "Feign"
        setPublico -> setConfigSvc "Lee configuración pública"
        setPublico -> setPoliticaSvc "Política vigente"
        setAdmin -> setConfigSvc "Guarda secciones"
        setAdmin -> setImagenSvc "Sube imágenes"
        setAdmin -> setPoliticaSvc "Publica versión"
        setConfigSvc -> setContraste "Valida contraste"
        setConfigSvc -> setRepos "Lee y escribe"
        setConfigSvc -> setOutbox "ConfiguracionActualizada"
        setConfigSvc -> redis "Caché settings:public" "RESP"
        setImagenSvc -> setStorage "Guarda archivos"
        setImagenSvc -> setRepos "Metadatos"
        setPoliticaSvc -> setRepos "Versiones"
        setUbigeoCtl -> setUbigeoSvc "Consulta"
        setUbigeoSvc -> setUbigeoRepos "Lee"
        setUbigeoSvc -> redis "Caché settings:ubigeo" "RESP"
        setReclamoCtl -> setReclamoSvc "Registra"
        setAdminReclamo -> setReclamoSvc "Atiende y responde"
        setReclamoSvc -> setDiasHabiles "Fecha límite"
        setReclamoSvc -> setReclamoRepos "Lee y escribe"
        setReclamoSvc -> setOutbox "ReclamoRegistrado, ReclamoRespondido"
        setRepos -> settingsDb "SQL" "JDBC"
        setUbigeoRepos -> settingsDb "SQL" "JDBC"
        setReclamoRepos -> settingsDb "SQL" "JDBC"
        setOutbox -> settingsDb "outbox_evento" "JDBC"
        setOutbox -> rabbitmq "Publica eventos" "AMQP"
        setStorage -> garage "Bucket público" "S3 API"

        # ================================================================ C3 · order
        gateway -> ordCarritoCtl "Enruta /carrito" "HTTP"
        gateway -> ordCheckoutCtl "Enruta /checkout y POST /pedidos" "HTTP"
        gateway -> ordPedidoCtl "Enruta /pedidos" "HTTP"
        gateway -> ordAdminCtl "Enruta /admin/pedidos" "HTTP"
        ordCarritoCtl -> ordCarritoSvc "Gestiona"
        ordCheckoutCtl -> ordCheckoutSvc "Resumen y confirmación"
        ordPedidoCtl -> ordPedidoSvc "Consulta y cancela"
        ordAdminCtl -> ordPedidoSvc "Avanza y cancela"
        ordAdminCtl -> ordResumenSvc "Dashboard"
        ordCarritoSvc -> ordCarritoRepo "Líneas"
        ordCarritoSvc -> ordCatalogCli "Cotiza"
        ordCheckoutSvc -> ordCarritoSvc "Lee y vacía"
        ordCheckoutSvc -> ordSettingsCli "Parámetros"
        ordCheckoutSvc -> ordAuthCli "Destinatario"
        ordCheckoutSvc -> ordCatalogCli "Cotiza y reserva"
        ordCheckoutSvc -> ordDeliveryCli "Envío"
        ordCheckoutSvc -> ordPedidoRepo "Guarda pedido"
        ordCheckoutSvc -> ordOutbox "PedidoCreado"
        ordPedidoSvc -> ordMaquina "Valida transición"
        ordPedidoSvc -> ordPedidoRepo "Actualiza"
        ordPedidoSvc -> ordOutbox "PedidoPagado, Cancelado, Expirado, ListoDespacho, ListoRecojo, Recogido, PagoNoAplicable, ReembolsoSolicitado"
        ordExpirador -> ordPedidoSvc "Expira"
        ordConsumidor -> ordPedidoSvc "Aplica eventos"
        ordResumenSvc -> ordPedidoRepo "Agrega"
        ordCatalogCli -> catalog "cotizar / reservar" "HTTP"
        ordAuthCli -> auth "/clientes/me" "HTTP"
        ordDeliveryCli -> delivery "/envios/cotizar" "HTTP"
        ordSettingsCli -> settings "/settings/public, /ubigeo" "HTTP"
        ordSettingsCli -> redis "Caché order:settings" "RESP"
        ordCarritoRepo -> redis "order:carrito:*" "RESP"
        ordPedidoRepo -> orderDb "SQL" "JDBC"
        ordOutbox -> orderDb "outbox_evento" "JDBC"
        ordOutbox -> rabbitmq "Publica eventos" "AMQP"
        ordConsumidor -> rabbitmq "Consume de order.eventos" "AMQP"

        # ================================================================ C3 · payment
        gateway -> payPagoCtl "Enruta /pagos" "HTTP"
        gateway -> payWebhookCtl "Enruta /pagos/webhooks" "HTTP"
        gateway -> payAdminCtl "Enruta /admin/pagos" "HTTP"
        notification -> payBoletaCtl "Descarga la boleta" "Feign"
        payPagoCtl -> payCobroSvc "Cobra"
        payPagoCtl -> payBoletaSvc "Descarga"
        payWebhookCtl -> payWebhookSvc "Procesa"
        payAdminCtl -> payReembolsoSvc "Reintenta o resuelve"
        payBoletaCtl -> payBoletaSvc "PDF"
        payConsumidor -> payIntencionSvc "Crea / anula"
        payConsumidor -> payReembolsoSvc "Solicita reembolso"
        payCobroSvc -> payRegistro "Estrategia"
        payCobroSvc -> payRepos "Bloquea y registra"
        payCobroSvc -> payOutbox "PagoConfirmado, PagoRechazado"
        payRegistro -> payCulqi ""
        payRegistro -> payMp ""
        payRegistro -> paySimulado ""
        payRegistro -> paySettingsCli "Proveedores habilitados"
        payWebhookSvc -> payRegistro "verificarWebhook / consultarEstado"
        payWebhookSvc -> payRepos "Registra"
        payVerificador -> payRegistro "consultarEstado"
        payReembolsoSvc -> payRegistro "reembolsar"
        payReembolsoSvc -> payRepos "Estado"
        payReembolsoSvc -> payOutbox "PagoReembolsado, ReembolsoFallido"
        payBoletaSvc -> paySettingsCli "Datos del emisor"
        payBoletaSvc -> payStorage "Guarda PDF"
        payBoletaSvc -> payRepos "Correlativo"
        payBoletaSvc -> payOutbox "BoletaEmitida"
        payIntencionSvc -> payRepos "Intenciones"
        payCulqi -> culqi "API REST" "HTTPS"
        payMp -> mercadoPago "API REST" "HTTPS"
        paySettingsCli -> settings "/settings/public" "HTTP"
        paySettingsCli -> redis "Caché payment:settings" "RESP"
        payRepos -> paymentDb "SQL" "JDBC"
        payOutbox -> paymentDb "outbox_evento" "JDBC"
        payOutbox -> rabbitmq "Publica eventos" "AMQP"
        payConsumidor -> rabbitmq "Consume de payment.eventos" "AMQP"
        payStorage -> garage "Bucket privado" "S3 API"

        # ================================================================ C3 · delivery
        gateway -> delEnvioCtl "Enruta /envios, /agencias" "HTTP"
        gateway -> delEntregaCtl "Enruta /admin/entregas" "HTTP"
        gateway -> delMaestrosCtl "Enruta /admin/maestros" "HTTP"
        order -> delEnvioCtl "Cotiza envío" "Feign"
        delEnvioCtl -> delCotizacionSvc "Cotiza"
        delEnvioCtl -> delMaestrosSvc "Agencias activas"
        delEntregaCtl -> delEntregaSvc "Avances"
        delMaestrosCtl -> delMaestrosSvc "CRUD"
        delConsumidor -> delEntregaSvc "Crea entrega"
        delEntregaSvc -> delMaquina "Valida transición"
        delEntregaSvc -> delRepos "Lee y escribe"
        delEntregaSvc -> delOutbox "EntregaActualizada"
        delCotizacionSvc -> delRepos "Tarifas"
        delMaestrosSvc -> delRepos "Lee y escribe"
        delRepos -> deliveryDb "SQL" "JDBC"
        delOutbox -> deliveryDb "outbox_evento" "JDBC"
        delOutbox -> rabbitmq "Publica eventos" "AMQP"
        delConsumidor -> rabbitmq "Consume de delivery.eventos" "AMQP"

        # ================================================================ C3 · notification
        gateway -> notEnviosCtl "Enruta /admin/notificaciones" "HTTP"
        gateway -> notPlantillaCtl "Enruta /admin/notificaciones/plantillas" "HTTP"
        notEnviosCtl -> notRepos "Consulta y reencola"
        notPlantillaCtl -> notPlantillaSvc "Edita"
        notPlantillaCtl -> notRenderizador "Vista previa"
        notPlantillaSvc -> notCatalogo "Variables permitidas"
        notPlantillaSvc -> notRepos "Textos"
        notEnrutador -> notCatalogo "Elige plantilla"
        notEnrutador -> notRepos "Envíos, marca, resumen"
        notEnrutador -> rabbitmq "Consume de notification.eventos" "AMQP"
        notDespachador -> notRepos "Pendientes"
        notDespachador -> notRenderizador "Arma el correo"
        notDespachador -> notSender "Envía"
        notDespachador -> notPaymentCli "Adjunto"
        notTemporizador -> notRepos "Crea envío sin adjunto"
        notLimpieza -> notRepos "Purga"
        notRenderizador -> notRepos "Textos y marca"
        notEnrutador -> notSettingsCli "Marca inicial"
        notPaymentCli -> payment "/internal/v1/boletas" "HTTP"
        notSettingsCli -> settings "/settings/public" "HTTP"
        notSender -> mailpit "SMTP (dev)" "SMTP"
        notSender -> ses "SMTP (cloud)" "SMTP"
        notRepos -> notificationDb "SQL" "JDBC"

        # ================================================================ C3 · gateway
        caddy -> gwSeguridad "Reenvía /api" "HTTP"
        gwSeguridad -> gwBloqueo "Siguiente filtro"
        gwBloqueo -> gwRate "Siguiente filtro"
        gwRate -> gwCorrelacion "Siguiente filtro"
        gwCorrelacion -> gwRutas "Resuelve destino"
        gwRutas -> gwFallback "Si falla"
        gwSeguridad -> auth "JWKS" "HTTP"
        gwRate -> redis "Contadores" "RESP"
        gwRutas -> discovery "Resuelve lb://" "HTTP"
        gwRutas -> auth "Enruta" "HTTP · Eureka"
        gwRutas -> catalog "Enruta" "HTTP · Eureka"
        gwRutas -> order "Enruta" "HTTP · Eureka"
        gwRutas -> payment "Enruta" "HTTP · Eureka"
        gwRutas -> delivery "Enruta" "HTTP · Eureka"
        gwRutas -> notification "Enruta" "HTTP · Eureka"
        gwRutas -> settings "Enruta" "HTTP · Eureka"
        gwDocs -> gwRutas "Lee /v3/api-docs de cada servicio"

        # ================================================================ Despliegue
        nube = deploymentEnvironment "Nube (demo)" {
            deploymentNode "VM Linux" "Azure for Students (alternativa: AWS EC2). 8 GB RAM, una sola VM." "Ubuntu 24.04 LTS" {
                deploymentNode "Docker Engine" "docker compose (infra/docker-compose.yml + docker-compose.cloud.yml)" "Docker + Compose" {
                    containerInstance caddy
                    containerInstance storefront
                    containerInstance backoffice
                    containerInstance gateway
                    containerInstance configServer
                    containerInstance discovery
                    containerInstance auth
                    containerInstance catalog
                    containerInstance order
                    containerInstance payment
                    containerInstance delivery
                    containerInstance notification
                    containerInstance settings
                    deploymentNode "postgres" "Una instancia, 7 bases y 7 usuarios" "PostgreSQL 17" {
                        containerInstance authDb
                        containerInstance catalogDb
                        containerInstance orderDb
                        containerInstance paymentDb
                        containerInstance deliveryDb
                        containerInstance notificationDb
                        containerInstance settingsDb
                    }
                    containerInstance redis
                    containerInstance rabbitmq
                    containerInstance garage
                    containerInstance jaeger
                }
            }
            deploymentNode "Internet" "Servicios externos (SaaS)" "HTTPS" {
                softwareSystemInstance culqi
                softwareSystemInstance mercadoPago
                softwareSystemInstance ses
                softwareSystemInstance letsEncrypt
                softwareSystemInstance duckDns
            }
        }

        local = deploymentEnvironment "Local (desarrollo)" {
            deploymentNode "Lyanna" "PC de desarrollo" "Ubuntu 24.04 LTS" {
                deploymentNode "Docker Engine" "docker compose (infra/docker-compose.yml + docker-compose.dev.yml)" "Docker + Compose" {
                    containerInstance gateway
                    containerInstance configServer
                    containerInstance discovery
                    containerInstance auth
                    containerInstance catalog
                    containerInstance order
                    containerInstance payment
                    containerInstance delivery
                    containerInstance notification
                    containerInstance settings
                    deploymentNode "postgres" "Una instancia, 7 bases y 7 usuarios" "PostgreSQL 17" {
                        containerInstance authDb
                        containerInstance catalogDb
                        containerInstance orderDb
                        containerInstance paymentDb
                        containerInstance deliveryDb
                        containerInstance notificationDb
                        containerInstance settingsDb
                    }
                    containerInstance redis
                    containerInstance rabbitmq
                    containerInstance garage
                    containerInstance jaeger
                    containerInstance mailpit
                }
                deploymentNode "Node.js 22" "ng serve" "Angular CLI" {
                    containerInstance storefront
                    containerInstance backoffice
                }
            }
            deploymentNode "Internet" "Pasarelas en sandbox" "HTTPS" {
                softwareSystemInstance culqi
                softwareSystemInstance mercadoPago
            }
        }
    }

    configuration {
        scope softwaresystem
    }

    views {

        systemContext fitstore "C1-Contexto" "C1 · Contexto del sistema FitStore." {
            include *
            autoLayout lr
        }

        container fitstore "C2-General" "C2 · Contenedores: vista de conjunto (entrada, servicios y mensajería). Datos, llamadas internas, eventos, pagos y plataforma tienen su propia vista." {
            include visitante cliente operador administrador storefront backoffice caddy gateway auth catalog order payment delivery notification settings rabbitmq
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "relationship.tag==Sincrono"
            exclude "relationship.tag==SoloLocal"
            exclude "caddy -> storefront"
            exclude "caddy -> backoffice"
            autoLayout lr
        }

        container fitstore "C2-Datos" "C2 · Persistencia: una base PostgreSQL por servicio, Redis y almacenamiento S3 (Garage)." {
            include auth catalog order payment delivery notification settings authDb catalogDb orderDb paymentDb deliveryDb notificationDb settingsDb redis garage gateway caddy
            exclude "relationship.tag!=Datos"
            include "gateway -> redis"
            include "caddy -> garage"
            autoLayout lr
        }

        container fitstore "C2-Pagos" "C2 · Flujo de pago: tokenización en el navegador, cargo en el backend y webhooks." {
            include cliente storefront caddy gateway order payment culqi mercadoPago
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==SoloLocal"
            exclude "relationship.tag==Datos"
            exclude "caddy -> storefront"
            exclude "gateway -> order"
            autoLayout lr
        }

        container fitstore "C2-Sincrono" "C2 · Llamadas síncronas del checkout y pagos (OpenFeign + Resilience4j)." {
            include gateway auth catalog order payment delivery notification settings culqi mercadoPago
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "gateway -> *"
            autoLayout lr
        }

        container fitstore "C2-Eventos" "C2 · Eventos de dominio entre servicios (vía RabbitMQ, saga coreografiada con outbox)." {
            include auth catalog order payment delivery notification settings
            exclude "relationship.tag==Sincrono"
            exclude "relationship.tag==Plataforma"
            autoLayout lr
        }

        container fitstore "C2-Plataforma" "C2 · Plataforma: configuración, descubrimiento y trazas." {
            include gateway configServer discovery jaeger auth catalog order payment delivery notification settings
            exclude "relationship.tag==Sincrono"
            exclude "relationship.tag==Evento"
            autoLayout tb
        }

        component catalog "C3-catalog" "C3 · Componentes de catalog-service." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "gateway -> order" "gateway -> redis" "order -> rabbitmq" "order -> redis"
            autoLayout lr
        }

        component settings "C3-settings" "C3 · Componentes de settings-service." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "gateway -> order" "gateway -> payment" "gateway -> redis" "order -> payment" "order -> rabbitmq" "order -> redis"
            exclude "payment -> garage" "payment -> order" "payment -> rabbitmq" "payment -> redis"
            autoLayout lr
        }

        component order "C3-order" "C3 · Componentes de order-service." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "auth -> rabbitmq" "auth -> redis" "catalog -> rabbitmq" "catalog -> redis" "delivery -> rabbitmq" "gateway -> auth"
            exclude "gateway -> catalog" "gateway -> delivery" "gateway -> redis" "gateway -> settings" "settings -> rabbitmq" "settings -> redis"
            autoLayout lr
        }

        component payment "C3-payment" "C3 · Componentes de payment-service." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "gateway -> notification" "gateway -> redis" "gateway -> settings" "notification -> rabbitmq" "notification -> settings" "settings -> garage"
            exclude "settings -> notification" "settings -> rabbitmq" "settings -> redis"
            autoLayout lr
        }

        component delivery "C3-delivery" "C3 · Componentes de delivery-service." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "gateway -> order" "order -> rabbitmq"
            autoLayout lr
        }

        component notification "C3-notification" "C3 · Componentes de notification-service." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "gateway -> payment" "gateway -> settings" "payment -> rabbitmq" "payment -> settings" "settings -> rabbitmq"
            autoLayout lr
        }

        component gateway "C3-gateway" "C3 · Componentes de api-gateway." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "auth -> discovery" "auth -> notification" "auth -> redis" "catalog -> discovery" "catalog -> notification" "catalog -> redis"
            exclude "delivery -> discovery" "delivery -> notification" "delivery -> order" "notification -> discovery" "notification -> payment" "notification -> settings"
            exclude "order -> auth" "order -> catalog" "order -> delivery" "order -> discovery" "order -> notification" "order -> payment"
            exclude "order -> redis" "order -> settings" "payment -> discovery" "payment -> notification" "payment -> order" "payment -> redis"
            exclude "payment -> settings" "settings -> discovery" "settings -> notification" "settings -> redis"
            autoLayout lr
        }

        component auth "C3-auth" "C3 · Componentes de auth-service." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "gateway -> order" "gateway -> redis" "order -> rabbitmq" "order -> redis"
            autoLayout lr
        }
        deployment fitstore nube "Despliegue-Nube" "Despliegue de la demo en una VM con docker compose." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            exclude "relationship.tag==SoloLocal"
            autoLayout lr
        }

        deployment fitstore local "Despliegue-Local" "Entorno de desarrollo en Lyanna." {
            include *
            exclude "relationship.tag==Plataforma"
            exclude "relationship.tag==Evento"
            autoLayout lr
        }

        styles {
            element "Element" {
                fontSize 22
            }
            element "Person" {
                shape Person
                background #0f766e
                color #ffffff
            }
            element "Staff" {
                background #115e59
            }
            element "Software System" {
                background #0d9488
                color #ffffff
            }
            element "Externo" {
                background #6b7280
                color #ffffff
            }
            element "Container" {
                background #14b8a6
                color #ffffff
            }
            element "Web" {
                shape WebBrowser
                background #0e7490
            }
            element "Borde" {
                shape Hexagon
                background #155e75
            }
            element "Infra" {
                background #64748b
            }
            element "Servicio" {
                shape RoundedBox
                background #0d9488
            }
            element "BaseDeDatos" {
                shape Cylinder
                background #334155
            }
            element "Cola" {
                shape Pipe
                background #b45309
            }
            element "Almacenamiento" {
                shape Folder
                background #475569
            }
            element "Controlador" {
                background #0e7490
            }
            element "Repositorio" {
                background #475569
            }
            element "Seguridad" {
                background #7c3aed
            }
            element "ClienteHttp" {
                background #b45309
            }
            element "Comun" {
                background #64748b
            }
            relationship "Relationship" {
                thickness 2
                fontSize 20
            }
            relationship "Sincrono" {
                color #0f766e
            }
            relationship "Evento" {
                style dashed
                color #b45309
            }
            relationship "SoloLocal" {
                style dotted
                color #0e7490
            }
            relationship "Plataforma" {
                style dotted
                color #94a3b8
            }
        }
    }
}
