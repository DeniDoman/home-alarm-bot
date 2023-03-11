FROM eclipse-temurin:11
RUN mkdir /app
COPY app.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]
