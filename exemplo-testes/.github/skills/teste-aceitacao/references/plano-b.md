# Plano B: trocar o motor, manter as especificações

O que tem valor de longo prazo são as **especificações** (`.feature` em português), que o negócio reconhece e valida, e a **massa SQL** com IDs fixos. O Cucumber é só quem as executa: elas são Gherkin padrão, e outro motor que leia Gherkin as executa sem reescrita.

| Situação | Motor |
|---|---|
| Só HTTP, como hoje | Cucumber + RestAssured (esta skill) |
| Cenários com mensageria (filas, tópicos), arquivos ou protocolos legados | Mesmo Cucumber, com passos que publicam e leem mensagens; ou Citrus, se a validação de mensagem ficar pesada |
| Cucumber incompatível com uma versão nova do Java ou do Quarkus | Runner JUnit 5 próprio lendo os mesmos `.feature` com o parser do Gherkin (`io.cucumber:gherkin`) e executando os passos |

Em qualquer troca: o vocabulário (termos do negócio), os IDs da massa, a sentinela e o par `*Test`/`*IT` não mudam.

Por que não Karate: as palavras-chave dele são fixas em inglês (`# language: pt` não é aceito pelo parser do Karate 1.5.2) e os passos são a DSL dele, não frases do negócio.
