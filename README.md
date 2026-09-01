# MAS-QUE-AMIGOS

Backend de una aplicación deportiva para gestionar equipos, jugadores, partidos, alineaciones y estadísticas de una competición o liga. La API está desarrollada con Spring Boot y expone endpoints REST para crear, consultar, actualizar y eliminar registros de un sistema de fútbol amateur o semiprofesional.

## ¿Qué puede hacer este proyecto?

El proyecto es capaz de:

- Registrar y consultar equipos
- Registrar y consultar jugadores asociados a un equipo
- Crear partidos entre dos equipos
- Registrar alineaciones de jugadores en partidos
- Consultar estadísticas por jugador o partido
- Manejar errores con respuestas consistentes en JSON
- Validar datos de entrada antes de persistirlos
- Exponer la API para consumo desde frontend web o móvil

## Arquitectura

El proyecto sigue una arquitectura en capas con Spring Boot:

- Capa Controllers: expone endpoints REST
- Capa Services: contiene la lógica de negocio
- Capa Repositories: acceso a datos con Spring Data JPA
- Capa Models / Entities: mapeo de tablas de base de datos
- Capa DTOs: transporte de datos hacia y desde la API
- Capa Mapper: conversión entre entidades y DTOs
- Capa Exceptions: manejo centralizado de errores

## Tecnologías utilizadas

- Java 8
- Spring Boot 2.7.18
- Spring Web
- Spring Data JPA
- Spring Validation
- H2 Database (por defecto para pruebas y desarrollo)
- MySQL (soporte para entorno real)
- Lombok
- Maven

## Estructura principal

- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/controller](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/controller): endpoints REST
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/service](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/service): lógica de negocio
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/repository](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/repository): repositorios JPA
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/model](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/model): entidades
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/dto](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/dto): objetos de transferencia
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/exception](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/exception): manejo centralizado de errores

## Requisitos

- JDK 8+
- Maven
- Git

## Ejecución

Desde la carpeta del proyecto:

```bash
./mvnw spring-boot:run
```

La aplicación queda disponible en:

- http://localhost:8080
- Consola H2: http://localhost:8080/h2-console

## Endpoints principales

### Equipos

- GET /api/equipos
- GET /api/equipos/{nombre}
- POST /api/equipos
- PUT /api/equipos
- DELETE /api/equipos/{id}

### Jugadores

- GET /api/jugadores
- GET /api/jugadores/{nombre}
- POST /api/jugadores
- PUT /api/jugadores
- DELETE /api/jugadores/{id}

### Partidos

- GET /api/partidos
- GET /api/partidos/{nombre}
- POST /api/partidos
- DELETE /api/partidos/{id}

### Estadísticas

- GET /api/estadisticas
- GET /api/estadisticas/{id}
- POST /api/estadisticas
- DELETE /api/estadisticas/{id}

## Ejemplo de payload para equipo

```json
{
  "nombre": "Nacional",
  "directorTecnico": "Rueda",
  "imagenUrl": "https://example.com/escudo.png",
  "titulos": 10,
  "tipoClasificacion": "Primer Division"
}
```

## Ejemplo de payload para jugador

```json
{
  "nombre": "Alex",
  "posicion": "DELANTERO",
  "edad": 26,
  "dorsal": 11,
  "idEquipo": 1
}
```

## Estado del proyecto

La aplicación está lista como API REST funcional para gestionar una liga deportiva con flujo básico de CRUD, validación de entrada y manejo de errores. Puede integrarse con un frontend para una experiencia completa de gestión deportiva.

## Mejoras aplicadas

- Corrección de la dependencia de validación incompatible con Spring Boot 2.7
- Activación de validaciones en DTOs y controladores
- Estandarización de rutas bajo /api
- Manejo más consistente de errores
- Documentación centralizada para uso y despliegue

            {
              "nombre": "Marino",
              "posicion": "DELANTERO",
              "edad": 26,
              "dorsal": 11,
              "idEquipo": 1
            }
         Ejemplo de Response (JSON)
           {
             "id": null,
             "nombre": null,
             "posicion": null,
             "edad": 0,
             "dorsal": 0,
             "indicadorRespuesta": "Success",
             "mensaje": "Gooool! El jugador Marino se guardÃ³ en la base de datos."
          }
     
