FROM alpine/java:21-jdk
WORKDIR /app
COPY target/notificacao.service-0.0.1-SNAPSHOT.jar.original app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]