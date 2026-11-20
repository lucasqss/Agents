---
name: teste-unidade
description: Use ao criar, alterar ou revisar testes de unidade em Java (JUnit 5, sem Quarkus) do domínio e dos casos de uso, ao cobrir uma regra de negócio, Value Object, tabela de estados/fluxo, tabela de decisão ou validação, ao escrever testes de propriedade com jqwik, ou ao matar mutantes sobreviventes do PITest. É o nível onde ficam as matrizes exaustivas. Para teste com o serviço montado (HTTP, banco, stubs) use teste-componente; para cenários de negócio em tabela use teste-aceitacao.
---

# Teste de unidade

O nível mais barato e mais rápido: JUnit puro, sem Quarkus, sem banco, sem HTTP. **É aqui que se prova cada regra de negócio de forma exaustiva**: todas as transições, cada ação inválida em cada estado, cada limite, cada combinação que decide o resultado. Os outros níveis não repetem esta cobertura.

**Princípio:** cada comportamento é provado no nível mais baixo que consegue prová-lo. Regra que mora no domínio é provada aqui, em milissegundos.

Os exemplos usam `pedido`, `conta` e `limite` como ilustração: não procure essas classes no repositório.

## Passo a passo

1. **Entreviste o dev** seguindo o [roteiro](references/entrevista.md). O que mudou, quais regras e limites, o que já está coberto, o que preocupa. Se existir um `caso-de-uso.md` da capacidade, use-o como ponto de partida, mas ele não é obrigatório: o que não estiver claro vira pergunta.
2. **Monte o plano de cobertura** no formato abaixo.
3. **Pare e aguarde a confirmação do dev.**
4. Escreva os testes, no mesmo pacote da classe testada (`src/test/java/.../domain/pedido/PedidoTest.java`).
5. Rode `./mvnw test` e, se o domínio mudou, `./mvnw verify -Pauditoria` ([mutação](references/mutacao.md)).
6. Relate: o que foi coberto, o que ficou para outro nível e as dúvidas que sobraram.

## Formato do plano

1. **Regras:** R1, R2... como você entendeu da entrevista.
2. **Técnica por regra:** qual das [técnicas](references/tecnicas.md) e qual critério de parada.
3. **Casos:** tabela com regra, pré-condição, entrada e **um** resultado esperado.
4. **Fica para outro nível:** o que este nível não prova (persistência, HTTP, segurança do endpoint, integração), com o nível certo.
5. **Dúvidas:** com a pergunta exata. Nenhuma dúvida vira asserção.

## Onde cada coisa é testada

| O que | Como | Referência |
|---|---|---|
| Regra de negócio do domínio | JUnit puro, pela técnica adequada | [técnicas](references/tecnicas.md) |
| Ciclo de vida (tabela de estados) | Matriz completa: toda transição, toda ação inválida em todo estado | [técnicas](references/tecnicas.md#máquina-de-estados-de-negócio) |
| Value Object (valor, documento, período) | Exemplos + propriedade para "vale para qualquer valor" | [propriedades](references/propriedades.md) |
| Tabela de fluxo por tipo | Exemplos + caminhos gerados | [propriedades](references/propriedades.md) |
| Caso de uso (orquestração) | `@QuarkusTest` + `@InjectMock` nas portas (repositório, relógio, gateway), sem banco | [use case](references/use-case.md) |
| Mapper e Assembler | Ida e volta preserva todos os campos | [use case](references/use-case.md#mapper-e-assembler) |
| Mutante sobrevivente | Teste que afirma a regra naquele ponto | [mutação](references/mutacao.md) |

## Regras

1. **Domínio sem Quarkus.** Teste de domínio é JUnit puro: nada de `@QuarkusTest`, `@Inject` ou banco. **Caso de uso** usa `@QuarkusTest` com `@InjectMock` nas portas: o Quarkus monta o bean com a configuração real (ex.: a alçada), e o banco fica de fora. Se a regra só aparece com o banco ou o HTTP, ela é da `teste-componente`.
2. **Um resultado esperado por caso.** Recusa confere também que **nada mudou** (estado, histórico, efeitos).
3. **O esperado vem da entrevista**, não do código. O código pode estar errado; abrir a implementação para decidir o esperado confirma o erro. Se o código diverge do que o dev confirmou, é defeito: deixe o teste vermelho e relate.
4. **Fronteira é caso.** Exatamente o máximo, um centavo acima, vazio, um a mais. Mutante de fronteira sobrevive quando ela falta.
5. **Mensagem é contrato** quando o usuário a vê: confira o texto.
6. **Teste de propriedade não substitui exemplo.** O exemplo documenta; a propriedade procura o caso que ninguém pensou.

## Antes de terminar

1. `./mvnw test` verde.
2. Domínio ou caso de uso alterado: `./mvnw verify -Pauditoria` sem mutante novo sobrevivente na classe mexida.
3. Relate ao dev o que ficou para os outros níveis.
