FROM tomcat:8-jre8-alpine
WORKDIR /usr/local/tomcat/webapps
EXPOSE 8080
COPY web/target/*.war hl7.war
