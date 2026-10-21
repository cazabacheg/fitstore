package com.fitstore.settings.pruebas;

import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Reemplaza a Garage en las pruebas: guarda los archivos en memoria. */
@Component
public class AlmacenEnMemoria implements FileStorage {

    public static final String BASE_PUBLICA = "http://media.prueba/media";

    private final Map<String, byte[]> archivos = new ConcurrentHashMap<>();

    @Override
    public void guardar(TipoBucket bucket, String clave, byte[] contenido, String contentType) {
        archivos.put(bucket + "/" + clave, contenido);
    }

    @Override
    public byte[] leer(TipoBucket bucket, String clave) {
        byte[] contenido = archivos.get(bucket + "/" + clave);
        if (contenido == null) {
            throw new RecursoNoEncontradoException("No existe " + clave);
        }
        return contenido;
    }

    @Override
    public void eliminar(TipoBucket bucket, String clave) {
        archivos.remove(bucket + "/" + clave);
    }

    @Override
    public boolean existe(TipoBucket bucket, String clave) {
        return archivos.containsKey(bucket + "/" + clave);
    }

    @Override
    public String urlPublica(String clave) {
        return BASE_PUBLICA + "/" + clave;
    }

    public void vaciar() {
        archivos.clear();
    }

    public int cantidad() {
        return archivos.size();
    }
}
