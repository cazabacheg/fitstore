package com.fitstore.catalog.catalogo.service.carga;

import com.fitstore.catalog.catalogo.dto.ErrorFilaDto;
import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.service.carga.ModeloCarga.ProductoCarga;
import com.fitstore.catalog.catalogo.service.carga.ModeloCarga.VarianteCarga;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida las filas de la carga masiva contra el catálogo (micro 4.4.1): no crea nada, solo arma el informe de errores por
 * fila y, de paso, los productos que se crearían. Las reglas: código nuevo, tallas, colores y categorías que existen,
 * precios válidos y los datos del producto iguales en todas sus filas.
 */
public final class ValidadorCarga {

    private static final Pattern CODIGO = Pattern.compile("^[A-Z0-9]{2,20}$");
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");
    private static final int STOCK_MAXIMO = 1_000_000;

    /** Resultado de validar: errores por fila y los productos válidos, en el orden del archivo. */
    public record Resultado(List<ErrorFilaDto> errores, List<ProductoCarga> productos, int variantes) {
    }

    private record PrimeraFila(int numero, String nombre, String descripcion, BigDecimal precio, Set<Long> categorias) {
    }

    private final Map<String, Talla> tallas = new LinkedHashMap<>();
    private final Map<String, Color> colores = new LinkedHashMap<>();
    private final Map<String, List<Categoria>> categorias = new LinkedHashMap<>();
    private final Set<String> codigosExistentes;

    public ValidadorCarga(List<Talla> tallasActivas, List<Color> coloresActivos, List<Categoria> categoriasActivas,
            Set<String> codigosExistentes) {
        tallasActivas.forEach(t -> tallas.put(t.getCodigo().toUpperCase(Locale.ROOT), t));
        coloresActivos.forEach(c -> colores.put(clave(c.getNombre()), c));
        categoriasActivas.forEach(c -> categorias.computeIfAbsent(clave(c.getNombre()), k -> new ArrayList<>()).add(c));
        this.codigosExistentes = codigosExistentes;
    }

    public static String clave(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public Resultado validar(List<FilaCarga> filas) {
        List<ErrorFilaDto> errores = new ArrayList<>();
        Map<String, PrimeraFila> primeras = new LinkedHashMap<>();
        Map<String, List<VarianteCarga>> variantesPorCodigo = new LinkedHashMap<>();
        Map<String, Integer> variantesVistas = new LinkedHashMap<>();
        Set<String> codigosConError = new LinkedHashSet<>();
        int variantesValidas = 0;

        for (FilaCarga fila : filas) {
            int antes = errores.size();
            String codigo = fila.valor("codigo").toUpperCase(Locale.ROOT);
            boolean primeraDelCodigo = !primeras.containsKey(codigo);

            if (codigo.isEmpty()) {
                error(errores, fila, "codigo", "Falta el código del producto");
            } else if (!CODIGO.matcher(codigo).matches()) {
                error(errores, fila, "codigo", "El código \"" + codigo + "\" debe tener de 2 a 20 letras o números");
            } else if (primeraDelCodigo && codigosExistentes.contains(codigo)) {
                error(errores, fila, "codigo", "El código " + codigo + " ya existe; la carga masiva solo crea productos nuevos");
            }

            String nombre = fila.valor("nombre");
            if (nombre.length() < 3 || nombre.length() > 120) {
                error(errores, fila, "nombre", "El nombre debe tener de 3 a 120 caracteres");
            }
            String descripcion = fila.valor("descripcion");
            if (descripcion.length() > 5000) {
                error(errores, fila, "descripcion", "La descripción no puede pasar de 5 000 caracteres");
            }
            BigDecimal precioBase = decimal(fila.valor("precio_base"));
            if (precioBase == null || precioBase.signum() <= 0 || precioBase.compareTo(PRECIO_MAXIMO) > 0) {
                error(errores, fila, "precio_base", "El precio base debe ser un monto mayor a 0 con hasta 2 decimales");
            }
            Set<Long> categoriaIds = categoriasDe(fila, errores);

            Talla talla = tallas.get(fila.valor("talla").toUpperCase(Locale.ROOT));
            if (fila.valor("talla").isEmpty()) {
                error(errores, fila, "talla", "Falta la talla");
            } else if (talla == null) {
                error(errores, fila, "talla", "La talla \"" + fila.valor("talla") + "\" no existe");
            }
            Color color = colores.get(clave(fila.valor("color")));
            if (fila.valor("color").isEmpty()) {
                error(errores, fila, "color", "Falta el color");
            } else if (color == null) {
                error(errores, fila, "color", "El color \"" + fila.valor("color") + "\" no existe");
            }

            BigDecimal precioVariante = null;
            if (!fila.valor("precio_variante").isEmpty()) {
                precioVariante = decimal(fila.valor("precio_variante"));
                if (precioVariante == null || precioVariante.signum() <= 0 || precioVariante.compareTo(PRECIO_MAXIMO) > 0) {
                    error(errores, fila, "precio_variante", "El precio de la variante debe ser un monto mayor a 0");
                    precioVariante = null;
                }
            }
            int stock = 0;
            if (!fila.valor("stock_inicial").isEmpty()) {
                try {
                    stock = Integer.parseInt(fila.valor("stock_inicial").replace(".0", ""));
                    if (stock < 0 || stock > STOCK_MAXIMO) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    error(errores, fila, "stock_inicial", "El stock inicial debe ser un entero de 0 o más");
                    stock = 0;
                }
            }

            if (!codigo.isEmpty() && CODIGO.matcher(codigo).matches()) {
                if (primeraDelCodigo) {
                    primeras.put(codigo, new PrimeraFila(fila.numero(), nombre, descripcion, precioBase, categoriaIds));
                } else {
                    comparar(errores, fila, primeras.get(codigo), nombre, descripcion, precioBase, categoriaIds);
                }
                if (talla != null && color != null) {
                    String variante = codigo + "|" + talla.getId() + "|" + color.getId();
                    Integer previa = variantesVistas.putIfAbsent(variante, fila.numero());
                    if (previa != null) {
                        error(errores, fila, "talla", "La variante (" + codigo + ", " + talla.getCodigo() + ", " + color.getNombre()
                                + ") está repetida; ya aparece en la fila " + previa);
                    }
                }
            }

            boolean filaValida = errores.size() == antes;
            if (!filaValida) {
                codigosConError.add(codigo);
            } else {
                variantesValidas++;
                variantesPorCodigo.computeIfAbsent(codigo, k -> new ArrayList<>())
                        .add(new VarianteCarga(talla.getId(), color.getId(), precioVariante, stock));
            }
        }

        List<ProductoCarga> productos = new ArrayList<>();
        variantesPorCodigo.forEach((codigo, variantes) -> {
            PrimeraFila p = primeras.get(codigo);
            productos.add(new ProductoCarga(codigo, p.nombre(), p.descripcion(), p.precio(), new ArrayList<>(p.categorias()), variantes));
        });
        return new Resultado(errores, productos, variantesValidas);
    }

    private void comparar(List<ErrorFilaDto> errores, FilaCarga fila, PrimeraFila primera, String nombre, String descripcion,
            BigDecimal precio, Set<Long> categoriaIds) {
        String campo = null;
        if (!primera.nombre().equals(nombre)) {
            campo = "nombre";
        } else if (!primera.descripcion().equals(descripcion)) {
            campo = "descripcion";
        } else if (primera.precio() != null && precio != null && primera.precio().compareTo(precio) != 0) {
            campo = "precio_base";
        } else if (!primera.categorias().equals(categoriaIds)) {
            campo = "categorias";
        }
        if (campo != null) {
            error(errores, fila, campo, "Los datos del producto no coinciden con los de la fila " + primera.numero()
                    + " (mismo código, distinto " + campo + ")");
        }
    }

    private Set<Long> categoriasDe(FilaCarga fila, List<ErrorFilaDto> errores) {
        Set<Long> ids = new LinkedHashSet<>();
        for (String nombre : Arrays.stream(fila.valor("categorias").split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList()) {
            List<Categoria> coincidencias = categorias.getOrDefault(clave(nombre), List.of());
            if (coincidencias.isEmpty()) {
                error(errores, fila, "categorias", "La categoría \"" + nombre + "\" no existe");
            } else if (coincidencias.size() > 1) {
                error(errores, fila, "categorias", "La categoría \"" + nombre + "\" es ambigua: hay varias con ese nombre");
            } else {
                ids.add(coincidencias.get(0).getId());
            }
        }
        return ids;
    }

    private static BigDecimal decimal(String texto) {
        if (texto.isEmpty()) {
            return null;
        }
        try {
            BigDecimal valor = new BigDecimal(texto.replace(',', '.'));
            return valor.scale() > 2 && valor.stripTrailingZeros().scale() > 2 ? null : valor.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    private static void error(List<ErrorFilaDto> errores, FilaCarga fila, String columna, String mensaje) {
        errores.add(new ErrorFilaDto(fila.numero(), columna, mensaje));
    }
}
