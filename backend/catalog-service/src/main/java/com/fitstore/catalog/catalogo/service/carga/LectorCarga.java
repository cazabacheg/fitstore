package com.fitstore.catalog.catalogo.service.carga;

import com.fitstore.common.api.FitstoreException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;

/**
 * Lee el archivo de la carga masiva (CSV con coma, punto y coma o tabulador, o XLSX) y lo convierte en filas por nombre de
 * columna. La cabecera puede venir en cualquier orden; los nombres se comparan sin importar mayúsculas.
 */
public final class LectorCarga {

    public static final List<String> COLUMNAS = List.of("codigo", "nombre", "descripcion", "precio_base", "categorias",
            "talla", "color", "precio_variante", "stock_inicial");
    private static final List<String> OBLIGATORIAS = List.of("codigo", "nombre", "descripcion", "precio_base", "categorias",
            "talla", "color");

    private LectorCarga() {
    }

    /** @param nombreArchivo decide el formato por la extensión: {@code .csv} o {@code .xlsx} */
    public static List<FilaCarga> leer(String nombreArchivo, byte[] contenido, int maximoFilas) {
        String nombre = nombreArchivo == null ? "" : nombreArchivo.toLowerCase(Locale.ROOT);
        List<FilaCarga> filas;
        try {
            if (nombre.endsWith(".csv")) {
                filas = leerCsv(contenido, maximoFilas);
            } else if (nombre.endsWith(".xlsx")) {
                filas = leerXlsx(contenido, maximoFilas);
            } else {
                throw rechazo("Sube un archivo CSV o XLSX");
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof FitstoreException f) {
                throw f;
            }
            throw rechazo("No se pudo leer el archivo; revisa que sea un CSV o XLSX válido");
        }
        if (filas.isEmpty()) {
            throw rechazo("El archivo no tiene filas de datos");
        }
        return filas;
    }

    private static List<FilaCarga> leerCsv(byte[] contenido, int maximoFilas) throws IOException {
        String texto = new String(contenido, StandardCharsets.UTF_8);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        String primeraLinea = texto.lines().findFirst().orElse("");
        char delimitador = delimitador(primeraLinea);
        CSVFormat formato = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(delimitador).setIgnoreEmptyLines(true)
                .setTrim(true).get();
        List<FilaCarga> filas = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(texto), formato)) {
            List<CSVRecord> registros = parser.getRecords();
            if (registros.isEmpty()) {
                throw rechazo("El archivo está vacío");
            }
            List<String> cabecera = normalizar(registros.get(0).toList());
            validarCabecera(cabecera);
            for (int i = 1; i < registros.size(); i++) {
                CSVRecord registro = registros.get(i);
                Map<String, String> valores = new LinkedHashMap<>();
                boolean vacia = true;
                for (int c = 0; c < cabecera.size(); c++) {
                    String valor = c < registro.size() ? registro.get(c) : "";
                    valores.put(cabecera.get(c), valor);
                    vacia &= valor.isBlank();
                }
                if (!vacia) {
                    filas.add(new FilaCarga((int) registro.getRecordNumber(), valores));
                    controlarMaximo(filas, maximoFilas);
                }
            }
        }
        return filas;
    }

    private static char delimitador(String cabecera) {
        long comas = cabecera.chars().filter(ch -> ch == ',').count();
        long puntosYComas = cabecera.chars().filter(ch -> ch == ';').count();
        long tabs = cabecera.chars().filter(ch -> ch == '\t').count();
        if (puntosYComas > comas && puntosYComas >= tabs) {
            return ';';
        }
        return tabs > comas ? '\t' : ',';
    }

    private static List<FilaCarga> leerXlsx(byte[] contenido, int maximoFilas) throws IOException {
        List<FilaCarga> filas = new ArrayList<>();
        DataFormatter formateador = new DataFormatter(Locale.ROOT);
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);
            Row filaCabecera = hoja.getRow(hoja.getFirstRowNum());
            if (filaCabecera == null) {
                throw rechazo("El archivo está vacío");
            }
            List<String> cabecera = new ArrayList<>();
            for (Cell celda : filaCabecera) {
                while (cabecera.size() < celda.getColumnIndex()) {
                    cabecera.add("");
                }
                cabecera.add(formateador.formatCellValue(celda));
            }
            cabecera = normalizar(cabecera);
            validarCabecera(cabecera);
            for (int i = hoja.getFirstRowNum() + 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) {
                    continue;
                }
                Map<String, String> valores = new LinkedHashMap<>();
                boolean vacia = true;
                for (int c = 0; c < cabecera.size(); c++) {
                    Cell celda = fila.getCell(c);
                    String valor = celda == null ? "" : formateador.formatCellValue(celda);
                    valores.put(cabecera.get(c), valor);
                    vacia &= valor.isBlank();
                }
                if (!vacia) {
                    filas.add(new FilaCarga(i + 1, valores));
                    controlarMaximo(filas, maximoFilas);
                }
            }
        }
        return filas;
    }

    private static List<String> normalizar(List<String> cabecera) {
        return cabecera.stream().map(c -> c.trim().toLowerCase(Locale.ROOT).replace("﻿", "")).toList();
    }

    private static void validarCabecera(List<String> cabecera) {
        List<String> faltan = OBLIGATORIAS.stream().filter(c -> !cabecera.contains(c)).toList();
        if (!faltan.isEmpty()) {
            throw rechazo("Faltan las columnas: " + String.join(", ", faltan) + ". Descarga la plantilla y úsala como base");
        }
    }

    private static void controlarMaximo(List<FilaCarga> filas, int maximo) {
        if (filas.size() > maximo) {
            String cantidad = maximo >= 1000 ? (maximo / 1000) + " " + String.format("%03d", maximo % 1000) : String.valueOf(maximo);
            throw rechazo("El archivo tiene más de " + cantidad + " filas; divídelo en varios archivos");
        }
    }

    private static FitstoreException rechazo(String mensaje) {
        return new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje);
    }
}
