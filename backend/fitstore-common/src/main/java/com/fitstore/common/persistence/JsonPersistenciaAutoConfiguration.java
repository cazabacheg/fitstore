package com.fitstore.common.persistence;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/** Registra el {@link JacksonJsonFormatMapper} en Hibernate para las columnas JSON de los servicios con JPA. */
@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass({HibernatePropertiesCustomizer.class, JsonMapper.class})
public class JsonPersistenciaAutoConfiguration {

    @Bean
    HibernatePropertiesCustomizer formatoJsonDeHibernate(JsonMapper json) {
        return propiedades -> propiedades.put(AvailableSettings.JSON_FORMAT_MAPPER, new JacksonJsonFormatMapper(json));
    }
}
