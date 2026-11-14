# Testes esperados em cada mudança

**Cada comportamento é provado no nível mais baixo que consegue prová-lo.** A regra de negócio é provada de forma exaustiva na unidade, em milissegundos; o serviço montado prova que ela está ligada; a aceitação prova os cenários de negócio de ponta a ponta. Repetir em cima o que embaixo já prova só deixa o build lento.

| Nível | Skill | Prova | Onde |
|---|---|---|---|
| Unidade | `teste-unidade` | regra de negócio (toda transição, toda recusa, todo limite), Value Object, tabela de fluxo, caso de uso com repositório em memória, mapper, assembler, propriedades (jqwik), mutação (PITest) | `src/test/java/.../domain/`, `application/`, `entrypoint/apresentacao/` |
| Componente | `teste-componente` | persistência, formato da entrada (`400`), identidade (`401`/`403` por endpoint), tradução de erro (`404`/`409`/`503`), concorrência, integração simulada, contrato REST, campos de canal | `src/test/java/.../integracao/` |
| Aceitação | `teste-aceitacao` | cenários de negócio (massa e cenários em YAML), pelo Karate, no par `*Test`/`*IT` | `src/test/resources/aceitacao/<capacidade>/` |

Todas as três entrevistam o dev antes de escrever e só seguem com o plano de cobertura confirmado.

## O que cada tipo de mudança pede

| Mudança | Unidade | Componente | Aceitação |
|---|---|---|---|
| Regra de negócio nova ou alterada | matriz da regra | um caso que prova a ligação | cenário, se o analista definiu |
| Estado, ação ou tipo novo no fluxo | toda transição e recusa nova | um caso aceito e um recusado pelo endpoint | cenário do caminho novo |
| Endpoint novo | — | formato, identidade, erros, contrato | cenário que o usa |
| Tabela ou coluna nova | mapper (ida e volta) | persistência pelo endpoint | — |
| Integração externa nova | caso de uso com gateway falso | simulador: sucesso, recusa, indisponível, timeout | registro na massa externa |
| Formato novo de canal | linha no teste do padrão de canal | nomes dos campos do `*Response` | — |

Antes de terminar qualquer tarefa, rode `./mvnw verify`. Os `*IT` da aceitação rodam contra o artefato empacotado só na geração de versão (`./mvnw verify -Pversao`); o teste de mutação, a cada pull request (`./mvnw verify -Pauditoria`).
