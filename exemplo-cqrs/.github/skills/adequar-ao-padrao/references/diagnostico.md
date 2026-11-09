# Diagnóstico (Passo 0)

Só leitura. Mostre o resultado e **espere aprovação** antes de alterar qualquer arquivo.

## O que levantar

1. **Tipo do componente**, pela árvore de decisão da skill `arquiteto-padrao`. O método cobre o tipo **Domínio**: as regras ArchUnit de ACL e BFF ainda não existem. Se for outro tipo, pare e avise o dono.
2. **Stack:** versão de Java e Quarkus no `pom.xml`. Se estiver abaixo de Java 25 / Quarkus 3.33, pare: atualizar a stack é um trabalho separado, feito antes.
3. **Módulos propostos:** capacidades de negócio, com as classes de cada uma. Mostre quais joins HQL ou chamadas cruzariam módulos: se forem muitos, proponha juntar.
4. **Inventário de sintomas**, com contagem e arquivos:
   - entidades `@Entity` com regra de negócio;
   - uso de Panache;
   - services com vários motivos para mudar (muitos métodos públicos, muitas dependências);
   - Resources que recebem ou devolvem entidade;
   - `@RegisterRestClient` usado fora de um Gateway;
   - Lombok, MapStruct, SQL nativo, herança JPA.
5. **Cobertura de testes** nos pontos críticos e os testes de caracterização que faltam.
6. **Riscos que exigem decisão do dono:** herança JPA, ciclos entre agregados, batch pesado, classes usadas por vários módulos.

## Formato da entrega

| Camada | Regras | Violações previstas | Receita | Risco |
|---|---|---|---|---|

Acrescente a tabela de módulos propostos e a lista de decisões pedidas ao dono.
