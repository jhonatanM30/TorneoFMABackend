# Despliegue — MAS-QUE-AMIGOS

Esta guía cubre requisitos, variables de entorno, build y las dos formas de
ejecutar el backend: manualmente (Maven) o con Docker Compose.

## Requisitos previos

- JDK 8 (para correr `./mvnw` localmente sin Docker)
- Maven no es estrictamente necesario: el proyecto trae el wrapper (`mvnw` /
  `mvnw.cmd`), que descarga la versión correcta de Maven la primera vez.
- MySQL 8.x accesible (propio, o vía Docker — ver más abajo). Este proyecto
  ya no soporta H2 como base de datos "real": desde la Fase 3, H2 queda
  exclusivamente para pruebas (`mvnw test`), la aplicación siempre requiere
  MySQL para arrancar.
- Docker y Docker Compose v2 (`docker compose`, sin guion) si vas a usar la
  ruta de contenedores.
- Git.

## Variables de entorno

La conexión a MySQL **no está definida** en `application.properties`: se
exige por completo desde el entorno. Sin estas 4 variables la aplicación no
arranca (a propósito — así una configuración faltante se nota de inmediato
en vez de conectarse en silencio a un valor por defecto incorrecto).

| Variable | Significado | Ejemplo |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Cadena de conexión JDBC completa a MySQL | `jdbc:mysql://localhost:3308/masqueamigos?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de MySQL | `masqueamigos` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de ese usuario | `desarrollo` |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | Driver JDBC | `com.mysql.cj.jdbc.Driver` |

Estos son los nombres exactos que Spring Boot reconoce automáticamente
(convención `spring.datasource.url` → `SPRING_DATASOURCE_URL`); no hace
falta declararlos en ningún `.properties`.

Para Docker Compose, en cambio, se arman a partir de piezas más pequeñas
(host, puerto, nombre de BD, usuario, clave) definidas en `.env` — ver
`.env.example` en la raíz del repo:

| Variable (`.env`) | Significado | Default en `.env.example` |
|---|---|---|
| `SERVER_PORT` | Puerto del host donde queda expuesto el backend | `57075` |
| `DB_HOST` | Host donde escucha MySQL, visto desde el contenedor del backend | `host.docker.internal` |
| `DB_PORT` | Puerto de MySQL | `3308` |
| `DB_NAME` | Nombre de la base de datos | `masqueamigos` |
| `DB_USER` | Usuario de MySQL | `masqueamigos` |
| `DB_PASSWORD` | Contraseña de ese usuario | `desarrollo` |
| `MYSQL_ROOT_PASSWORD` | Solo si usas la Opción B (Compose crea su propio MySQL) | `root` |

## Build

```bash
./mvnw clean package -DskipTests
```

Genera el jar ejecutable en `mas-que-amigos/target/*.jar`. Quita
`-DskipTests` si quieres que también corra la suite de pruebas (usa H2, no
necesita MySQL).

## Ejecución local, sin Docker

Ya tienes que tener un MySQL accesible (el contenedor creado en la Fase 3,
por ejemplo, publicado en `localhost:3308`). Exporta las 4 variables y
arranca:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3308/masqueamigos?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true"
$env:SPRING_DATASOURCE_USERNAME="masqueamigos"
$env:SPRING_DATASOURCE_PASSWORD="desarrollo"
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.mysql.cj.jdbc.Driver"

.\mvnw.cmd spring-boot:run
```

Esas variables solo viven en esa sesión de PowerShell; si abres una
terminal nueva hay que volver a exportarlas (o guardarlas como variables de
entorno de usuario en Windows si las vas a usar seguido).

## Ejecución con Docker Compose

### Opción A — Ya tienes un contenedor MySQL corriendo (caso actual)

Es lo que trae `docker-compose.yml` tal cual: solo define el servicio
`backend`, y se conecta al MySQL que ya tienes corriendo en Docker (el de
la Fase 3, publicado en el puerto 3308 del host) a través de
`host.docker.internal`, el nombre especial que Docker usa para que un
contenedor alcance servicios publicados en el host.

```bash
cp .env.example .env
# Ajusta .env si tu contenedor de MySQL usa otro puerto/usuario/clave.

docker compose up --build
```

El backend queda disponible en `http://localhost:57075` (o el puerto que
hayas puesto en `SERVER_PORT`). No se crea ningún contenedor de MySQL
nuevo — se reutiliza el que ya existe, con los mismos datos.

### Opción B — No tienes MySQL corriendo (otra máquina, un servidor nuevo)

Agrega este servicio a `docker-compose.yml` y cambia `DB_HOST=mysql` en tu
`.env` (el nombre del servicio pasa a ser el host, porque ambos contenedores
comparten la red interna que crea Compose):

```yaml
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: ${DB_NAME:-masqueamigos}
      MYSQL_USER: ${DB_USER:-masqueamigos}
      MYSQL_PASSWORD: ${DB_PASSWORD:-desarrollo}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
    ports:
      - "${DB_PORT:-3308}:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  mysql_data:
```

Y en el servicio `backend`, agrega para que espere a que MySQL esté listo:

```yaml
    depends_on:
      mysql:
        condition: service_healthy
```

Con esto, `docker compose up --build` levanta MySQL desde cero (imagen
oficial, datos persistidos en el volumen `mysql_data`) y el backend, todo
con un solo comando — que es el escenario que describe originalmente esta
fase para un entorno sin nada preexistente (un servidor nuevo, la máquina
de otro desarrollador, etc.).

## Migraciones de base de datos

No requieren ningún paso manual. Flyway está habilitado
(`spring.flyway.enabled=true`) y Spring Boot lo ejecuta automáticamente al
arrancar la aplicación, antes de que Hibernate valide el esquema: revisa
`src/main/resources/db/migration/`, aplica cualquier script nuevo
(`V2__...sql`, `V3__...sql`, etc., siguiendo la convención de Flyway) y dejan
registro en la tabla `flyway_schema_history` de qué versión quedó aplicada.

No hay un plugin de Maven de Flyway instalado (solo las dependencias
`flyway-core`/`flyway-mysql`, que Spring Boot usa vía autoconfiguración), así
que no existe un comando tipo `mvnw flyway:migrate` en este proyecto —
arrancar la aplicación (localmente o vía Docker Compose) es lo que dispara
las migraciones.

Para agregar un cambio de esquema nuevo: crear un archivo
`V<N>__descripcion.sql` en `src/main/resources/db/migration/` con el
siguiente número de versión disponible; se aplica solo la próxima vez que
arranque la aplicación.
