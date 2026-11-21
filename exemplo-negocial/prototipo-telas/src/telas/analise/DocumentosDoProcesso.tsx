import { Check, Eye, X } from "lucide-react";
import { Botao, Cartao, Etiqueta, Vazio } from "../../componentes";
import type { DocumentoExigido, Parte } from "../../jornada/api";
import { SITUACOES_DO_DOCUMENTO } from "../../jornada/vocabulario";

/** Documentos agrupados por parte, com ver, aceitar e recusar. `conferindo`: o processo está em análise. */
export function DocumentosDoProcesso({ documentos, partes, conferindo, aoVer, aoAceitar, aoRecusar }: {
  documentos: DocumentoExigido[]; partes: Parte[]; conferindo: boolean;
  aoVer: (d: DocumentoExigido) => void; aoAceitar: (d: DocumentoExigido) => void; aoRecusar: (d: DocumentoExigido) => void;
}) {
  const aceitos = documentos.filter((d) => d.situacao === "ACEITO").length;
  return (
    <Cartao titulo="Documentos" acoes={<span className="text-xs text-neutral-500">{aceitos} de {documentos.length} aceitos</span>}>
      {!documentos.length && <Vazio>Nenhum documento exigido.</Vazio>}
      <div className="space-y-5">
        {partes.map((parte) => {
          const daParte = documentos.filter((d) => d.parteId === parte.id);
          if (!daParte.length) return null;
          return (
            <section key={parte.id} aria-label={`Documentos de ${parte.nome}`}>
              <h3 className="mb-2 text-sm font-medium text-neutral-300">{parte.nome} <span className="font-normal text-neutral-500">· {parte.papel}</span></h3>
              <ul className="divide-y divide-neutral-800 rounded-lg border border-neutral-800">
                {daParte.map((d) => (
                  <li key={d.id} className="grid gap-2 px-3 py-2.5 sm:flex sm:items-center sm:gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="text-sm text-neutral-100">{d.texto}{d.bem && <span className="text-neutral-500"> · {d.bem}</span>}</p>
                      {d.arquivo && <p className="truncate text-xs text-neutral-500">{d.arquivo}</p>}
                      {d.motivoRecusa && <p className="text-xs text-red-300">Recusado: {d.motivoRecusa}</p>}
                    </div>
                    <span className="justify-self-start"><Etiqueta tom={SITUACOES_DO_DOCUMENTO[d.situacao].tom}>{SITUACOES_DO_DOCUMENTO[d.situacao].texto}</Etiqueta></span>
                    <div className="flex gap-1.5">
                      {d.arquivo && <Botao aria-label={`Ver ${d.texto} de ${parte.nome}`} onClick={() => aoVer(d)} icone={<Eye className="size-4" aria-hidden />}>Ver</Botao>}
                      {conferindo && d.situacao === "ENVIADO" && (
                        <Botao aria-label={`Aceitar ${d.texto} de ${parte.nome}`} onClick={() => aoAceitar(d)} icone={<Check className="size-4" aria-hidden />}>Aceitar</Botao>
                      )}
                      {conferindo && (d.situacao === "ENVIADO" || d.situacao === "ACEITO") && (
                        <Botao variante="perigo" aria-label={`Recusar ${d.texto} de ${parte.nome}`} onClick={() => aoRecusar(d)} icone={<X className="size-4" aria-hidden />}>Recusar</Botao>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          );
        })}
      </div>
    </Cartao>
  );
}
