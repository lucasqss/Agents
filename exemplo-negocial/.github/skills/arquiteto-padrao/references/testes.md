# Testes esperados em cada mudança

Os testes ficam no mesmo pacote da classe testada (`src/test/java/.../domain/cota/CotaTest.java`). Testes de API ficam em `src/test/java/.../integracao/`.

- **Regra de negócio:** teste JUnit puro, sem Quarkus, em `domain/<agregado>/`.
- **Endpoint novo:** `@QuarkusTest` + RestAssured, cobrindo o caminho feliz e o erro principal. Em canal, conferir os **nomes exatos dos campos** e os textos formatados (em `src/test/java/.../integracao/CanaisContratoTest.java`; crie se ainda não existir).
- **Formato novo de canal:** linha nova em `src/test/java/.../entrypoint/apresentacao/PadroesDeCanalTest.java` (crie se ainda não existir).
- **Mapper:** a ida e volta `paraEntidade(paraDominio(entity))` preserva todos os campos, inclusive enum, subtipo e versão.
- **Value Object:** teste JUnit puro de cada regra (validação, arredondamento, rateio que fecha a soma).
- **Concorrência:** caso de uso com bloqueio ou chamada externa ganha um teste que dispara a operação em paralelo e confere que o efeito aconteceu uma vez só. Agregado com versão ganha um teste do 409.
- **Sistema externo:** nunca chamar o real nos testes. Use um Service Stub ([exemplo](exemplos-codigo.md)) com chave para simular falha, e teste o 503.
- **Rotina com sistema externo:** o stub tem verbos de negócio (confirmar, recusar, derrubar) e deixa consultar o que recebeu. Um teste de concorrência roda a rotina duas vezes ao mesmo tempo e confere que o resultado foi aplicado uma vez. Havendo circuito, não conte os pedidos recebidos de vários itens enquanto o stub responde com erro: com o circuito aberto nem toda tentativa chega.
- **Teste de aceitação de usuário:** siga a skill `teste-aceitacao-modelos`, que parte só do requisito e do contrato, ou, sem requisito escrito, a `teste-aceitacao-legado`, que deriva as regras do código e as confirma com o usuário. Os testes ficam em `src/test/java/.../aceitacao/<capacidade>/`, e o Service Stub JAX-RS não serve para eles, porque não entra no artefato testado pelo `*IT`.

Antes de terminar qualquer tarefa, rode `./mvnw verify`.
