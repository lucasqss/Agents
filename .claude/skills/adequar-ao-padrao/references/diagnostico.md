# Diagnóstico (Passo 0)

Só leitura. Mostre o resultado e **espere aprovação** antes de alterar qualquer arquivo.

## O que levantar

1. **Tipo do componente**, pela árvore de decisão da skill `arquiteto-padrao`. As receitas e a ordem de camadas deste método cobrem o tipo **Negocial**. Para ACL ou BFF as regras existem (instale as da pasta do tipo), mas o caminho de adequação não está escrito: mostre o diagnóstico e combine a ordem com o dono.
2. **Stack:** versão de Java e Quarkus no `pom.xml`. Se estiver abaixo de Java 25 / Quarkus 3.33, pare: atualizar a stack é um trabalho separado, feito antes.
3. **Agregados:** liste os agregados e as classes de cada um, com o destino de cada classe. Classe que não pertence a nenhum agregado e não é um tipo aceito na raiz da camada é decisão do dono.
4. **Inventário de sintomas**, com contagem e arquivos:
   - entidades `@Entity` com regra de negócio;
   - uso de Panache;
   - services com vários motivos para mudar (muitos métodos públicos, muitas dependências);
   - Resources que recebem ou devolvem entidade;
   - `@RegisterRestClient` usado fora de um Gateway;
   - Lombok, MapStruct, SQL nativo, herança JPA.
5. **Cobertura de testes** nos pontos críticos e os testes de caracterização que faltam.
6. **Riscos que exigem decisão do dono:** herança JPA, ciclos entre agregados, batch pesado, classes usadas por vários agregados fora da lista da raiz.

## Formato da entrega

| Camada | Regras | Violações previstas | Receita | Risco |
|---|---|---|---|---|

Acrescente a tabela de agregados e a lista de decisões pedidas ao dono.
