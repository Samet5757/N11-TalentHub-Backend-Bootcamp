# syntax=docker/dockerfile:1.6

FROM maven:3.9.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN mvn -q -pl ${MODULE} -am -DskipTests package

FROM openjdk:17-jdk-slim
ARG MODULE
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
