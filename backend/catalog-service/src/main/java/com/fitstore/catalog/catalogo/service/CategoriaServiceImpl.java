package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.CategoriaAdminDto;
import com.fitstore.catalog.catalogo.dto.CategoriaAdminHijaDto;
import com.fitstore.catalog.catalogo.dto.CategoriaRequest;
import com.fitstore.catalog.catalogo.dto.GuiaTallasDto;
import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.GuiaTallas;
import com.fitstore.catalog.catalogo.repository.CategoriaRepository;
import com.fitstore.catalog.catalogo.repository.GuiaTallasRepository;
import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categorias;
    private final GuiaTallasRepository guias;
    private final CacheCatalogo cache;

    public CategoriaServiceImpl(CategoriaRepository categorias, GuiaTallasRepository guias, CacheCatalogo cache) {
        this.categorias = categorias;
        this.guias = guias;
        this.cache = cache;
    }

    // ---------------------------------------------------------------- lectura

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaAdminDto> listar() {
        return arbol();
    }

    private List<CategoriaAdminDto> arbol() {
        List<Categoria> todas = categorias.findAllByOrderByOrdenAscNombreAsc();
        Map<Long, Integer> cantidades = new HashMap<>();
        for (Object[] fila : categorias.contarProductosPorCategoria()) {
            cantidades.put((Long) fila[0], ((Long) fila[1]).intValue());
        }
        Set<Long> conGuia = guias.findAll().stream().map(GuiaTallas::getCategoriaId).collect(Collectors.toSet());
        Map<Long, List<Categoria>> hijasPorPadre = todas.stream().filter(c -> c.getPadreId() != null)
                .collect(Collectors.groupingBy(Categoria::getPadreId));
        List<CategoriaAdminDto> raices = new ArrayList<>();
        for (Categoria raiz : todas) {
            if (raiz.getPadreId() != null) {
                continue;
            }
            List<CategoriaAdminHijaDto> hijas = hijasPorPadre.getOrDefault(raiz.getId(), List.of()).stream()
                    .map(h -> aHijaDto(h, cantidades, conGuia)).toList();
            raices.add(new CategoriaAdminDto(raiz.getId(), null, raiz.getNombre(), raiz.getSlug(), raiz.getOrden(),
                    raiz.isActivo(), cantidades.getOrDefault(raiz.getId(), 0), conGuia.contains(raiz.getId()), hijas));
        }
        return raices;
    }

    private static CategoriaAdminHijaDto aHijaDto(Categoria h, Map<Long, Integer> cantidades, Set<Long> conGuia) {
        return new CategoriaAdminHijaDto(h.getId(), h.getPadreId(), h.getNombre(), h.getSlug(), h.getOrden(), h.isActivo(),
                cantidades.getOrDefault(h.getId(), 0), conGuia.contains(h.getId()));
    }

    /** La categoría como la ve el admin (una hija se devuelve como nodo sin hijas). */
    private CategoriaAdminDto dtoDe(Long id) {
        Categoria c = buscar(id);
        int cantidad = (int) categorias.contarProductos(id);
        boolean conGuia = guias.findByCategoriaId(id).isPresent();
        List<CategoriaAdminHijaDto> hijas = categorias.findByPadreIdOrderByOrdenAscNombreAsc(id).stream().map(h ->
                new CategoriaAdminHijaDto(h.getId(), h.getPadreId(), h.getNombre(), h.getSlug(), h.getOrden(), h.isActivo(),
                        (int) categorias.contarProductos(h.getId()), guias.findByCategoriaId(h.getId()).isPresent())).toList();
        return new CategoriaAdminDto(c.getId(), c.getPadreId(), c.getNombre(), c.getSlug(), c.getOrden(), c.isActivo(),
                cantidad, conGuia, hijas);
    }

    private Categoria buscar(Long id) {
        return categorias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos la categoría"));
    }

    // ---------------------------------------------------------------- escritura

    @Override
    public CategoriaAdminDto crear(CategoriaRequest peticion) {
        String nombre = peticion.nombre().trim();
        validarPadre(peticion.padreId(), null);
        validarNombreLibre(peticion.padreId(), nombre, null);

        Categoria nueva = new Categoria();
        nueva.setNombre(nombre);
        nueva.setPadreId(peticion.padreId());
        nueva.setSlug(Slugs.unico(nombre, 100, categorias::existsBySlug));
        nueva.setOrden(siguienteOrden(peticion.padreId()));
        categorias.saveAndFlush(nueva);
        invalidar();
        return dtoDe(nueva.getId());
    }

    @Override
    public CategoriaAdminDto editar(Long id, CategoriaRequest peticion) {
        Categoria categoria = buscar(id);
        String nombre = peticion.nombre().trim();
        boolean cambiaPadre = !java.util.Objects.equals(categoria.getPadreId(), peticion.padreId());
        validarPadre(peticion.padreId(), categoria);
        validarNombreLibre(peticion.padreId(), nombre, categoria);
        categoria.setNombre(nombre);
        if (cambiaPadre) {
            categoria.setPadreId(peticion.padreId());
            categoria.setOrden(siguienteOrden(peticion.padreId()));
        }
        categorias.saveAndFlush(categoria);
        invalidar();
        return dtoDe(id);
    }

    /** El padre debe existir y ser raíz (2 niveles); una categoría con hijas no puede pasar a ser hija. */
    private void validarPadre(Long padreId, Categoria editada) {
        if (padreId == null) {
            return;
        }
        Categoria padre = buscar(padreId);
        if (padre.getPadreId() != null) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "CATEGORIA_NIVEL_MAXIMO",
                    "Una subcategoría no puede tener subcategorías: el árbol tiene máximo 2 niveles");
        }
        if (editada != null && (editada.getId().equals(padreId) || categorias.existsByPadreId(editada.getId()))) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "CATEGORIA_NIVEL_MAXIMO",
                    "Una categoría con subcategorías no puede pasar a ser subcategoría");
        }
    }

    private void validarNombreLibre(Long padreId, String nombre, Categoria editada) {
        if (editada != null && editada.getNombre().equalsIgnoreCase(nombre)
                && java.util.Objects.equals(editada.getPadreId(), padreId)) {
            return;
        }
        boolean existe = padreId == null
                ? categorias.existsByPadreIdIsNullAndNombreIgnoreCase(nombre)
                : categorias.existsByPadreIdAndNombreIgnoreCase(padreId, nombre);
        if (existe) {
            throw new ConflictoNegocioException("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre en este nivel");
        }
    }

    private int siguienteOrden(Long padreId) {
        List<Categoria> hermanas = padreId == null ? categorias.findByPadreIdIsNullOrderByOrdenAscNombreAsc()
                : categorias.findByPadreIdOrderByOrdenAscNombreAsc(padreId);
        return hermanas.stream().mapToInt(Categoria::getOrden).max().orElse(-1) + 1;
    }

    @Override
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (categorias.existsByPadreId(id) || categorias.contarProductos(id) > 0) {
            throw new ConflictoNegocioException("CATEGORIA_EN_USO",
                    "No se puede eliminar: tiene subcategorías o productos. Desactívala en su lugar");
        }
        guias.findByCategoriaId(id).ifPresent(guias::delete);
        categorias.delete(categoria);
        categorias.flush();
        invalidar();
    }

    @Override
    public CategoriaAdminDto desactivar(Long id) {
        return cambiarActivo(id, false);
    }

    @Override
    public CategoriaAdminDto activar(Long id) {
        return cambiarActivo(id, true);
    }

    private CategoriaAdminDto cambiarActivo(Long id, boolean activo) {
        Categoria categoria = buscar(id);
        categoria.setActivo(activo);
        categorias.saveAndFlush(categoria);
        invalidar();
        return dtoDe(id);
    }

    @Override
    public List<CategoriaAdminDto> ordenar(Long id, int orden) {
        Categoria categoria = buscar(id);
        List<Categoria> hermanas = new ArrayList<>(categoria.getPadreId() == null
                ? categorias.findByPadreIdIsNullOrderByOrdenAscNombreAsc()
                : categorias.findByPadreIdOrderByOrdenAscNombreAsc(categoria.getPadreId()));
        hermanas.removeIf(h -> h.getId().equals(id));
        hermanas.add(Math.min(orden, hermanas.size()), categoria);
        for (int i = 0; i < hermanas.size(); i++) {
            hermanas.get(i).setOrden(i);
        }
        categorias.saveAllAndFlush(hermanas);
        invalidar();
        categorias.flush();
        return arbol();
    }

    @Override
    public GuiaTallasDto guardarGuiaTallas(Long categoriaId, GuiaTallasDto dto) {
        buscar(categoriaId);
        int columnas = dto.columnas().size();
        for (int i = 0; i < dto.filas().size(); i++) {
            if (dto.filas().get(i).size() != columnas) {
                throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION",
                        "La fila " + (i + 1) + " debe tener " + columnas + " valores, uno por columna");
            }
        }
        GuiaTallas guia = guias.findByCategoriaId(categoriaId).orElseGet(() -> {
            GuiaTallas nueva = new GuiaTallas();
            nueva.setCategoriaId(categoriaId);
            return nueva;
        });
        guia.setTabla(GuiaTallasConversor.aTabla(dto));
        guia.setNota(dto.nota() == null || dto.nota().isBlank() ? null : dto.nota().trim());
        guias.saveAndFlush(guia);
        cache.invalidarTodosLosProductos();
        return GuiaTallasConversor.aDto(guia);
    }

    private void invalidar() {
        cache.invalidar(CacheCatalogo.ARBOL_CATEGORIAS);
        cache.invalidarTodosLosProductos();
    }
}
