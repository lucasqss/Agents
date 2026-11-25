# POC: telas da jornada do processo de pagamento

A aplicação de referência do front. Mostra, numa jornada real (o processo de pagamento do `exemplo-testes`), cada convenção que as skills `ui-jornada` (arquitetura) e `react-jornada` (receitas) ensinam. **Agente que vai criar uma tela copia daqui.**

Ela é um rascunho para conversar sobre as jornadas e testar a API, não o produto final. Roda com o serviço de verdade ou com um serviço simulado no navegador.

## Subir

```bash
npm install
npm run dev:falso     # só as telas, com o serviço simulado (sem Java, sem Docker): http://localhost:5173
npm run dev           # contra o serviço de verdade (VITE_API, padrão http://localhost:8080)
```

O serviço de verdade é o `exemplo-testes` em modo dev (`cd ../../exemplo-testes && ./mvnw quarkus:dev`), que já libera o CORS para `localhost:5173`.

## Comandos

| Comando | O que faz |
|---|---|
| `npm run gerar:api` | Junta o OpenAPI do serviço e o contrato proposto (origens em `contrato.config.json`) e gera `src/api/esquema.d.ts` e `src/api/propostas.gerado.ts`. Rode quando o contrato mudar. |
| `npm run contrato:propostas` | O que as telas propõem e o serviço ainda não tem, e o que ele já publicou (igual ou diferente da proposta). |
| `npm run verificar` | As barreiras rápidas, em ordem: contrato em dia (`conferir:api`), `typecheck`, `lint`, `arquitetura` (UI-001 a UI-006) e `test:unidade`. |
| `npm run test:unidade` | Testes de unidade (Vitest) do núcleo e dos componentes: `useRecurso`, cliente HTTP, simulado, identidade, formato, `LimiteDeErro`. |
| `npm run test:e2e` | Os percursos das jornadas no navegador (Playwright), contra o build em modo simulado, em computador e celular, com a checagem de acessibilidade (axe) em cada tela. `PW_PORTA` muda a porta (padrão 4174). |

## As pastas

| Pasta | Camada | O que tem |
|---|---|---|
| `src/nucleo/` | núcleo | cliente HTTP (openapi-fetch, transporte real ou simulado, registro das chamadas), `useRecurso`, `todas`, rotas por `#hash`, `guardado`, `formato` |
| `contrato.config.json`, `contrato/proposto.yaml` | contrato | de onde vem o OpenAPI do serviço, e as operações propostas pelas telas (telas primeiro) |
| `src/api/` | api | `esquema.d.ts` e `propostas.gerado.ts` (gerados) e `processos.ts` (uma função por endpoint) |
| `src/jornadas/` | jornada | `processo-pagamento.ts`: rótulos e tons por código, caminhos, personas, textos |
| `src/componentes/` | componentes | o catálogo (`index.ts`) e os primitivos do shadcn/ui (`ui/`) |
| `src/telas/` e `src/App.tsx` | telas | `Inicio` (lista local), `ListaProcessos` (lista da API com filtro e páginas, proposta), `AbrirProcesso` (formulário), `Processo` (detalhe com ações e documentos), `AnexarDocumento` (assistente com envio de arquivo, proposta), `contexto.ts` (persona e recentes) |
| `src/simulado/` | fora das camadas | o serviço simulado: cópia das regras do serviço, para o modo sem backend |
| `e2e/` | | um `jNN-<jornada>.spec.ts` por jornada, escrito a partir da especificação; `apoio.ts` tem o `test` (navegador limpo, sem erro no console), os passos da jornada e a checagem de acessibilidade |

## Decisões de stack

- **React 19, Vite, Tailwind 4, shadcn/ui (Radix) e lucide-react.** A comparação com outras stacks está na análise publicada à parte.
- **TypeScript 6.0 (strict, `noUncheckedIndexedAccess`).** O `typescript-eslint` ainda não aceita o TypeScript 7. O `openapi-typescript` declara suporte só ao 5; o `overrides` do `package.json` o faz usar o 6, e a geração funciona.
- **ESLint 9.** O `eslint-plugin-jsx-a11y` ainda não declara o ESLint 10.
- **Cliente gerado do OpenAPI** (`openapi-typescript` + `openapi-fetch`): caminho, parâmetros, corpo e respostas checados pelo TypeScript contra o contrato.
- **Dados por `useRecurso`, rotas por `#hash`, sem biblioteca de estado nem de formulário.** Há um jeito só de buscar dados, e as telas não escrevem `useEffect`.
- **O pacote `cn`** (do próprio shadcn, no lugar de clsx + tailwind-merge) é o que o CLI do shadcn 4 usa nos componentes que ele gera.
