# Smoke da API de consórcio

Verificação rápida de que a aplicação empacotada está de pé e atende os endpoints principais: documentação,
identidade, cadastro de cliente e cota, e as consultas. Não testa regra de negócio (isso é dos testes do próprio
serviço); só que cada caminho principal responde como deve.

Cada cenário cria os seus próprios dados, com valores únicos, e pode rodar em qualquer ordem e quantas vezes quiser.

## A documentação da API está publicada

* A documentação OpenAPI responde

## Sem token, a API recusa

* Consultar "/clientes" sem token responde "401"
* Consultar "/processos" sem token responde "401"

## Quem não é do backoffice não cadastra cliente

* Com o papel "cliente", cadastrar um cliente responde "403"

## O backoffice cadastra um cliente e o encontra na lista

* Com o papel "backoffice", cadastrar o cliente "Cliente Smoke"
* O cliente cadastrado aparece na lista de clientes

## O backoffice cadastra uma cota para o cliente

* Com o papel "backoffice", cadastrar o cliente "Cliente Cota Smoke"
* Cadastrar uma cota de crédito "250000.00" para esse cliente
* A cota cadastrada mostra crédito disponível de "250000.00"

## As consultas respondem para qualquer papel autenticado

* Com o papel "backoffice", as consultas respondem
   |caminho                     |status|
   |----------------------------|------|
   |/clientes                   |200   |
   |/processos                  |200   |
   |/processos?pagina=1&tamanho=5|200  |
   |/cotas/999999999            |404   |
