# Receita: novo endpoint de canal

Entradas necessárias: o canal (web, mobile, taa ou um canal novo), o agregado principal e a tela, campo a campo, com o formato esperado. Se faltar, pergunte. Modelos de Assembler, `Padrao<Canal>` e Resource estão em [exemplos de código](../exemplos-codigo.md).

1. Garanta que existe a consulta (record em `persistence/<agregado>/` + caso de uso). Se não existir, crie antes com a receita [nova consulta](nova-consulta.md).
2. Em `entrypoint/<canal>/<agregado>/`:
   - `XCanalResponse` (record): campos com nomes estáveis; para cada valor exibido, o bruto e o formatado.
   - `XCanalAssembler` (final, estático, sem CDI): monta o Response usando `Padrao<Canal>.INSTANCIA`.
   - `XCanalResource` em `/<canal>/v1/...` com `@Tag(name = "Canal <canal>")`.
3. Formato que ainda não existe: acrescente o método em `entrypoint/apresentacao/PadraoApresentacao.java` e implemente em **todos** os canais; acrescente a linha em `src/test/java/<pacote raiz>/entrypoint/apresentacao/PadroesDeCanalTest.java`. Crie esses arquivos se ainda não existirem.
4. Canal novo: crie `entrypoint/<canal>/Padrao<Canal>.java`.
5. Teste de contrato em `src/test/java/<pacote raiz>/integracao/CanaisContratoTest.java` (crie se ainda não existir): conjunto exato de campos e textos formatados.
6. Rode `./mvnw verify`.
