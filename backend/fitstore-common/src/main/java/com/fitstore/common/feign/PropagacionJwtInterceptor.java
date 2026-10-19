package com.fitstore.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Reenvía el JWT del usuario de la petición entrante en las llamadas Feign a otros servicios. */
public class PropagacionJwtInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate plantilla) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest peticion = atributos.getRequest();
            String autorizacion = peticion.getHeader("Authorization");
            if (autorizacion != null && !autorizacion.isBlank()) {
                plantilla.header("Authorization", autorizacion);
            }
        }
    }
}
