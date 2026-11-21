---
name: prototipo-de-jornada
description: Use para montar ou evoluir a UI de teste e prototipação de jornada de um serviço - telas de rascunho em React + Vite + TypeScript + Tailwind que preenchem e disparam as requisições da API, mostram as jornadas ao negócio e rodam também sem backend (serviço simulado no navegador) - a partir do modelo em exemplo-testes/prototipo-telas. Use quando pedirem "telas para mostrar/testar a jornada X", "um front para testar a API", "protótipo de tela", ou ao ajustar telas, campos e botões desse protótipo a pedido. Não use para o front de produto (outra stack, outro processo) nem para testar regra de negócio (é da teste-unidade/teste-aceitacao).
---

# Protótipo de jornada

Uma UI **de rascunho** por serviço, montada aos poucos com o usuário: ele descreve, você altera, mostra e espera o próximo pedido. Serve para conversar sobre as jornadas com telas de verdade e para testar a API sem montar JSON à mão. **Não é o produto final.**

O modelo é `exemplo-testes/prototipo-telas/` (leia o README dele antes de começar). A stack e o porquê estão na ADR 002 (`exemplo-testes/docs/adr/ADR_002_prototipo_react_vite_tailwind.md`).

## Projeto novo

1. **Copie o modelo** para `<serviço>/prototipo-telas/`, sem `node_modules/`, `dist/`, `test-results/` e `playwright-report/`. Fica fora de `src/`: o build do serviço não pode vê-lo.
2. **Leia o contrato da API:** os resources REST (ou o `/q/openapi`), o formato de erro e os cabeçalhos exigidos (identidade, autor). Leia também as especificações de aceitação: o vocabulário de negócio vem delas.
3. **Reescreva só o que é do projeto** (o resto fica como está):
   - `src/jornada/vocabulario.ts`: termos do negócio ↔ códigos da API, tons das situações, personas;
   - `src/jornada/api.ts`: uma função tipada por endpoint;
   - `src/jornada/estado.ts`: o que o navegador lembra (persona, recentes);
   - `src/jornada/servicoFalso.ts`: as mesmas rotas, status, mensagens e regras do serviço, em memória;
   - `src/telas/*.tsx`, `src/App.tsx` (rotas, menu, persona);
   - `e2e/jNN-<jornada>.spec.ts`: uma spec por jornada;
   - `README.md`: como subir e a jornada que dá para mostrar.
4. **CORS só em dev** no serviço: `%dev.quarkus.http.cors.enabled=true` e `%dev.quarkus.http.cors.origins=http://localhost:5173,http://127.0.0.1:5173`. Nada fora de `%dev`.
5. **Verifique** com o [checklist](references/verificacao.md) e mostre os screenshots ao usuário.

## Ciclo com o usuário

Para cada pedido ("põe o campo X", "essa tela precisa de um filtro", "o botão Y some depois de pagar"):

1. Altere o mínimo: a tela, e o vocabulário/`api.ts`/serviço simulado se o pedido mexer com dados.
2. `npm run typecheck`.
3. Abra no navegador pelo MCP do Playwright (`npm run dev:falso`, ou `npm run dev` com o serviço no ar), percorra o trecho que mudou, tire um screenshot e leia o console.
4. Mostre o screenshot e diga o que mudou. Se a jornada mudou, ajuste a spec e rode `npm run test:e2e`.
5. Espere o próximo pedido.

## Regras

- **Componentes primeiro:** monte a tela com `src/componentes/` (Layout, Cartao, Botao, Campo, Opcoes, Etiqueta, Aviso, Tabela, Indicadores, Caminho, LinhaDoTempo, DialogoTexto, Vazio, e para assistentes e listas: Passos, Abas, CartaoDeEscolha, Interruptor, EnvioDeArquivo). Componente novo só quando nenhum serve, genérico e em `src/componentes/`. Tela não tem cor própria.
- **Linguagem de negócio** em rótulos, botões e mensagens. Código da API (`EM_ANALISE`) só no `vocabulario.ts` e no "Por trás da tela".
- **Sem dependência nova** além de React, Vite, Tailwind, lucide-react e Playwright, salvo pedido do usuário. Sem roteador (rotas por `#hash`), sem biblioteca de estado nem de formulário.
- **Tudo pela `src/api`:** tela nunca chama `fetch`. É o que faz o "Por trás da tela" e o modo simulado funcionarem.
- **O serviço simulado é cópia:** a regra que vale é a do domínio. Ao mudar um fluxo no serviço, mude a cópia também. Se a cópia e o serviço divergirem, quem está certo é o serviço.
- **Selo "Protótipo"** sempre visível. Celular (390px) funciona.
- **Os e2e rodam contra o serviço simulado** e provam que a jornada é navegável. Não repita neles regra que já é da aceitação (Cucumber).

## Exemplos

- `exemplo-testes/prototipo-telas`: uma jornada, sem login (cabeçalho `X-Usuario`).
- `exemplo-negocial/prototipo-telas`: três jornadas escolhidas na tela inicial, identidade por JWT de desenvolvimento, upload de arquivo, link por e-mail sem login, e endpoints ainda inexistentes simulados e listados em `src/jornada/contrato-proposto.md` ("telas primeiro": o contrato proposto vira o caso de uso depois).
