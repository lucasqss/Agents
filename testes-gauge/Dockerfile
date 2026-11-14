# Executor dos smoke tests: JDK 25 + Gauge + plugins java e html-report, instalados pelas releases oficiais
# (github.com/getgauge). O projeto (specs, passos, pom) é montado como volume pelo docker-compose.yaml.
FROM eclipse-temurin:25-jdk

ARG GAUGE_VERSION=1.6.43
ARG GAUGE_JAVA_VERSION=1.0.5
ARG HTML_REPORT_VERSION=4.4.9
# Preenchido pelo Docker com a arquitetura da máquina (amd64 ou arm64).
ARG TARGETARCH

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

RUN arquitetura=$([ "$TARGETARCH" = "arm64" ] && echo arm64 || echo x86_64) \
    && curl -fsSL -o /tmp/gauge.zip \
        "https://github.com/getgauge/gauge/releases/download/v${GAUGE_VERSION}/gauge-${GAUGE_VERSION}-linux.${arquitetura}.zip" \
    && unzip -q /tmp/gauge.zip -d /usr/local/bin \
    && rm /tmp/gauge.zip \
    && gauge install java --version "${GAUGE_JAVA_VERSION}" \
    && gauge install html-report --version "${HTML_REPORT_VERSION}" \
    && gauge version

# O plugin java do Gauge chama "mvn" para conferir a versão do gauge-java no pom.xml: delega ao wrapper do projeto.
RUN printf '#!/bin/sh\nexec ./mvnw "$@"\n' > /usr/local/bin/mvn && chmod +x /usr/local/bin/mvn

WORKDIR /testes
ENTRYPOINT ["./mvnw", "-B", "test-compile", "gauge:execute"]
