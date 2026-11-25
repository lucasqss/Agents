---
name: react-jornada
description: Use ao criar ou alterar telas, componentes, chamadas de API ou o desenho de jornada de um front React deste repositório (React 19 + Vite + TypeScript strict + Tailwind 4 + shadcn/ui + lucide-react, cliente gerado do OpenAPI com openapi-fetch, hook useRecurso, rotas por #hash, serviço simulado, Playwright). Dá a receita de cada tarefa e aponta o arquivo da POC (frontend/poc) que serve de modelo. A arquitetura e as regras (UI-001 a UI-009) são da skill ui-jornada; use as duas juntas.
---

# Telas de jornada em React

A arquitetura (o front descreve, o backend decide; cinco camadas) está na skill `ui-jornada`. Esta skill diz **como fazer em React**, e o modelo de tudo é a POC em `frontend/poc/`. **Antes de escrever, abra o arquivo-modelo da tarefa e copie a estrutura.** Copie a estrutura, **não os textos**: rótulos, avisos e mensagens de vazio são os da especificação da tarefa, ao pé da letra, mesmo que o modelo diga outra coisa.

## Onde fica cada coisa

| Camada | Pasta | O que tem | Modelo |
|---|---|---|---|
| núcleo | `src/nucleo/` | cliente HTTP (`criarCliente`, `resposta`, `todas`), `useRecurso`, rotas `#hash` (`useRota`, `ir`), `guardado`, `formato` | não mude; é igual em todo projeto |
| contrato | `contrato.config.json`, `contrato/proposto.yaml` | de onde vem o OpenAPI do serviço, e o que as telas propõem e o serviço ainda não tem | `contrato/proposto.yaml` |
| api | `src/api/` | `esquema.d.ts` e `propostas.gerado.ts` (**gerados**, nunca à mão) e uma função por endpoint | `src/api/processos.ts` |
| jornada | `src/jornadas/` | rótulos e tons por código (`Record<Situacao, …>`), caminhos, personas, textos, tradutores (`situacao(codigo)`) | `src/jornadas/processo-pagamento.ts` |
| componentes | `src/componentes/` e `src/componentes/ui/` (shadcn) | peças visuais puras | `src/componentes/index.ts` (catálogo) |
| telas | `src/telas/` e `src/App.tsx` | uma por rota, mais o estado de apresentação (`contexto.ts`) | `src/telas/Processo.tsx` (detalhe), `src/telas/AbrirProcesso.tsx` (formulário), `src/telas/Inicio.tsx` (lista) |
| (backend falso) | `src/simulado/` | cópia das regras do serviço, para o modo sem backend | `src/simulado/servicoFalso.ts` |

## Receita por tarefa

**Tela de detalhe com ações:** copie `src/telas/Processo.tsx`.
1. **Busca:** uma só, `useRecurso(chave, () => todas({ ... }))`. A chave leva tudo de que a busca depende (`processo:${id}`). Nada de `useEffect` na tela.
2. **Aviso:** um só estado de aviso. O de entrada ("Processo aberto.") é o valor inicial, e o resultado de cada ação o substitui.
3. **Botões:** só das ações que a API lista, filtradas na ordem da jornada (`Object.keys(ACOES).filter((a) => acoes.includes(a))`), com o rótulo e o estilo da jornada.
   - Motivo: num `DialogoTexto` quando a jornada marca `motivo`.
   - Durante a ação e a recarga: botões desabilitados (`agindo || recurso.atualizando`).
4. **Depois de agir:**
   - aviso `"<Ação>: feito. O processo agora está <Situação>."` com a situação que a API devolveu;
   - ou `"<Ação>: não foi possível. <mensagem da API>"`;
   - depois, `recurso.recarregar()`.
5. **Caminho:** a etapa atual é a da API, e as feitas vêm do histórico. A etapa `talvez` fica "talvez" até aparecer no histórico; nunca é decidida por valor.
6. **Carregando:** `<Carregando linhas={n} />` (esqueleto), nunca tela vazia nem só um texto.
7. **Erro ao carregar:** `<Aviso tipo="erro">` com a mensagem da API e um link de volta. Se `podeTentarDeNovo(status)` (sem rede ou 5xx), o aviso ganha `acao={{ texto: "Tentar de novo", aoClicar: recurso.recarregar }}`. 404 não oferece "Tentar de novo".

**Formulário:** copie `src/telas/AbrirProcesso.tsx`.
- Valide no front só o formato que a API não consegue receber (texto onde vai número).
- O resto (obrigatório, tamanho, regra) vem da API. `r.erro.campos` vira o erro de cada `Campo`, e `r.erro.mensagem` vira um `Aviso`.
- Depois de criar: `ir("/x/<id>?aberto=1")`.

**Lista vinda da API, com filtro e páginas:** copie `src/telas/ListaProcessos.tsx`. A chave do `useRecurso` leva o filtro e a página (`lista:${filtro}:${pagina}`); mudar o filtro volta à página 0; `Paginacao` embaixo. Lista guardada no navegador (sem endpoint de listagem): `src/telas/Inicio.tsx`.
- **Abas ou filtros:** o `Tabs` do shadcn (`npx shadcn@4 add tabs`). Nunca botões fazendo papel de aba: sem `role="tab"`, não é aba para o teclado nem para o leitor de tela.
- **Agrupar por situação** (ex.: em andamento × finalizados): o grupo é dado da jornada (ex.: `final: true` em `SITUACOES`), nunca uma lista de códigos na tela.
- **Ações em cada linha:** a mesma receita do detalhe. Há um aviso só para a tela inteira, um diálogo de motivo só, e botões desabilitados durante a ação e a recarga.
- **Cada estado vazio tem o seu texto**, o da especificação: a lista sem nenhum item, e cada aba vazia.

**Assistente em passos, com envio de arquivo:** copie `src/telas/AnexarDocumento.tsx`. `Passos` em cima; cada passo só libera o "Continuar" quando tem o que precisa; o envio é no último passo, depois de conferir; arquivo com `EnvioDeArquivo` (confere formato e tamanho antes, o backend confere de novo); deu certo, volta para a tela de origem com um aviso de entrada.

**Telas primeiro (o backend ainda não tem o endpoint):** é o caso comum no rascunho da jornada.
1. Escreva a operação em `contrato/proposto.yaml`, com as regras da `contrato-openapi.md` (da skill `arquiteto-padrao`): erros com `ErroResponse`, corpo JSON (ou multipart para arquivo), nulo marcado, enum publicado. Pode citar os schemas do serviço por `$ref`.
2. `npm run gerar:api`: a operação ganha tipos e entra em `propostas.gerado.ts` (as pendentes).
3. Na api, a função passa por `proposta(pendente("GET /caminho"), () => resposta(api.GET(...)))`: no modo real, enquanto o serviço não publicar, devolve 501 sem chamar.
4. No simulado (`src/simulado/servicoFalso.ts`), a rota com as mesmas regras, no bloco `PROPOSTA`.
5. Na tela, `SeloProposta` no título, e o 501 mostrado como texto do bloco (`<Vazio>`), nunca como `Aviso`: o aviso (`role="status"`) é só para o resultado do que a pessoa fez, e dois avisos ao mesmo tempo confundem. O resto da tela segue.
6. O e2e da jornada proposta fica em `test.describe(..., { tag: "@proposta" }, ...)`: roda no simulado e é pulado contra o serviço real.
7. `npm run contrato:propostas` diz o que ainda é proposta e o que o serviço já publicou (igual ou diferente). Publicou: tire do `proposto.yaml`, rode `gerar:api` e ajuste a tela se mudou.

**Endpoint novo, ou contrato mudou:**
1. No backend, o contrato segue a referência `contrato-openapi.md` da skill `arquiteto-padrao`: erros declarados, corpo JSON, campo nulo marcado, enum publicado.
2. `npm run gerar:api` (lê as origens de `contrato.config.json`).
3. Uma função em `src/api/<recurso>.ts`: `nome: (...) => resposta(api.GET("/caminho/{id}", { params: { path: { id } } }))`. O caminho é checado pelo TypeScript contra o contrato.
4. `npm run typecheck` aponta quem precisa mudar.

**Situação, ação, tipo ou etapa nova:** só em `src/jornadas/`. As chaves são do contrato, então o typecheck exige o rótulo de todo código novo.

**Identidade (quem age):**
- Cabeçalho do contrato (ex.: `X-Usuario`): a função da api recebe o usuário como parâmetro tipado, e a tela passa a persona de `useEu()` (como em `executar`).
- Token (`Authorization: Bearer`): registre uma vez em `src/main.tsx`: `definirIdentidade(async () => ({ Authorization: \`Bearer ${await tokenDeDesenvolvimento({ iss, sub, groups }, chavePrivada)}\` }))` (`src/nucleo/identidade.ts`). O núcleo põe o cabeçalho em toda chamada. No simulado a assinatura é falsa e o serviço simulado lê quem é com `lerToken`; no modo real, só vale com a chave privada de desenvolvimento do serviço (perfil `%dev`).
- 401 e 403 sem corpo já viram mensagem clara no núcleo; a tela mostra o `Aviso` como em qualquer erro.

**Rota nova:** um caso em `src/App.tsx` (título, trilha e corpo) e, se for de primeiro nível, um item no menu da `Moldura`. O que não casa com nenhuma rota já cai em "Página não encontrada", e a `Moldura` já envolve cada tela num `LimiteDeErro` (defeito ao desenhar vira aviso, não página em branco): não repita isso na tela.

**Ver a tela sem rede (no simulado):** `localStorage.setItem("simulado:sem-rede", "1")` no console do navegador; para voltar, `removeItem`. Serve para mostrar ao negócio e para o e2e (`e2e/j05-estados.spec.ts`).

**Elemento visual novo:**
1. Procure primeiro no catálogo (`src/componentes/index.ts`) e nos `ui/` do shadcn.
2. Faltou um primitivo: `npx shadcn@4 add <nome>`. Ele vai para `src/componentes/ui/` e importa `cn` do pacote `cn` (é o padrão do shadcn 4).
3. Faltou uma composição: um componente em `src/componentes/`, só com props, sem chamar a API nem ler a jornada. Cores só pelas classes do tema (`bg-primary`, `text-muted-foreground`, os tons de `tons.ts`), nunca `#hex` nem `text-blue-500`.

## Teste da jornada (Playwright)

O teste no navegador é a única barreira que pega o que o lint e o typecheck não veem: aba que não é aba, aviso duplicado, texto diferente do pedido, tela inacessível. Mas só pega **se for escrito a partir da especificação**. Teste escrito depois, olhando a tela pronta, confirma o erro: num teste com agentes, o que montou abas com botões escreveu um teste que clicava no texto da aba, e ele passou.

1. **Primeiro o spec, depois a tela.** Copie `assets/jornada.spec.ts` (desta skill) para `e2e/jNN-<jornada>.spec.ts`. Escreva os passos e os textos esperados copiando da especificação, ao pé da letra. Rode: ele falha. Faça a tela até ele passar.
2. **Importe `test` e `expect` de `e2e/apoio.ts`.** Cada teste começa com o navegador limpo e falha se houver erro no console. O apoio também tem os passos da jornada: `abrirProcesso`, `souEu`, `agir`, `aviso` e `bloco`.
3. **Seletores pelo papel e pelo texto que a pessoa lê:** `getByRole("button" | "tab" | "dialog" | "status" | "alert" | "row" | "link" | "heading", { name })`, `getByLabel` e texto exato. Se a especificação diz "abas", o teste usa `getByRole("tab")`; se diz "aviso", usa `aviso(page)`. Nunca classe CSS nem estrutura do DOM. `data-testid` só no painel "Por trás da tela".
4. **`semProblemasDeAcessibilidade(page)` em cada tela visitada** (o axe, WCAG 2 A e AA): rótulo de campo, nome de botão, contraste do tema, papéis ARIA.
5. **O esperado não se troca pelo que a tela mostra.** Se spec e tela discordam, corrija a tela. Mudar texto esperado só com a confirmação de quem escreveu a especificação: é a mesma regra das especificações de aceitação no backend.
6. **Fora do e2e:** regra de negócio (é da aceitação, com Cucumber, no backend) e cada combinação de campo. O e2e prova que a jornada é navegável e acessível, contra o serviço simulado.

Para rodar dois projetos ao mesmo tempo, mude a porta: `PW_PORTA=4180 npm run test:e2e`.

**O simulado é cópia: confira contra o serviço.** `npm run test:e2e:real` roda os mesmos specs contra o serviço de verdade em dev (os `@proposta` e os `@simulado` ficam de fora). Spec que passa no simulado e falha no real é o simulado que divergiu: corrija o simulado (quem está certo é o serviço). Por isso os specs não dependem de dados fixos: usam o número que `abrirProcesso` devolve, nunca "o processo 1".

## Armadilhas (vistas em agentes)

- Empilhar "Processo aberto." e o resultado da ação: dois avisos. O resultado **substitui**.
- `if (p.valor > 50000)` ou `p.estado === "PAGO"` na tela é regra de negócio no front. A API diz o que vale; a jornada traduz.
- Trocar `por <usuário>` pelo nome da persona: use o texto que a especificação pede, com o usuário que a API devolve.
- Escrever em variável de fora durante a renderização (`document.title = ...`): use `useEffect`. O lint (React Compiler) acusa.
- Chamar `fetch`, ou o `api` do openapi-fetch, numa tela: a tela chama a função de `src/api/`.
- Editar `src/api/esquema.d.ts` à mão: rode `npm run gerar:api`.
- `<div onClick>` como botão: use `Button` (ou a `Tabela` com `aoEscolher`, que já trata o teclado). O lint de acessibilidade acusa.
- Montar com `Button` o que o shadcn já tem com acessibilidade (abas, menu, seleção, diálogo). O lint **não** acusa: botões que trocam de aba passam no lint e quebram a navegação por teclado e o leitor de tela.
- Escrever o e2e depois da tela, olhando para ela: o teste confirma o erro em vez de pegá-lo.
- Copiar do modelo o texto junto com a estrutura (ex.: o "Nenhum processo ainda..." da tela inicial numa aba que pede "Nenhum processo em andamento.").

## Antes de terminar

1. `npm run verificar`, a partir de `frontend/poc` ou do seu front. Ele roda o contrato em dia (`conferir:api`), o `typecheck`, o `lint`, a `arquitetura` (o `verificar.sh` da `ui-jornada`) e os testes de unidade (`test:unidade`). Regra que falha se corrige no código. Mexeu no núcleo (`src/nucleo/`) ou num componente com lógica: o teste de unidade vem junto, ao lado do arquivo (`*.test.ts`).
2. `npm run test:e2e`: os percursos das jornadas no navegador (modo simulado, computador e celular), com o axe em cada tela. Jornada nova ganha o seu `e2e/jNN-<jornada>.spec.ts`, escrito antes da tela (seção acima).
3. Relate o que mudou em cada camada e o que rodou.
