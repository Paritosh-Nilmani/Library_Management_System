FROM maven:3.9.16-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml ./
COPY src ./src

RUN mvn --batch-mode --no-transfer-progress clean package

FROM tomcat:9.0.123-jdk17-temurin

ENV CATALINA_HOME=/usr/local/tomcat

RUN rm -rf "$CATALINA_HOME/webapps/"*

COPY --from=build /build/target/library-management-system.war "$CATALINA_HOME/webapps/ROOT.war"
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod 755 /usr/local/bin/docker-entrypoint.sh

EXPOSE 10000

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]