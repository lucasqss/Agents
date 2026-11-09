# Fase A: ler o código

Leia de fora para dentro: do endpoint até a decisão de negócio. Trate **um endpoint por vez** e anote as regras dele antes de passar ao próximo.

## A2. Achar os endpoints da capacidade

```bash
grep -rn "@Path\|@GET\|@POST\|@PUT\|@PATCH\|@DELETE" src/main/java | grep -v import
```

- Fique só com os caminhos da capacidade pedida, inclusive os de canal (ex.: `/mobile/v1/clientes`).
- Escopo duvidoso (ex.: o pedido não citou exclusão, mas o endpoint existe) vira dúvida.
- Anote o corpo de entrada (record ou DTO do parâmetro) e o que o método devolve.

## A3. Seguir cada endpoint até as regras

Para cada endpoint, abra nesta ordem:

1. **O método do resource:** o status de sucesso (`Response.created` = 201; método que devolve objeto = 200; `void` = 204) e as anotações de validação do corpo.
2. **O caso de uso ou service que ele chama:** buscas que lançam "não encontrado", consultas de existência ("já existe?"), bloqueios e chamadas a sistema externo.
3. **O domínio ou a entidade:** guardas (`if (...) throw`), transições de situação, cálculos.
4. **O mapeamento de exceção para status:**

```bash
grep -rn "ExceptionMapper\|@ServerExceptionMapper\|Response.status\|Status\.\(BAD_REQUEST\|NOT_FOUND\|CONFLICT\|SERVICE_UNAVAILABLE\)" src/main/java
```

Para cada exceção lançada no caminho do endpoint, anote o status. **Exceção sem mapeamento vira 500**: anote como suspeito.

5. **Os limites no banco:**

```bash
grep -rn "VARCHAR\|NOT NULL\|UNIQUE\|CHECK" src/main/resources/db
```

### Como anotar cada regra

Cada regra tem quatro colunas, nesta forma:

| Regra | Descrição em linguagem de negócio | Origem | Trecho |
|---|---|---|---|
| R3 | Dois clientes não podem ter o mesmo CPF | `CriarClienteUseCase.java:22` | `if (clientes.existePorCpf(cpf))` |

- **A origem vem do `grep -n`**, nunca de contagem de cabeça: `grep -n "existePorCpf" src/main/java -r`.
- **O trecho é copiado da linha**, sem reescrever.
- **A descrição fala do negócio:** "processo pago não pode ser cancelado", e não "`cancelar()` lança exceção se `finalizado()`".
- Uma regra que está em dois lugares (ex.: validação no request e `UNIQUE` no banco) leva as duas origens.

## A3 (continuação). Achar os suspeitos

Rode os comandos e confira cada resultado **que está no caminho dos endpoints da capacidade**.

| Comando | O que procurar | Por que é suspeito |
|---|---|---|
| `grep -rn "catch (Exception\|catch (Throwable\|catch (RuntimeException" src/main/java` | erro engolido ou trocado por sucesso | o cliente recebe 2xx sem efeito |
| `grep -rn "TODO\|FIXME\|XXX" src/main/java` | perto de uma regra | o autor sabia que estava incompleto |
| exceções do caminho sem `ExceptionMapper` (passo 4) | exceção de negócio que vira 500 | erro do cliente respondido como falha do servidor |
| consulta "existe?" seguida de gravação, sem bloqueio nem `UNIQUE` | corrida | pedidos simultâneos furam a regra |
| a mesma validação em dois endpoints | limites ou anotações diferentes | um dos dois está errado |
| `@Size`, `@NotBlank` contra `VARCHAR(n)` | `@Size` maior que a coluna, ou campo sem validação | erro do banco vira 500 |
| busca que pode devolver `null` e é usada direto | `NullPointerException` | 500 em vez de 404 |
| `Location` ou link para caminho sem `@GET` | link quebrado | o cliente segue um link que dá 404 ou 405 |
| dado pessoal (CPF, e-mail) em mensagem de erro | vazamento | LGPD |

Cada suspeito vira uma linha no plano, com a origem, o trecho e a pergunta: **"fixar como está ou é defeito? Se for defeito, o correto seria X."**

Um suspeito não é defeito confirmado. Quem decide é o usuário.

## O que não vale como fonte

- Outros testes (`src/test`). Eles podem estar errados. Use-os, na fase B, só para copiar a mecânica: atalhos HTTP e stub.
- Comentários, README e Javadoc como regra. Eles servem só como pista, e a regra vem do código executável.

## Quando o código não basta

| Situação | O que fazer |
|---|---|
| Ramo que nenhuma requisição alcança (ex.: guarda no domínio que a validação do request já barra) | Registre como dúvida: "sem teste de aceitação?" |
| Status gerado pelo framework (415, id não numérico no caminho, JSON malformado) | Liste no contrato como "framework". Se precisar do valor exato, pergunte ou confirme na exploração da fase B |
| Comportamento que depende de configuração do ambiente | Pergunte |
| Dois trechos que se contradizem | Suspeito, com as duas origens |
