# Entrypoints, canais e apresentação

Os fronts são gerados por low-code e não formatam nada: o backend entrega pronto para exibir.

**Todo entrypoint segue `entrypoint/<via>/<agregado>/`**, nesta ordem. A via é `rest`, um canal (`web`, `mobile`, `taa`), `mensageria` ou `batch`.

**Cada endpoint pertence a um agregado.** Tela que mostra dados de vários agregados fica no agregado principal dela, e a junção vem da consulta. Exemplo: `CotaDetalhe` junta cota, cliente e processo e é servido em `entrypoint/web/cota/`.

## API neutra (`entrypoint/rest/<agregado>/`)

Recebe HTTP, chama o caso de uso e devolve o resultado. Sem regra de negócio, sem acesso a repositório, sem formatação.

**Entrada:** um `XRequest` (record) por operação, com Bean Validation para o **formato** (`@NotBlank`, `@NotNull`, `@Size(max = <tamanho da coluna>)`, `@Positive`), recebido como `@Valid @NotNull XRequest` e convertido para o comando do caso de uso (`paraDados()`). Não anote os records de `application/` nem o domínio: a regra de negócio continua no domínio.

**Erros:** saem como `ErroResponse`, com a lista `campos`. Exceção vira HTTP em `ExceptionMappers`, na raiz de `entrypoint/`:

| Exceção | HTTP |
|---|---|
| `DominioException` | 400 |
| `TransicaoInvalidaException`, `ConflitoException` | 409 |
| `NaoEncontradoException` | 404 |
| `SistemaExternoIndisponivelException` | 503 |

## Canais (`entrypoint/<canal>/<agregado>/`)

Na pasta do agregado ficam:

- `XCanalResource` (`@Path("/<canal>/v1/...")`, `@Tag(name = "Canal <canal>")`);
- `XCanalResponse` (record: **o contrato do front**);
- `XCanalAssembler` (classe final, métodos estáticos, sem CDI).

Na raiz do canal fica só `entrypoint/<canal>/Padrao<Canal>.java`.

Regras:

- O Assembler decide **o que** o canal recebe e usa `Padrao<Canal>.INSTANCIA` para decidir **como** exibir.
- Mande o valor bruto **e** o formatado (`valorCredito` + `valorCreditoFormatado`, `estado` + `situacao`).
- Somar ou formatar o que já veio da consulta é papel do Assembler. Número com significado de negócio nasce numa consulta em `persistence/<agregado>/`.
- O Resource nunca devolve record de `persistence/`. Um canal nunca usa outro canal nem o `Padrao` de outro canal.
- **Contrato congelado:** só mudanças aditivas no `*Response`. Renomear ou remover campo quebra o front low-code; quebra vira `/<canal>/v2`.

## Apresentação comum (`entrypoint/apresentacao/`)

- `Formatador` (funções básicas, locale pt-BR explícito, sem o espaço U+00A0) e `Rotulos` (catálogo por extenso) são comuns e não conhecem canais.
- Cada canal tem `entrypoint/<canal>/Padrao<Canal> implements PadraoApresentacao`. Formato novo entra na interface, e todos os canais implementam.
- Todo formato novo ganha linha em `PadroesDeCanalTest`; todo endpoint de canal, teste de contrato em `CanaisContratoTest` com os nomes exatos dos campos.
