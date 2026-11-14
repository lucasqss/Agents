# Plano B: trocar o motor, manter as tabelas

O que tem valor de longo prazo são as **tabelas** (`cenarios.csv` e `massa/*.csv`): é o que o analista reconhece e o que cresce. O Karate é só quem as executa. Se ele deixar de servir, o motor muda e as tabelas ficam como estão.

| Situação | Motor |
|---|---|
| Só HTTP, como hoje | Karate (esta skill) |
| Karate incompatível com uma versão nova do Java ou do Quarkus | Runner JUnit 5 próprio: um `@TestFactory` que lê as mesmas tabelas com `Massa.ler` e `Tabelas.agrupar`, gera um `DynamicTest` por cenário e executa cada `acao` com RestAssured |
| Cenários com mensageria (filas, tópicos), arquivos ou protocolos legados | Citrus, que tem endpoints de mensageria e validação de mensagem; as tabelas alimentam os testes dele |

Em qualquer troca: os nomes das colunas, as chaves de massa e a regra "chave inexistente falha antes de rodar" não mudam, e o par `*Test`/`*IT` continua.
