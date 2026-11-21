# Protótipo de telas (rascunho)

Telas de **rascunho** sobre a API do `exemplo-testes`. Servem para duas coisas:

- conversar com o negócio sobre as jornadas, com telas de verdade, sem Swagger;
- apoiar o teste manual da API: preencher e acionar os endpoints sem montar JSON à mão.

**Não é o produto final:** um selo "Protótipo" na barra lateral lembra isso, sem dominar a tela.

Esta pasta também é o **modelo** que a skill [`prototipo-de-jornada`](../.github/skills/prototipo-de-jornada/SKILL.md) copia para montar o protótipo de outros projetos. A decisão de stack está na [ADR 002](../docs/adr/ADR_002_prototipo_react_vite_tailwind.md).

## Como subir

Requisitos: Node 20+. Para usar o serviço de verdade, também JDK 25 e Docker (o Dev Services sobe o PostgreSQL).

```bash
./subir.sh          # o serviço em modo dev + as telas contra ele; Ctrl+C derruba os dois
./subir.sh falso    # só as telas, com o serviço simulado no navegador (sem Java, sem Docker)
```

Ou, à mão: `cd exemplo-testes && ./mvnw quarkus:dev` num terminal e `npm install && npm run dev` (ou `npm run dev:falso`) nesta pasta.

Depois, abra http://localhost:5173.

| Comando | O que faz |
|---|---|
| `npm run dev` | Telas contra o serviço em `VITE_API` (padrão `http://localhost:8080`) |
| `npm run dev:falso` | Telas contra o serviço simulado no navegador |
| `npm run build` | Typecheck + build (em `dist/`) |
| `npm run build:falso` | Build que roda sozinho, para publicar num site estático e o negócio clicar |
| `npm run test:e2e` | Jornadas no Playwright, contra o serviço simulado, no computador e no celular |

## A jornada que dá para mostrar

1. **Persona:** escolha quem você é na barra lateral, em "Quem sou eu" (Ana, analista; Vitor, vistoriador; Carla, aprovadora; Tesouraria). O nome vai no cabeçalho `X-Usuario` e aparece no histórico.
2. **Abrir:** abra um processo imobiliário de R$ 80.000,00.
3. **Ana:** inicia e conclui a análise. O caminho mostra que o imobiliário vai para a vistoria.
4. **Vitor:** conclui a vistoria. Como o valor está acima da alçada, o processo vai para "Aguardando alçada".
5. **Carla:** aprova na alçada.
6. **Tesouraria:** paga.
7. **Histórico:** cada passo, com quem fez e quando.

Para mostrar as regras na prática:
- "Reprovar" e "Cancelar" pedem motivo, que entra no histórico.
- Um serviço de R$ 50.000,00 é aprovado direto na análise.
- O rodapé "Por trás da tela" mostra a requisição e a resposta da última chamada.

## Como é feito

React + Vite + TypeScript + Tailwind, com ícones do `lucide-react`. Nada além disso.

```
src/
  api/           GENÉRICO: chamada à API, registro para o "Por trás da tela", transporte real e simulado
  apoio/         GENÉRICO: rotas por #hash, valores guardados no navegador, formatação (R$, data)
  componentes/   GENÉRICO: Layout, Cartao, Botao, Campo, Opcoes, Etiqueta, Aviso, Tabela, Indicadores, Passos, Abas,
                 CartaoDeEscolha, Interruptor, EnvioDeArquivo,
                 Caminho, LinhaDoTempo, DialogoTexto, PorTrasDaTela, Vazio
  jornada/       DESTE PROJETO: vocabulário, chamadas tipadas, persona/recentes, serviço simulado
  telas/         DESTE PROJETO: Inicio, AbrirProcesso, Processo
  App.tsx        DESTE PROJETO: rotas, menu e persona
e2e/             DESTE PROJETO: uma spec por jornada, [J-xx] no nome
```

- **Outro projeto** copia tudo e reescreve só o que é "deste projeto". O genérico fica como está.
- **Visual em um lugar só:** as cores ficam nos componentes (`src/componentes/`, tons em `tons.ts`). As telas não têm cor própria.
- **Dois modos, mesma tela:** `src/api/api.ts` escolhe o transporte por `VITE_MODO`. O serviço simulado (`jornada/servicoFalso.ts`) imita rotas, status, mensagens e a tabela de fluxo do serviço. **É uma cópia:** a regra que vale é a do domínio (`TabelaDosFluxosTest`). Mudou o fluxo, mude a cópia também.
- **Celular:** a barra lateral vira barra de topo, e o caminho do processo vira lista vertical.
- **Fora do jar:** a pasta fica fora de `src/`, então o Maven nem a vê.
- **Mudança no serviço:** só o CORS, que vale apenas no perfil `%dev` (`application.properties`). Em teste e em produção, nada muda.
- **Outro ambiente:** `VITE_API=https://... npm run dev`. O ambiente precisa liberar CORS para esta página.
- **Lista de processos:** a API não tem listagem, então os "Recentes" são os processos que este navegador abriu ou consultou (`localStorage`).
- **Perfis:** todos veem todos os botões da situação. O perfil de cada pessoa ainda vai ser definido; quando existir na API, os botões passam a vir filtrados.
