FROM maven@sha256:b37da91062d450f3c11c619187f0207bbb497fc89d265a46bbc6dc5f17c02a2b AS build
# The above is a temporary fix
# See:
# https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=911925
# https://github.com/carlossg/docker-maven/issues/92
# FROM maven:3-jdk-8-slim AS build

#Pass build args into env vars
ARG CI
ENV CI=$CI

ARG SONAR_HOST_URL
ENV SONAR_HOST_URL=$SONAR_HOST_URL

ARG SONAR_LOGIN
ENV SONAR_LOGIN=$SONAR_LOGIN

RUN if getent ahosts "sslhelp.doi.net" > /dev/null 2>&1; then \
                wget 'https://s3-us-west-2.amazonaws.com/prod-owi-resources/resources/InstallFiles/SSL/DOIRootCA.cer' && \
                keytool -import -trustcacerts -file DOIRootCA.cer -alias DOIRootCA2.cer -keystore $JAVA_HOME/jre/lib/security/cacerts -noprompt -storepass changeit; \
        fi


COPY pom.xml /build/pom.xml
WORKDIR /build

#download all maven dependencies (this will only re-run if the pom has changed)
RUN mvn -B dependency:go-offline

# copy git history into build image so that sonar can report trends over time
COPY .git /build

COPY src /build/src

# the -D option supresses INFO-level logs about dependency downloads. This enables the build to finish within Travis' log length limit.
# The -P option skips the dependency security check in favor of build stability -- the official NVD server is rate-limited, and external builds lack access to our internal NVD mirror
ARG BUILD_COMMAND="mvn -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn clean verify jacoco:report -P=\!dependency-security-check"
RUN ${BUILD_COMMAND}

FROM usgswma/wma-spring-boot-base:8-jre-slim-0.0.4

ENV serverPort=6026
ENV mlrgateway_springFrameworkLogLevel=info
ENV mlrgateway_ddotServers=http://localhost:6028
ENV mlrgateway_legacyTransformerServers=http://localhost:6020
ENV mlrgateway_legacyValidatorServers=http://localhost:6027
ENV mlrgateway_legacyCruServers=http://localhost:6010
ENV mlrgateway_fileExportServers=http://localhost:6024
ENV mlrgateway_notificationServers=http://localhost:6025
ENV ribbonMaxAutoRetries=0
ENV ribbonConnectTimeout=6000
ENV ribbonReadTimeout=60000
ENV hystrixThreadTimeout=10000000
ENV maintenanceRoles='default roles'
ENV dbConnectionUrl=postgresUrl
ENV dbUsername=mlr_db_username
ENV oauthClientId=client-id
ENV oauthClientAccessTokenUri=https://example.gov/oauth/token
ENV oauthClientAuthorizationUri=https://example.gov/oauth/authorize
ENV oauthResourceTokenKeyUri=https://example.gov/oauth/token_key
ENV oauthResourceId=resource-id
ENV HEALTHY_RESPONSE_CONTAINS='{"status":"UP"}'

COPY --chown=1000:1000 --from=build /build/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -k "https://127.0.0.1:${serverPort}${serverContextPath}${HEALTH_CHECK_ENDPOINT}" | grep -q ${HEALTHY_RESPONSE_CONTAINS} || exit 1