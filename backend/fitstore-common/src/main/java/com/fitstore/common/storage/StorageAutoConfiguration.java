package com.fitstore.common.storage;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

/** Almacenamiento de archivos, activo con {@code fitstore.storage.habilitado=true}. */
@AutoConfiguration
@ConditionalOnClass(S3Client.class)
@ConditionalOnBooleanProperty("fitstore.storage.habilitado")
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    S3Client s3Client(StorageProperties propiedades) {
        return S3FileStorage.crearCliente(propiedades);
    }

    @Bean
    @ConditionalOnMissingBean(FileStorage.class)
    FileStorage fileStorage(S3Client cliente, StorageProperties propiedades) {
        return new S3FileStorage(cliente, propiedades);
    }
}
