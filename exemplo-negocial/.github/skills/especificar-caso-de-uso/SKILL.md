---
name: especificar-caso-de-uso
description: Use quando houver uma ideia, pedido ou necessidade de negócio ainda sem requisito escrito e for preciso transformá-la em um caso de uso confirmado, antes de escrever código ou teste. Serve também para MUDAR um caso de uso já confirmado (regra nova, alterada ou removida), por um rascunho da mudança que o dono confirma. Entrevista o dono, ou registra o que um intermediário trouxe dele, e grava as regras de negócio, o ciclo de vida, os erros e os exemplos no caso-de-uso.md que as skills arquiteto-padrao e teste-aceitacao-modelos usam como entrada. Não use para sistema existente sem requisito (use teste-aceitacao-legado) nem para decidir arquitetura.
---

# Especificar caso de uso

Transforma um pedido em linguagem de negócio num caso de uso escrito e confirmado, ou muda um já confirmado. É o primeiro passo de uma funcionalidade nova: o código e os testes de aceitação partem deste arquivo.

**Princípio:** você escreve o que o dono disse, não o que parece razoável. Regra que ele não disse é pergunta, nunca texto do caso de uso.

**Por quê:** uma regra inventada vira teste, depois código, e passa a parecer requisito. Descobrir isso depois custa a funcionalidade inteira.

Esta skill não escreve código, teste, tabela nem endpoint, e não escolhe pasta nem padrão. Nomes como `cota`, `processo`, `tesouraria` e `alçada` nos exemplos são ilustrativos.

## Passo a passo

1. **Leia o que já existe.** Outros `caso-de-uso.md` em `src/test/resources/aceitacao/`, para usar o mesmo vocabulário e não contradizer regra já confirmada. Não leia `src/main`: o caso de uso descreve o negócio, não a solução atual. Se a capacidade já tem `caso-de-uso.md` confirmado, o trabalho é uma **mudança**: siga [mudar um caso de uso confirmado](references/mudar-caso-de-uso-confirmado.md).
2. **Dê nome à capacidade** no vocabulário do dono (ex.: "pagamento de processo"). Uma capacidade é o que uma pessoa consegue fazer do começo ao fim. Se o pedido cobre duas, separe e pergunte por qual começar.
3. **Entreviste** seguindo o [roteiro](references/roteiro-de-perguntas.md). Poucas perguntas por vez, das que mudam mais o desenho para as que mudam menos. Ofereça opções concretas quando souber as alternativas comuns; pergunta aberta só quando não houver. Sem acesso direto ao dono, veja "Quando você não fala com o dono".
4. **Peça exemplos concretos.** Para cada regra, um exemplo com valores ("cota de R$ 250.000,00, processo aprovado, solicitar pagamento → fica em pagamento solicitado"). Exemplo que o dono não consegue dar é regra que ainda não está decidida. Se o valor (limite, prazo, intervalo) ainda é dúvida, use um valor marcado como ilustrativo ("alçada de R$ 50.000,00 (ilustrativa, D1)") e aponte a dúvida.
5. **Escreva o rascunho** no [modelo](references/modelo-caso-de-uso.md), com `Situação: RASCUNHO`. Tudo o que você supôs vai para a seção de dúvidas, com a sua proposta.
6. **Confira** com a lista "Antes de apresentar".
7. **Apresente ao dono e pare.** Ele responde as dúvidas e corrige as regras.
8. **Registre as respostas sem apagar nada.** Dúvida respondida vai de "Abertas" para "Respondidas", com a resposta e a data; identificador de regra e de dúvida nunca é renumerado. Dúvida resolvida por decorrência de outra resposta diz isso na linha, e o dono pode contestar.
9. **Só com a confirmação**, grave `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md` com `Situação: CONFIRMADO` e a data. Dúvida que ficou sem resposta continua na seção de dúvidas, e a regra que depende dela não entra.
10. **Indique o próximo passo:** a receita "nova funcionalidade" da skill `arquiteto-padrao`, que divide o caso de uso em fatias e chama a `teste-aceitacao-modelos`.

## Quando você não fala com o dono

Outra pessoa ou agente conversou com o dono e você só escreve. O que veio de terceiro não é dito pelo dono até alguém confirmar.

- Toda regra tem **origem**: "dono, <data>" (o intermediário relata que o dono disse) ou "proposta" (veio de quem escreveu ou intermediou).
- Toda regra "proposta" está ligada a uma dúvida aberta com a pergunta exata, para o intermediário levar ao dono.
- Peça ao intermediário a data e, se possível, as palavras do dono. Sem isso, a origem é "proposta".
- A confirmação continua sendo do dono, não do intermediário.

## O que é uma boa regra

| Boa | Ruim | Por quê |
|---|---|---|
| "Processo só pode ser pago depois de aprovado" | "Validar o processo antes de pagar" | diz a condição e o efeito |
| "Uma cota tem no máximo um processo em andamento" | "Não permitir duplicidade" | diz de quê e em que situação |
| "Valor do crédito é maior que zero" | "Valor deve ser válido" | dá para escrever um exemplo que passa e um que falha |
| "Se a tesouraria não responder, o processo continua aprovado e o usuário é avisado" | "Tratar erros" | diz o que o usuário vê e o que acontece com o dado |

Cada regra tem um identificador (R1, R2...), uma frase, uma origem, e ao menos um exemplo que cumpre e um que viola.

## O que não entra

- Nome de classe, tabela, coluna, pasta, padrão ou framework.
- Caminho e formato de endpoint, código técnico de resposta (409, 503) e formato de dado. Você registra **o que** entra e sai em palavras do negócio; o contrato HTTP é definido depois, na implementação, e acrescentado ao mesmo arquivo.
- Tela desenhada. Registre o que a pessoa precisa ver e fazer, não como.

**Já está escrito, ou o dono falou assim.** Regra antiga com código de resposta ou formato de dado, ou dono que diz "endpoint", "job" ou o nome de um padrão: reescreva em palavras de negócio. Regra reescrita só na redação leva "(alterada só na redação)". A decisão técnica que o dono expressou não se perde: vai para "Decisões de solução citadas pelo dono" no modelo, fora das regras, com data, para quem for implementar. Essa nota não é regra de negócio e não gera teste.

## Antes de apresentar

- [ ] Toda regra tem identificador, origem, uma condição e um efeito, e exemplo que cumpre e que viola.
- [ ] Todo estado tem como se entra nele e o que se pode fazer a partir dele; a tabela situação × ação não tem célula vazia, e cada célula é "vai para X", "recusada", "não se aplica" ou "dúvida Dn".
- [ ] Toda recusa diz o que o usuário é informado e o que acontece com o dado.
- [ ] Para cada sistema externo: o que se pede a ele, o que ele devolve, e o que acontece se não responder.
- [ ] Para cada rotina automática e regra de tempo: de quanto em quanto tempo, o que acontece se ficar parado, limite de tentativas, quem é avisado; o que falta está em dúvidas.
- [ ] Para cada ação: quem pode executá-la (papel), com origem.
- [ ] O que acontece com dois pedidos ao mesmo tempo e com o mesmo pedido repetido está dito, ou está em dúvidas.
- [ ] "Fora do escopo" tem ao menos o que o dono citou e adiou.
- [ ] Nenhuma regra veio de você: o que você supôs está em dúvidas, com proposta.
- [ ] Nenhuma palavra da solução (classe, tabela, endpoint, padrão, código de resposta) nas regras.
- [ ] Cada regra foi comparada com o caso de uso confirmado e com os outros casos de uso do projeto: as contradições e as regras que mudam estão listadas.
- [ ] O caso de uso confirmado não está desatualizado em relação ao que o dono descreve como comportamento de hoje. Se está, o rascunho diz isso e vira pergunta; não se corrige em silêncio.

## Quando o caso de uso muda

Mudança em caso de uso confirmado segue o mesmo caminho: rascunho da mudança, confirmação, e só então o confirmado é substituído. O rascunho é o documento inteiro, fica em `caso-de-uso-rascunho.md` ao lado do confirmado, e regra removida não é apagada: fica riscada, com a data e o motivo. Detalhes, marcas e cuidados com testes: [mudar um caso de uso confirmado](references/mudar-caso-de-uso-confirmado.md).
