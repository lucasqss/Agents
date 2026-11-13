# Receita: nova decisão de arquitetura (ADR)

O ADR 001 define o padrão de todos os componentes. O que ele deixa em aberto é decidido por projeto, e fica escrito para que o próximo (pessoa ou agente) não decida diferente.

## Quando escrever

- A escolha tem alternativa razoável e custo para voltar atrás: forma de tratar transação com sistema externo, moeda, identidade de quem executa, mensageria, cache.
- Algo passa a valer para mais de um agregado ou muda uma regra ArchUnit.
- Você parou em "peça ao dono" e ele respondeu.

Não escreva ADR para o que o padrão já decide, nem para escolha local e fácil de desfazer (nome, tamanho de coluna).

## Como

1. Você não decide: apresenta ao dono o problema, as opções e o custo de cada uma, com a sua recomendação.
2. Com a resposta, grave `docs/adr/ADR_<nnn>_<assunto>.md` no projeto, a partir do [modelo](../../adr/MODELO.md), com o próximo número. O ADR 001 fica com a skill; os do projeto ficam no projeto.
3. Se a decisão vira regra verificável, peça ao dono a regra ArchUnit correspondente; não crie por conta própria.
4. Decisão que substitui outra: o ADR antigo recebe "Substituído pelo ADR <nnn>" e não é apagado.
