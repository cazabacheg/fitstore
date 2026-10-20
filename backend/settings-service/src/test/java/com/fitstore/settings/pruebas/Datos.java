package com.fitstore.settings.pruebas;

import com.fitstore.settings.configuracion.model.Beneficio;
import com.fitstore.settings.configuracion.model.ConfiguracionTienda;
import com.fitstore.settings.configuracion.model.Contenido;
import com.fitstore.settings.configuracion.model.Identidad;
import com.fitstore.settings.configuracion.model.ImagenTienda;
import com.fitstore.settings.configuracion.model.MetodoPago;
import com.fitstore.settings.configuracion.model.ModalidadEntrega;
import com.fitstore.settings.configuracion.model.Parametros;
import com.fitstore.settings.configuracion.model.Politica;
import com.fitstore.settings.configuracion.model.Tema;
import com.fitstore.settings.configuracion.model.TipoImagen;
import com.fitstore.settings.configuracion.model.TipoPolitica;
import com.fitstore.settings.reclamaciones.model.BienContratado;
import com.fitstore.settings.reclamaciones.model.Consumidor;
import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.Reclamo;
import com.fitstore.settings.reclamaciones.model.TipoBien;
import com.fitstore.settings.reclamaciones.model.TipoDocumento;
import com.fitstore.settings.reclamaciones.model.TipoReclamo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Fábricas de datos válidos para las pruebas. Son propios de cada prueba: no dependen de las semillas. */
public final class Datos {

    private Datos() {
    }

    public static ConfiguracionTienda configuracion() {
        ConfiguracionTienda c = new ConfiguracionTienda();
        c.setId(1);
        Identidad i = new Identidad();
        i.setNombre("Tienda de prueba");
        i.setRazonSocial("Tienda de Prueba S.A.C.");
        i.setRuc("20123456789");
        i.setDireccionFiscal("Jr. Gamarra 123, La Victoria");
        c.setIdentidad(i);
        Tema t = new Tema();
        t.setColorPrimario("#C9B28C");
        t.setColorSecundario("#2B2621");
        t.setColorAcento("#4E5B45");
        t.setColorFondo("#FAF9F7");
        t.setColorTexto("#26221D");
        c.setTema(t);
        Contenido ct = new Contenido();
        ct.setHeroTitulo("Muévete con estilo");
        ct.setBeneficios(List.of(new Beneficio("🚚", "Envíos a todo el Perú")));
        c.setContenido(ct);
        Parametros p = new Parametros();
        p.setMetodosPago(Set.of(MetodoPago.CULQI, MetodoPago.YAPE));
        p.setModalidadesEntrega(Set.of(ModalidadEntrega.DELIVERY_LOCAL));
        p.setExpiracionPedidoMinutos(30);
        p.setDiasReembolsoPostventa(30);
        p.setCorreosAlertas(List.of("alertas@tienda.pe"));
        c.setParametros(p);
        c.setActualizadoEn(Instant.now());
        c.setActualizadoPor("admin-1");
        return c;
    }

    public static ImagenTienda imagen(TipoImagen tipo, int orden) {
        ImagenTienda i = new ImagenTienda();
        i.setTipo(tipo);
        i.setClave("tienda/" + tipo.name().toLowerCase() + "/" + UUID.randomUUID() + ".webp");
        i.setOrden(orden);
        i.setTextoAlternativo("Imagen " + orden);
        i.setCreadoEn(Instant.now());
        return i;
    }

    public static Politica politica(TipoPolitica tipo, int version) {
        Politica p = new Politica();
        p.setTipo(tipo);
        p.setVersion(version);
        p.setContenido("# Política v" + version);
        p.setPublicadaEn(Instant.now());
        p.setPublicadaPor("admin-1");
        return p;
    }

    public static Reclamo reclamo(int anio, int correlativo) {
        Reclamo r = new Reclamo();
        r.setNumero("RC-%d-%06d".formatted(anio, correlativo));
        r.setAnio(anio);
        r.setCorrelativo(correlativo);
        r.setTipo(TipoReclamo.RECLAMO);
        r.setEstado(EstadoReclamo.REGISTRADO);
        Consumidor c = new Consumidor();
        c.setNombres("María");
        c.setApellidos("Quispe Huamán");
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento("45678912");
        c.setDomicilio("Av. Perú 456, Lima");
        c.setTelefono("987654321");
        c.setEmail("maria@correo.pe");
        c.setEsMenor(false);
        r.setConsumidor(c);
        BienContratado b = new BienContratado();
        b.setTipo(TipoBien.PRODUCTO);
        b.setDescripcion("Legging deportivo talla M");
        b.setMontoReclamado(new BigDecimal("89.90"));
        b.setPedidoCodigo("FS-000123");
        r.setBien(b);
        r.setDetalle("El producto llegó con la costura abierta.");
        r.setPedidoConsumidor("Solicito el cambio por uno nuevo.");
        r.setFechaLimite(LocalDate.of(anio, 3, 20));
        r.setIp("190.1.2.3");
        r.setCreadoEn(Instant.now());
        return r;
    }
}
