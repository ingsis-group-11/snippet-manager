FROM gradle:8.10.1-jdk21 AS build
WORKDIR /home/gradle/src
COPY . .
RUN --mount=type=secret,id=gpr_user,env=USERNAME,required \
    --mount=type=secret,id=gpr_token,env=TOKEN,required \
    gradle assemble --no-daemon
FROM openjdk:21-jdk-slim
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/snippet-manager-service.jar
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=production","/app/snippet-manager-service.jar"]