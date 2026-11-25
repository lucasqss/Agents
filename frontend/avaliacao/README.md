# Avaliação das skills do front (A/B com agentes)

Mede se as skills `ui-jornada` e `react-jornada` fazem um agente, inclusive um modelo mais simples, construir uma tela certa. Rode de novo a cada mudança relevante numa skill ou na POC (*harness quality evaluation*). Os resultados não ficam versionados: só o kit.

## O kit

| Arquivo | O que é |
|---|---|
| `fila/TAREFA.md` | A tarefa em termos de negócio, com os textos exatos: a tela "Fila de trabalho" (abas, ações por linha, motivo, aviso único). |
| `fila/AGENTS.sem.md`, `fila/AGENTS.com.md` | As instruções dos dois grupos. A única diferença é a linha que manda usar as skills. |
| `fila/oraculo/fila.spec.ts`, `fila/oraculo.config.ts` | O teste no navegador, escondido do agente. Estrito (`ESTRITO=1`: abas por papel, um aviso só) ou tolerante. |
| `fila/avaliar.sh` | Mede uma execução: `verificar`, oráculo estrito e tolerante, código da API na tela, `useEffect`, `useRecurso`, botões desabilitados, avisos, arquivos fora do lugar. |
| `montar-base.sh` | Monta a base a partir da POC atual: autônoma, com o contrato junto e a TAREFA. |
| `montar-execucoes.sh` | Cria `temp-sem-skill/rN` e `temp-com-skill/rN`, cada uma com a sua porta de e2e. |

## Como rodar

```bash
SP=/tmp/ab                                       # fora do repositório
frontend/avaliacao/montar-base.sh $SP/base
frontend/avaliacao/montar-execucoes.sh $SP/base . 4    # cria ./temp-{sem,com}-skill/r1..r4
```

Depois:
1. **Rode os agentes.** Um agente por pasta, em segundo plano e com o modelo que você quer medir (ex.: nível Haiku), com o prompt: "Trabalhe somente dentro da pasta <pasta>. Comece lendo <pasta>/AGENTS.md e <pasta>/TAREFA.md, e cumpra a tarefa. Ao terminar, responda em até 5 linhas: o que fez e se o npm run verificar passou."
2. **Avalie em série** (o oráculo usa a porta 4176):

   ```bash
   for g in sem com; do for r in 1 2 3 4; do
     echo "== $g$r"; BASE=$SP/base PW_CHROMIUM=<chromium> frontend/avaliacao/fila/avaliar.sh temp-$g-skill/r$r
   done; done
   ```
3. **Leia o código das execuções que falharam.** É o modo de falha que diz o que ajustar na skill.
4. **Confirme o ajuste** com uma rodada só do grupo com skill.
5. **Apague as pastas** `temp-*-skill`.

**Antes de confiar no oráculo:** ele tem de falhar na base (que não tem a tela) e passar numa implementação sua.

## Histórico

| Data | Tarefa | Sem skill (estrito) | Com skill (estrito) | O que mudou na skill |
|---|---|---|---|---|
| 2026-11-25 | Fila de trabalho | 4/4 | 2/4 → 4/4 após o ajuste | Abas do shadcn (nunca botões); copiar a estrutura do vizinho, não os textos |
