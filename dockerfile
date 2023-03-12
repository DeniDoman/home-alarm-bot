FROM gradle:jdk11 as gradleimage
COPY . /home/gradle/source
WORKDIR /home/gradle/source
RUN gradle shadowJar

FROM eclipse-temurin:11
COPY --from=gradleimage /home/gradle/source/app/build/libs/app-all.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]
