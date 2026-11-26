# `pom.xml` obrigatório

A stack é **Java 25 e Quarkus 3.33**. Confira cada trecho abaixo no `pom.xml` do componente e acrescente o que faltar. As dependências sem `<version>` têm a versão definida pelo `quarkus-bom`.
Onde aparece `PACOTE_RAIZ`, use o pacote raiz do componente (o mesmo em que fica a pasta `arquitetura` dos testes).

## Versões

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
    <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
    <quarkus.platform.version>3.33.4</quarkus.platform.version>
    <archunit.version>1.4.1</archunit.version>
    <compiler-plugin.version>3.15.0</compiler-plugin.version>
    <surefire-plugin.version>3.5.6</surefire-plugin.version>
    <!-- Barreiras de qualidade (seção própria abaixo) -->
    <enforcer-plugin.version>3.7.0</enforcer-plugin.version>
    <errorprone.version>2.52.0</errorprone.version>
    <nullaway.version>0.14.3</nullaway.version>
    <nullaway.pacotes>PACOTE_RAIZ</nullaway.pacotes>
    <jspecify.version>1.0.0</jspecify.version>
    <jetbrains-annotations.version>26.1.0</jetbrains-annotations.version>
    <jqwik.version>1.10.2</jqwik.version>
    <openapi-diff.version>2.1.7</openapi-diff.version>
    <pitest.version>1.30.2</pitest.version>
    <pitest-junit5.version>1.2.3</pitest-junit5.version>
    <pitest.limite>0</pitest.limite>
    <!-- Os *IT repetem os testes de aceitação contra o pacote final e rodam só na geração de versão (-Pversao) -->
    <skipITs>true</skipITs>
</properties>
```

O `maven-failsafe-plugin` (goals `integration-test` e `verify`, versão `${surefire-plugin.version}`) respeita `skipITs`. Com ele `true` por padrão, os `*IT` ficam fora do `./mvnw verify` do dia a dia. O perfil que os liga fica em `<profiles>`:

```xml
<profile>
    <id>versao</id>
    <properties>
        <skipITs>false</skipITs>
    </properties>
</profile>
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

<!-- Quando houver job agendado (intervalo nos testes de 1 s ou mais) -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-scheduler</artifactId></dependency>
<!-- Alternativa, mesma anotação @Scheduled, store em memória: quando os testes precisam de intervalo abaixo de 1 s (use uma ou outra, não as duas) -->
<!-- <dependency><groupId>io.quarkus</groupId><artifactId>quarkus-quartz</artifactId></dependency> -->

<!-- Quando houver identidade (token JWT verificado pela aplicação) -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-smallrye-jwt</artifactId></dependency>

<!-- Persistência: Jakarta Data sobre PostgreSQL, com schema pelo Flyway -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-hibernate-orm</artifactId></dependency>
<dependency><groupId>jakarta.data</groupId><artifactId>jakarta.data-api</artifactId></dependency>
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-jdbc-postgresql</artifactId></dependency>
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-flyway</artifactId></dependency>
<dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>

<!-- Nulos declarados (NullAway): só anotações, nada em tempo de execução -->
<dependency><groupId>org.jspecify</groupId><artifactId>jspecify</artifactId><version>${jspecify.version}</version></dependency>
<!-- Só se algum método precisar de @Contract("!null -> !null") (ex.: Padrao de canal) -->
<dependency><groupId>org.jetbrains</groupId><artifactId>annotations</artifactId><version>${jetbrains-annotations.version}</version><scope>provided</scope></dependency>

<!-- Testes -->
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-junit</artifactId><scope>test</scope></dependency>
<dependency><groupId>io.rest-assured</groupId><artifactId>rest-assured</artifactId><scope>test</scope></dependency>
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>${archunit.version}</version>
    <scope>test</scope>
</dependency>
<!-- Testes de propriedade (*PropriedadesTest) -->
<dependency><groupId>net.jqwik</groupId><artifactId>jqwik</artifactId><version>${jqwik.version}</version><scope>test</scope></dependency>
<!-- Aceitação (skill teste-aceitacao): especificações em português executadas pelo Cucumber; banco próprio por Testcontainers;
     simulador de sistema externo -->
<dependency><groupId>io.cucumber</groupId><artifactId>cucumber-java</artifactId><version>7.34.9</version><scope>test</scope></dependency>
<dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-postgresql</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.wiremock</groupId><artifactId>wiremock-standalone</artifactId><version>3.13.2</version><scope>test</scope></dependency>
```

O Cucumber 7.34.9 foi verificado com Java 25, Quarkus 3.33 e o Enforcer ligado (no `exemplo-testes`).

A aceitação roda em **execução própria** do surefire e do failsafe, numa JVM sem nenhum outro teste (a configuração que um recurso de teste devolve no `*IT` vale para todos os jars que sobem depois na mesma JVM). As execuções respeitam `skipITs`:

```xml
<!-- maven-surefire-plugin -->
<executions>
    <execution><id>default-test</id><configuration><excludes><exclude>cucumber/**</exclude></excludes></configuration></execution>
    <execution><id>cucumber</id><goals><goal>test</goal></goals>
        <configuration><includes><include>cucumber/*Test.java</include></includes></configuration></execution>
</executions>
<!-- maven-failsafe-plugin: integracao e cucumber-it com summaryFile próprio; verificar lê os dois -->
<executions>
    <execution><id>integracao</id><goals><goal>integration-test</goal></goals><configuration>
        <excludes><exclude>cucumber/**</exclude></excludes>
        <summaryFile>${project.build.directory}/failsafe-reports/failsafe-summary-integracao.xml</summaryFile></configuration></execution>
    <execution><id>cucumber-it</id><goals><goal>integration-test</goal></goals><configuration>
        <includes><include>cucumber/*IT.java</include></includes>
        <summaryFile>${project.build.directory}/failsafe-reports/failsafe-summary-cucumber.xml</summaryFile></configuration></execution>
    <execution><id>verificar</id><goals><goal>verify</goal></goals><configuration><summaryFiles>
        <summaryFile>${project.build.directory}/failsafe-reports/failsafe-summary-integracao.xml</summaryFile>
        <summaryFile>${project.build.directory}/failsafe-reports/failsafe-summary-cucumber.xml</summaryFile>
    </summaryFiles></configuration></execution>
</executions>
```

Para o relatório trazer o nome de cada cenário (e não `cenarios()[3]`), configure nos dois plugins o `statelessTestsetReporter` `org.apache.maven.plugin.surefire.extensions.junit5.JUnit5Xml30StatelessReporter` com `usePhrasedTestCaseMethodName` ligado. O `pom.xml` completo está no `exemplo-testes`.

Com identidade, a configuração segue o ADR do projeto: emissor (`mp.jwt.verify.issuer`) e chave pública (`mp.jwt.verify.publickey.location`) por propriedade, **sem valor padrão em `%prod`** (a aplicação não sobe sem a chave do provedor). A chave de teste fica em `src/test/resources` e só vale no perfil `%test`.

## Compilador

Sem `hibernate-processor`, os repositórios Jakarta Data não são gerados e o HQL não é validado (ACL e BFF não o têm). Sem `<parameters>true</parameters>`, os parâmetros nomeados de `@Query` não funcionam.

O ErrorProne e o NullAway rodam como plugin do javac: nulo não declarado e erro semântico conhecido viram erro de compilação, e `-Werror` impede que aviso se acumule. No código de teste só o ErrorProne roda (os testes passam nulo de propósito para provar as validações). `nullaway.pacotes` é o pacote raiz; em legado, siga a adoção por camadas da skill `adequar-ao-padrao`.

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
            <path>
                <groupId>com.google.errorprone</groupId>
                <artifactId>error_prone_core</artifactId>
                <version>${errorprone.version}</version>
            </path>
            <path>
                <groupId>com.uber.nullaway</groupId>
                <artifactId>nullaway</artifactId>
                <version>${nullaway.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
    <executions>
        <execution>
            <id>default-compile</id>
            <configuration>
                <compilerArgs>
                    <arg>-XDcompilePolicy=simple</arg>
                    <arg>--should-stop=ifError=FLOW</arg>
                    <arg>-Werror</arg>
                    <arg>-XDaddTypeAnnotationsToSymbol=true</arg>
                    <arg>-Xplugin:ErrorProne -XepDisableWarningsInGeneratedCode -XepExcludedPaths:.*/generated-sources/.* -Xep:NullAway:ERROR -XepOpt:NullAway:AnnotatedPackages=${nullaway.pacotes} -XepOpt:NullAway:CheckContracts=true -XepOpt:NullAway:ExcludedClassAnnotations=jakarta.persistence.Entity,jakarta.persistence.Embeddable -XepOpt:NullAway:ExcludedFieldAnnotations=jakarta.inject.Inject,org.eclipse.microprofile.config.inject.ConfigProperty,org.eclipse.microprofile.rest.client.inject.RestClient</arg>
                </compilerArgs>
            </configuration>
        </execution>
        <execution>
            <id>default-testCompile</id>
            <configuration>
                <compilerArgs>
                    <arg>-XDcompilePolicy=simple</arg>
                    <arg>--should-stop=ifError=FLOW</arg>
                    <arg>-Werror</arg>
                    <arg>-Xplugin:ErrorProne -XepDisableWarningsInGeneratedCode -XepExcludedPaths:.*/generated-test-sources/.* -Xep:NullAway:OFF</arg>
                </compilerArgs>
            </configuration>
        </execution>
    </executions>
</plugin>
```

O ErrorProne precisa abrir pacotes internos do javac. Crie `.mvn/jvm.config` (sem ele a compilação falha com `IllegalAccessError`):

```
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
```

## Barreiras de qualidade

Na ordem do ciclo do Maven: a falha barata aborta antes de subir banco ou rodar mutação.

**`validate`: Maven Enforcer.** JDK e Maven do projeto, dependências que convergem e as proibidas ([estilo e proibidos](estilo-e-proibidos.md)) barradas antes de compilar.

```xml
<plugin>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>${enforcer-plugin.version}</version>
    <executions>
        <execution>
            <id>enforce</id>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <rules>
                    <requireJavaVersion><version>[25,)</version></requireJavaVersion>
                    <requireMavenVersion><version>[3.9,)</version></requireMavenVersion>
                    <banDuplicatePomDependencyVersions/>
                    <dependencyConvergence/>
                    <bannedDependencies>
                        <excludes>
                            <exclude>org.projectlombok</exclude>
                            <exclude>org.mapstruct</exclude>
                            <exclude>io.quarkus:quarkus-hibernate-orm-panache*</exclude>
                            <exclude>io.quarkus:quarkus-panache*</exclude>
                        </excludes>
                        <message>Dependência proibida (AGENTS.md, "Proibido"): sem Lombok, MapStruct nem Panache.</message>
                    </bannedDependencies>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Se o `dependencyConvergence` acusar duas versões de uma mesma biblioteca, declare a versão que a árvore já traz (`./mvnw dependency:tree -Dincludes=<groupId>`), não a mais nova.

**`test`: jqwik.** A configuração vai no Surefire. **Não crie `junit-platform.properties`:** ele substitui o do Quarkus, e os `@QuarkusTest` abortam por perder a ordem das classes.

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>${surefire-plugin.version}</version>
    <configuration>
        <argLine>@{argLine}</argLine>
        <properties>
            <configurationParameters>
                jqwik.reporting.onlyfailures=true
                jqwik.database=target/.jqwik-database
            </configurationParameters>
        </properties>
        <systemPropertyVariables>
            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
            <maven.home>${maven.home}</maven.home>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

Acrescente `.jqwik-database` ao `.gitignore` (o PITest o grava na raiz).

**`verify`: contrato REST.** O build grava o OpenAPI e o openapi-diff o compara com a linha de base versionada (`docs/api/openapi-base.json`, gerada no primeiro build: `./mvnw package -DskipTests && mkdir -p docs/api && cp target/openapi/openapi.json docs/api/openapi-base.json`). Ele **não** trata como quebra a remoção de campo de resposta não obrigatório; isso fica com o `ContratoRestTest` ([testes](testes.md)). BFF e ACL têm contrato REST como qualquer componente: as duas peças valem para eles.

```properties
# application.properties
quarkus.smallrye-openapi.store-schema-directory=target/openapi
# operationId = nome do método do resource: nome estável para o cliente gerado no front (ver contrato-openapi.md).
quarkus.smallrye-openapi.operation-id-strategy=METHOD
```

```xml
<plugin>
    <groupId>org.openapitools.openapidiff</groupId>
    <artifactId>openapi-diff-maven</artifactId>
    <version>${openapi-diff.version}</version>
    <executions>
        <execution>
            <id>contrato-rest</id>
            <phase>verify</phase>
            <goals><goal>diff</goal></goals>
            <configuration>
                <oldSpec>${project.basedir}/docs/api/openapi-base.json</oldSpec>
                <newSpec>${project.build.directory}/openapi/openapi.json</newSpec>
                <failOnIncompatible>true</failOnIncompatible>
                <markdownOutputFileName>${project.build.directory}/openapi/diff.md</markdownOutputFileName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**`verify -Pauditoria`: PITest.** Teste de mutação no domínio e nos use cases, só com testes puros (sem Quarkus). Na adoção, rode uma vez com `pitest.limite` em `0`, leia `target/pit-reports/index.html` e fixe o limite no valor medido, arredondado para baixo.

```xml
<profile>
    <id>auditoria</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <version>${pitest.version}</version>
                <dependencies>
                    <dependency>
                        <groupId>org.pitest</groupId>
                        <artifactId>pitest-junit5-plugin</artifactId>
                        <version>${pitest-junit5.version}</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <id>mutacao</id>
                        <phase>verify</phase>
                        <goals><goal>mutationCoverage</goal></goals>
                    </execution>
                </executions>
                <configuration>
                    <targetClasses>
                        <param>PACOTE_RAIZ.domain.*</param>
                        <param>PACOTE_RAIZ.application.*</param>
                    </targetClasses>
                    <targetTests>
                        <param>PACOTE_RAIZ.domain.*</param>
                        <param>PACOTE_RAIZ.application.*</param>
                    </targetTests>
                    <threads>4</threads>
                    <timestampedReports>false</timestampedReports>
                    <outputFormats><param>HTML</param><param>XML</param></outputFormats>
                    <mutationThreshold>${pitest.limite}</mutationThreshold>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

Em ACL e BFF, troque `domain.*` pelo pacote que tem regra (tradução, composição); sem regra própria, o profile pode ficar de fora.

## Rodar

```bash
./mvnw verify            # domínio, ArchUnit e @QuarkusTest (inclusive aceitação) e empacota
./mvnw verify -Pversao    # o anterior + os *IT contra o artefato empacotado (geração de versão)
./mvnw verify -Pauditoria # o anterior (sem os *IT) + teste de mutação; rode a cada pull request
```

Se o repositório não tiver `mvnw`, ou se ele falhar no Linux ou WSL com `cannot execute: required file not found` (fim de linha CRLF), use `mvn verify`, com o `JAVA_HOME` apontando para um JDK 25.
