---
name: teste-aceitacao-legado
description: Use ao criar, alterar ou revisar testes de aceitação de usuário que acionam endpoints HTTP de um sistema Quarkus que já existe e não tem requisito escrito, derivando as regras de negócio do código atual (testes de caracterização), ou ao criar pares *Test e *IT com @QuarkusTest e @QuarkusIntegrationTest para proteger um legado antes de mudá-lo. Se houver requisito, caso de uso ou critério de aceite, use a skill teste-aceitacao-modelos.
---

# Teste de aceitação de legado, baseado em modelos

Este documento é a referência da equipe e dos agentes de IA. Nomes como `cota`, `cliente`, `processo` e `tesouraria` nos exemplos são ilustrativos: não procure essas classes no repositório.

Esta skill é irmã de [`teste-aceitacao-modelos`](../teste-aceitacao-modelos/SKILL.md). Os critérios de criação são os mesmos: modelos, critérios mínimos, par `*Test` e `*IT`, só HTTP, um resultado por caso, efeito conferido por outro endpoint. **O que muda é a fonte das regras:** aqui é o código que já existe, e não um requisito.

**Princípio:** o código é a fonte do **modelo**, não da verdade. Você lê o código para descobrir as regras que ninguém escreveu. Cada regra é **confirmada pelo usuário** antes de virar asserção. O teste continua caixa-preta: conhece só o endpoint, a requisição e a resposta.

**Por quê:** num legado, o comportamento atual é o único requisito disponível, e esses testes são a rede de segurança para mudá-lo. Um teste que apenas copia o código confirma até os erros dele. Por isso, cada regra lida passa pelo usuário, e o que parece defeito vira pergunta.

Cada teste roda duas vezes, sem duplicar código: na fase de teste unitário (`*Test`, com `@QuarkusTest`) e na fase de integração (`*IT`, com `@QuarkusIntegrationTest`, contra o sistema empacotado).

Esta skill cobre apenas testes de aceitação pelos endpoints. Não use para testes de unidade, de arquitetura ou de componentes internos.

## Passo a passo

**Não pule etapas nem mude a ordem.**

1. **Entre em modo plan.** Sempre, antes de escrever qualquer teste. Ferramenta sem modo plan: faça as etapas 2 a 5 sem criar arquivos e aguarde a confirmação do usuário na etapa 5.
2. **Leia o código da capacidade, a partir dos endpoints** ([onde achar cada coisa](references/extrair-modelos-do-codigo.md)). Liste cada regra de negócio com um identificador (R1, R2...) e com a **origem** (`arquivo:linha`). Descreva a regra em linguagem de negócio, não de implementação. A capacidade inclui todos os endpoints que a expõem, inclusive os de canal. Se o escopo não estiver claro, pergunte. Para confirmar o que o código produz e a leitura não resolve, como status gerado pelo framework ou conversão de tipos, você pode subir o sistema e chamar os endpoints, só por HTTP. O observado entra no plano como "observado" e ainda precisa da confirmação do usuário.
3. **Monte os modelos e escolha os critérios** (tabela "Critérios mínimos"). Leia antes [modelos-e-criterios.md](../teste-aceitacao-modelos/references/modelos-e-criterios.md) inteiro. A mecânica Quarkus só é necessária na etapa 6. Anote, enquanto lê, todo **comportamento suspeito**.
4. **Derive os casos.** Cada caso tem pré-condição, entrada, **um** resultado esperado e a verificação do efeito.
5. **Confirme com o usuário o comportamento adequado**, usando o "Formato do plano". Pare e aguarde.
6. **Só depois da confirmação**, escreva o `*Test` e o `*IT` ([mecânica Quarkus](../teste-aceitacao-modelos/references/quarkus-test-e-it.md)).
7. **Faça ao menos uma sessão exploratória** por capacidade ([como fazer](../teste-aceitacao-modelos/references/testes-exploratorios.md)). Aqui ela também confere se o sistema rodando se comporta como o modelo que você leu no código. Comportamento novo que deva virar teste volta à etapa 1.
8. **Rode a verificação** ("Antes de terminar").

O exemplo completo da skill irmã vale para a mecânica e para o formato dos arquivos: [exemplo-pagamento-processo.md](../teste-aceitacao-modelos/references/exemplo-pagamento-processo.md). O modelo de `caso-de-uso.md` desta skill está em [extrair-modelos-do-codigo.md](references/extrair-modelos-do-codigo.md#modelo-de-caso-de-usomd).

## Fontes permitidas

| Pode usar | Não pode usar |
|---|---|
| Código de produção: `src/main`, inclusive entrypoints, `ExceptionMapper`s, domínio e services | Outros testes como fonte de regra, de formato ou de resultado esperado |
| Migrations e `application.properties`, para limites, unicidade e chaves de configuração | Import de classe de produção nos testes: eles declaram os próprios payloads |
| Contrato do endpoint: método, caminho, requisição, resposta, status (ex.: OpenAPI em `/q/openapi`) | Detalhes que não aparecem no HTTP: logs, nomes de classe, textos internos de exceção |
| Respostas observadas pelos endpoints, em sessão exploratória | Comportamento suspeito fixado sem confirmação do usuário |
| Documentação, comentários e histórico do git como pistas, nunca como regra confirmada | |

O que o código não deixa claro vira **pergunta ao usuário**: ramo que o HTTP não alcança, código morto, comportamento que depende de configuração do ambiente ou dois trechos que se contradizem. Nunca escolha sozinho.

**Sistema externo:** como na skill irmã, só a classe do stub conhece o contrato do sistema externo e a chave de configuração da URL. O cenário fala em negócio: `sistemaDePagamentos.ficarIndisponivel()`.

## Formato do plano

O plano da etapa 5 contém estas seções, nesta ordem:

1. **Regras de negócio:** R1, R2... em linguagem de negócio, cada uma com a **origem no código** (`arquivo:linha`).
2. **Contrato:** endpoints, campos, valores aceitos, status. Inclua os status que o código produz, inclusive os não documentados e os gerados pelo framework (ex.: 415 por `Content-Type` errado, 404 por id não numérico no caminho).
3. **Modelos e critérios:** qual modelo para qual regra.
4. **Casos:** tabela com regra, pré-condição, entrada, resultado esperado único e verificação do efeito.
5. **Comportamentos suspeitos:** o que parece defeito, com a origem e a pergunta exata: **fixar como está ou é defeito?** Para "é defeito", diga qual seria o resultado correto.
6. **Dúvidas:** tudo que o código deixa em aberto, com a pergunta exata. Nenhuma dúvida vira asserção.

## Critérios mínimos

Os mesmos da skill irmã. Explicação de cada um, com exemplos: [modelos-e-criterios.md](../teste-aceitacao-modelos/references/modelos-e-criterios.md). Onde achar cada modelo no código: [extrair-modelos-do-codigo.md](references/extrair-modelos-do-codigo.md).

| O código mostra | Modelo | Mínimo obrigatório | Aprofunde quando |
|---|---|---|---|
| Qualquer endpoint | Gramática do contrato | **Cobertura de terminais:** cada campo, valor de enumeração, ação do caminho e status de resposta aparece em ao menos um teste | Há campos opcionais ou alternativas: cobertura de produções |
| Dados de entrada | Particionamento do espaço de entrada | Classes válidas e inválidas, valores-limite, *Base Choice* | Características interagem: *Pair-Wise* |
| Ciclo de vida ou fluxo | Máquina de estados de negócio | Todas as transições, cada estado final alcançado, cada ação inválida em cada estado | O grafo é pequeno: caminhos primos |
| Regra com condições combinadas | Tabela de decisão | Cada condição decide sozinha o resultado | Regra crítica: todas as combinações |
| O que o código não mostra | Sessão exploratória e suposição de erro | Uma sessão por capacidade | Mais sessões, conforme o risco |

## Regras

1. **Só HTTP.** Sem `@Inject`, `@InjectMock`, `QuarkusMock`, banco, nem import de classe de produção. Os testes declaram os próprios payloads.
2. **Todo `*Test` tem um `*IT` de corpo vazio**, que apenas herda. `@DisabledOnIntegrationTest` só com aprovação do dono do sistema.
3. **Um resultado esperado por caso.** Resultado aberto (`400 ou 409`, qualquer 2xx) só se o usuário confirmar no plano.
4. **O esperado vem do código confirmado pelo usuário.** Para cada comportamento suspeito, vale a resposta do usuário:
   - **Fixar como está:** o teste trava o comportamento atual. O `@DisplayName` começa com `[atual]` e o caso entra em **Comportamento atual suspeito**, no `caso-de-uso.md`. Quando alguém corrigir a produção, o teste falha de propósito e é atualizado junto, com aprovação do usuário.
   - **É defeito:** o teste espera o resultado correto. Nesse caso, pare: deixe o teste vermelho, registre em **Defeitos**, no `caso-de-uso.md`, relate ao usuário e não faça commit. O usuário decide entre corrigir a produção ou aprovar `@Disabled("Defeito: <descrição>")`.
   - Se o sistema rodando diverge do que foi confirmado, é defeito ou erro de leitura do código. Relate. Não ajuste o teste para passar e não corrija a produção sem pedido.
5. **Dados próprios e únicos** em cada caso, criados pelos endpoints, sem depender de ordem ou de banco limpo.
6. **Confirme o efeito de negócio por outro endpoint** depois de cada ação. Status certo não prova que o efeito aconteceu.
7. **Organize por capacidade de negócio:** `src/test/java/<pacote-base>/aceitacao/<capacidade>/`. Nunca por camada ou classe, mesmo que o legado esteja organizado assim.
8. **Guarde as regras e a rastreabilidade** em `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md`: regra → origem no código → modelo → critério → teste.

## Errado × certo

| Errado | Certo | Por quê |
|---|---|---|
| Subir o sistema, chamar os endpoints e copiar as respostas como esperado | Montar o modelo a partir do código e derivar os casos pelo critério | Cópia de resposta não tem critério de cobertura e fixa defeitos sem ninguém ver |
| "O código faz assim, então é o certo" | Listar a regra com a origem e confirmar no plano | O código pode estar errado |
| Fixar um 500 ou um 400 sem corpo sem perguntar | Registrar como comportamento suspeito no plano | Só o usuário decide se é regra ou defeito |
| Teste `deveChamarValidarCpfDoClienteService` | Teste `cpfRepetidoERecusado` | O nome descreve o negócio, que sobrevive à refatoração |
| Conferir mensagem de log ou texto interno de exceção | Conferir só o que o contrato expõe: status, campos e efeito | Detalhe interno muda na refatoração, e o teste quebra sem defeito |
| Copiar formato de resposta de outro teste | Usar o código do entrypoint ou o OpenAPI | Outro teste pode estar errado ou desatualizado |
| `verify` só com `statusCode(200)` | Conferir o efeito em outro endpoint | O sistema pode responder 200 sem ter feito nada |
| Pasta `aceitacao/service/` porque o legado é organizado assim | Pasta `aceitacao/aberturapedido/` | O negócio muda menos que a estrutura do código |

## Desculpas que não valem

| Desculpa | Realidade |
|---|---|
| "O usuário está com pressa e disse para não perguntar" | A confirmação continua obrigatória. Apresente o plano curto e pare |
| "É caracterização, então fixo tudo como está" | Fixar como está é uma resposta do usuário para cada suspeito, e não um padrão |
| "Está no código, não há o que confirmar" | Regra lida no código é hipótese até o usuário confirmar |
| "Vou escrever e listar as suposições no final" | Suposição em asserção já virou regra. Pergunte antes |
| "Já existem testes parecidos, posso aproveitar" | A mecânica pode ser reaproveitada. Regras e resultados esperados, não |
| "Aceitar 400 ou 409 é mais robusto" | É menos capaz de detectar defeito. Pergunte qual é o certo |

## Sinais de alerta: pare

- Vai criar um arquivo de teste sem confirmação do usuário.
- Uma regra do plano não tem origem no código.
- Um comportamento suspeito virou asserção sem a resposta "fixar como está".
- Escreveu "assumi", "suponho" ou "convém confirmar" sobre um resultado esperado.
- Um caso aceita mais de um resultado sem confirmação.
- O teste importa classe de produção ou confere detalhe que não aparece no HTTP.
- Mudou um teste só para ele passar.

Em qualquer um desses casos, volte à etapa 5.

## Antes de terminar

1. Rode o build com testes (`./mvnw verify`). Build vermelho por defeito de produção segue a regra 4.
2. Confirme que os relatórios `target/surefire-reports` e `target/failsafe-reports` mostram o **mesmo número** de testes de aceitação.
3. Confirme que nenhum arquivo em `aceitacao/` importa classe de produção.
4. Confirme que toda regra do `caso-de-uso.md` tem origem no código, confirmação do usuário e ao menos um teste.
5. Relate ao usuário defeitos, comportamentos fixados como suspeitos, dúvidas abertas e o resultado da sessão exploratória.

Fontes: Feathers, *Working Effectively with Legacy Code* (testes de caracterização); Myers, Sandler e Badgett, *The Art of Software Testing*; Ammann e Offutt, *Introduction to Software Testing*; guia [Testing your application](https://quarkus.io/version/3.33/guides/getting-started-testing/#quarkus-integration-test) do Quarkus.
