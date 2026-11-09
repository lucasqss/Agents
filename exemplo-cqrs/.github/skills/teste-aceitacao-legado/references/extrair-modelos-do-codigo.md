# Extrair modelos do código

Este texto vale para projetos que seguem o padrão da skill `arquiteto-padrao` e também para legados: Panache, service god class, entidade JPA na API. Os nomes dos exemplos são ilustrativos.

## Comece pelos endpoints

Leia de fora para dentro. Cada endpoint é o ponto de partida de uma regra que o teste consegue alcançar. Regra que nenhum endpoint alcança não vira teste de aceitação: registre como dúvida.

1. Liste os endpoints: classes com `@Path`, métodos com `@GET`, `@POST`, `@PUT`, `@PATCH`, `@DELETE`. Com o sistema rodando, `/q/openapi` dá a mesma lista.
2. Siga cada endpoint até onde a decisão de negócio acontece: use case, service, domínio, entidade com lógica, consulta.
3. Anote cada decisão como regra, com a origem: `ProcessoService.java:142`.
4. Descreva a regra em linguagem de negócio. "Processo pago não pode ser cancelado", e não "`cancelar()` lança exceção se `status == PAGO`".

## Onde achar cada modelo

| Modelo | Onde procurar no código | O que extrair |
|---|---|---|
| **Terminais do contrato** | `@Path` e métodos HTTP; records ou DTOs de entrada e saída; `@QueryParam` e `@PathParam`; `ExceptionMapper`s; `catch` que monta `Response`; `Response.status(...)` | Campos, valores de enumeração, ações do caminho e **todos** os status que o código produz, inclusive os não documentados |
| **Partições de entrada** | Bean Validation (`@NotBlank`, `@Size`, `@Positive`, `@Pattern`); tamanho e `NOT NULL` das colunas nas migrations; guardas que lançam exceção (`if (valor <= 0) throw ...`); conversões de tipo | Classes válidas e inválidas e os valores-limite exatos |
| **Máquina de estados** | `enum` ou `sealed` de situação; `switch` ou cadeia de `if` sobre o status; métodos de transição; `setStatus(...)` espalhados em services | Estados, ações e a tabela situação × ação, inclusive as células que recusam |
| **Tabela de decisão** | Condições combinadas com `&&` e `\|\|`; consultas de existência antes de gravar ("já existe processo em andamento?"); regras que dependem de tipo, de situação ou de outro agregado | Cada condição e o resultado de cada combinação |
| **Unicidade** | `UNIQUE` nas migrations; consulta seguida de inserção; `catch` de violação de constraint | Qual campo é único, o status esperado e se há corrida possível |
| **Sistema externo** | REST clients (`@RegisterRestClient`), gateways, chave de URL na configuração (`quarkus.rest-client.<nome>.url`), tratamento de timeout e falha | O contrato e a chave, **só** para a classe do stub; o status que o sistema devolve quando o externo falha |

**Sem consulta por id:** quando o efeito só aparece numa listagem, procure o item pelo id, página a página, como `consultarProcesso` no [exemplo da skill irmã](../../teste-aceitacao-modelos/references/exemplo-pagamento-processo.md). O banco é compartilhado: nunca confira totais, contagens ou ordem da lista inteira. Confira só os itens criados pelo caso, com prefixo único.

**Mesmo DTO em vários endpoints:** os valores-limite de um DTO compartilhado são cobertos uma vez. Nos demais endpoints, basta uma classe inválida por campo, para provar que a validação está ligada.

Num legado, a mesma regra pode estar em mais de um lugar, por exemplo no service e numa trigger do banco. Liste todas as origens.

## Comportamentos suspeitos

Anote enquanto lê. Cada um vai para a seção **Comportamentos suspeitos** do plano, com a pergunta "fixar como está ou é defeito?".

| Sinal no código | Por que é suspeito |
|---|---|
| Exceção de negócio sem `ExceptionMapper`, que vira 500 | Erro do cliente respondido como falha do servidor |
| `catch (Exception e)` genérico, ou `catch` vazio | Erro engolido; a resposta pode ser sucesso sem efeito |
| Consulta de existência seguida de inserção, sem trava nem `UNIQUE` | Pedidos simultâneos furam a regra |
| O mesmo campo validado de forma diferente em endpoints distintos | Uma das duas formas provavelmente está errada |
| Erro com formato diferente dos demais (sem corpo, texto puro) | O cliente não consegue tratar o erro |
| `TODO`, `FIXME`, `XXX`, código comentado perto da regra | O autor sabia que estava incompleto |
| Ramo que nenhuma entrada alcança | Regra morta ou regra quebrada |
| Status que contraria o efeito (200 sem gravar, 201 sem id) | O cliente acredita num efeito que não aconteceu |
| Regra só no front, ou só documentada, sem verificação no backend | O endpoint aceita o que o negócio recusa |

Suspeito não é defeito confirmado. Só o usuário decide.

## Mini-exemplo

Código encontrado (legado):

```java
// ProcessoService.java
public void cancelar(Long id) {                                     // linha 88
    Processo p = Processo.findById(id);
    if (p == null) throw new NotFoundException();                    // linha 90
    if (p.status == Status.PAGO) throw new RegraException("pago");   // linha 91
    p.status = Status.CANCELADO;                                     // linha 92
}

public void pagar(Long id) {                                        // linha 95
    Processo p = Processo.findById(id);
    if (p.status != Status.APROVADO) throw new RegraException("...");// linha 97
    try {
        tesouraria.emitir(p.id);
    } catch (Exception e) {                                         // linha 101
        log.warn("tesouraria fora", e);
    }
    p.status = Status.PAGO;                                          // linha 104
}
```

Regras, em linguagem de negócio:

| Regra | Descrição | Origem |
|---|---|---|
| R1 | Cancelar processo inexistente responde 404 | `ProcessoService.java:90` |
| R2 | Processo pago não pode ser cancelado | `ProcessoService.java:91` |
| R3 | Qualquer processo não pago pode ser cancelado, **inclusive um já cancelado** | `ProcessoService.java:91-92` |
| R4 | Só processo aprovado pode ser pago | `ProcessoService.java:97` |

Tabela de transições, como o código está hoje:

| Situação | cancelar | pagar |
|---|---|---|
| ABERTO | CANCELADO | recusa |
| APROVADO | CANCELADO | PAGO |
| PAGO | recusa | recusa |
| CANCELADO | CANCELADO (?) | recusa |

Comportamentos suspeitos do plano:

| Suspeito | Origem | Pergunta |
|---|---|---|
| Cancelar um processo já cancelado responde sucesso | `ProcessoService.java:91-92` | Fixar como está, ou é defeito (o correto seria 409)? |
| `pagar` em processo inexistente: `p.status` gera `NullPointerException` e 500 | `ProcessoService.java:97` | Fixar como está, ou é defeito (o correto seria 404, como em R1)? |
| Com a tesouraria fora, o processo vira `PAGO` sem ordem emitida | `ProcessoService.java:101-104` | Fixar como está, ou é defeito (o correto seria 503 e o processo continuar `APROVADO`)? |
| `RegraException` não tem `ExceptionMapper`: qual status sai? | `ProcessoService.java:91` | Confirmar na sessão exploratória e perguntar se é o status desejado |

## Modelo de `caso-de-uso.md`

````markdown
# Caso de uso: <capacidade>

As regras abaixo foram lidas do código e confirmadas pelo usuário em <data>. Os testes conhecem apenas estas regras e o contrato HTTP.

## Regras de negócio

| Regra | Descrição | Origem no código | Confirmada |
|---|---|---|---|
| R1 | ... | `Arquivo.java:42` | sim |

## Contrato usado

| Operação | Entrada | Resposta |
|---|---|---|
| `POST /...` | ... | ... |

## Modelo

Tabela de transições, partições ou tabela de decisão.

## Rastreabilidade

| Regra | Modelo e critério | Teste | Casos |
|---|---|---|---|
| R1 | ... | `nomeDoTeste` | 1 |

## Comportamento atual suspeito

Fixados como estão, por decisão do usuário. Quando a produção for corrigida, estes testes falham de propósito e são atualizados junto.

| Comportamento | Origem no código | Teste (`[atual]`) | Decidido por e quando |
|---|---|---|---|

## Sessão exploratória

Missão: ...

### Defeitos

| Regra | Tentativa | Observado |
|---|---|---|

### Perguntas

| Tentativa | Observado |
|---|---|

### Confirmado

...
````

## Referências da skill irmã que valem aqui

- [modelos-e-criterios.md](../../teste-aceitacao-modelos/references/modelos-e-criterios.md): vale inteira. Onde ela diz "o requisito", leia "a regra confirmada".
- [quarkus-test-e-it.md](../../teste-aceitacao-modelos/references/quarkus-test-e-it.md): vale inteira.
- [testes-exploratorios.md](../../teste-aceitacao-modelos/references/testes-exploratorios.md): vale, exceto a proibição de ler o código e o `application.properties`, que aqui não se aplica. A exploração continua só por HTTP. Um achado que contradiz o modelo lido do código é **defeito** ou **erro de leitura**: releia o trecho e relate.
- [exemplo-pagamento-processo.md](../../teste-aceitacao-modelos/references/exemplo-pagamento-processo.md): vale para a mecânica (modelo executável, atalhos HTTP, stub). A fonte das regras de lá é o requisito, e a daqui é o código.
