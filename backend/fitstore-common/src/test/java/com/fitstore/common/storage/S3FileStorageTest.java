package com.fitstore.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.api.RecursoNoEncontradoException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CA-0a-16: FileStorage guarda, lee y elimina en los dos buckets de Garage; solo el público es anónimo. */
class S3FileStorageTest {

    static FileStorage almacenamiento;

    @BeforeAll
    static void preparar() {
        var propiedades = new StorageProperties();
        propiedades.setEndpoint(GarageDePrueba.endpointS3());
        propiedades.setAccessKey(GarageDePrueba.ACCESS_KEY);
        propiedades.setSecretKey(GarageDePrueba.SECRET_KEY);
        propiedades.setBucketPublico(GarageDePrueba.BUCKET_PUBLICO);
        propiedades.setBucketPrivado(GarageDePrueba.BUCKET_PRIVADO);
        propiedades.setMediaBaseUrl("https://tienda.ejemplo.duckdns.org/media");
        almacenamiento = new S3FileStorage(S3FileStorage.crearCliente(propiedades), propiedades);
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("CA-0a-16 guarda, lee y elimina un archivo en el bucket público")
    void ciclocompletoPublico() {
        almacenamiento.guardar(TipoBucket.PUBLICO, "productos/polo.txt", bytes("polo azul"), "text/plain");

        assertThat(almacenamiento.existe(TipoBucket.PUBLICO, "productos/polo.txt")).isTrue();
        assertThat(almacenamiento.leer(TipoBucket.PUBLICO, "productos/polo.txt")).isEqualTo(bytes("polo azul"));

        almacenamiento.eliminar(TipoBucket.PUBLICO, "productos/polo.txt");

        assertThat(almacenamiento.existe(TipoBucket.PUBLICO, "productos/polo.txt")).isFalse();
        assertThatThrownBy(() -> almacenamiento.leer(TipoBucket.PUBLICO, "productos/polo.txt"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("CA-0a-16 guarda, lee y elimina un archivo en el bucket privado")
    void cicloCompletoPrivado() {
        almacenamiento.guardar(TipoBucket.PRIVADO, "boletas/B001-1.pdf", bytes("pdf de prueba"), "application/pdf");

        assertThat(almacenamiento.leer(TipoBucket.PRIVADO, "boletas/B001-1.pdf")).isEqualTo(bytes("pdf de prueba"));

        almacenamiento.eliminar(TipoBucket.PRIVADO, "boletas/B001-1.pdf");

        assertThat(almacenamiento.existe(TipoBucket.PRIVADO, "boletas/B001-1.pdf")).isFalse();
    }

    @Test
    @DisplayName("CA-0a-16 los archivos de cada bucket son independientes")
    void bucketsIndependientes() {
        almacenamiento.guardar(TipoBucket.PUBLICO, "mismo-nombre.txt", bytes("publico"), "text/plain");

        assertThat(almacenamiento.existe(TipoBucket.PRIVADO, "mismo-nombre.txt")).isFalse();

        almacenamiento.eliminar(TipoBucket.PUBLICO, "mismo-nombre.txt");
    }

    @Test
    @DisplayName("CA-0a-16 solo los archivos del bucket público son accesibles sin credenciales")
    void soloElPublicoEsAnonimo() throws Exception {
        almacenamiento.guardar(TipoBucket.PUBLICO, "logo.txt", bytes("logo publico"), "text/plain");
        almacenamiento.guardar(TipoBucket.PRIVADO, "secreto.txt", bytes("dato privado"), "text/plain");

        var publico = GarageDePrueba.obtenerAnonimoPorWeb(GarageDePrueba.BUCKET_PUBLICO, "logo.txt");
        var privadoPorWeb = GarageDePrueba.obtenerAnonimoPorWeb(GarageDePrueba.BUCKET_PRIVADO, "secreto.txt");
        var privadoPorS3 = GarageDePrueba.obtenerAnonimoPorS3(GarageDePrueba.BUCKET_PRIVADO, "secreto.txt");

        assertThat(publico.codigo()).isEqualTo(200);
        assertThat(publico.cuerpo()).isEqualTo("logo publico");
        assertThat(privadoPorWeb.codigo()).isEqualTo(404);
        assertThat(privadoPorS3.codigo()).isEqualTo(403);
        assertThat(privadoPorS3.cuerpo()).doesNotContain("dato privado");
    }

    @Test
    @DisplayName("CA-0a-16 la URL pública se arma con la base de medios")
    void urlPublica() {
        assertThat(almacenamiento.urlPublica("productos/polo.webp"))
                .isEqualTo("https://tienda.ejemplo.duckdns.org/media/productos/polo.webp");
    }
}
