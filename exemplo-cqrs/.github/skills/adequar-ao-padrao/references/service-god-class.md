# Service god class

Um service que concentra regras, orquestração, consultas e chamadas externas vira vários casos de uso, um por classe, e a regra desce para o domínio.

Fonte dos movimentos: Fowler, *Refactoring* (Extract Class, Move Function); Feathers, *Working Effectively with Legacy Code* (testes de caracterização).

## Passo 0: mostre a classificação antes de alterar

Liste cada método e cada trecho com papel próprio:

| Método/trecho | É o quê | Vai para |
|---|---|---|
| validação ou decisão de negócio | regra | entidade de `domain` |
| sequência buscar → decidir → salvar | orquestração | `*UseCase` em `application` |
| busca que alimenta tela | leitura | `query/persistence` + `query/application` |
| consulta para decidir uma escrita | leitura de decisão | interface de repositório do `domain` |
| chamada a sistema de fora | integração | `*Gateway` |
| moeda, máscara, rótulo | formatação | Assembler do canal |
| método privado usado por vários | a decidir | conforme o conteúdo; se for regra, vai para o domínio |

Mostre também, para cada método público: **quem o chama, quais métodos chama e onde a transação começa.**

## Passos

1. **Caracterize.** Escreva testes que fixem o comportamento atual dos métodos públicos, antes de mexer.
2. **Extraia um `*UseCase` por método público**, começando pelo mais simples. O use case recebe por construtor só as dependências que usa.
3. **Mantenha o service antigo como Facade temporária:** cada método só delega ao novo use case. Assim os chamadores não mudam de uma vez.
4. **Desça as regras** para a entidade de domínio. Se o use case tem `if` de negócio, a regra ainda não desceu.
5. **Migre os chamadores** para os use cases, um de cada vez.
6. **Apague a Facade** quando não restar chamador. Ela não pode sobreviver ao commit da camada `application`.

## Armadilhas

- **Fronteira de transação.** O que hoje roda numa só transação precisa continuar assim. Dois use cases `@Transactional` chamados em sequência pelo Resource viram **duas** transações: se o segundo falhar, o primeiro já gravou. Quando a operação for uma só, ela é um único use case.
- **Use case chamando use case** costuma indicar fronteira errada. Se for a orquestração de um fluxo do módulo, mantenha um único caso de uso dono do fluxo, usando repositórios e gateways.
- **Estado em campo do service** (`private List<...> cache`) não pode ser levado sem análise: o service é compartilhado entre requisições. Pare e pergunte ao dono.
- **Ordem com sistema externo:** a transição do domínio vem **antes** da chamada externa.
- **`@Transactional` só em `application`:** se o service antigo era chamado por outro service dentro de uma transação, reveja o fluxo inteiro.
- Dependência circular entre services aparece na camada `ciclos`; se ela impedir a extração, pare e pergunte.
