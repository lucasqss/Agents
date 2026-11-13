# Regras de arquitetura (ArchUnit)

A pasta `src/test/java/**/arquitetura/` é a fitness function da arquitetura. Mudar essa pasta é decisão do dono da arquitetura.

## Instalar as regras em um repositório

As regras viajam com esta skill, em `assets/arquitetura/`, em quatro pastas:

| Pasta | O que tem | Quem copia |
|---|---|---|
| `comum/` | `Raiz`, `ClassesExternas`, `Condicoes`, `FormatoDaFalha`, `CatalogoDeRegrasTest` e as regras GER, DOM, CMD, QRY, SHR, CAN, TEC, NOM | todos |
| `negocial/` | `Tipo`, `ArquiteturaTest` | componente Negocial |
| `acl/` | `Tipo`, `ArquiteturaTest`, `RegrasAcl` | componente ACL |
| `bff/` | `Tipo`, `ArquiteturaTest`, `RegrasBff` | componente BFF |

Todos os arquivos vão para a **mesma** pasta de destino. Regra de uma pasta que o componente não tem (um ACL não tem `domain`) passa vazia. O `Tipo.java` diz quais pastas o componente pode ter (GER-001).

O **pacote raiz** é o pai de `entrypoint` e `application` (ex.: `org.acme.consorcio`).

Se `src/test/java/<pacote raiz>/arquitetura/` já existe, não há nada a instalar. Se não existe:

1. Copie os `.java` de `assets/arquitetura/comum/` **e** os da pasta do tipo do componente para `src/test/java/<pacote raiz>/arquitetura/`.
2. Em cada arquivo copiado, troque `PACOTE_RAIZ` pelo pacote raiz.
3. Em `ClassesExternas.java`, ajuste a lista de `CONFIGURACAO`: um `groupId` Maven por sistema externo cujas dependências o componente usa, no lugar do exemplo `org.acme.sistema`. O `GROUP_ID_DO_PROJETO` (lido do `pom.xml`) já cobre as dependências do catálogo corporativo que têm o mesmo `groupId` do componente. Se um jar desses `groupId`s não for de sistema externo, liste o pacote em `PACOTES_QUE_NAO_SAO_EXTERNOS`. É o único arquivo da pasta que se edita por projeto.
4. Confira as dependências em [pom obrigatório](pom-obrigatorio.md).
5. **Código já no padrão ou projeto novo:** rode `./mvnw verify`.
6. **Código legado:** não rode o `verify` agora. Continue no baseline da skill `adequar-ao-padrao`, em `references/metodo-archignore.md`.

No Linux, WSL ou Git Bash, os passos 1 e 2 ficam assim:

```bash
ORIGEM=.github/skills/arquiteto-padrao/assets/arquitetura
TIPO=negocial   # ou acl, ou bff
DESTINO=src/test/java/org/acme/consorcio/arquitetura
mkdir -p "$DESTINO" && cp "$ORIGEM"/comum/*.java "$ORIGEM"/$TIPO/*.java "$DESTINO"/
sed -i 's/PACOTE_RAIZ/org.acme.consorcio/' "$DESTINO"/*.java
```

## Regras em vigor

- **Não altere, apague, desligue nem afrouxe regras para fazer o build passar.** Quando uma regra falha, o código está no lugar errado: corrija o código. A falha traz um ID (ex.: `DOM-002`): procure-o nessa pasta e siga o "Corrija:" da mensagem.
- A lista de `groupId`s em `ClassesExternas` é configuração do projeto, mantida pelo dono: acrescentar um sistema externo é esperado; tirar um `groupId` para o build passar é afrouxar regra.
- `Tipo.java` declara o tipo do componente. Trocar o tipo ou acrescentar uma pasta em `CAMADAS` é decisão do dono.
- Regra que cita uma anotação de biblioteca opcional (JPA, Jakarta Data, transação, REST client) usa o nome dela em `Condicoes` (`Condicoes.ENTITY`), nunca o `.class`: um ACL ou BFF não tem essas dependências e a pasta precisa compilar.
- Não adicione `allowEmptyShould`, exceções, `FreezingArchRule` nem `@ArchIgnore` sem pedido explícito do dono. A única exceção é o baseline da skill `adequar-ao-padrao`.
- Os padrões são sempre montados com `Raiz.pacote(...)`, `Raiz.exato(...)` e `Raiz.emQualquer(...)`, nunca com o nome do projeto escrito à mão: é isso que deixa a pasta pronta para copiar entre projetos.
- Mudança de regra é decisão de arquitetura: descreva o motivo no PR e espere a revisão do dono da arquitetura.

## Criando ou mudando uma regra (só com pedido do dono)

- **Uma regra, um motivo.** Se a frase tem "nem", "ou" ou uma lista de assuntos diferentes, quebre em várias regras.
- **ID estável:** prefixo do grupo (GER, DOM, CMD, QRY, SHR, CAN, TEC, NOM, ACL, BFF) + o próximo número livre do arquivo. Um ID nunca é reaproveitado.
- **Formato:** `.as("DOM-008: TÍTULO EM CAIXA ALTA").because("por quê. Corrija: o que fazer")`. O `.as` vem antes do `.because`, e o `allowEmptyShould`, se houver, vem antes do `.as`.
- **Sem `import static`:** chame `Raiz.pacote(...)`, `Condicoes.usamGateway()`, `ArchRuleDefinition.noClasses()`, `SlicesRuleDefinition.slices()`. As classes dessa pasta estão no mesmo pacote e não precisam de import.
- **Nome do campo** começa pelo ID: `dom008Assunto`.
- `CatalogoDeRegrasTest`, instalado junto com as regras, falha se o formato, o ID ou o prefixo estiverem errados.
