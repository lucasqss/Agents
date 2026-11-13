# Concorrência e idempotência

Duas requisições podem chegar juntas, e a mesma requisição pode chegar duas vezes. Cada caso de uso de escrita precisa dizer o que acontece nos dois casos.

## Qual proteção usar

| A operação é assim | Use |
|---|---|
| Lê o agregado, altera e grava | **Versão** (Optimistic Offline Lock [PoEAA]). É o padrão de todo agregado que é alterado |
| Confere uma condição e age em seguida ("já existe um ativo?", "ainda há saldo?") | **Bloqueio** (Pessimistic Offline Lock [PoEAA]) na linha que serializa a decisão |
| A regra é "só pode existir um por dono" | Bloqueio na **raiz dona da regra** (Coarse-Grained Lock [PoEAA]): a linha do dono, não as dos filhos, que podem ainda nem existir |
| Chama sistema externo com efeito (cria, paga, envia) | Bloqueio do agregado, mais **chave de idempotência** na chamada ([transação e sistema externo](transacao-e-sistema-externo.md)) |
| Unicidade simples (número, código) | Restrição `unique` no banco, além da verificação no caso de uso. A verificação dá a mensagem boa; a restrição dá a garantia |

## Versão

- Coluna de versão na tabela e `@Version` na `*Entity`. O domínio guarda a versão lida (`versao()`), e o mapper a leva e traz.
- Gravação com versão antiga falha. O adapter converte a falha do framework na exceção do domínio (`AlteracaoConcorrenteException`), que vira 409. O caso de uso não captura nem tenta de novo sozinho: quem chamou decide.
- Não substitui o bloqueio quando há verificação antes da escrita: duas requisições podem passar pela verificação e gravar linhas **diferentes**, e a versão não percebe.

## Bloqueio

- Exposto pelo repositório do domínio como `buscarPorIdComBloqueio`, para o caso de uso não conhecer o mecanismo ([persistência](persistencia-jakarta-data.md)).
- Dura até o fim da transação do caso de uso. Bloqueie o mínimo e o mais tarde possível, e sempre na mesma ordem quando forem dois agregados, para não haver impasse.
- Quem espera o bloqueio relê o estado depois de consegui-lo: a regra do domínio é que recusa o segundo pedido.

## Idempotência

A mesma operação recebida duas vezes produz o efeito uma vez só.

- **Ao chamar sistema externo:** o Gateway envia uma chave estável, derivada do que identifica a operação no nosso lado (tipo da operação + id do agregado), nunca aleatória por tentativa. Assim uma nova tentativa não cria um segundo efeito lá.
- **Ao receber:** operação que já foi feita responde como conflito pela regra do domínio (a transição não vale mais no estado atual), sem repetir efeito. Se o cliente precisa receber a mesma resposta de sucesso ao repetir, isso é decisão de contrato: peça ao dono.
- Mudança de estado é naturalmente idempotente quando o ciclo de vida recusa a ação repetida.

## Testes

Todo caso de uso com bloqueio ou chamada externa ganha um teste que dispara a operação em paralelo e confere que o efeito aconteceu uma vez ([testes](testes.md)).
