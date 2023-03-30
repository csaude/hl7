FROM eclipse-temurin:8-jre-alpine
WORKDIR /opt/app
EXPOSE 8080
COPY target/*.jar /opt/app/*.jar
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar" ]
