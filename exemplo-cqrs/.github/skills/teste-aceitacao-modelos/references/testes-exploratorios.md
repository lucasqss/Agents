# Testes exploratórios

Os testes baseados em modelos cobrem o que o requisito diz. A sessão exploratória procura o que ninguém especificou. Quem explora projeta e executa ao mesmo tempo, guiado pelo que acabou de observar.

## Como fazer

1. **Escreva a missão** em uma frase. Ex.: "Procurar respostas que contrariem as regras de abertura de pedido".
2. **Defina um tempo limite**, por exemplo 30 minutos.
3. **Suba o sistema empacotado**, seguindo as instruções de execução do repositório. Se não houver, pergunte ao usuário como subir e quais são o banco e as URLs dos sistemas externos. Não leia `application.properties` nem o código para descobrir. Use **apenas HTTP**: `curl`, Postman ou RestAssured. Não abra o código para entender uma resposta.
4. **Registre** cada tentativa e a resposta obtida.
5. **Classifique cada achado** pela tabela abaixo.

## Destino de cada achado

| O que aconteceu | Classificação | O que fazer |
|---|---|---|
| Confirma uma regra | Confirmado | Se a regra ainda não tem caso, volte ao modo plan, confirme e automatize |
| Contradiz uma regra ou o contrato | **Defeito** | Relate. Automatize apenas se o resultado for determinístico |
| A regra não diz nada | **Pergunta** | Registre a pergunta. **Não fixe o comportamento em teste** |

Um comportamento observado não é, sozinho, o comportamento correto. Só o requisito ou o usuário definem isso.

## Ideias de exploração

| Área | Tentativas |
|---|---|
| Identificadores | zero, negativo, texto, maior que o inteiro longo |
| Corpo | vazio, malformado, campo extra, tipo errado, `null`, enumeração em minúsculas |
| Cabeçalhos | `Content-Type` errado ou ausente |
| Sequência | repetir a ação, voltar a um passo anterior, agir depois do estado final |
| Paginação e filtros | página ou tamanho zero, negativo, enorme, texto; filtro desconhecido |
| Concorrência | vários pedidos simultâneos para a mesma regra de unicidade |
| Sistema externo | indisponível, depois recuperado |
| Formato de erro | todo erro segue o formato do contrato? |

## Concorrência

Defeitos de concorrência dependem do momento em que os pedidos chegam. Relate quantas vezes ocorreu e em quantas tentativas. Evite criar teste automatizado que passe e falhe ao acaso.

## Registro

No `caso-de-uso.md`, mantenha três listas: **Defeitos**, **Perguntas** e **Confirmado**. Cada item traz a tentativa e o que foi observado.
