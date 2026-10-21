package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.ResultadoCargaDto;
import com.fitstore.catalog.catalogo.dto.ValidacionCargaDto;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/** Carga masiva de productos por CSV o XLSX: validar sin crear nada y confirmar todo o nada (HU-2-12, micro 4.4.1). */
public interface CargaMasivaService {

    /** Archivo descargable de la plantilla. */
    record Archivo(byte[] contenido, String tipo, String nombre) {
    }

    Archivo plantilla(String formato);

    /** Valida el archivo (máximo 2 000 filas y 5 MB) y guarda el informe 30 minutos; no crea nada. */
    ValidacionCargaDto validar(MultipartFile archivo);

    /**
     * Crea todo lo validado en una sola transacción, en BORRADOR, con una entrada de stock "Carga masiva" por cada
     * {@code stock_inicial} mayor que 0. Una validación vencida, con errores o ya usada responde 409.
     */
    ResultadoCargaDto confirmar(UUID validacionId, UUID usuarioId);
}
