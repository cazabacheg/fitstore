package com.fitstore.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Hibernate 7.2 solo trae FormatMapper para Jackson 2; Boot 4 usa Jackson 3, así que la librería aporta el suyo. */
class JacksonJsonFormatMapperTest {

    private final JacksonJsonFormatMapper mapper = new JacksonJsonFormatMapper(JsonMapper.builder().build());

    @Test
    @DisplayName("Una columna JSON mapeada a String se guarda y se lee tal cual")
    void cadenaJsonTalCual() {
        JavaType<String> tipo = StringJavaType.INSTANCE;

        assertThat(mapper.toString("{\"a\":1}", tipo, null)).isEqualTo("{\"a\":1}");
        assertThat(mapper.fromString("{\"a\":1}", tipo, null)).isEqualTo("{\"a\":1}");
    }

    @Test
    @DisplayName("Un mapa se convierte a JSON y vuelve")
    void mapaIdaYVuelta() {
        @SuppressWarnings("unchecked")
        JavaType<Object> tipo = org.mockito.Mockito.mock(JavaType.class);
        org.mockito.Mockito.when(tipo.getJavaType()).thenReturn(Map.class);
        Map<String, Object> valor = Map.of("intentos", List.of(1, 2));

        String json = mapper.toString(valor, tipo, null);
        Object leido = mapper.fromString(json, tipo, null);

        assertThat(json).isEqualTo("{\"intentos\":[1,2]}");
        assertThat(leido).isEqualTo(valor);
    }
}
