---
applyTo: "**/entrypoint/**,**/shared/apresentacao/**"
---
# Entrypoints, canais e apresentação

Os fronts são gerados por low-code e não formatam nada: o backend entrega pronto para exibir.

Entrypoint de escrita (`command/entrypoint`) e de leitura neutro (`query/entrypoint`): recebem HTTP, chamam o use case, devolvem o resultado.

Entrada: um `XRequest` (record) por operação, com Bean Validation para o **formato** (`@NotBlank`, `@NotNull`, `@Size(max = <tamanho da coluna>)`, `@Positive`), recebido como `@Valid @NotNull XRequest` e convertido para o comando do use case (`paraDados()`). Não anote os records do `application` nem o domínio: a regra de negócio continua no domínio. O erro sai como `ErroResponse` com a lista `campos`. Exceção vira HTTP em `ExceptionMappers`. Sem regra de negócio, sem acesso a repositório.

Canais (`query/entrypoint/<canal>/`, ex.: `web`, `mobile`, `taa`):
- Um `XCanalResource` (`@Path("/<canal>/v1/...")`, `@Tag(name = "Canal <canal>")`), um `XCanalResponse` (record: **o contrato do front**) e um `XCanalAssembler` (classe final, métodos estáticos, sem CDI).
- O Assembler decide **o que** o canal recebe e usa `Padrao<Canal>.INSTANCIA` para decidir **como** exibir.
- Mande o valor bruto **e** o formatado (`valorCredito` + `valorCreditoFormatado`, `estado` + `situacao`).
- Somar ou formatar o que já veio da consulta é papel do Assembler. Número com significado de negócio nasce numa consulta em `query/persistence`.
- O Resource nunca devolve record de `persistence`. Um canal nunca usa outro canal nem o `Padrao` de outro canal.
- Contrato congelado: só mudanças aditivas no `*Response`. Quebra vira `/<canal>/v2`.

Apresentação (`shared/apresentacao`):
- `Formatador` (funções básicas, locale pt-BR explícito, sem o espaço U+00A0) e `Rotulos` (catálogo por extenso) são comuns e não conhecem canais.
- Cada canal tem `<canal>/Padrao<Canal> implements PadraoApresentacao`. Formato novo entra na interface, e todos os canais implementam.
- Todo formato novo ganha linha em `PadroesDeCanalTest`; todo endpoint de canal, teste de contrato em `CanaisContratoTest` com os nomes exatos dos campos.
