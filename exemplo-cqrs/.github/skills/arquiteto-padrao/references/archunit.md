# Regras de arquitetura (ArchUnit)

A pasta `src/test/java/**/arquitetura/` é a fitness function da arquitetura e tem dono no CODEOWNERS.

- **Não altere, apague, desligue nem afrouxe regras para fazer o build passar.** Quando uma regra falha, o código está no lugar errado: corrija o código. A falha traz um ID (ex.: `DOM-002`): procure-o nessa pasta e na tabela de regras do README e siga o "Corrija:" da mensagem.
- Não adicione `allowEmptyShould`, exceções, `FreezingArchRule` nem `@ArchIgnore` sem pedido explícito do dono. A única exceção é o baseline da skill `adequar-ao-padrao`.
- Os padrões são sempre montados com `Raiz.pacote(...)`, `Raiz.exato(...)` e `Raiz.emQualquer(...)`, nunca com o nome do projeto escrito à mão: é isso que deixa a pasta pronta para copiar entre projetos.
- Mudança de regra é decisão de arquitetura: descreva o motivo no PR e espere a revisão do CODEOWNER.

## Criando ou mudando uma regra (só com pedido do dono)

- **Uma regra, um motivo.** Se a frase tem "nem", "ou" ou uma lista de assuntos diferentes, quebre em várias regras.
- **ID estável:** prefixo do grupo (GER, DOM, CMD, QRY, SHR, CAN, TEC, NOM) + o próximo número livre do arquivo. Um ID nunca é reaproveitado.
- **Formato:** `.as("DOM-008: TÍTULO EM CAIXA ALTA").because("por quê. Corrija: o que fazer")`. O `.as` vem antes do `.because`, e o `allowEmptyShould`, se houver, vem antes do `.as`.
- **Sem `import static`:** chame `Raiz.pacote(...)`, `Condicoes.usamGateway()`, `ArchRuleDefinition.noClasses()`, `SlicesRuleDefinition.slices()`. As classes dessa pasta estão no mesmo pacote e não precisam de import.
- **Nome do campo** começa pelo ID: `dom008Assunto`.
- Atualize a tabela de regras do README. `CatalogoDeRegrasTest` falha se o formato, o ID ou o prefixo estiverem errados.
