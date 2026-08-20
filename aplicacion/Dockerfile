# Etapa 1: compilación. Se separa la resolución de dependencias del copiado
# del código fuente para que Docker reutilice la capa de dependencias
# mientras solo cambien los .java (build mucho más rápido en iteraciones).
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B package -DskipTests

# Etapa 2: runtime. Imagen mínima, sin herramientas de build, usuario no root.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S freestyle && adduser -S freestyle -G freestyle \
    && mkdir -p /app/uploads && chown -R freestyle:freestyle /app

COPY --from=build /build/target/*.jar app.jar

USER freestyle
EXPOSE 8080
VOLUME /app/uploads

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
