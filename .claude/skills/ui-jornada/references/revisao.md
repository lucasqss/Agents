# Revisão: checklist e erros comuns

## Checklist (antes de dar por pronto, ou ao revisar um PR)

- [ ] `scripts/verificar.sh` da skill verde (UI-001 a UI-006), com os caminhos das camadas do projeto.
- [ ] A jornada percorrida na aplicação no ar; sem a faixa "Jornada desatualizada".
- [ ] A jornada tem teste no navegador escrito a partir da especificação (seletor por papel acessível e texto exato), com checagem de acessibilidade (axe) em cada tela.
- [ ] Nenhum botão, etapa ou mensagem decidida no front: ações vêm de "ações disponíveis", situação vem da API, erro vem da API (por campo, no `data-campo` certo).
- [ ] Texto novo de tela que é desenho da jornada foi para `textos` na jornada, não escrito na tela.
- [ ] Componente novo é puro (só props), tem bloco em `componentes.css` e linha no [catálogo](componentes.md).
- [ ] Nenhuma cor, raio ou sombra fora de `tokens.css`.
- [ ] Todo texto vindo da API ou do usuário passou por `h()` (ou pelo binding seguro do framework; nunca `innerHTML`/`dangerouslySetInnerHTML`/`v-html` com dado cru).
- [ ] Funciona em 390px de largura (celular) e com teclado (foco visível, diálogo fecha com Esc).

## Erros comuns de agente (e a correção)

| Erro | Por que é erro | Correção |
|---|---|---|
| `if (p.valor > 50000) mostraAlcada()` | regra de negócio no front; o número muda no backend e o front mente | mostrar a situação que a API devolve; texto de ajuda sem número |
| `caminho.filter(c => !c.talvez \|\| p.valor > ALCADA)` | mesma regra de alçada, só que escondida no desenho do caminho | etapa opcional como "talvez"; feita se aparece no histórico |
| `p.estado === "PENDENTE_DOCUMENTO" ? 2 : ...` para posicionar o caminho | código da API comparado na tela (UI-005) | posição pela situação atual na lista de etapas da jornada, e pelo histórico |
| aviso de entrada ("Processo aberto.") que fica na tela junto com o resultado da ação | dois avisos ao mesmo tempo: a pessoa não sabe qual vale | um só estado de aviso; o resultado da ação substitui o anterior |
| `if (p.estado === "APROVADO") <botão Pagar>` | o front decidindo se a ação vale | botões só das "ações disponíveis" |
| `fetch(API + "/processos/" + id)` dentro da tela | HTTP fora do núcleo, endpoint fora da api (UI-001, UI-002) | `processos.consultar(id)` |
| componente que importa `api/` para "se buscar sozinho" | componente deixa de ser reutilizável e testável (UI-003) | a tela busca e passa por props |
| `"Em análise"` escrito na tela, ou `"EM_ANALISE"` comparado na tela | rótulo duplicado / código da API fora da jornada (UI-005) | `situacao(codigo).rotulo` |
| ação nova acrescentada só na tela | o desenho sai da jornada e a conferência não vê | acrescentar em `acoes` da jornada |
| transição na jornada (`"PAGAR": { "vai": "PAGO" }`) | é o backend quem decide para onde vai | usar a situação da resposta |
| cor nova `#3b82f6` no CSS do componente | tema espalhado; trocar a paleta vira caça ao tesouro | token novo em `tokens.css` (ou um existente) |
| biblioteca de UI/CDN para um botão ou modal | dependência a manter para algo que já existe | catálogo de componentes |
| `catch` engolindo erro da API e seguindo | a pessoa não sabe que falhou | `aviso({ tipo: "erro", texto: r.corpo?.mensagem })` |

## O que o `verificar.sh` não pega

Ele é só `grep`: confere pastas e padrões de texto. Continua sendo de revisão:
- regra de negócio escrita sem código da API (ex.: `valor > 50000`);
- endpoint escrito como texto em tela sem passar por `fetch`/`chamar` (ex.: montar um link `href` para a API);
- componente com estado global ou efeito escondido.
