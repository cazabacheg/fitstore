package com.fitstore.common.storage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Garage real en Testcontainers (contenedor genérico) inicializado con el mismo {@code infra/garage/garage-init.sh}
 * que usa el compose, para verificar también esa inicialización (SPEC-0a I5).
 */
public final class GarageDePrueba {

    public static final String ACCESS_KEY = "GK0123456789abcdef01234567";
    public static final String SECRET_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    public static final String BUCKET_PUBLICO = "fitstore-media";
    public static final String BUCKET_PRIVADO = "fitstore-privado";

    private static final Path INFRA_GARAGE = Path.of("..", "..", "infra", "garage").toAbsolutePath().normalize();
    private static final String RPC_SECRET_DE_PRUEBA = "0000000000000000000000000000000000000000000000000000000000000001";
    private static final String TOKEN_ADMIN_DE_PRUEBA = "token-admin-de-prueba";

    private static final Network RED = Network.newNetwork();
    private static final GenericContainer<?> GARAGE = new GenericContainer<>("dxflrs/garage:v2.4.1")
            .withNetwork(RED)
            .withNetworkAliases("garage")
            .withEnv("GARAGE_RPC_SECRET", RPC_SECRET_DE_PRUEBA)
            .withEnv("GARAGE_ADMIN_TOKEN", TOKEN_ADMIN_DE_PRUEBA)
            .withCopyFileToContainer(MountableFile.forHostPath(INFRA_GARAGE.resolve("garage.toml")), "/etc/garage.toml")
            .withExposedPorts(3900, 3902, 3903)
            .waitingFor(Wait.forListeningPorts(3900, 3903));

    static {
        GARAGE.start();
        try (GenericContainer<?> inicializador = new GenericContainer<>("curlimages/curl:8.22.0")
                .withNetwork(RED)
                .withEnv("GARAGE_ADMIN_TOKEN", TOKEN_ADMIN_DE_PRUEBA)
                .withEnv("S3_ACCESS_KEY", ACCESS_KEY)
                .withEnv("S3_SECRET_KEY", SECRET_KEY)
                .withEnv("BUCKET_PUBLICO", BUCKET_PUBLICO)
                .withEnv("BUCKET_PRIVADO", BUCKET_PRIVADO)
                .withCopyFileToContainer(MountableFile.forHostPath(INFRA_GARAGE.resolve("garage-init.sh"), 0755), "/garage-init.sh")
                .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("sh", "/garage-init.sh"))
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(120)))) {
            inicializador.start();
        }
    }

    private GarageDePrueba() {
    }

    public static String endpointS3() {
        return "http://" + GARAGE.getHost() + ":" + GARAGE.getMappedPort(3900);
    }

    /** GET anónimo al endpoint web de Garage (puerto 3902) con el Host del bucket; devuelve el código HTTP y el cuerpo. */
    public static RespuestaWeb obtenerAnonimoPorWeb(String bucket, String clave) throws Exception {
        return peticionCruda(GARAGE.getMappedPort(3902), bucket + ".web.localhost", "/" + clave);
    }

    /** GET anónimo a la API S3 (puerto 3900), sin credenciales. */
    public static RespuestaWeb obtenerAnonimoPorS3(String bucket, String clave) throws Exception {
        return peticionCruda(GARAGE.getMappedPort(3900), GARAGE.getHost() + ":" + GARAGE.getMappedPort(3900), "/" + bucket + "/" + clave);
    }

    public record RespuestaWeb(int codigo, String cuerpo) {
    }

    private static RespuestaWeb peticionCruda(int puerto, String host, String ruta) throws Exception {
        try (Socket socket = new Socket(GARAGE.getHost(), puerto)) {
            socket.setSoTimeout(10_000);
            String peticion = "GET " + ruta + " HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(peticion.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            try (InputStream in = socket.getInputStream()) {
                in.transferTo(salida);
            }
            String texto = salida.toString(StandardCharsets.UTF_8);
            int codigo = Integer.parseInt(texto.split(" ", 3)[1]);
            int separador = texto.indexOf("\r\n\r\n");
            return new RespuestaWeb(codigo, separador >= 0 ? texto.substring(separador + 4) : "");
        }
    }
}
