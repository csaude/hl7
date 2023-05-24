FROM tomcat:8-jre8-alpine
WORKDIR /usr/local/tomcat/webapps
EXPOSE 8080
COPY target/*.war hl7.war
