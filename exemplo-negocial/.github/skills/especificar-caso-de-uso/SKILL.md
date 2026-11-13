---
name: especificar-caso-de-uso
description: Use quando houver uma ideia, pedido ou necessidade de negócio ainda sem requisito escrito e for preciso transformá-la em um caso de uso confirmado, antes de escrever código ou teste. Entrevista o dono, registra regras de negócio, ciclo de vida, erros e exemplos, e grava o caso-de-uso.md que as skills arquiteto-padrao e teste-aceitacao-modelos usam como entrada. Não use para sistema existente sem requisito (use teste-aceitacao-legado) nem para decidir arquitetura.
---

# Especificar caso de uso

Transforma um pedido em linguagem de negócio num caso de uso escrito e confirmado. É o primeiro passo de uma funcionalidade nova: o código e os testes de aceitação partem deste arquivo.

**Princípio:** você escreve o que o dono disse, não o que parece razoável. Regra que ele não disse é pergunta, nunca texto do caso de uso.

**Por quê:** uma regra inventada vira teste, depois código, e passa a parecer requisito. Descobrir isso depois custa a funcionalidade inteira.

Esta skill não escreve código, teste, tabela nem endpoint, e não escolhe pasta nem padrão. Nomes como `cota`, `processo` e `tesouraria` nos exemplos são ilustrativos.

## Passo a passo

1. **Leia o que já existe.** Outros `caso-de-uso.md` em `src/test/resources/aceitacao/`, para usar o mesmo vocabulário e não contradizer regra já confirmada. Não leia `src/main`: o caso de uso descreve o negócio, não a solução atual.
2. **Dê nome à capacidade** no vocabulário do dono (ex.: "pagamento de processo"). Uma capacidade é o que uma pessoa consegue fazer do começo ao fim. Se o pedido cobre duas, separe e pergunte por qual começar.
3. **Entreviste** seguindo o [roteiro](references/roteiro-de-perguntas.md). Poucas perguntas por vez, das que mudam mais o desenho para as que mudam menos. Ofereça opções concretas quando souber as alternativas comuns; pergunta aberta só quando não houver.
4. **Peça exemplos concretos.** Para cada regra, um exemplo com valores ("cota de R$ 250.000,00, processo aprovado, pagar → fica pago com o protocolo"). Exemplo que o dono não consegue dar é regra que ainda não está decidida.
5. **Escreva o rascunho** no [modelo](references/modelo-caso-de-uso.md), com `Situação: RASCUNHO`. Tudo o que você supôs vai para a seção de dúvidas, com a sua proposta.
6. **Confira** com a lista "Antes de apresentar".
7. **Apresente ao dono e pare.** Ele responde as dúvidas e corrige as regras.
8. **Só com a confirmação**, grave `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md` com `Situação: CONFIRMADO` e a data. Dúvida que ficou sem resposta continua na seção de dúvidas, e a regra que depende dela não entra.
9. **Indique o próximo passo:** a receita "nova funcionalidade" da skill `arquiteto-padrao`, que divide o caso de uso em fatias e chama a `teste-aceitacao-modelos`.

## O que é uma boa regra

| Boa | Ruim | Por quê |
|---|---|---|
| "Processo só pode ser pago depois de aprovado" | "Validar o processo antes de pagar" | diz a condição e o efeito |
| "Uma cota tem no máximo um processo em andamento" | "Não permitir duplicidade" | diz de quê e em que situação |
| "Valor do crédito é maior que zero" | "Valor deve ser válido" | dá para escrever um exemplo que passa e um que falha |
| "Se a tesouraria não responder, o processo continua aprovado e o usuário é avisado" | "Tratar erros" | diz o que o usuário vê e o que acontece com o dado |

Cada regra tem um identificador (R1, R2...), uma frase, e ao menos um exemplo que cumpre e um que viola.

## O que não entra

- Nome de classe, tabela, coluna, pasta, padrão ou framework.
- Caminho e formato de endpoint. Você registra **o que** entra e sai em palavras do negócio; o contrato HTTP é definido depois, na implementação, e acrescentado ao mesmo arquivo.
- Tela desenhada. Registre o que a pessoa precisa ver e fazer, não como.

## Antes de apresentar

- [ ] Toda regra tem identificador, uma condição e um efeito, e exemplo que cumpre e que viola.
- [ ] Todo estado tem como se entra nele e o que se pode fazer a partir dele; a tabela situação × ação não tem célula vazia.
- [ ] Toda recusa diz o que o usuário é informado e o que acontece com o dado.
- [ ] Para cada sistema externo: o que se pede a ele, o que ele devolve, e o que acontece se não responder.
- [ ] O que acontece com dois pedidos ao mesmo tempo e com o mesmo pedido repetido está dito, ou está em dúvidas.
- [ ] "Fora do escopo" tem ao menos o que o dono citou e adiou.
- [ ] Nenhuma regra veio de você: o que você supôs está em dúvidas, com proposta.
- [ ] Nenhuma palavra da solução (classe, tabela, endpoint, padrão).

## Quando o caso de uso muda

Mudança em caso de uso confirmado segue o mesmo caminho: rascunho da mudança, confirmação, e só então o arquivo é alterado. Regra removida não é apagada: fica riscada, com a data e o motivo, para que os testes que dependem dela sejam achados.
