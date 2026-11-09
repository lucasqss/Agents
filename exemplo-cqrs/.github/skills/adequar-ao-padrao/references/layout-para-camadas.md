# Layout antigo → camadas e agregados

Camada `estrutura`. Só mover e renomear pacotes: **nenhuma lógica muda aqui.** O destino está na seção "Onde cada coisa mora" da skill `arquiteto-padrao`.

## De onde para onde

Para cada classe, responda: que tipo de coisa é, e de qual agregado.

| Hoje | Destino |
|---|---|
| `controller/`, `resource/`, `rest/`, `command/entrypoint`, `query/entrypoint` | `entrypoint/rest/<agregado>/` |
| Resource específico de canal | `entrypoint/<canal>/<agregado>/` |
| Resource com endpoints de vários agregados | um Resource por agregado, com as mesmas URLs |
| `service/`, `command/application`, `query/application` | `application/<agregado>/`. A quebra em casos de uso vem na camada `application` |
| `model/`, `entity/`, com regra | `domain/<agregado>/` nesta camada. A separação de JPA vem na camada `dominio` |
| `command/domain` | `domain/<agregado>/` |
| `repository/`, `command/persistence`, `query/persistence`, `shared/persistence` | `persistence/<agregado>/` |
| `client/`, `integration/`, `shared/gateway/<sistema>` | `gateway/<sistema>/` |
| `shared/apresentacao` | `entrypoint/apresentacao/`, e cada `Padrao<Canal>` em `entrypoint/<canal>/` |
| `util/`, `common/`, `helper/` | caso a caso: o agregado que usa. Se vários usam e não é um tipo listado para a raiz, pare e pergunte ao dono |

Os tipos aceitos na raiz de cada camada estão na skill `arquiteto-padrao`.

## Passos

1. Mova um agregado de cada vez, usando a refatoração da IDE ou atualizando todos os imports e strings com nome de pacote (HQL com `select new <pacote>.X`, `application.properties`, `META-INF`).
2. **Visibilidade:** `XDataRepository` e `XMapper` são package-private e continuam junto do adapter, em `persistence/<agregado>/`. Classe package-private usada de outra pasta precisa virar `public`, ou é sinal de que está no lugar errado: confira antes.
3. Atualize os `package-info.java`.
4. Mova os testes para o pacote equivalente.
5. Rode `./mvnw verify` e faça o commit da camada.
