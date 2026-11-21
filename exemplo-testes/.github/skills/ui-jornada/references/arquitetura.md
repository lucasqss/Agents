# Arquitetura: thin client com a jornada como dado

## Por que thin client

A regra de negócio já está no backend, testada e verificada pelo build (domínio, tabelas de fluxo, aceitação). Se o front repetir a regra, aparece uma **segunda fonte da verdade**, que diverge em silêncio: o backend muda a alçada e o front continua mostrando o número antigo. Por isso o front:

- **pergunta, em vez de calcular:** pede as ações disponíveis, a situação atual e os erros de validação;
- **desenha a jornada:** etapas, rótulos, textos, ordem das telas e personas. Isso é apresentação, e é dele;
- **é conferido contra o contrato:** a jornada cita códigos da API (ações, situações, tipos), e a conferência compara esses códigos com os enums do OpenAPI. Quando algum some ou muda de nome, a faixa "Jornada desatualizada" aparece.

O BPMN executável continua no backend. O front guarda o **desenho** dele, em um JSON que o negócio lê.

## Camadas e dependências

```
telas  ──►  api  ──►  nucleo
  │  ╲                  ▲
  │   ╲──► componentes ─┘
  └──────► jornadas (dado; ninguém mais a importa direto, só o contexto das telas)
```

| Camada | Contém | Não contém |
|---|---|---|
| `nucleo` | `http` (único com `fetch`; põe cabeçalhos; transforma falha de rede em `{ ok: false }`; avisa quem quer registrar a chamada), `rotas` (tabela rota → tela), `armazenamento` (local, à prova de falha), `formato` (escape `h()`, moeda, data) | nada de processo, jornada ou tela |
| `api` | um arquivo por recurso REST (`processos`, `contrato`): funções nomeadas como a operação de negócio (`abrir`, `consultar`, `executar`). Cada uma devolve `{ ok, status, corpo }` | decisão do que mostrar; texto de tela |
| `jornadas` | `<jornada>.json`: o desenho (ver [jornada](jornada.md)) | condição, número de regra, código executável |
| `componentes` | funções puras: entram dados prontos (rótulo, cor, lista de passos), sai a visão. O CSS do componente fica em `componentes.css`, com o mesmo nome | chamada de API; leitura da jornada; estado global |
| `telas` | uma por rota; `contexto` (jornada carregada + persona + `situacao()`/`acao()`/`tipo()`/`texto()`); ajudantes da própria tela (ex.: `recentes`) | `fetch`; código da API escrito à mão; `if` de regra |
| `app` | a inicialização: carrega a jornada, liga o núcleo, monta a tabela de rotas e roda a conferência | lógica de tela |

**Contexto das telas.** A jornada é lida uma vez (pelo `app`) e entregue ao `contexto`. As telas traduzem os códigos da API por ele: `situacao("EM_ANALISE")` devolve `{ rotulo: "Em análise", cor: "andamento" }`. Um código desconhecido aparece como veio, e a conferência avisa.

## Estrutura de pastas de referência

Uma estrutura que segue a arquitetura ao pé da letra, sem build (JavaScript puro com ES modules nativos). Em framework, troque `js/` por `src/` e mantenha as pastas, ou mapeie as camadas para as pastas que o projeto já tem (como o protótipo React deste repositório, mapeado no `SKILL.md`).

```
<front>/
  index.html               casca: menu, área de conteúdo, faixa de divergência, "por trás da tela"
  config.js                URL da API: o único ponto que muda por ambiente
  estilo/
    tokens.css             o tema inteiro (cores, espaços, raios); só aqui há valor de cor
    base.css  layout.css  componentes.css
  js/
    app.js                 inicialização: carrega a jornada, liga o núcleo, monta as rotas, roda a conferência
    conferencia.js         jornada × enums do OpenAPI
    nucleo/   http.js  rotas.js  armazenamento.js  formato.js
    api/      <recurso>.js  contrato.js
    jornadas/ <jornada>.json
    componentes/  aviso.js  caminho.js  dialogo-motivo.js  etiqueta.js  indicadores.js  linha-do-tempo.js  pagina.js  tabela.js
    telas/    contexto.js  <tela>.js ...
```

## Como cresce

| Mudança | Onde mexer |
|---|---|
| Etapa, ação, situação, persona, texto | só o JSON da jornada |
| Endpoint novo | uma função em `api/<recurso>` |
| Recurso novo | um arquivo em `api/` |
| Tela nova | um arquivo em `telas/` e a rota no `app` |
| Jornada nova (outro processo) | outro JSON em `jornadas/`, o contexto apontando para ela, e as telas dela |
| Visual | `tokens.css` (cor, espaço); componente novo + bloco em `componentes.css` |
| Ambiente | `config.js` |

## Desenvolvimento local e CORS

O front é servido à parte (servidor estático ou o servidor de dev do framework), fora do jar do backend. O backend libera CORS **só no perfil de desenvolvimento**, para a origem do front. Em JavaScript puro, ES modules não funcionam abrindo o arquivo direto (`file://`): suba com um servidor estático.
