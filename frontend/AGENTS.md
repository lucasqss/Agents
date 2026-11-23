# Instruções para agentes de IA: front

Convenções de front para os serviços deste repositório: telas que **desenham a jornada e acionam os endpoints do backend** (thin client). A stack é **React + Vite + TypeScript strict + Tailwind**; as convenções detalhadas da stack estão sendo definidas (ver `README.md`).

**Use a skill `ui-jornada`** em toda tarefa de tela, componente, cliente de API ou desenho de jornada. Ela define a fronteira (o front descreve, o backend decide), as cinco camadas (núcleo, api, jornada, componentes, telas) e as regras UI-001 a UI-009.

## Como trabalhar

1. **Copie o vizinho.** Antes de criar uma tela ou um componente, leia o mais parecido que já existe e faça igual.
2. **Mude o mínimo.** Só o que a tarefa pede.
3. **Verifique rodando:** typecheck, o `verificar.sh` da skill e o percurso no navegador (modo simulado). Código que você não rodou não está pronto.
4. **Relate o que é fato:** o que mudou em cada camada, o que rodou e o resultado.

## Proibido

- Regra de negócio na tela (comparar valor com limite, decidir se uma ação vale). Quem decide é a API.
- `fetch` fora do núcleo; caminho de endpoint fora da camada api.
- Código da API (`EM_ANALISE`, `"PAGO"`) fora da jornada.
- Desligar, afrouxar ou editar o `verificar.sh` ou as regras de lint para o build passar. Se achar a regra errada, diga e pare.
