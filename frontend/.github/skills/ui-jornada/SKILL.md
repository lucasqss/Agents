---
name: ui-jornada
description: Use ao criar, alterar ou revisar telas, componentes, clientes de API ou o desenho de jornada de um front thin client (camada de apresentação que só chama endpoints e desenha a jornada/BPMN), em qualquer framework (JavaScript puro, React, Vue, Angular). Define a fronteira "o front descreve, o backend decide", as cinco camadas (núcleo, api, jornada, componentes, telas) e as regras UI-001 a UI-006, com script de verificação. Complementa a prototipo-de-jornada (que fixa a stack React do protótipo) e vale também para o front de produto.
---

# UI de jornada: arquitetura thin client, agnóstica de framework

O front é a **camada de apresentação**: chama endpoints e desenha a jornada. Não decide regra de negócio. Esta skill dá a **arquitetura e as regras**, em qualquer framework. A stack e o ciclo de trabalho do protótipo deste repositório estão na skill `prototipo-de-jornada`. Use as duas juntas quando for o protótipo.

## A fronteira: o front descreve, o backend decide

| O front **descreve** (desenho da jornada) | O backend **decide** (o front nunca repete) |
|---|---|
| Etapas de cada tipo, em ordem, e o nome de cada uma | Se uma ação vale agora (lista de ações disponíveis) |
| Persona sugerida por etapa | Para onde a ação leva (a situação nova vem na resposta) |
| Rótulo de cada ação; quais **pedem** motivo na tela | Se o motivo é obrigatório de fato (recusa com 400) |
| Textos de ajuda ("acima da alçada...") | Valores de regra (a alçada) e comparações |
| Ordem das telas e navegação | Validação de dados (erros por campo) |

Na prática:
- **Botão:** só aparece se a API lista a ação.
- **Situação:** é a que a API devolve.
- **Erro:** é o da API, por campo.
- **Divergência entre o desenho e o contrato:** a jornada pode ser conferida contra os enums do OpenAPI (ver [jornada](references/jornada.md)).

Quando houver um **serviço simulado** (modo sem backend), ele é cópia das regras. Quem está certo é sempre o serviço real.

## As cinco camadas

| Camada | Papel | Pode depender de |
|---|---|---|
| **núcleo** | HTTP (transporte, cabeçalhos, erros, registro da chamada), rotas, armazenamento local, formatação e escape | nada do projeto |
| **api** | Uma função por endpoint, agrupadas por recurso; **único lugar com caminho de endpoint**; devolve a resposta (ok, status, dados ou erro) | núcleo |
| **jornada** | O desenho: situações (rótulo, cor), ações (rótulo, pede motivo, estilo), etapas por tipo, personas, textos. É **dado** (JSON ou constantes tipadas); sem condição nem valor de regra | nada (no máximo tipos) |
| **componentes** | Visuais e puros: dados prontos entram, visão sai. Não chamam API nem leem a jornada | núcleo |
| **telas** | Uma por rota: buscam pela api, traduzem pela jornada e montam com componentes | todas acima |

O nome das pastas varia por projeto; o papel não. **A referência em React é a POC `frontend/poc/`**, com a stack do repositório (React, shadcn/ui, cliente gerado do OpenAPI) e as receitas na skill `react-jornada`. Ela usa os nomes padrão: `src/nucleo/`, `src/api/`, `src/jornadas/`, `src/componentes/` e `src/telas/` (mais `App.tsx`). O serviço simulado fica fora das camadas, em `src/simulado/`.

Os protótipos mais antigos de `exemplo-testes/prototipo-telas` e `exemplo-negocial/prototipo-telas` usam outros nomes: núcleo em `src/api/` e `src/apoio/`, cliente em `src/jornada/api.ts`, desenho em `src/jornada/vocabulario.ts`.

Em Angular standalone a organização é a mesma, sob `src/app/`: núcleo em `api/` e `apoio/`, cliente em `jornada/api.ts`, desenho em `jornada/vocabulario.ts`, componentes em `componentes/` e telas em `telas/` e `app.ts`. A forma de cada peça (signals, `input()`, CDK, Lucide) está em [frameworks](references/frameworks.md#a-mesma-tela-ações-disponíveis-em-cada-um).

O porquê de cada regra e a estrutura de referência estão em [arquitetura](references/arquitetura.md). Os outros frameworks estão em [frameworks](references/frameworks.md).

## Passo a passo por tarefa

1. **Antes de tudo:** leia a jornada do projeto e a tela mais parecida. Faça igual.
2. **Ação, situação, etapa, persona ou texto novo:** mude só a jornada ([formato](references/jornada.md)). A chave é o código da API.
3. **Endpoint novo:** uma função na camada api, tipada quando o projeto for tipado. Nunca `fetch` na tela.
4. **Tela nova:**
   - um arquivo na camada de telas, mais a rota;
   - busque pela api e traduza códigos pela jornada (rótulo, cor);
   - monte com os componentes;
   - mostre botões só a partir das ações disponíveis da API.
   - Na tela de detalhe com ações, siga a receita:
     - **Carregar:** busque juntos o recurso, as ações disponíveis e o histórico.
     - **Avisar:** mantenha **um só aviso** de estado. Ele começa com o aviso de entrada (ex.: "Processo aberto.") e o resultado de cada ação **substitui** o anterior. Nunca empilhe avisos.
     - **Agir:** chame a api com o motivo, se a jornada pedir, e recarregue tudo da API. A situação nova é a que volta, não uma que você calcula.
     - **Erro:** use a mensagem da API no aviso. Se o recurso não existir, mostre o erro e o link de volta, em vez da tela.
     - **Etapa opcional** do caminho (ex.: alçada): mostre-a como "talvez" e marque-a como feita **pelo histórico**. Nunca a decida pelo valor nem pelo limite: isso é regra do backend.
     - **Textos da especificação:** use-os ao pé da letra. Onde ela diz `por <usuário>`, mostre o usuário que a API devolve, sem trocá-lo pelo nome da persona.
5. **Elemento visual novo:** procure no catálogo do projeto ([componentes](references/componentes.md)). Se não houver, crie um componente genérico e puro, que use só os tokens de tema.
6. **Outro framework:** as camadas e a jornada são as mesmas; muda a forma de cada peça ([frameworks](references/frameworks.md)). Os modelos em JavaScript puro estão em [`assets/`](assets/).

## Regras

1. **[UI-001]** HTTP só no núcleo. Tela e componente nunca chamam `fetch`, `axios` nem `HttpClient`.
2. **[UI-002]** Endpoint e URL base só na camada api (e no núcleo, que é o transporte).
3. **[UI-003]** Componente não importa cliente da api, jornada nem telas.
4. **[UI-004]** O cliente da api não depende de telas nem de componentes.
5. **[UI-005]** Código da API (`EM_ANALISE`, `PAGAR`) só na jornada. A tela mostra o rótulo.
6. **[UI-006]** Jornada em JSON é JSON válido.
7. **Nenhum `if` de regra de negócio na tela.**
   - Proibido: `valor > 50000` e `estado == APROVADO → mostra Pagar`.
   - O certo: pergunte à API e mostre.
8. **Todo texto da API ou do usuário é escapado:** binding do framework ou `h()`. Nunca HTML cru com dado.
9. **Sem cor, raio ou sombra fora dos tokens de tema,** e sem dependência nova para o que o catálogo já resolve.

Erros comuns e checklist: [revisão](references/revisao.md).

## Verificação automática

[`scripts/verificar.sh`](scripts/verificar.sh) confere de UI-001 a UI-006 só com `grep`, em qualquer framework. Os caminhos das camadas vêm de variáveis, e os padrões valem para `js/` ou `src/` com `nucleo`, `api`, `jornadas`, `componentes` e `telas`. Na POC (`frontend/poc`), é o `npm run arquitetura`, parte do `npm run verificar`. Para o protótipo React mais antigo de `exemplo-testes`, a partir da raiz do repositório:

```bash
UI_NUCLEO="src/api src/apoio" UI_API="src/api src/jornada/api.ts" \
UI_JORNADA="src/jornada/vocabulario.ts src/jornada/servicoFalso.ts" \
UI_COMPONENTES="src/componentes" UI_TELAS="src/telas src/App.tsx" \
frontend/.github/skills/ui-jornada/scripts/verificar.sh exemplo-testes/prototipo-telas
```

Para um Angular standalone organizado como acima (o script reconhece a raiz `src/app` sozinho), rode a partir da pasta do front:

```bash
UI_NUCLEO="src/app/api src/app/apoio" UI_API="src/app/api src/app/jornada/api.ts" \
UI_JORNADA="src/app/jornada/vocabulario.ts src/app/jornada/servicoFalso.ts" \
UI_COMPONENTES="src/app/componentes" UI_TELAS="src/app/telas src/app/app.ts" \
.claude/skills/ui-jornada/scripts/verificar.sh .
```

Se uma regra falhar, **corrija o código, não a regra**. O script não pega regra de negócio escrita sem código da API (ex.: `valor > 50000`). Isso fica para a revisão.

## Antes de terminar

1. `verificar.sh` verde.
2. Um teste no navegador para cada jornada, **escrito a partir da especificação antes da tela**: seletores pelo papel acessível e pelo texto que a pessoa lê, e uma checagem de acessibilidade (axe) em cada tela. Teste escrito depois, olhando a tela, confirma o erro em vez de pegá-lo.
3. A checagem do projeto: em React, `npm run verificar` e `npm run test:e2e`, como manda a `react-jornada` (no protótipo antigo, `npm run typecheck` e o percurso, como manda a `prototipo-de-jornada`). Em Angular, `typecheck` **e** `lint`, e o percurso no navegador no modo simulado.
4. Relate o que mudou em cada camada e o que não conseguiu verificar.
