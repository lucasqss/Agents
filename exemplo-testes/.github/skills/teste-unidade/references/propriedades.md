# Testes de propriedade (jqwik)

O exemplo prova que a regra vale **para os valores que você escolheu**. A propriedade prova que ela vale **para qualquer valor**: o jqwik gera milhares de entradas e, se uma falha, encolhe até o menor exemplo que quebra a regra (`0,03 em 2 partes`).

Ficam em `*PropriedadesTest`, ao lado do teste de exemplos. A dependência e a configuração do Surefire estão no `pom.xml` deste projeto (sem `junit-platform.properties`: ele quebra a ordem dos `@QuarkusTest`).

## Quando usar

| Situação | Propriedade típica |
|---|---|
| Conta com dinheiro | o rateio sempre fecha a soma; somar e subtrair se desfazem; o resultado tem sempre duas casas |
| Igualdade de Value Object | iguais independentemente da forma informada (escala, máscara, maiúsculas) |
| Tabela de estados | estado final não tem saída; toda ação fora da tabela é recusada e não muda nada |
| Regra que vale em qualquer caminho | "nada é pago acima da alçada sem aprovação na alçada", em qualquer sequência de ações válidas |
| Ida e volta | `ler(escrever(x)) == x` (mapper, formatador, serialização) |

Não use para regra de um valor específico ("o limite é 500"): isso é exemplo, com fronteira.

## Forma

```java
class ValorPropriedadesTest {

    @Property
    void rateioSempreFechaASoma(@ForAll("valores") Valor valor, @ForAll @IntRange(min = 1, max = 500) int partes) {
        assertEquals(valor, valor.ratear(partes).stream().reduce(Valor.ZERO, Valor::somar));
    }

    @Provide
    Arbitrary<Valor> valores() {
        return Arbitraries.longs().between(-1_000_000_00L, 1_000_000_00L).map(c -> Valor.de(BigDecimal.valueOf(c, 2)));
    }
}
```

## Caminhos gerados em tabela de estados

Gere uma lista de escolhas (`List<@IntRange(min = 0, max = 1000) Integer>`). A partir do estado inicial, cada escolha seleciona uma das ações **válidas** no estado atual (`escolha % validas.size()`). Aplique e registre o passo `(de, ação, para)`. Depois confira a propriedade sobre o caminho inteiro. Assim o gerador só anda por caminhos possíveis e chega a estados profundos.

Modelos no exemplo `exemplo-negocial`: `domain/DinheiroPropriedadesTest.java` (valor monetário) e `domain/processo/tipo/FluxosPropriedadesTest.java` (caminhos na tabela de fluxo de cada tipo).

## Para provar que a propriedade pega

Introduza o defeito à mão (um centavo a menos no rateio), rode, veja o menor exemplo e desfaça. Propriedade que não falha com o defeito não está afirmando nada.
