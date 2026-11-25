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
| api | `src/api/` | `esquema.d.ts` (**gerado**, nunca à mão) e uma função por endpoint | `src/api/processos.ts` |
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
6. **Erro ao carregar:** `<Aviso tipo="erro">` com a mensagem da API e um link de volta.

**Formulário:** copie `src/telas/AbrirProcesso.tsx`.
- Valide no front só o formato que a API não consegue receber (texto onde vai número).
- O resto (obrigatório, tamanho, regra) vem da API. `r.erro.campos` vira o erro de cada `Campo`, e `r.erro.mensagem` vira um `Aviso`.
- Depois de criar: `ir("/x/<id>?aberto=1")`.

**Lista:** copie `src/telas/Inicio.tsx`. Use `useRecurso` com a chave da lista, e `Tabela` com `aoEscolher` para abrir o item.
- **Abas ou filtros:** o `Tabs` do shadcn (`npx shadcn@4 add tabs`). Nunca botões fazendo papel de aba: sem `role="tab"`, não é aba para o teclado nem para o leitor de tela.
- **Agrupar por situação** (ex.: em andamento × finalizados): o grupo é dado da jornada (ex.: `final: true` em `SITUACOES`), nunca uma lista de códigos na tela.
- **Ações em cada linha:** a mesma receita do detalhe. Há um aviso só para a tela inteira, um diálogo de motivo só, e botões desabilitados durante a ação e a recarga.
- **Cada estado vazio tem o seu texto**, o da especificação: a lista sem nenhum item, e cada aba vazia.

**Endpoint novo, ou contrato mudou:**
1. No backend, o contrato segue a referência `contrato-openapi.md` da skill `arquiteto-padrao`: erros declarados, corpo JSON, campo nulo marcado, enum publicado.
2. `npm run gerar:api`.
3. Uma função em `src/api/<recurso>.ts`: `nome: (...) => resposta(api.GET("/caminho/{id}", { params: { path: { id } } }))`. O caminho é checado pelo TypeScript contra o contrato.
4. `npm run typecheck` aponta quem precisa mudar.

**Situação, ação, tipo ou etapa nova:** só em `src/jornadas/`. As chaves são do contrato, então o typecheck exige o rótulo de todo código novo.

**Rota nova:** um caso em `src/App.tsx` (título, trilha e corpo) e, se for de primeiro nível, um item no menu da `Moldura`.

**Elemento visual novo:**
1. Procure primeiro no catálogo (`src/componentes/index.ts`) e nos `ui/` do shadcn.
2. Faltou um primitivo: `npx shadcn@4 add <nome>`. Ele vai para `src/componentes/ui/` e importa `cn` do pacote `cn` (é o padrão do shadcn 4).
3. Faltou uma composição: um componente em `src/componentes/`, só com props, sem chamar a API nem ler a jornada. Cores só pelas classes do tema (`bg-primary`, `text-muted-foreground`, os tons de `tons.ts`), nunca `#hex` nem `text-blue-500`.

## Armadilhas (vistas em agentes)

- Empilhar "Processo aberto." e o resultado da ação: dois avisos. O resultado **substitui**.
- `if (p.valor > 50000)` ou `p.estado === "PAGO"` na tela é regra de negócio no front. A API diz o que vale; a jornada traduz.
- Trocar `por <usuário>` pelo nome da persona: use o texto que a especificação pede, com o usuário que a API devolve.
- Escrever em variável de fora durante a renderização (`document.title = ...`): use `useEffect`. O lint (React Compiler) acusa.
- Chamar `fetch`, ou o `api` do openapi-fetch, numa tela: a tela chama a função de `src/api/`.
- Editar `src/api/esquema.d.ts` à mão: rode `npm run gerar:api`.
- `<div onClick>` como botão: use `Button` (ou a `Tabela` com `aoEscolher`, que já trata o teclado). O lint de acessibilidade acusa.
- Montar com `Button` o que o shadcn já tem com acessibilidade (abas, menu, seleção, diálogo). O lint **não** acusa: botões que trocam de aba passam no lint e quebram a navegação por teclado e o leitor de tela.
- Copiar do modelo o texto junto com a estrutura (ex.: o "Nenhum processo ainda..." da tela inicial numa aba que pede "Nenhum processo em andamento.").

## Antes de terminar

1. `npm run verificar`, a partir de `frontend/poc` ou do seu front. Ele roda o contrato em dia (`conferir:api`), o `typecheck`, o `lint` e a `arquitetura` (o `verificar.sh` da `ui-jornada`). Regra que falha se corrige no código.
2. `npm run test:e2e`: os percursos das jornadas no navegador, modo simulado, computador e celular. Jornada nova ou mudada ganha ou ajusta o seu `e2e/jNN-<jornada>.spec.ts`.
3. Relate o que mudou em cada camada e o que rodou.
