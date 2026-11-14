# `pom.xml` obrigatório

A stack é **Java 25 e Quarkus 3.33**. Confira cada trecho abaixo no `pom.xml` do componente e acrescente o que faltar. As dependências sem `<version>` têm a versão definida pelo `quarkus-bom`.

## Versões

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
    <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
    <quarkus.platform.version>3.33.4</quarkus.platform.version>
    <archunit.version>1.4.1</archunit.version>
    <compiler-plugin.version>3.15.0</compiler-plugin.version>
    <surefire-plugin.version>3.5.6</surefire-plugin.version>
</properties>
```

## Dependências

Inclua só as dos recursos que o componente usa. Um componente sem sistema externo não precisa das duas de integração. **ACL e BFF não têm o bloco de persistência** (nem o `hibernate-processor` no compilador): a regra ACL-001/BFF-001 falha se houver JPA, Jakarta Data ou transação.

```xml
<!-- REST, OpenAPI e validação de formato -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-rest-jackson</artifactId></dependency>
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-smallrye-openapi</artifactId></dependency>
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-hibernate-validator</artifactId></dependency>

<!-- Integração com sistemas externos -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-rest-client-jackson</artifactId></dependency>
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-smallrye-fault-tolerance</artifactId></dependency>

<!-- Quando houver job agendado -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-scheduler</artifactId></dependency>

<!-- Persistência: Jakarta Data sobre PostgreSQL, com schema pelo Flyway -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-hibernate-orm</artifactId></dependency>
<dependency><groupId>jakarta.data</groupId><artifactId>jakarta.data-api</artifactId></dependency>
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-jdbc-postgresql</artifactId></dependency>
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-flyway</artifactId></dependency>
<dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>

<!-- Testes -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-junit5</artifactId><scope>test</scope></dependency>
<dependency><groupId>io.rest-assured</groupId><artifactId>rest-assured</artifactId><scope>test</scope></dependency>
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>${archunit.version}</version>
    <scope>test</scope>
</dependency>
```

## Compilador

Sem `hibernate-processor`, os repositórios Jakarta Data não são gerados e o HQL não é validado. Sem `<parameters>true</parameters>`, os parâmetros nomeados de `@Query` não funcionam.

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>${compiler-plugin.version}</version>
    <configuration>
        <parameters>true</parameters>
        <annotationProcessorPathsUseDepMgmt>true</annotationProcessorPathsUseDepMgmt>
        <annotationProcessorPaths>
            <path>
                <groupId>org.hibernate.orm</groupId>
                <artifactId>hibernate-processor</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Rodar

```bash
./mvnw verify
```

Se o repositório não tiver `mvnw`, ou se ele falhar no Linux ou WSL com `cannot execute: required file not found` (fim de linha CRLF), use `mvn verify`, com o `JAVA_HOME` apontando para um JDK 25.
