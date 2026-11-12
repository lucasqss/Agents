# Regras de arquitetura (ArchUnit)

A pasta `src/test/java/**/arquitetura/` é a fitness function da arquitetura. Mudar essa pasta é decisão do dono da arquitetura.

## Instalar as regras em um repositório

As regras viajam com esta skill, em `assets/arquitetura/`. O **pacote raiz** é o pai de `entrypoint`, `application` e `domain` (ex.: `org.acme.consorcio`).

Se `src/test/java/<pacote raiz>/arquitetura/` já existe, não há nada a instalar. Se não existe:

1. Copie todos os `.java` de `assets/arquitetura/` para `src/test/java/<pacote raiz>/arquitetura/`.
2. Em cada arquivo copiado, troque `PACOTE_RAIZ` pelo pacote raiz.
3. Em `Catalogo.java`, troque o exemplo `org.acme.sistema..` de `PACOTES` pelos pacotes das dependências do catálogo corporativo que o componente usa, um por sistema. Se não usa nenhuma, deixe como está. Jars com o mesmo `groupId` do componente já são detectados sozinhos; se um deles não for do catálogo, liste o pacote em `FORA_DO_CATALOGO`. É o único arquivo da pasta que muda de projeto para projeto.
4. Confira as dependências em [pom obrigatório](pom-obrigatorio.md).
5. **Código já no padrão ou projeto novo:** rode `./mvnw verify`.
6. **Código legado:** não rode o `verify` agora. Continue no baseline da skill `adequar-ao-padrao`, em `references/metodo-archignore.md`.

No Linux, WSL ou Git Bash, os passos 1 e 2 ficam assim:

```bash
ORIGEM=.github/skills/arquiteto-padrao/assets/arquitetura
DESTINO=src/test/java/org/acme/consorcio/arquitetura
mkdir -p "$DESTINO" && cp "$ORIGEM"/*.java "$DESTINO"/
sed -i 's/PACOTE_RAIZ/org.acme.consorcio/' "$DESTINO"/*.java
```

## Regras em vigor

- **Não altere, apague, desligue nem afrouxe regras para fazer o build passar.** Quando uma regra falha, o código está no lugar errado: corrija o código. A falha traz um ID (ex.: `DOM-002`): procure-o nessa pasta e siga o "Corrija:" da mensagem.
- `Catalogo.PACOTES` e `Catalogo.FORA_DO_CATALOGO` são configuração do projeto, mantida pelo dono: acrescentar um sistema do catálogo é esperado; tirar um pacote para o build passar é afrouxar regra.
- Não adicione `allowEmptyShould`, exceções, `FreezingArchRule` nem `@ArchIgnore` sem pedido explícito do dono. A única exceção é o baseline da skill `adequar-ao-padrao`.
- Os padrões são sempre montados com `Raiz.pacote(...)`, `Raiz.exato(...)` e `Raiz.emQualquer(...)`, nunca com o nome do projeto escrito à mão: é isso que deixa a pasta pronta para copiar entre projetos.
- Mudança de regra é decisão de arquitetura: descreva o motivo no PR e espere a revisão do dono da arquitetura.

## Criando ou mudando uma regra (só com pedido do dono)

- **Uma regra, um motivo.** Se a frase tem "nem", "ou" ou uma lista de assuntos diferentes, quebre em várias regras.
- **ID estável:** prefixo do grupo (GER, DOM, CMD, QRY, SHR, CAN, TEC, NOM) + o próximo número livre do arquivo. Um ID nunca é reaproveitado.
- **Formato:** `.as("DOM-008: TÍTULO EM CAIXA ALTA").because("por quê. Corrija: o que fazer")`. O `.as` vem antes do `.because`, e o `allowEmptyShould`, se houver, vem antes do `.as`.
- **Sem `import static`:** chame `Raiz.pacote(...)`, `Condicoes.usamGateway()`, `ArchRuleDefinition.noClasses()`, `SlicesRuleDefinition.slices()`. As classes dessa pasta estão no mesmo pacote e não precisam de import.
- **Nome do campo** começa pelo ID: `dom008Assunto`.
- `CatalogoDeRegrasTest`, instalado junto com as regras, falha se o formato, o ID ou o prefixo estiverem errados.
