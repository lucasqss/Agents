---
name: teste-componente
description: Use ao criar, alterar ou revisar testes do serviço montado com @QuarkusTest (HTTP, banco real via Dev Services e stubs dos sistemas externos), para provar o que só aparece com tudo ligado - persistência, validação de formato (400), identidade (401/403 por endpoint, quando há), tradução de erro para status (404/409/503), concorrência, resiliência de integração e contrato REST. Não use para regra de negócio (é da teste-unidade) nem para cenários de negócio em tabela (é da teste-aceitacao).
---

# Teste de componente

O serviço inteiro, sozinho: `@QuarkusTest` com HTTP de verdade, o banco real (Dev Services) e os sistemas externos simulados. **Prova o que só aparece com o serviço montado.** Regra de negócio já provada na unidade não se repete aqui: o componente prova que a regra está **ligada** (um caso basta), não que ela está **certa** (isso é da `teste-unidade`).

Os exemplos usam `pedido`, `conta` e `antifraude` como ilustração: não procure essas classes no repositório.

## Passo a passo

1. **Entreviste o dev** seguindo o [roteiro](references/entrevista.md): o que mudou no serviço (endpoint, tabela, integração, papel), o que já está coberto e o que preocupa.
2. **Monte o plano de cobertura** no formato abaixo.
3. **Pare e aguarde a confirmação do dev.**
4. Escreva os testes em `src/test/java/.../componente/` (um arquivo por assunto: `ValidacaoEntradaTest`, `ConcorrenciaPedidoTest`, `IdentidadePedidoTest`), e o `*IT` de corpo vazio (`@QuarkusIntegrationTest`, herda o `*Test`) para os que provam o jar empacotado.
5. Rode `./mvnw verify`, e `./mvnw verify -Pversao` se o projeto deixa os `*IT` para a geração de versão (no `exemplo-testes`, o `*IT` roda no próprio `verify`).
6. Relate o que foi coberto, o que ficou para outro nível e as dúvidas.

## Formato do plano

1. **O que mudou** no serviço: endpoints, tabelas, integrações, papéis.
2. **Casos por assunto** (tabela abaixo), com entrada e **um** resultado esperado.
3. **Fica para outro nível:** a regra de negócio (unidade) e o cenário de negócio de ponta a ponta (aceitação).
4. **Dúvidas** com a pergunta exata.

## O que este nível cobre

| Assunto | O que provar | Referência |
|---|---|---|
| Persistência | Criar pelo endpoint e ler de volta por outro devolve todos os campos; a migration nova sobe | [mecânica](references/mecanica-quarkus.md) |
| Formato da entrada | Campo obrigatório ausente, tamanho, faixa: `400` com os campos, nunca `500`; o limite da coluna não chega ao banco | [mecânica](references/mecanica-quarkus.md) |
| Identidade | Com token: matriz papel × endpoint (sem token `401`, papel errado `403`, papel certo passa), no formato de erro padrão. Com o autor por cabeçalho (ex.: `X-Usuario` no `exemplo-testes`): cabeçalho ausente dá `400` | [identidade](references/identidade.md) |
| Tradução de erro | Inexistente `404`, conflito ou transição inválida `409`, sistema externo fora `503`: um caso por tradução | [mecânica](references/mecanica-quarkus.md) |
| Concorrência | Duas operações ao mesmo tempo: o efeito acontece uma vez; versão desatualizada dá `409` | [mecânica](references/mecanica-quarkus.md#concorrência) |
| Integração | Timeout, erro e circuito aberto do sistema externo viram a resposta prevista; a chamada leva a chave de idempotência | [stubs](references/stubs.md) |
| Contrato REST | `ContratoRestTest` (campo de resposta não some nem muda de tipo) e o openapi-diff do build | [contrato](references/contrato.md) |
| Canal | Nomes exatos dos campos e textos formatados do `*Response` | [mecânica](references/mecanica-quarkus.md#canal) |

## Regras

1. **Um caso por ligação, não por regra.** A tabela de estados inteira é da unidade; aqui, um caso de transição aceita e um de recusa provam que o endpoint chama o domínio e traduz o erro.
2. **Dados próprios e únicos** em cada caso (`Api.unico("CPF")`, `Api.abrirOk(...)`), criados pelos endpoints. O banco é compartilhado entre os testes: nunca conte registros de forma absoluta.
3. **Confira o efeito por outro endpoint**, ou pelo repositório injetado quando não houver endpoint de leitura. Status certo não prova que o efeito aconteceu.
4. **Sistema externo sempre simulado** ([stubs](references/stubs.md)); nunca o real.
5. **O esperado vem da entrevista e do contrato**, não do código. Divergência é defeito a relatar, não teste ajustado.
6. `@Inject` é permitido (é o mesmo processo), mas prefira HTTP: o teste fica perto do que o cliente vê.

## Antes de terminar

1. `./mvnw verify` verde, com o `ContratoRestTest` e o openapi-diff.
2. Endpoint novo: tem caso de sucesso, de `400` e de `404` no `*Test` que o `*IT` herda; e, com identidade, a linha dele na matriz.
3. Relate ao dev o que ficou para os outros níveis.
