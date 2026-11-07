/**
 * BFF do canal web: *Resource (HTTP em /web/v1), *Request (Bean Validation do formato), *Response (contrato
 * congelado: só mudanças aditivas) e WebAssembler (read model -> contrato, função pura).
 * <p>Proibido: regra de negócio, acesso a persistence e usar outro canal.
 */
package br.com.claudio.consorcio.canal.web.entrypoint;
