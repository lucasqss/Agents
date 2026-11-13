# Mudar um caso de uso confirmado

Vale quando a capacidade já tem `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md` com `Situação: CONFIRMADO`. O princípio é o mesmo da skill: você registra o que o dono disse, e o que não foi dito é pergunta.

## Onde fica e o que é o rascunho

- O rascunho fica em `caso-de-uso-rascunho.md`, **ao lado** do confirmado, na mesma pasta.
- É o documento **inteiro** como ficaria depois da mudança, no [modelo](modelo-caso-de-uso.md), com `Situação: RASCUNHO, aguardando confirmação`. Não é só a diferença.
- O `caso-de-uso.md` confirmado **continua sendo a fonte dos testes** até a confirmação. Não o edite antes.

## Como marcar cada regra

| Situação da regra | Como aparece no rascunho |
|---|---|
| Continua igual | Mesmo identificador, sem marca |
| Muda | Mesmo identificador, com "(alterada)" |
| Muda só a redação (ver "O que não entra", no [`SKILL.md`](../SKILL.md)) | Mesmo identificador, "(alterada só na redação)" |
| Nova | Continua a numeração, com "(nova)" |
| Deixa de valer | Fica na tabela, riscada, com data e motivo |

Identificador de regra e de dúvida **nunca é renumerado**, nem o de regra riscada. Número usado não é reaproveitado. A origem de cada regra segue a coluna "Origem" do modelo; uma regra "(alterada)" tem a origem da alteração.

## O que o rascunho traz no início

Logo depois de `Situação`, um bloco **Para quem mantém os testes**:

- regras alteradas, novas e removidas, por identificador, com uma frase do que muda;
- regras que mudaram só a redação (os testes não mudam);
- defeitos e perguntas ainda abertos da sessão exploratória do confirmado, por regra, para não se perderem na substituição.

Esse bloco é o que a skill `teste-aceitacao-modelos` precisa para achar os testes afetados. Ele sai do arquivo na confirmação ou fica como histórico, a critério do dono.

## O que o rascunho não copia

O confirmado costuma ter, além das regras, seções acrescentadas depois pela implementação e pela skill `teste-aceitacao-modelos`: contrato, modelo de teste, rastreabilidade e sessão exploratória. O rascunho **não as copia**: elas descrevem testes e contrato que ainda não existem para as regras novas. Depois da confirmação, essas seções são refeitas pela skill de testes a partir das regras confirmadas.

## Antes de apresentar o rascunho

Além da lista da skill:

1. Compare **cada regra do rascunho** com o confirmado. Liste as contradições e as regras que mudam, no bloco acima. Contradição que o dono não pediu é pergunta, não texto.
2. Confira se o confirmado está **desatualizado** em relação ao que o dono descreve como comportamento de hoje. Quando o sistema já faz o que o documento não registra, diga isso no rascunho e faça a pergunta ("Isto já acontece hoje e não está no caso de uso: vale como regra?"). Não corrija em silêncio, nem trate o comportamento como regra confirmada.

## Confirmação

1. Registre as respostas como no passo 8 da skill (sem apagar nem renumerar).
2. Regra do rascunho que depende de dúvida sem resposta não entra: volte-a ao texto do confirmado, ou tire-a se é nova. A dúvida continua na seção de dúvidas.
3. Com a confirmação do dono, troque a situação para `CONFIRMADO em <data> por <dono>`. O rascunho **substitui** o `caso-de-uso.md` e o arquivo de rascunho é **removido**.
4. Avise quem mantém os testes: a skill `teste-aceitacao-modelos` refaz contrato, modelo, rastreabilidade e sessão exploratória, partindo da lista de regras que mudaram. Indique depois a receita "nova funcionalidade" da `arquiteto-padrao` para o que precisa de código.

Se o dono não confirma, o rascunho fica, e o confirmado continua valendo sem alteração.
