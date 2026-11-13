# Receita: nova funcionalidade, de ponta a ponta

Entrada necessária: o caso de uso escrito e confirmado pelo dono (`src/test/resources/aceitacao/<capacidade>/caso-de-uso.md`). Se não existir, use antes a skill `especificar-caso-de-uso`. Não comece pelo código.

Entregue em **fatias**: cada fatia é uma operação que funciona do endpoint ao banco, com teste, e passa no `./mvnw verify`. Não construa uma camada inteira antes da outra.

1. **Divida em fatias.** Liste as operações do caso de uso e ordene: primeiro a que cria o dado, depois as que o alteram, por fim as consultas e telas. Mostre a lista ao dono antes de começar.
2. **Decisões novas.** Se a funcionalidade pede algo que o padrão não cobre (agregado novo, sistema externo novo, forma de tratar transação com sistema externo, tipo novo na raiz de uma camada), pare e siga [nova decisão](nova-decisao.md).
3. **Para cada fatia**, na ordem:
   1. Teste de aceitação da fatia (skill `teste-aceitacao-modelos`). Ele falha: ainda não há código.
   2. Agregado novo? [Novo agregado](novo-agregado.md).
   3. Escrita: [nova operação de escrita](novo-caso-de-uso-escrita.md) ou, se for mudança de estado, [novo estado, ação ou tipo](novo-estado-acao-ou-tipo.md).
   4. Sistema externo: [nova integração externa](nova-integracao-externa.md), e [concorrência](../concorrencia.md).
   5. Consulta ou tela: [nova consulta](nova-consulta.md), [novo endpoint de canal](novo-endpoint-de-canal.md).
   6. `./mvnw verify` verde, com o teste de aceitação da fatia passando.
4. **Ao fim de cada fatia**, diga o que ficou pronto, o que falta e o que você não conseguiu verificar. Divergência entre o caso de uso e o que foi possível fazer é pergunta para o dono, não decisão sua.

Funcionalidade que acrescenta filhos a um agregado já existente: a migration vem com o filho padrão para as linhas atuais ([migrations](../migrations.md)), e a consulta dos filhos é fatia própria ([nova consulta](nova-consulta.md)).

Não antecipe: campo, estado, endpoint ou tabela que nenhuma fatia pedida usa fica para quando for pedido.
