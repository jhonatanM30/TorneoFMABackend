# Guía de desarrollo

Esta guía cubre el día a día para desarrollar sobre **Más que Amigos**:
clonar, levantar el backend y el frontend en local, y correr las pruebas.
Para variables de entorno completas, build de producción y Docker Compose
en detalle, ver `DEPLOYMENT.md` en la raíz del repositorio — esta guía no
la duplica, la complementa.

## Requisitos previos

- **JDK 8** (el proyecto usa Java 8 / Spring Boot 2.7)
- Maven no es obligatorio: el proyecto trae el wrapper (`mvnw` / `mvnw.cmd`)
- **MySQL 8.x** accesible para desarrollo (H2 solo se usa en pruebas, nunca
  como base de datos real — ver `DEPLOYMENT.md`)
- **Docker** y **Docker Compose v2**, si prefieres levantar el backend en
  contenedor en vez de con Maven directo
- **Git**

## 1. Clona el repositorio

```bash
git clone <url-del-repo>
cd mas-que-amigos
```

## 2. Levanta la base de datos

Necesitas un MySQL accesible antes de arrancar el backend. Dos opciones:

- **Ya tienes un MySQL corriendo** (por ejemplo, en Docker, publicado en el
  puerto 3308): úsalo tal cual, solo ajusta las variables de conexión (ver
  paso 3).
- **No tienes ninguno todavía**: sigue la "Opción B" de `DEPLOYMENT.md`
  para levantar también MySQL desde `docker compose`.

## 3. Levanta el backend

### Opción rápida — Maven directo (recomendada para desarrollo día a día)

Exporta las 4 variables de conexión y arranca con el wrapper:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3308/masqueamigos?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true"
$env:SPRING_DATASOURCE_USERNAME="masqueamigos"
$env:SPRING_DATASOURCE_PASSWORD="desarrollo"
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.mysql.cj.jdbc.Driver"

.\mvnw.cmd spring-boot:run
```

Sin estas 4 variables la aplicación no arranca — es intencional (ver
`application.properties`), para que una configuración faltante se note de
inmediato en vez de conectarse en silencio a un valor por defecto
incorrecto. Solo viven en esa sesión de terminal; si la cierras, hay que
volver a exportarlas.

Al arrancar, Flyway aplica automáticamente cualquier migración pendiente en
`src/main/resources/db/migration/` — no hace falta ningún paso manual.

La API queda disponible en `http://localhost:57075`.

### Opción con Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Ver `DEPLOYMENT.md` para el detalle de variables, persistencia de archivos
subidos (`uploads_data`) y la opción con MySQL también en contenedor.

## 4. Levanta el frontend

El frontend vive en un repositorio **aparte**, `MasQueAmigos-Torneo`
(carpeta hermana de este backend). No tiene build ni framework: es
HTML/CSS/JS plano.

```bash
git clone <url-del-repo-frontend>
cd MasQueAmigos-Torneo
```

Sírvelo con Live Server (extensión de VS Code) u otro servidor estático
para desarrollo. El backend ya tiene CORS habilitado para los orígenes
típicos de ese flujo (`CorsConfig.java`):

- `http://127.0.0.1:5500`
- `http://localhost:5500`
- `http://localhost:5173`

Si tu backend no corre en `http://localhost:57075`, ajusta `js/config.js`
en el repo del frontend para que apunte al host/puerto correcto.

## 5. Documentación de la API mientras desarrollas

Con el backend corriendo, la documentación interactiva está en:

- Swagger UI: `http://localhost:57075/swagger-ui.html`
- OpenAPI JSON: `http://localhost:57075/v3/api-docs`

Se genera en caliente a partir de las anotaciones de los controladores, así
que siempre refleja el código actual (ver `docs/backend-standards.md`,
sección Documentación de la API).

## 6. Ejecuta las pruebas

```bash
./mvnw test
```

Usa H2 en memoria (`src/test/resources/application.properties`), no
necesita el MySQL de desarrollo corriendo. JUnit 5 + Mockito + AssertJ (ver
`docs/backend-standards.md`, sección Testing).

## 7. Prueba manual de endpoints

Para probar la API a mano (con curl, Postman o Insomnia) sin pasar por el
frontend, sigue el flujo recomendado y los ejemplos de request en
`GUIA_PRUEBAS.md`, y las herramientas sugeridas en `HERRAMIENTAS_TESTING.md`
(ambos en la raíz del repositorio).

## Ver también

- `DEPLOYMENT.md` — variables de entorno completas, build de producción,
  Docker Compose en detalle, migraciones, persistencia de archivos subidos.
- `docs/backend-standards.md` — convenciones de código, arquitectura en
  capas, manejo de errores, testing.
- `docs/frontend-standards.md` — referencia rápida al repositorio del
  frontend.
- `docs/data-model.md` — modelo de datos y reglas de negocio por entidad.
- `docs/api-spec.yml` — especificación OpenAPI estática de la API.
