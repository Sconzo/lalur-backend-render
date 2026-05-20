# ============================================================================
# Estágio 1: BUILD
# ============================================================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace/app

# Copiar arquivos Maven para cache de dependências
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dar permissão de execução ao mvnw e baixar dependências
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline

# Copiar código fonte
COPY src src

# Build da aplicação
RUN ./mvnw clean package -DskipTests

# ============================================================================
# Estágio 2: RUNTIME
# ============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Adicionar usuário não-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar JAR do estágio de build
COPY --from=build /workspace/app/target/*.jar app.jar

# Expor porta da aplicação
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Entry point com profile prod por padrão
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
