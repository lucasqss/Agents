# Plano B: trocar o motor, manter os dados

O que tem valor de longo prazo são os **dados** (`cenarios.yaml` e `massa/*.yaml`): é o que o analista reconhece e o que cresce. O Karate é só quem as executa. Se ele deixar de servir, o motor muda e as tabelas ficam como estão.

| Situação | Motor |
|---|---|
| Só HTTP, como hoje | Karate (esta skill) |
| Karate incompatível com uma versão nova do Java ou do Quarkus | Runner JUnit 5 próprio: um `@TestFactory` que lê os mesmos arquivos com `Massa.ler` e `Tabelas.validar`, gera um `DynamicTest` por cenário e executa cada `acao` com RestAssured |
| Cenários com mensageria (filas, tópicos), arquivos ou protocolos legados | Citrus, que tem endpoints de mensageria e validação de mensagem; os mesmos arquivos alimentam os testes dele |

Em qualquer troca: os nomes dos campos, as chaves de massa e a regra "chave inexistente falha antes de rodar" não mudam, e o par `*Test`/`*IT` continua.
