# Transação do banco e chamada a sistema externo

O banco desfaz o que gravou; o sistema externo, não. Quando um caso de uso grava **e** chama um sistema externo com efeito (cria, paga, envia), alguma coisa pode ficar pela metade. Não existe forma sem custo: escolha uma das três abaixo e registre a escolha em ADR ([nova decisão](receitas/nova-decisao.md)).

Chamada que só consulta não tem esse problema: faça antes de abrir a transação ou dentro dela, tanto faz.

Nas três formas valem duas regras:

- Confira a regra do domínio **antes** da chamada, para que pedido inválido nem chegue ao sistema externo.
- Envie uma **chave de idempotência** estável ([concorrência](concorrencia.md)).

## 1. Chamada dentro da transação, com o agregado bloqueado

Bloqueia, confere, chama, altera, grava. Um caso de uso, uma transação.

| Se falhar | O que acontece |
|---|---|
| A chamada | Nada é gravado. Pode repetir |
| A gravação, depois de a chamada dar certo | O efeito existe lá e não aqui. Repetir resolve, **porque** a chave é a mesma e o sistema externo devolve o mesmo resultado |

Use quando a chamada é rápida, o sistema externo respeita a chave de idempotência e o volume é baixo. Custo: a conexão e o bloqueio ficam presos enquanto o sistema externo responde; o `@Timeout` do client precisa ser curto.

## 2. Estado intermediário

O ciclo de vida ganha um estado "solicitado". São três passos, cada um no seu caso de uso:

1. Transação curta: confere a regra e grava "solicitado".
2. Sem transação aberta: chama o sistema externo com a chave de idempotência.
3. Transação curta: grava "confirmado" com o retorno (ou "recusado").

| Se falhar | O que acontece |
|---|---|
| O passo 2 ou o 3 | O agregado fica em "solicitado", visível. Uma retomada (nova tentativa do usuário ou job) repete o passo 2 com a mesma chave e conclui |

Use quando a chamada é lenta ou instável, ou quando o negócio precisa enxergar "em andamento". Custo: um estado e uma ação a mais, e uma rotina de retomada para o que ficou parado.

## 3. Outbox

A transação grava a mudança e, na mesma transação, uma linha "a enviar". Um processo separado lê as linhas e chama o sistema externo ou publica a mensagem, marcando o que já foi.

Use quando o efeito externo não precisa da resposta na hora, ou quando há mensageria. Custo: tabela, processo de envio, e o chamador não recebe o resultado na mesma requisição. Precisa de decisão do dono: não existe no padrão ainda.

## O que não fazer

- Chamar o sistema externo, depois abrir a transação para gravar, sem chave de idempotência: uma falha na gravação deixa o efeito órfão.
- Capturar a falha da chamada e gravar "sucesso" mesmo assim.
- `@Retry` em chamada com efeito sem chave de idempotência.
