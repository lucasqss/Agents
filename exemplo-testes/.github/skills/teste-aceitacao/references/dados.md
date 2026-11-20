# Dados: massa e cenários em YAML

> Neste projeto a massa de cadastro é SQL com IDs fixos ([IDs fixos](ids-fixos.md)): o cenário cita o registro pelo ID. A massa por `chave` em YAML, criada pelos endpoints, descrita abaixo, continua valendo para os sistemas externos simulados (`massa/externa-<sistema>.yaml`) e para serviços sem massa SQL.

Arquivos YAML em UTF-8, no repositório, ao lado da feature da capacidade. O Karate os lê sem conversor, com os tipos de cada valor (texto, número, verdadeiro/falso, lista). O analista revisa a mudança no pull request: o diff mostra só o cenário que mudou.

## `cenarios.yaml`

Um item por cenário, com os passos em lista, na ordem em que acontecem.

| Campo | Obrigatório | O que é |
|---|---|---|
| `cenario` | sim | identificador entre aspas (`'1.2'`); vira o nome do teste com a `descricao` |
| `regra` | sim | a regra que o cenário prova (`R1`): rastreabilidade |
| `descricao` | sim | o cenário em uma frase de negócio |
| `tipo` | sim | `positivo` ou `negativo` |
| `passos` | sim | a lista de passos |
| `passos[].acao` | sim | o que a pessoa faz; é a tag da cena em `passos.feature` (`criar` chama `@criar`) |
| `passos[]` entradas | conforme a ação | os dados do passo; campo com o nome de uma massa (`cliente`) aponta para ela pela `chave`; listas aninhadas são permitidas |
| `passos[].espera` | sim | o resultado: `status` e o que conferir depois (`estado`, valores de negócio) |
| `passos[].observacao` | não | texto livre |

Valores-padrão ficam num bloco `padroes` no início do arquivo, como âncora (`&pedido`). Cada passo herda com `<<: *pedido` e escreve só o que muda:

```yaml
padroes:
  pedido: &pedido
    acao: criar
    cliente: ana
    valor: 10.00
    itens:
      - { produto: caneta, quantidade: 2 }

cenarios:
  - cenario: '1.2'
    regra: R1
    descricao: Pedido acima do limite é recusado
    tipo: negativo
    passos:
      - <<: *pedido
        valor: 1000.01
        espera: { status: 201, estado: RECUSADO }

  - cenario: '3.1'
    regra: R3
    descricao: Pedido aprovado pode ser cancelado uma vez
    tipo: positivo
    passos:
      - <<: *pedido
        espera: { status: 201, estado: APROVADO }
      - acao: cancelar
        espera: { status: 200, estado: CANCELADO }
      - acao: cancelar
        observacao: cancelar de novo não muda nada
        espera: { status: 409, estado: CANCELADO }
```

Arquivo completo: `assets/exemplo-pedidos/resources/aceitacao/pedidos/cenarios.yaml`.

## `massa/<entidade>.yaml` (cadastro)

Uma lista de registros, cada um com a `chave` (nome curto, em minúsculas: `ana`, `conta-bloqueada`). O cenário aponta para a chave; o passo cria o registro pelo endpoint de cadastro e usa o id que o serviço devolveu. **Id gerado nunca entra no arquivo.**

```yaml
- chave: ana
  nome: Ana Souza
  documento: '11111111111'
  limite: 1000.00
```

## `massa/externa-<sistema>.yaml` (sistemas externos)

Uma lista de respostas do sistema externo, identificadas pelo valor que o serviço envia (documento, código). O `<Sistema>Simulado` transforma cada registro numa resposta do WireMock. O porquê de cada registro vai em comentário.

```yaml
- documento: '22222222222'
  aprovado: false     # antifraude recusa este documento
```

## Convenções

- **Documento, código, CPF, conta e identificador sempre entre aspas.** Sem aspas, o YAML lê como número: `documento: 01234567` vira `342391` (octal), sem erro nenhum, no Karate e no lado Java. Só valor que é número de verdade (valor, quantidade, limite) fica sem aspas.
- **Valores** com ponto decimal (`1000.00`), sem separador de milhar.
- **Datas relativas** (`d+0`, `d+1`) quando o resultado depende do dia; o passo converte. Data fixa só quando a regra é sobre aquela data.
- **Comentário** (`#`) para explicar um valor ao analista, em vez de um campo `porque`.
- **Nunca no arquivo:** id gerado, URL, nome de classe, detalhe técnico, status HTTP como única conferência quando há uma situação de negócio para conferir.

## Quando o arquivo fica grande

Centenas de cenários por capacidade são normais. Separe por regra (`cenarios-limite.yaml`, `cenarios-cancelamento.yaml`), cada um com o seu `Scenario Outline`. Se muitos cenários repetem a mesma variação com valores diferentes, a variação provavelmente é da `teste-unidade`.
