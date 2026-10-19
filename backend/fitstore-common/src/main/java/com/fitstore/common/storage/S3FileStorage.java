package com.fitstore.common.storage;

import com.fitstore.common.api.RecursoNoEncontradoException;
import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Adaptador de {@link FileStorage} sobre la API S3 (Garage en local y en la VM; S3 real en AWS). */
public class S3FileStorage implements FileStorage {

    private final S3Client cliente;
    private final StorageProperties propiedades;

    /**
     * Cliente S3 según las propiedades. Los checksums solo se calculan cuando la operación los exige: los S3
     * compatibles como Garage no soportan los checksums por defecto que el SDK 2.30+ agrega a cada petición.
     */
    public static S3Client crearCliente(StorageProperties propiedades) {
        var constructor = S3Client.builder()
                .region(Region.of(propiedades.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(propiedades.getAccessKey(), propiedades.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(propiedades.isPathStyle()).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        if (propiedades.getEndpoint() != null && !propiedades.getEndpoint().isBlank()) {
            constructor.endpointOverride(URI.create(propiedades.getEndpoint()));
        }
        return constructor.build();
    }

    public S3FileStorage(S3Client cliente, StorageProperties propiedades) {
        this.cliente = cliente;
        this.propiedades = propiedades;
    }

    @Override
    public void guardar(TipoBucket bucket, String clave, byte[] contenido, String contentType) {
        cliente.putObject(
                PutObjectRequest.builder().bucket(nombre(bucket)).key(clave).contentType(contentType).build(),
                RequestBody.fromBytes(contenido));
    }

    @Override
    public byte[] leer(TipoBucket bucket, String clave) {
        try {
            return cliente.getObjectAsBytes(GetObjectRequest.builder().bucket(nombre(bucket)).key(clave).build())
                    .asByteArray();
        } catch (NoSuchKeyException e) {
            throw new RecursoNoEncontradoException("No encontramos el archivo " + clave);
        }
    }

    @Override
    public void eliminar(TipoBucket bucket, String clave) {
        cliente.deleteObject(DeleteObjectRequest.builder().bucket(nombre(bucket)).key(clave).build());
    }

    @Override
    public boolean existe(TipoBucket bucket, String clave) {
        try {
            cliente.headObject(HeadObjectRequest.builder().bucket(nombre(bucket)).key(clave).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public String urlPublica(String clave) {
        String base = propiedades.getMediaBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/" + clave;
    }

    private String nombre(TipoBucket bucket) {
        return bucket == TipoBucket.PUBLICO ? propiedades.getBucketPublico() : propiedades.getBucketPrivado();
    }
}
