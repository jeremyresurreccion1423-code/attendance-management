# Multi-stage build — bypasses Railway Railpack (avoids railpack-frontend pull failures)
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
    && cp target/attendance-management-system-*.jar /app/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app
USER app
COPY --chown=app:app --from=build /app/app.jar /app/app.jar
ENV JAVA_OPTS=""
ENV UPLOAD_DIR=/app/uploads
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
