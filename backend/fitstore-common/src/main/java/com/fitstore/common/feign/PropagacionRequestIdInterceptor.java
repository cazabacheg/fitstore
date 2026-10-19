package com.fitstore.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Reenvía el {@code X-Request-Id} de la petición entrante para que la correlación llegue a todos los servicios. */
public class PropagacionRequestIdInterceptor implements RequestInterceptor {

    public static final String CABECERA = "X-Request-Id";

    @Override
    public void apply(RequestTemplate plantilla) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest peticion = atributos.getRequest();
            String requestId = peticion.getHeader(CABECERA);
            if (requestId != null && !requestId.isBlank()) {
                plantilla.header(CABECERA, requestId);
            }
        }
    }
}
