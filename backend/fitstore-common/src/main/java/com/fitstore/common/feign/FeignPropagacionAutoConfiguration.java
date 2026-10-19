package com.fitstore.common.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Interceptores de Feign que propagan el JWT del usuario y el {@code X-Request-Id}. */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FeignPropagacionAutoConfiguration {

    @Bean
    PropagacionJwtInterceptor propagacionJwtInterceptor() {
        return new PropagacionJwtInterceptor();
    }

    @Bean
    PropagacionRequestIdInterceptor propagacionRequestIdInterceptor() {
        return new PropagacionRequestIdInterceptor();
    }
}
