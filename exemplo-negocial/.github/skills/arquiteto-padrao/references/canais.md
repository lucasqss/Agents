# Entrypoints, canais e apresentação

Os fronts são gerados por low-code e não formatam nada: o backend entrega pronto para exibir.

**Todo entrypoint segue `entrypoint/<via>/<agregado>/`**, nesta ordem. A via é `rest`, um canal (`web`, `mobile`, `taa`), `mensageria` ou `batch`.

**Cada endpoint pertence a um agregado.** Tela que mostra dados de vários agregados fica no agregado principal dela, e a junção vem da consulta. Exemplo: `CotaDetalhe` junta cota, cliente e processo e é servido em `entrypoint/web/cota/`.

## API neutra (`entrypoint/rest/<agregado>/`)

Recebe HTTP, chama o caso de uso e devolve o resultado. Sem regra de negócio, sem acesso a repositório, sem formatação.

**Entrada:** um `XRequest` (record) por operação, com Bean Validation para o **formato** (`@NotBlank`, `@NotNull`, `@Size(max = <tamanho da coluna>)`, `@Positive`), recebido como `@Valid @NotNull XRequest` e convertido para o comando do caso de uso (`paraDados()`). Valor monetário leva `@Positive` (se a regra pede) e `@Digits(integer = <colunas>, fraction = 2)`. Formato inválido responde 400 antes de qualquer busca. Não anote os records de `application/` nem o domínio: a regra de negócio continua no domínio.

**Ação com dado obrigatório:** o dado vem em um record de request próprio (`@NotBlank`, `@Size(max = <tamanho da coluna>)`), com o corpo declarado obrigatório (`@RequestBody(required = true) @Valid @NotNull`), para o OpenAPI mostrar o corpo como obrigatório. Corpo ausente, vazio ou grande demais responde 400 com o nome do campo, antes de buscar o agregado (400 vence 404 e 409). Ações sem dado seguem aceitando requisição sem corpo (`required = false`); os dois estilos convivem no mesmo resource. Quem decide se a ação exige o dado é o domínio ([domínio](dominio.md)); o entrypoint só confere o formato.

**Erros:** saem como `ErroResponse`, com a lista `campos`. Exceção vira HTTP em `ExceptionMappers`, na raiz de `entrypoint/`:

| Exceção | HTTP |
|---|---|
| `DominioException` | 400 |
| `TransicaoInvalidaException`, `ConflitoException` | 409 |
| Subclasse de `DominioException` para conflito com o estado atual (`CreditoInsuficienteException`) | 409, mapeada à parte; o mapper de `DominioException` não a captura |
| `NaoEncontradoException` | 404 |
| `SistemaExternoIndisponivelException` | 503 |
| `ConstraintViolationException` (Bean Validation) | 400, com a lista `campos` |
| `MismatchedInputException` (JSON com tipo ou valor incompatível) | 400 |
| `WebApplicationException` (recusa do próprio HTTP, como rota inexistente) | mantém o status |

## Contrato do catálogo (quando o componente provê para outro sistema)

Se o contrato da API vem do catálogo corporativo (interface ou DTOs gerados numa dependência Maven), ele fica em `entrypoint/rest/<agregado>/` e não passa dali (TEC-011):

- o `XResource` implementa ou usa o contrato gerado;
- um `XAssembler` (final, estático, sem CDI) converte o DTO do catálogo no `DadosX` do caso de uso e o resultado de volta no DTO do catálogo;
- o caso de uso recebe e devolve só tipos nossos: `DadosX`, tipos simples, records de consulta.

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
- O Resource de canal nunca devolve record de `persistence/`: ele devolve o próprio `*Response`, e o Assembler recebe o record. Na API neutra (`entrypoint/rest/<agregado>/`), o Resource pode devolver o record. Um canal nunca usa outro canal nem o `Padrao` de outro canal.
- **Contrato congelado:** só mudanças aditivas no `*Response`. Renomear ou remover campo quebra o front low-code; quebra vira `/<canal>/v2`.

## Apresentação comum (`entrypoint/apresentacao/`)

Se a pasta ainda não existir, crie-a na primeira tela de canal ([exemplo](exemplos-codigo.md)).

- `Formatador` (funções básicas, locale pt-BR explícito, sem o espaço U+00A0) e `Rotulos` (catálogo por extenso) são comuns e não conhecem canais.
- Cada canal tem `entrypoint/<canal>/Padrao<Canal> implements PadraoApresentacao`. Formato novo entra na interface, e todos os canais implementam.
- Todo formato novo ganha linha em `src/test/java/<pacote raiz>/entrypoint/apresentacao/PadroesDeCanalTest.java`; todo endpoint de canal, teste de contrato em `src/test/java/<pacote raiz>/integracao/CanaisContratoTest.java` com os nomes exatos dos campos. Crie esses arquivos se ainda não existirem.
