# Receita: novo endpoint de canal

Entradas necessárias: o canal (web, mobile, taa ou um canal novo) e a tela, campo a campo, com o formato esperado. Se faltar, pergunte. Use como exemplo os canais `web`, `mobile` e `taa` existentes.

1. Garanta que existe a consulta (record em `query/persistence` + use case). Se não existir, crie antes com a receita [nova consulta](nova-consulta.md).
2. Em `query/entrypoint/<canal>/`:
   - `XCanalResponse` (record): campos com nomes estáveis; para cada valor exibido, o bruto e o formatado.
   - `XCanalAssembler` (final, estático, sem CDI): monta o Response usando `Padrao<Canal>.INSTANCIA`.
   - `XCanalResource` em `/<canal>/v1/...` com `@Tag(name = "Canal <canal>")`.
3. Formato que ainda não existe: acrescente o método em `PadraoApresentacao` e implemente em **todos** os canais; acrescente a linha em `PadroesDeCanalTest`.
4. Canal novo: crie `<canal>/Padrao<Canal>` no módulo `apresentacao`.
5. Teste de contrato em `CanaisContratoTest`: conjunto exato de campos e textos formatados.
6. Rode `./mvnw verify`.
