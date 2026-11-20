FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 aplicacion
COPY --from=compilacion /app/target/gestor-franquicias-0.0.1-SNAPSHOT.jar app.jar
USER aplicacion
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
