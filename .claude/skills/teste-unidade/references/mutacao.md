# Mutação (PITest)

`./mvnw verify -Pauditoria` roda o PITest no domínio e nos casos de uso, só com os testes de unidade. Ele muda o código compilado (troca `>` por `>=`, apaga uma chamada, devolve `null`, inverte um `if`) e roda os testes. **Mutante que sobrevive é teste que passa sem afirmar a regra naquele ponto.** O build falha se a porcentagem de mutantes mortos ficar abaixo de `pitest.limite` (no `pom.xml`).

Relatório: `target/pit-reports/index.html`, por classe e linha. Verde é mutante morto; vermelho, sobrevivente; sem cor, código que nenhum teste de unidade alcança.

## Como matar um sobrevivente

| Mutação sobrevivente | O que falta | Teste |
|---|---|---|
| `changed conditional boundary` | o caso de fronteira | exatamente o máximo é aceito; um acima é recusado |
| `replaced return value with ""` em mensagem | conferir o texto | `assertEquals("Para cancelar é preciso ter o papel gerente", e.getMessage())` |
| `negated conditional` | o caso do outro lado do `if` | os dois ramos, cada um com o seu resultado |
| `removed call to salvar` | conferir o efeito colateral | contador de `salvar` no repositório em memória |
| `replaced boolean return with false` | conferir o retorno | `assertTrue(...)` no caminho que devolve verdadeiro |

## Sem cobertura não é sobrevivente

Linha "sem cobertura" no relatório costuma ser um caso de uso testado só com o serviço montado (`@QuarkusTest`, fora do PITest). Se a orquestração merece prova, escreva o teste com repositório em memória ([use case](use-case.md)). Se é só "chama e devolve", a cobertura do componente basta.

## Falso positivo conhecido

Código que só roda na inicialização de um `enum` ou de um campo `static` (a tabela montada uma vez) sobrevive sempre: o PITest aplica o mutante depois dela. Registre no comentário do profile `auditoria` em vez de escrever teste para ele.

## O limite

Na adoção, rode com `pitest.limite` em `0`, leia o resultado e fixe o limite no valor medido, arredondado para baixo. Suba-o quando a cobertura subir; nunca baixe para o build passar.
