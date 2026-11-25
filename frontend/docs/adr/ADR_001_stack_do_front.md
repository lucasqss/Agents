# ADR 001: stack do front das jornadas

- Situação: aceita
- Data: 2026-11-25
- Substitui: a ADR 002 do `exemplo-testes` (protótipo em React + Vite + Tailwind, sem kit de componentes nem cliente gerado)

## Contexto

Cada serviço ganha telas de **rascunho e POC das jornadas**: chamam os endpoints JSON do backend Quarkus, mostram a jornada ao negócio (inclusive o que o backend ainda não tem) e rodam sem backend, com um serviço simulado. A arquitetura é a da skill `ui-jornada`: o front descreve, o backend decide, em cinco camadas.

Quem gera as telas são agentes de IA, inclusive modelos mais simples. Os critérios da escolha foram:
- **acerto do agente fraco;**
- **o kit de componentes para quem usa as telas;**
- **a análise estática no build;**
- **a simplicidade de manter;**
- **os tokens por tela;**
- **o encaixe no thin client.**

A comparação de sete stacks foi publicada à parte ("Stack do front das jornadas"), com:
- **o Thoughtworks Tech Radar:** React e Svelte em Adopt no volume 34;
- **dois benchmarks públicos:** DesignBench e Web-Bench;
- **testes A/B com agentes nível Haiku:** React e Angular acertaram 4 de 4 com a skill e 0 de 4 sem ela.

## Decisão

| Peça | Escolha | Por quê |
|---|---|---|
| UI | **React 19** + Vite | É o que a IA gera melhor (benchmarks e A/B) e está em Adopt no Radar |
| Linguagem | **TypeScript 6.0 strict** (`noUncheckedIndexedAccess`) | O `typescript-eslint` ainda não aceita o TS 7 (pede menos que 6.1) |
| Estilo e componentes | **Tailwind 4** + **shadcn/ui** (Radix) + lucide-react | Kit acessível e maduro, com o código dentro do projeto; o pacote `cn` é o que o CLI do shadcn 4 usa |
| Contrato | **openapi-typescript** + **openapi-fetch**, gerados do OpenAPI do serviço mais o **contrato proposto** | Caminho, parâmetros, corpo e respostas checados contra o contrato; telas primeiro com a proposta tipada e simulada |
| Dados | Hook **`useRecurso`** do núcleo, sem biblioteca | Um jeito só de buscar; as telas não escrevem `useEffect` (o erro mais comum de agente em React) |
| Rotas | **`#hash`**, sem roteador | Rascunho: zero dependência, funciona em hospedagem estática |
| Análise estática | **ESLint 9**: typescript-eslint com tipos, react-hooks 7 (regras do React Compiler), jsx-a11y; o **`verificar.sh`** da `ui-jornada` | O jsx-a11y ainda não declara o ESLint 10 |
| Testes | **Vitest** (núcleo); **Playwright** + **axe** (jornadas, no simulado e no serviço real) | O teste no navegador, escrito a partir da especificação, foi a única barreira que pegou o que o lint não via |

## Consequências

- **Para manter:** a referência é a POC `frontend/poc`; projeto novo nasce dela com o `novo-front.sh`, e as receitas estão na skill `react-jornada`.
- **Barreiras:** o `npm run verificar` roda no fim de cada turno do agente (o harness) e no CI, junto com os e2e.
- **Versões fixadas por compatibilidade:** TypeScript 6 e ESLint 9 sobem quando o `typescript-eslint` aceitar o TS 7 e o `eslint-plugin-jsx-a11y` declarar o ESLint 10. Reavalie a cada trimestre.
- **Telas primeiro:** o que o serviço ainda não tem vive no `contrato/proposto.yaml`, com selo "Proposta" na tela. O `contrato:propostas` diz quando o serviço publicou.
- **Simulado:** é cópia das regras do serviço e pode divergir. O `test:e2e:real` acusa a divergência.
- **Fora de escopo:** este é o front de rascunho e POC. Um front de produto pode pedir roteador, cache de dados e i18n; a arquitetura (`ui-jornada`) continua valendo.
