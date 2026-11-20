package br.com.claudio.pagamento.entrypoint.rest.processo;

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
import org.jspecify.annotations.Nullable;

/**
 * Deixa o corpo das ações ser de fato opcional. Sem {@code Content-Type} o Quarkus REST procura um leitor para
 * {@code *}{@code /*}, o Jackson só lê {@code application/json} e a resposta seria 415. Corpo vazio vira "sem motivo" (e o
 * domínio recusa com 400 se a ação o exige); corpo com conteúdo e tipo que não é JSON continua 415. Com JSON declarado,
 * este leitor nem entra na disputa: segue o Jackson.
 */
@Provider
@Consumes(MediaType.WILDCARD)
public class CorpoAusenteReader implements MessageBodyReader<Object> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ProcessoResource.Motivo.class && !ehJson(mediaType);
    }

    private static boolean ehJson(@Nullable MediaType mediaType) {
        return mediaType != null && !mediaType.isWildcardSubtype() && mediaType.getSubtype().endsWith("json");
    }

    @Override
    public @Nullable Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        if (entityStream.read() != -1) {
            throw new NotSupportedException("O corpo, quando enviado, deve ser application/json");
        }
        return null;
    }
}
