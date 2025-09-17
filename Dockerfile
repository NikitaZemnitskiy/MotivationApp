FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
# Normalize line endings and ensure wrapper is executable
RUN sed -i 's/\r$//' mvnw || true && chmod +x mvnw && ./mvnw -q -DskipTests package && ls -la target

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV JAVA_OPTS=""
COPY --from=build /app/target/buseiny-app-1.0.0.jar /opt/app/app.jar
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/app/app.jar"]

