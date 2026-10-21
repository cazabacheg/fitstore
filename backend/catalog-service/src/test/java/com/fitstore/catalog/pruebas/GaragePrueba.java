package com.fitstore.catalog.pruebas;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Garage en Testcontainers (la misma imagen del compose, {@code dxflrs/garage:v2.4.1}): un nodo con su layout, la clave de
 * acceso y el bucket público, creados por la API de administración igual que {@code infra/garage/garage-init.sh}.
 * Las claves son solo de prueba.
 */
public final class GaragePrueba {

    public static final String BUCKET = "fitstore-media";
    public static final String ACCESS_KEY = "GK0123456789abcdef01234567";
    public static final String SECRET_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String TOKEN_ADMIN = "token-admin-de-prueba";

    private static final GenericContainer<?> GARAGE = new GenericContainer<>("dxflrs/garage:v2.4.1")
            .withEnv("GARAGE_RPC_SECRET", "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210")
            .withEnv("GARAGE_ADMIN_TOKEN", TOKEN_ADMIN)
            .withCopyFileToContainer(MountableFile.forClasspathResource("garage-prueba.toml"), "/etc/garage.toml")
            .withExposedPorts(3900, 3903)
            // la imagen no trae shell: se espera con peticiones HTTP desde afuera (200 = sano, 503 = aún sin layout)
            .waitingFor(Wait.forHttp("/health").forPort(3903).forStatusCodeMatching(c -> c == 200 || c == 503)
                    .withStartupTimeout(Duration.ofSeconds(90)));

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static boolean listo;

    private GaragePrueba() {
    }

    /** Arranca Garage una vez, lo deja con bucket y clave, y apunta el almacenamiento del servicio a él. */
    public static synchronized void registrar(DynamicPropertyRegistry registro) {
        if (!listo) {
            GARAGE.start();
            inicializar();
            listo = true;
        }
        Supplier<Object> endpoint = () -> "http://" + GARAGE.getHost() + ":" + GARAGE.getMappedPort(3900);
        registro.add("fitstore.storage.endpoint", endpoint);
        registro.add("fitstore.storage.region", () -> "garage");
        registro.add("fitstore.storage.access-key", () -> ACCESS_KEY);
        registro.add("fitstore.storage.secret-key", () -> SECRET_KEY);
        registro.add("fitstore.storage.bucket-publico", () -> BUCKET);
    }

    private static void inicializar() {
        try {
            String estado = llamar("GET", "/GetClusterStatus", null);
            String nodo = estado.replaceAll("(?s).*?\"id\"\\s*:\\s*\"([0-9a-f]{64})\".*", "$1");
            llamar("POST", "/UpdateClusterLayout",
                    "{\"roles\":[{\"id\":\"" + nodo + "\",\"zone\":\"dc1\",\"capacity\":1073741824,\"tags\":[]}]}");
            llamar("POST", "/ApplyClusterLayout", "{\"version\":1}");
            llamar("POST", "/ImportKey", "{\"accessKeyId\":\"" + ACCESS_KEY + "\",\"secretAccessKey\":\"" + SECRET_KEY
                    + "\",\"name\":\"prueba\"}");
            esperarSano();
            llamar("POST", "/CreateBucket", "{\"globalAlias\":\"" + BUCKET + "\"}");
            String info = llamar("GET", "/GetBucketInfo?globalAlias=" + BUCKET, null);
            String bucketId = info.replaceAll("(?s).*?\"id\"\\s*:\\s*\"([0-9a-f]{64})\".*", "$1");
            llamar("POST", "/AllowBucketKey", "{\"bucketId\":\"" + bucketId + "\",\"accessKeyId\":\"" + ACCESS_KEY
                    + "\",\"permissions\":{\"read\":true,\"write\":true,\"owner\":true}}");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar Garage para las pruebas", e);
        }
    }

    private static void esperarSano() throws Exception {
        for (int i = 0; i < 60; i++) {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(adminUri("/health")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 200) {
                return;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Garage no quedó sano");
    }

    private static URI adminUri(String ruta) {
        String base = "http://" + GARAGE.getHost() + ":" + GARAGE.getMappedPort(3903);
        return URI.create(base + (ruta.startsWith("/health") ? ruta : "/v2" + ruta));
    }

    private static String llamar(String metodo, String ruta, String cuerpo) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(adminUri(ruta))
                .header("Authorization", "Bearer " + TOKEN_ADMIN).header("Content-Type", "application/json");
        peticion.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
        HttpResponse<String> r = HTTP.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
        // 409 = ya existía (igual que garage-init.sh)
        if (r.statusCode() != 200 && r.statusCode() != 409) {
            throw new IllegalStateException(metodo + " " + ruta + " respondió " + r.statusCode() + ": " + r.body());
        }
        return r.body();
    }
}
