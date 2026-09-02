# ---- Etapa 1: build ----
# Usa una imagen con Maven + JDK 8 ya instalados, así no se depende del
# wrapper (mvnw) dentro del contenedor: la versión de Maven la fija la
# propia imagen base.
FROM maven:3.8.4-eclipse-temurin-8 AS build
WORKDIR /app

# Copiar primero solo el pom.xml permite que Docker cachee la descarga de
# dependencias en una capa aparte: mientras no cambie el pom.xml, un rebuild
# tras editar código fuente no vuelve a descargar todo Maven Central.
COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Etapa 2: runtime ----
# Imagen final liviana: solo el JRE 8, sin Maven ni el código fuente.
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 57075

ENTRYPOINT ["java", "-jar", "app.jar"]
