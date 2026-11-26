# Roteiro de entrevista (unidade)

Poucas perguntas por vez, das que mais mudam o plano para as que menos mudam. Ofereça opções concretas quando souber as alternativas comuns.

## O que mudou

- Qual regra foi criada, alterada ou removida? Em uma frase, do jeito que o negócio diria.
- Quais classes do domínio ou casos de uso estão envolvidos? (Para saber **onde** testar, não **o que** esperar.)

## As regras

- Quais são os valores aceitos e os limites de cada entrada? O limite é inclusivo ("até 500") ou exclusivo ("menos de 500")?
- Existe ciclo de vida? Quais situações e quais ações em cada uma? Em quais situações cada ação é recusada?
- A regra combina condições ("se tem saldo **e** a conta está ativa")? Qual condição sozinha muda o resultado?
- Quem pode fazer cada ação (papel, dono do registro, outra pessoa que não a que fez o passo anterior)?
- O que o usuário vê quando é recusado (texto da mensagem)?

## O que já existe

- Já há testes para essa regra? Algum está errado ou frágil?
- Existe `caso-de-uso.md`? Ele está atualizado com esta mudança?

## Riscos

- O que já quebrou antes nessa área?
- Há conta com dinheiro, arredondamento ou rateio? (Propriedade com jqwik.)

## Encerramento

Repita as regras como você entendeu e pergunte: "Falta alguma regra ou limite?" Tudo que ficou sem resposta vai para **Dúvidas** no plano.
