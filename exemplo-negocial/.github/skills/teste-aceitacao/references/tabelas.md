# Tabelas: massa e cenários

Arquivos CSV em UTF-8, separados por vírgula, primeira linha com os nomes das colunas. Valor com vírgula vai entre aspas duplas (`"bom pagador, sem restrição"`). Abra e salve no Excel como "CSV UTF-8 (delimitado por vírgulas)"; o formato "CSV" comum do Excel em português usa ponto e vírgula e quebra a leitura.

O GitHub mostra CSV como tabela: o analista revisa a mudança no navegador, no pull request.

## `cenarios.csv`

Uma linha por **passo**. Linhas com o mesmo `cenario` são passos de um mesmo cenário, executados na ordem do arquivo.

| Coluna | Obrigatória | O que é |
|---|---|---|
| `cenario` | sim | identificador do cenário (`1.2`); vira o nome do teste com a `descricao` |
| `passo` | sim | ordem do passo no cenário (1, 2, 3...) |
| `regra` | sim | a regra que o cenário prova (`R1`): rastreabilidade |
| `descricao` | sim | o cenário em uma frase de negócio; repetida em todos os passos |
| `tipo` | sim | `positivo` ou `negativo` |
| `acao` | sim | o que a pessoa faz neste passo; é a tag da cena em `passos.feature` (`criar` chama `@criar`) |
| entradas | conforme a ação | os dados do passo; coluna com o mesmo nome de uma massa (`cliente`) aponta para ela pela `chave` |
| esperados | sim | o resultado do passo: `status` e o que conferir depois (`estado`, valores de negócio) |
| `observacao` | não | texto livre do analista |

Exemplo (`assets/exemplo-pedidos/resources/aceitacao/pedidos/cenarios.csv`):

```csv
cenario,passo,regra,descricao,tipo,acao,cliente,valor,status,estado,observacao
1.1,1,R1,Pedido dentro do limite é aprovado,positivo,criar,ana,1000.00,201,APROVADO,valor igual ao limite
3.1,1,R3,Pedido aprovado pode ser cancelado uma vez,positivo,criar,ana,10.00,201,APROVADO,
3.1,2,R3,Pedido aprovado pode ser cancelado uma vez,positivo,cancelar,,,200,CANCELADO,
3.1,3,R3,Pedido aprovado pode ser cancelado uma vez,negativo,cancelar,,,409,CANCELADO,cancelar de novo não muda nada
```

## `massa/<entidade>.csv` (cadastro)

Uma linha por registro, com a coluna `chave` (nome curto, em minúsculas: `ana`, `conta-bloqueada`). O cenário aponta para a chave; o passo cria o registro pelo endpoint de cadastro e usa o id que o serviço devolveu. **Id gerado nunca entra na tabela.**

## `massa/externa-<sistema>.csv` (sistemas externos)

Uma linha por resposta que o sistema externo dá, identificada pelo valor que o serviço envia (documento, código). O `<Sistema>Simulado` transforma cada linha numa resposta do WireMock. Uma coluna `por que` explica a linha para o analista.

## Convenções

- **Datas relativas** (`d+0`, `d+1`) quando o resultado depende do dia; o passo converte. Data fixa só quando a regra é sobre aquela data.
- **Valores** com ponto decimal (`1000.00`), sem separador de milhar.
- **"padrão"** numa coluna quer dizer "o valor de sempre", definido uma vez no passo; evita repetir o mesmo valor em todas as linhas.
- **Nunca na tabela:** id gerado, URL, nome de classe, detalhe técnico, status HTTP como única conferência quando há uma situação de negócio para conferir.

## Quando a tabela fica grande

Centenas de linhas por capacidade são normais. Separe por regra (`cenarios-limite.csv`, `cenarios-cancelamento.csv`), cada um com o seu `Scenario Outline`. Se as linhas repetem a mesma variação com valores diferentes, a variação provavelmente é da `teste-unidade`.
