---
applyTo: "canal-*/**"
---
# Módulos de canal (BFFs)

Cada canal (web, mobile, TAA...) é um **módulo Maven e uma aplicação própria**: sobe sozinho, tem rota própria no API gateway e leva só as **dependências corporativas daquele canal** (segurança, log, utilitários). Os fronts são gerados por low-code e não formatam nada: o canal entrega pronto para exibir.

Dependências:
- Um canal depende só de `canal-comum` (que traz o `nucleo`). **Nunca** de outro canal.
- Dependência corporativa do canal entra só no `pom.xml` deste módulo. Nunca no `nucleo` nem no `canal-comum`.

Pacotes (`canal/<canal>/`):
- `entrypoint/`: um `XCanalResource` (`@Path("/<canal>/v1/...")`, `@Tag(name = "Canal <canal>")`), um `XCanalResponse` (record: **o contrato do front**), um `XCanalAssembler` (classe final, métodos estáticos, sem CDI) e um `XRequest` por operação de escrita.
- `apresentacao/`: `Padrao<Canal> implements PadraoApresentacao`, com `INSTANCIA`.
- `seguranca/`: `MapeadorDeToken<Canal> implements MapeadorDeToken`: lê o token do canal (com a biblioteca corporativa) e devolve o `Solicitante`. O `FiltroDeToken` do `canal-comum` faz o resto (401 sem token).

Entrada:
- `XRequest` (record) com Bean Validation para o **formato** (`@NotBlank`, `@NotNull`, `@Size(max = <tamanho da coluna>)`, `@Positive`), recebido como `@Valid @NotNull XRequest`.
- Regra de negócio, inclusive a que muda por canal, fica no domínio (`CanalDeAtendimento`). O Resource passa `SolicitanteDaRequisicao.atual()` ao use case e não tem `if (canal)`.
- Erro sai como `ErroResponse`; exceção vira HTTP em `canal.comum.erro.ExceptionMappers`.

Saída:
- O Assembler decide **o que** o canal recebe e usa `Padrao<Canal>.INSTANCIA` para decidir **como** exibir.
- Mande o valor bruto **e** o formatado (`valorCredito` + `valorCreditoFormatado`, `estado` + `situacao`).
- Somar ou formatar o que já veio da consulta é papel do Assembler. Número com significado de negócio nasce numa consulta em `query/persistence`.
- O Resource nunca devolve record de `persistence` nem acessa repositório.
- Contrato congelado: só mudanças aditivas no `*Response`. Quebra vira `/<canal>/v2`.

Escrita bloqueada: o `package-info.java` de `canal/<canal>` com `@SomenteLeitura` faz o build falhar se o canal usar `command` (regra CAN-010).

Apresentação comum (`canal-comum`, `canal/comum/apresentacao`): `Formatador` (locale pt-BR explícito, sem o espaço U+00A0) e `Rotulos` (catálogo por extenso) não conhecem canais. Formato novo entra em `PadraoApresentacao`, e todos os canais implementam.

Testes: formato novo ganha linha no `Padrao<Canal>Test`; todo endpoint ganha teste de contrato (`Contrato<Canal>Test`) com os nomes exatos dos campos, com e sem token.
