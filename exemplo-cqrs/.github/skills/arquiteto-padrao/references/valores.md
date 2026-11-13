# Value Object e dinheiro

Um conceito que tem regra própria e não tem identidade (dinheiro, CPF, período, percentual) vira um **Value Object** [PoEAA]: um `record` imutável que só existe válido. Dois valores iguais são o mesmo valor.

## Quando criar

- O mesmo dado carrega uma validação ou uma conta que se repete (formato, arredondamento, intervalo).
- Dois dados andam sempre juntos (início e fim, valor e moeda).

Um `String` ou `BigDecimal` solto serve enquanto ninguém faz conta nem valida nada com ele.

## Como escrever

- `record` em Java puro. O construtor compacto valida e normaliza, e lança `DominioException`.
- Operações devolvem um valor novo; nada de setter.
- Fábrica estática curta quando ajuda a leitura (`Dinheiro.de("10.00")`).
- Teste JUnit puro para cada regra do valor.

## Onde fica

| Quem usa | Pasta |
|---|---|
| Um agregado | `domain/<agregado>/` |
| Dois ou mais agregados | raiz de `domain/`. Só entra tipo que a regra GER-003 lista; hoje, `Dinheiro`. Para outro, peça ao dono |

## Dinheiro (Money [PoEAA])

Todo valor monetário do domínio é `Dinheiro` ([exemplo](exemplos-codigo.md)), nunca `BigDecimal` solto, `double` ou `float`.

- Guarda o valor com as casas da moeda (duas, no real) e recusa valor com mais casas em vez de arredondar calado.
- Soma, subtração e comparação saem dele. Multiplicação por taxa arredonda numa regra só, declarada no tipo.
- **Dividir é ratear:** `ratear(n)` devolve partes que somam exatamente o total, entregando os centavos que sobram às primeiras partes. Dividir e arredondar cada parte perde ou cria centavo.
- Moeda única: o tipo guarda só o valor. Com mais de uma moeda, ela vira o segundo componente do record, e as operações recusam misturar moedas.

## Nas outras camadas

| Camada | Como o valor aparece |
|---|---|
| `entrypoint/` | tipo simples no `XRequest` (`BigDecimal`, `String`), com Bean Validation de formato |
| `application/` | `DadosX` leva o tipo simples; o caso de uso constrói o Value Object ao chamar o domínio |
| `persistence/` (escrita) | a `*Entity` guarda o tipo simples da coluna; o mapper converte nos dois sentidos (Embedded Value [PoEAA]). Valor de dois componentes ocupa duas colunas |
| `persistence/` (leitura) | o record projetado leva o tipo simples; leitura não usa o domínio |
| Canal | o Assembler formata (símbolo, separadores). O Value Object não formata para tela |
