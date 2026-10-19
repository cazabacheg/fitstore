package com.fitstore.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** CA-0a-16: ramas de S3FileStorage que no se alcanzan con Garage (errores del servicio y URL pública). */
class S3FileStorageRamasTest {

    private final S3Client cliente = mock(S3Client.class);
    private final StorageProperties propiedades = new StorageProperties();

    private S3FileStorage almacenamiento() {
        propiedades.setMediaBaseUrl("https://tienda.ejemplo.duckdns.org/media");
        return new S3FileStorage(cliente, propiedades);
    }

    private static S3Exception errorS3(int estado) {
        return (S3Exception) S3Exception.builder().statusCode(estado).message("error " + estado).build();
    }

    @Test
    @DisplayName("CA-0a-16 existe devuelve false cuando S3 responde 404")
    void existeEn404() {
        when(cliente.headObject(any(HeadObjectRequest.class))).thenThrow(errorS3(404));

        assertThat(almacenamiento().existe(TipoBucket.PUBLICO, "no-esta.png")).isFalse();
    }

    @Test
    @DisplayName("CA-0a-16 existe relanza los demás errores de S3 (no los confunde con 'no existe')")
    void existeRelanzaOtrosErrores() {
        when(cliente.headObject(any(HeadObjectRequest.class))).thenThrow(errorS3(500));

        assertThatThrownBy(() -> almacenamiento().existe(TipoBucket.PRIVADO, "a.pdf"))
                .isInstanceOf(S3Exception.class)
                .satisfies(e -> assertThat(((S3Exception) e).statusCode()).isEqualTo(500));
    }

    @Test
    @DisplayName("CA-0a-16 la URL pública se arma igual con la base con y sin '/' final")
    void urlPublicaConYSinBarraFinal() {
        propiedades.setMediaBaseUrl("https://tienda.ejemplo.duckdns.org/media/");
        S3FileStorage conBarra = new S3FileStorage(cliente, propiedades);
        propiedades.setMediaBaseUrl("https://tienda.ejemplo.duckdns.org/media");
        S3FileStorage sinBarra = new S3FileStorage(cliente, propiedades);

        assertThat(conBarra.urlPublica("a/b.webp")).isEqualTo("https://tienda.ejemplo.duckdns.org/media/a/b.webp");
        assertThat(sinBarra.urlPublica("a/b.webp")).isEqualTo("https://tienda.ejemplo.duckdns.org/media/a/b.webp");
    }
}
