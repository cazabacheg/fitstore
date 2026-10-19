-- 7 bases y 7 usuarios de PostgreSQL, uno por servicio (ADR-006, SPEC-0a CA-0a-07).
-- Cada usuario solo puede conectarse a su propia base; las demás le rechazan hasta la conexión.
-- Las contraseñas y los nombres de usuario NO están aquí: llegan por variables de entorno (.env) y se leen con
-- \getenv de psql, que es el que ejecuta este archivo al crear el contenedor por primera vez.

-- auth-service
\getenv auth_usuario AUTH_DB_USER
\getenv auth_clave AUTH_DB_PASSWORD
CREATE ROLE :"auth_usuario" LOGIN PASSWORD :'auth_clave';
CREATE DATABASE auth_db OWNER :"auth_usuario";
REVOKE CONNECT ON DATABASE auth_db FROM PUBLIC;
GRANT CONNECT ON DATABASE auth_db TO :"auth_usuario";

-- catalog-service
\getenv catalog_usuario CATALOG_DB_USER
\getenv catalog_clave CATALOG_DB_PASSWORD
CREATE ROLE :"catalog_usuario" LOGIN PASSWORD :'catalog_clave';
CREATE DATABASE catalog_db OWNER :"catalog_usuario";
REVOKE CONNECT ON DATABASE catalog_db FROM PUBLIC;
GRANT CONNECT ON DATABASE catalog_db TO :"catalog_usuario";

-- settings-service
\getenv settings_usuario SETTINGS_DB_USER
\getenv settings_clave SETTINGS_DB_PASSWORD
CREATE ROLE :"settings_usuario" LOGIN PASSWORD :'settings_clave';
CREATE DATABASE settings_db OWNER :"settings_usuario";
REVOKE CONNECT ON DATABASE settings_db FROM PUBLIC;
GRANT CONNECT ON DATABASE settings_db TO :"settings_usuario";

-- order-service
\getenv order_usuario ORDER_DB_USER
\getenv order_clave ORDER_DB_PASSWORD
CREATE ROLE :"order_usuario" LOGIN PASSWORD :'order_clave';
CREATE DATABASE order_db OWNER :"order_usuario";
REVOKE CONNECT ON DATABASE order_db FROM PUBLIC;
GRANT CONNECT ON DATABASE order_db TO :"order_usuario";

-- payment-service
\getenv payment_usuario PAYMENT_DB_USER
\getenv payment_clave PAYMENT_DB_PASSWORD
CREATE ROLE :"payment_usuario" LOGIN PASSWORD :'payment_clave';
CREATE DATABASE payment_db OWNER :"payment_usuario";
REVOKE CONNECT ON DATABASE payment_db FROM PUBLIC;
GRANT CONNECT ON DATABASE payment_db TO :"payment_usuario";

-- delivery-service
\getenv delivery_usuario DELIVERY_DB_USER
\getenv delivery_clave DELIVERY_DB_PASSWORD
CREATE ROLE :"delivery_usuario" LOGIN PASSWORD :'delivery_clave';
CREATE DATABASE delivery_db OWNER :"delivery_usuario";
REVOKE CONNECT ON DATABASE delivery_db FROM PUBLIC;
GRANT CONNECT ON DATABASE delivery_db TO :"delivery_usuario";

-- notification-service
\getenv notification_usuario NOTIFICATION_DB_USER
\getenv notification_clave NOTIFICATION_DB_PASSWORD
CREATE ROLE :"notification_usuario" LOGIN PASSWORD :'notification_clave';
CREATE DATABASE notification_db OWNER :"notification_usuario";
REVOKE CONNECT ON DATABASE notification_db FROM PUBLIC;
GRANT CONNECT ON DATABASE notification_db TO :"notification_usuario";
