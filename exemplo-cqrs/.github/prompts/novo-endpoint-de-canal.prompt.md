---
mode: agent
description: Expõe uma consulta ou operação num canal (web, mobile, TAA...) com Response próprio, Assembler e padrão de apresentação do canal.
---
Crie o endpoint de canal descrito abaixo seguindo `.github/copilot-instructions.md`, `.github/instructions/canais.instructions.md` e os exemplos em `canal-web`, `canal-mobile` e `canal-taa`.

Canal: ${input:canal:web, mobile, taa ou um canal novo (para canal novo, use antes o prompt novo-canal)}
Tela: ${input:tela:O que a tela mostra, campo a campo, com o formato esperado}

1. Garanta que existe a consulta no `nucleo` (record em `query/persistence` + use case). Se não existir, crie antes (prompt `nova-consulta`).
2. No módulo `canal-<canal>`, em `canal/<canal>/entrypoint/`:
   - `XCanalResponse` (record): campos com nomes estáveis; para cada valor exibido, o bruto e o formatado.
   - `XCanalAssembler` (final, estático, sem CDI): monta o Response usando `Padrao<Canal>.INSTANCIA`.
   - `XCanalResource` em `/<canal>/v1/...` com `@Tag(name = "Canal <canal>")`. Se escrever, passe `SolicitanteDaRequisicao.atual()` ao use case; canal `@SomenteLeitura` não escreve.
3. Formato que ainda não existe: acrescente o método em `PadraoApresentacao` (`canal-comum`) e implemente em **todos** os canais, com linha no `Padrao<Canal>Test` de cada um.
4. Teste de contrato em `Contrato<Canal>Test`: conjunto exato de campos, textos formatados, 401 sem o token do canal.
5. Rode `./mvnw verify` na raiz.
