# ==========================================================================
# Build stage — Maven + JDK 25 (LTS) para compilar el proyecto
# Misma estructura que spring-boot-medassistant, para que las dos imágenes
# se comporten igual (mismo usuario, mismo TZ, mismo ENTRYPOINT).
# ==========================================================================
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# Primero copiamos solo el pom.xml y descargamos dependencias.
# Esto aprovecha el cache de Docker: si el pom no cambia entre builds,
# no se vuelven a descargar las dependencias.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================================================
# Runtime stage — solo JRE 25 sobre Ubuntu Noble
# ==========================================================================
FROM eclipse-temurin:25-jre-noble

# Timezone: por default los containers arrancan en UTC.
# Lo seteamos explícitamente para que los logs reflejen la zona horaria correcta.
ENV TZ=America/Bogota
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Usuario non-root para correr la app
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Copiamos SOLO el jar construido en la etapa anterior.
# El JDK, Maven, y el código fuente quedan afuera de esta imagen.
COPY --from=builder /build/target/*.jar app.jar

# Damos ownership del directorio al usuario non-root
RUN chown -R spring:spring /app

USER spring

# Perfil de producción hardcodeado: esta imagen ES la de producción.
ENV SPRING_PROFILES_ACTIVE=aws

# Puerto que expone la app (documentación, no publicación automática)
EXPOSE 8080

# ENTRYPOINT con MaxRAMPercentage para que la JVM se adapte a la
# memoria del container dinámicamente.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
