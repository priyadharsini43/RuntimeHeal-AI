FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY agent/pom.xml agent/pom.xml
COPY agent/src agent/src
RUN mvn -B -f agent/pom.xml clean install -DskipTests

COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN mvn -B -f backend/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /opt/runtimeheal

COPY --from=build /workspace/agent/target/runtimeheal-agent-0.0.1-SNAPSHOT.jar /opt/runtimeheal/runtimeheal-agent.jar
COPY --from=build /workspace/backend/target/runtimeheal-backend-0.0.1-SNAPSHOT.jar /opt/runtimeheal/runtimeheal-backend.jar

ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx192m -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=32m -Xss512k -XX:+UseSerialGC"

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/opt/runtimeheal/runtimeheal-agent.jar", "-jar", "/opt/runtimeheal/runtimeheal-backend.jar"]
