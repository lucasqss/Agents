package br.com.claudio.consorcio.entrypoint.rest.processo;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Deixa o corpo das ações ser de fato opcional. Sem {@code Content-Type} o Quarkus REST procura um leitor para
 * {@code *}{@code /*}, o Jackson só lê {@code application/json} e a resposta seria 415. Este leitor cobre o que
 * o Jackson não cobre: corpo vazio vira "sem observação" na {@code Observacao} opcional e
 * um {@code Motivo} sem texto no obrigatório (a validação recusa com 400, apontando o campo {@code motivo}). Corpo com conteúdo e tipo que não é JSON continua 415.
 * Quando o tipo declarado é JSON este leitor não participa: segue o Jackson, com a validação e os erros de sempre.
 */
@Provider
@Consumes(MediaType.WILDCARD)
public class CorpoAusenteReader implements MessageBodyReader<Object> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean corpoDeAcao = type == ProcessoResource.Observacao.class || type == ProcessoResource.Motivo.class;
        return corpoDeAcao && !ehJson(mediaType);
    }

    /** Com JSON declarado o leitor é o do Jackson: este aqui nem entra na disputa. */
    private static boolean ehJson(MediaType mediaType) {
        return mediaType != null && !mediaType.isWildcardSubtype() && mediaType.getSubtype().endsWith("json");
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException {
        if (entityStream.read() != -1) {
            throw new NotSupportedException("O corpo, quando enviado, deve ser application/json");
        }
        return ((Class<?>) type) == ProcessoResource.Motivo.class ? new ProcessoResource.Motivo(null) : null;
    }
}
