# ADR 002: protótipo de telas em React + Vite + Tailwind, com serviço simulado

- Situação: aceita
- Data: 2026-11-21

## Contexto

Cada serviço ganha uma **UI de teste e prototipação de jornada**: telas de rascunho que preenchem e disparam as requisições da API, para mostrar as jornadas ao negócio e testar a API sem montar JSON à mão. A UI é montada por um agente, aos poucos: o usuário descreve o que quer e o agente altera. O `prototipo-telas/` deste exemplo é o modelo que a skill `prototipo-de-jornada` copia.

A primeira versão era HTML, CSS e JavaScript puro, sem build. Funcionava, mas tudo ficava em um `app.js` de HTML em string, e cada tela nova repetia marcação.

No **episteme** (https://lucasqss.github.io/episteme/), feito com React + Vite + TypeScript + Tailwind, o agente montou uma interface de qualidade muito rapidamente. Isso é a evidência prática desta decisão.

## Decisão

1. **React + Vite + TypeScript + Tailwind v4 + lucide-react.** Nada de roteador (rotas por `#hash`), biblioteca de estado ou de formulário.
2. **Dividido em genérico e específico.** `src/api`, `src/apoio` e `src/componentes` são iguais em todo projeto. `src/jornada`, `src/telas`, `App.tsx` e `e2e/` são reescritos por projeto.
3. **A API atrás de uma interface, com dois transportes** (como o `src/lib/api.ts` do episteme): o real (`VITE_API`) e um **serviço simulado no navegador** (`VITE_MODO=falso`), com as mesmas rotas, status e mensagens. A tela não sabe qual está em uso.
4. **Playwright em dois papéis:**
   - os olhos do agente, pelo MCP: screenshot e console a cada pedido;
   - uma spec por jornada (`[J-xx]`), contra o serviço simulado, no computador e no celular.
5. **Fora do serviço.** A pasta fica fora de `src/` e o Maven não a vê. No serviço, muda só o CORS, e só em `%dev`.

## Consequências

- **Agente rápido e visual consistente:** essa é a stack que os modelos mais dominam. O Tailwind põe o visual no componente, e os componentes prontos dão uma cara profissional sem desenhar nada.
- **Precisa de Node e de `npm install`:** o custo fica contido na pasta do protótipo.
- **Dá para mostrar a jornada antes de a API existir,** e publicar o build simulado num site estático (`npm run build:falso`) para o negócio clicar.
- **O serviço simulado é uma cópia das regras.** Pode divergir do domínio. A regra que vale é a do serviço (`TabelaDosFluxosTest`), e os e2e não provam regra: provam que a jornada é navegável.
- **Rejeitados:**
  - **Angular:** mais cerimônia (módulos, injeção, decorators), mais código por pedido e mais lento para o agente, sem ganho para um rascunho;
  - **JS puro sem build:** simples de subir, mas o agente gera HTML em string, sem tipos e sem componentes, e a qualidade visual depende de CSS escrito à mão;
  - **suíte de testes de UI contra o serviço real:** duplicaria a aceitação (Cucumber) e quebraria a cada mudança de tela.
- **Revisar** se o protótipo for virar o front de produto: aí entram as decisões de produto (design system, autenticação, roteador, testes de componente).
