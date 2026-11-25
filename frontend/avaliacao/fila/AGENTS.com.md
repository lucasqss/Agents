# Instruções para agentes

Protótipo de telas de um serviço de processos de pagamento: React 19 + Vite + TypeScript strict + Tailwind 4 + shadcn/ui + lucide-react, com o cliente da API gerado do OpenAPI (`src/api/esquema.d.ts`) e um serviço simulado para rodar sem backend. A tarefa está em `TAREFA.md`.

- Antes de criar algo, leia o código parecido que já existe no projeto e faça igual.
- Componente novo do shadcn/ui: `npx shadcn@4 add <nome>`. Não instale outras dependências.
- Verifique: `npm run verificar` (contrato, typecheck, lint, arquitetura) e `npm run test:e2e` (Playwright, com o Chromium do ambiente: `PW_CHROMIUM=$(ls -d /opt/pw-browsers/chromium-*/chrome-linux*/chrome | head -1) npm run test:e2e`).

**Use as skills `ui-jornada` e `react-jornada`:** leia `.claude/skills/ui-jornada/SKILL.md` e `.claude/skills/react-jornada/SKILL.md` antes de começar e siga-as. Este projeto é a POC que as skills citam (`frontend/poc`).
