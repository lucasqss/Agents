---
name: teste-aceitacao-modelos
description: Use ao criar, alterar ou revisar testes de aceitação de usuário que acionam endpoints HTTP em projetos Quarkus, ao transformar requisito, regra de negócio, caso de uso ou critério de aceite em testes, ou ao criar pares *Test e *IT com @QuarkusTest e @QuarkusIntegrationTest. Se não houver requisito e as regras precisarem sair do código de um sistema existente, use a skill teste-aceitacao-legado.
---

# Teste de aceitação baseado em modelos

Este documento é a referência da equipe e dos agentes de IA. Nomes como `cota`, `cliente`, `processo` e `tesouraria` nos exemplos são ilustrativos: não procure essas classes no repositório.

**Princípio:** o teste de aceitação depende apenas do **requisito de negócio**. O máximo que se conhece da solução é o **endpoint, a requisição e a resposta**. Todo o resto é inferido do comportamento de negócio.

**Por quê:** um teste que olha o código repete o que o código faz e confirma até os erros dele. Um teste que parte do negócio continua válido quando a solução é reescrita.

Cada teste roda duas vezes, sem duplicar código: na fase de teste unitário (`*Test`, com `@QuarkusTest`) e na fase de integração (`*IT`, com `@QuarkusIntegrationTest`, contra o sistema empacotado).

Esta skill cobre apenas testes de aceitação pelos endpoints. Não use para testes de unidade, de arquitetura ou de componentes internos. Requisito ainda não escrito, para funcionalidade nova: use antes a skill [`especificar-caso-de-uso`](../especificar-caso-de-uso/SKILL.md), e parta do `caso-de-uso.md` que ela grava. Sistema existente sem requisito escrito, em que as regras precisam sair do código: use a skill [`teste-aceitacao-legado`](../teste-aceitacao-legado/SKILL.md).

## Passo a passo

**Não pule etapas nem mude a ordem.**

1. **Entre em modo plan.** Sempre, antes de escrever qualquer teste. Ferramenta sem modo plan: faça as etapas 2 a 5 sem criar arquivos e aguarde a confirmação do usuário na etapa 5.
2. **Reúna as fontes permitidas** (tabela abaixo). Liste cada regra de negócio com um identificador: R1, R2...
3. **Monte os modelos e escolha os critérios** (tabela "Critérios mínimos").
4. **Derive os casos.** Cada caso tem pré-condição, entrada, **um** resultado esperado e a verificação do efeito.
5. **Confirme com o usuário o comportamento adequado**, usando o "Formato do plano". Pare e aguarde.
6. **Só depois da confirmação**, escreva o `*Test` e o `*IT` ([mecânica Quarkus](references/quarkus-test-e-it.md)).
7. **Faça ao menos uma sessão exploratória** por capacidade ([como fazer](references/testes-exploratorios.md)). Comportamento novo que deva virar teste volta à etapa 1.
8. **Rode a verificação** ("Antes de terminar").

Exemplo completo, do caso de uso ao teste: [exemplo-pagamento-processo.md](references/exemplo-pagamento-processo.md).

## Fontes permitidas

| Pode usar | Não pode usar |
|---|---|
| Requisito, caso de uso, regra de negócio, critério de aceite | Código de produção: `src/main`, classes, camadas, padrões de projeto |
| Contrato do endpoint: método, caminho, requisição, resposta, status (ex.: OpenAPI) | Banco, tabelas, migrations, `application.properties` |
| Respostas observadas pelos endpoints, em sessão exploratória | Outros testes como fonte de regra, de formato ou de resultado esperado |
| Instruções de execução do repositório (como subir o sistema), só para a sessão exploratória | Skills e documentos de arquitetura, inclusive as instruções de arquitetura em `AGENTS.md` e `CLAUDE.md` |

Requisito ou contrato omisso vira **pergunta ao usuário**. Nunca abra o código para decidir.

**Única exceção:** para simular um sistema externo, o ambiente de teste precisa conhecer o contrato dele e a chave de configuração da URL. Esse conhecimento fica só na classe do stub. O cenário fala em negócio: `sistemaDePagamentos.ficarIndisponivel()`.

## Formato do plano

O plano da etapa 5 contém estas seções, nesta ordem:

1. **Regras de negócio:** R1, R2... como você entendeu.
2. **Contrato:** endpoints, campos, valores aceitos, status.
3. **Modelos e critérios:** qual modelo para qual regra.
4. **Casos:** tabela com regra, pré-condição, entrada, resultado esperado único e verificação do efeito.
5. **Dúvidas:** tudo que o requisito e o contrato não definem, com a pergunta exata. Nenhuma dúvida vira asserção.

## Critérios mínimos

Explicação de cada um, com exemplos: [modelos-e-criterios.md](references/modelos-e-criterios.md).

| O requisito descreve | Modelo | Mínimo obrigatório | Aprofunde quando |
|---|---|---|---|
| Qualquer endpoint | Gramática do contrato | **Cobertura de terminais:** cada campo, valor de enumeração, ação do caminho e status de resposta aparece em ao menos um teste | Há campos opcionais ou alternativas: cobertura de produções |
| Dados de entrada | Particionamento do espaço de entrada | Classes válidas e inválidas, valores-limite, *Base Choice* | Características interagem: *Pair-Wise* |
| Ciclo de vida ou fluxo | Máquina de estados de negócio | Todas as transições, cada estado final alcançado, cada ação inválida em cada estado | O grafo é pequeno: caminhos primos |
| Regra com condições combinadas | Tabela de decisão | Cada condição decide sozinha o resultado | Regra crítica: todas as combinações |
| O que ninguém especificou | Sessão exploratória e suposição de erro | Uma sessão por capacidade | Mais sessões, conforme o risco |

## Regras

1. **Só HTTP.** Sem `@Inject`, `@InjectMock`, `QuarkusMock`, banco, nem import de classe de produção. Os testes declaram os próprios payloads.
2. **Todo `*Test` tem um `*IT` de corpo vazio**, que apenas herda. `@DisabledOnIntegrationTest` só com aprovação do dono do sistema.
3. **Um resultado esperado por caso.** Resultado aberto (`400 ou 409`, qualquer 2xx) só se o usuário confirmar no plano.
4. **O esperado vem do requisito e do contrato.** Se o sistema diverge, é defeito a relatar: mantenha o teste, sem ajustá-lo para passar e sem corrigir produção sem pedido. Nesse caso, pare: deixe o teste vermelho, registre em **Defeitos** no `caso-de-uso.md`, relate ao usuário e não faça commit. O usuário decide entre corrigir a produção ou aprovar `@Disabled("Defeito: <descrição>")`.
5. **Dados próprios e únicos** em cada caso, criados pelos endpoints, sem depender de ordem ou de banco limpo.
6. **Confirme o efeito de negócio por outro endpoint** depois de cada ação. Status certo não prova que o efeito aconteceu.
7. **Organize por capacidade de negócio:** `src/test/java/<pacote-base>/aceitacao/<capacidade>/`. Nunca por camada ou classe.
8. **Guarde o caso de uso e a rastreabilidade** em `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md`: regra → modelo → critério → teste.

## Errado × certo

| Errado | Certo | Por quê |
|---|---|---|
| Abrir o código para descobrir os estados possíveis | Perguntar ao usuário, ou usar o requisito | O código pode estar errado, e o teste passaria a confirmar o erro |
| `statusCode(anyOf(is(400), is(409)))` porque o contrato não diz | Registrar a dúvida no plano | Teste que aceita tudo não detecta nada |
| "Assumi que CPF com máscara é aceito" no relatório final | A mesma dúvida no plano, antes do teste | Suposição do agente não é regra de negócio |
| Copiar formato de resposta de outro teste | Usar o contrato; se faltar, perguntar | Outro teste pode depender da implementação |
| `verify` só com `statusCode(200)` | Conferir o efeito em outro endpoint | O sistema pode responder 200 sem ter feito nada |
| Alterar o teste porque o sistema respondeu diferente | Relatar o defeito | O teste existe para mostrar a divergência |
| Pasta `aceitacao/controller/` | Pasta `aceitacao/aberturapedido/` | O negócio muda menos que a estrutura do código |

## Desculpas que não valem

| Desculpa | Realidade |
|---|---|
| "O usuário está com pressa e disse para não perguntar" | A confirmação continua obrigatória. Apresente o plano curto e pare |
| "Vou escrever e listar as suposições no final" | Suposição em asserção já virou regra. Pergunte antes |
| "O requisito é simples, não há o que confirmar" | Requisito simples gera plano curto, mas gera plano |
| "Só vou olhar o código para acertar o formato" | Formato vem do contrato. Sem contrato, pergunte |
| "Já existem testes parecidos, posso aproveitar" | A mecânica pode ser reaproveitada. Regras e resultados esperados, não |
| "Aceitar 400 ou 409 é mais robusto" | É menos capaz de detectar defeito. Pergunte qual é o certo |

## Sinais de alerta: pare

- Vai criar um arquivo de teste sem confirmação do usuário.
- Vai abrir `src/main`, migrations ou `application.properties`.
- Escreveu "assumi", "suponho" ou "convém confirmar" sobre um resultado esperado.
- Um caso aceita mais de um resultado sem confirmação.
- Mudou um teste só para ele passar.

Em qualquer um desses casos, volte à etapa 5.

## Antes de terminar

1. Rode o build com testes (`./mvnw verify`). Build vermelho por defeito de produção segue a regra 4.
2. Confirme que os relatórios `target/surefire-reports` e `target/failsafe-reports` mostram o **mesmo número** de testes de aceitação.
3. Confirme que nenhum arquivo em `aceitacao/` importa classe de produção.
4. Relate ao usuário defeitos, dúvidas abertas e resultado da sessão exploratória.

Fontes: Myers, Sandler e Badgett, *The Art of Software Testing*; Ammann e Offutt, *Introduction to Software Testing*; guia [Testing your application](https://quarkus.io/version/3.33/guides/getting-started-testing/#quarkus-integration-test) do Quarkus.
