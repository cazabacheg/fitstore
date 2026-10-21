package com.fitstore.catalog.catalogo.service.carga;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Plantilla descargable de la carga masiva: una fila por variante (CSV o XLSX con una hoja de instrucciones). */
public final class PlantillaCarga {

    private static final List<List<String>> EJEMPLOS = List.of(
            List.of("LEG01", "Legging Pro Compresión", "Tela de compresión y tiro alto", "59.90", "Leggings", "S", "negro", "", "10"),
            List.of("LEG01", "Legging Pro Compresión", "Tela de compresión y tiro alto", "59.90", "Leggings", "M", "negro", "64.90", "5"));

    private static final List<String> INSTRUCCIONES = List.of(
            "Carga masiva de productos - instrucciones",
            "",
            "Cada fila es una variante (una talla y un color). Las filas con el mismo código forman un solo producto.",
            "codigo: base del SKU, de 2 a 20 letras mayúsculas o números. Debe ser nuevo: la carga solo crea productos.",
            "nombre: de 3 a 120 caracteres. Igual en todas las filas del mismo código.",
            "descripcion: texto del producto, hasta 5 000 caracteres. Igual en todas las filas del mismo código.",
            "precio_base: precio en soles con IGV, mayor que 0, con punto o coma decimal. Igual en todas las filas del mismo código.",
            "categorias: nombres de categorías que ya existen, separados por | (por ejemplo: Leggings|Nueva colección).",
            "talla: código de una talla que ya existe (XS, S, M, L, XL, XXL...).",
            "color: nombre de un color que ya existe en la lista de colores.",
            "precio_variante: opcional. Precio propio de esa variante, mayor que 0. Si queda vacío usa el precio base.",
            "stock_inicial: opcional. Unidades que ingresan al crear la variante (entero, 0 o más). Genera una entrada de stock.",
            "",
            "Los productos se crean en BORRADOR; las imágenes se suben después y luego se publican.",
            "Máximo 2 000 filas y 5 MB por archivo. Primero se valida (no se crea nada) y luego se confirma.",
            "El informe de validación vale 30 minutos; pasado ese tiempo hay que subir el archivo de nuevo.");

    private PlantillaCarga() {
    }

    public static byte[] csv() {
        StringBuilder texto = new StringBuilder("﻿").append(String.join(",", LectorCarga.COLUMNAS)).append("\n");
        for (List<String> fila : EJEMPLOS) {
            texto.append(String.join(",", fila.stream().map(PlantillaCarga::escapar).toList())).append("\n");
        }
        return texto.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escapar(String valor) {
        return valor.contains(",") || valor.contains("\"") ? "\"" + valor.replace("\"", "\"\"") + "\"" : valor;
    }

    public static byte[] xlsx() {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloCabecera = libro.createCellStyle();
            estiloCabecera.setFont(negrita);

            Sheet productos = libro.createSheet("Productos");
            Row cabecera = productos.createRow(0);
            for (int c = 0; c < LectorCarga.COLUMNAS.size(); c++) {
                cabecera.createCell(c).setCellValue(LectorCarga.COLUMNAS.get(c));
                cabecera.getCell(c).setCellStyle(estiloCabecera);
                productos.setColumnWidth(c, 5000);
            }
            for (int f = 0; f < EJEMPLOS.size(); f++) {
                Row fila = productos.createRow(f + 1);
                for (int c = 0; c < EJEMPLOS.get(f).size(); c++) {
                    String valor = EJEMPLOS.get(f).get(c);
                    if (!valor.isEmpty()) {
                        fila.createCell(c).setCellValue(valor);
                    }
                }
            }

            Sheet instrucciones = libro.createSheet("Instrucciones");
            instrucciones.setColumnWidth(0, 20000);
            for (int i = 0; i < INSTRUCCIONES.size(); i++) {
                instrucciones.createRow(i).createCell(0).setCellValue(INSTRUCCIONES.get(i));
            }
            instrucciones.getRow(0).getCell(0).setCellStyle(estiloCabecera);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar la plantilla", e);
        }
    }
}
