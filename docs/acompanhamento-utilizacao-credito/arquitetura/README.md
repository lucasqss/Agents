# Arquitetura: diagramas ArchiMate (PlantUML)

São visões ArchiMate 3.x do [modelo de domínio](../modelo-dominio.md). Elas usam a biblioteca padrão do PlantUML (`!include <archimate/Archimate>`), sem nenhuma dependência externa.

| # | Arquivo | Viewpoint ArchiMate | O que mostra |
|---|---|---|---|
| 1 | [01-informacao-negocio.puml](01-informacao-negocio.puml) ([svg](01-informacao-negocio.svg)) | Information Structure | Business Objects de definição e de execução, instanciação e StatusCliente |
| 2 | [02-processo-negocio.puml](02-processo-negocio.puml) ([svg](02-processo-negocio.svg)) | Business Process | Fluxo do template *Imóvel*: etapas, tarefas, disparos e junções |
| 3 | [03-aplicacao-cooperacao.puml](03-aplicacao-cooperacao.puml) ([svg](03-aplicacao-cooperacao.svg)) | Application Cooperation | ServiceNow (Flows genéricos e APIs), Backend, App e Notificações |
| 4 | [04-realizacao-dados.puml](04-realizacao-dados.puml) ([svg](04-realizacao-dados.svg)) | Layered (Negócio ↔ Aplicação) | Tabelas `x_<esc>_*` e projeções realizando os Business Objects |
| 5 | [05-responsabilidades.puml](05-responsabilidades.puml) ([svg](05-responsabilidades.svg)) | Business Process Cooperation | Papéis e filas atribuídos a cada tarefa, e objetos acessados |

## Convenções

- **Aninhamento** representa **composição**. Exemplo: Processo ◆ Etapa ◆ Tarefa, no diagrama 2.
- **Dependência entre tarefas** (`FIM_INICIO`) é representada por **Triggering**. Tarefas paralelas convergem numa **junção AND** (●).
- `«interna»` marca as tarefas com `visivelCliente: false`. `«condicional»` marca as que têm `regraAplicabilidade`.
- O papel da Administradora corresponde a uma fila (`assignment_group`) no ServiceNow.

## Renderizar

```bash
java -jar plantuml.jar -tsvg docs/arquitetura/*.puml
```

Os arquivos usam `!pragma layout smetana`, o motor de layout embutido do PlantUML, e por isso não precisam do Graphviz. Para usar o Graphviz, remova essa linha.

> Observação: a macro `Junction_And` do stdlib gera sintaxe inválida no PlantUML 1.2025.x. Por isso os arquivos definem um procedimento local `Juncao_E`.
