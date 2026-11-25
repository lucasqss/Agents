import { CheckCircle2, FileText } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { MODO } from "../../api/api";
import { dataHora } from "../../apoio/formato";
import { Aviso, BotaoLink, Cartao, EnvioDeArquivo, Etiqueta, Vazio } from "../../componentes";
import { envios, type DocumentoExigido, type Envio } from "../../jornada/api";
import { SITUACOES, SITUACOES_DO_DOCUMENTO, TIPOS } from "../../jornada/vocabulario";

/** "Carnê do IPTU" -> "carnê do IPTU": só a primeira letra, para não estragar siglas. */
const minuscula = (t: string) => t[0].toLowerCase() + t.slice(1);

/** A página que a parte abre pelo link do e-mail: só os dados e os documentos dela (LGPD). */
export function EnvioDaParte({ link }: { link: string }) {
  const [envio, setEnvio] = useState<Envio | null>(null);
  const [falha, setFalha] = useState<string | null>(null);
  const [aviso, setAviso] = useState<{ tipo: "sucesso" | "erro"; texto: string } | null>(null);
  const [enviando, setEnviando] = useState<number | null>(null);

  const carregar = useCallback(async () => {
    const r = await envios.ver(link);
    if (r.ok) setEnvio(r.dados); else setFalha(r.erro.mensagem);
  }, [link]);
  useEffect(() => { carregar(); }, [carregar]);

  async function enviar(d: DocumentoExigido, arquivo: File) {
    setEnviando(d.id);
    const r = await envios.enviar(link, d.id, arquivo);
    setEnviando(null);
    setAviso(r.ok ? { tipo: "sucesso", texto: `${d.texto}: enviado.` } : { tipo: "erro", texto: `${d.texto}: não foi possível enviar. ${r.erro.mensagem}` });
    await carregar();
  }

  if (falha) return <><Aviso tipo="erro">{falha}</Aviso><BotaoLink href="#/documentos">Voltar à caixa de e-mail</BotaoLink></>;
  if (!envio) return <Cartao><Vazio>Carregando...</Vazio></Cartao>;

  const antesDaAnalise = SITUACOES[envio.situacaoDoProcesso].aguardaAsPartes;
  const podeEnviar = (d: DocumentoExigido) => d.acoes.includes("enviar");
  const enviados = envio.documentos.filter((d) => SITUACOES_DO_DOCUMENTO[d.situacao].entregue).length;
  const recusados = envio.documentos.filter((d) => SITUACOES_DO_DOCUMENTO[d.situacao].recusado).length;
  const tudoEnviado = enviados === envio.documentos.length;

  const grupos = new Map<string, DocumentoExigido[]>();
  for (const d of envio.documentos) {
    const g = d.bem ?? "Seus documentos";
    grupos.set(g, [...(grupos.get(g) ?? []), d]);
  }

  return (
    <>
      <Cartao>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-lg font-semibold text-neutral-100">Olá, {envio.parte.nome}</p>
            <p className="text-sm text-neutral-400">
              Você é <strong className="text-neutral-200">{envio.parte.papel}</strong> no processo de pagamento nº {envio.processoId} ({TIPOS[envio.tipo].texto.toLowerCase()}).
              Aqui aparecem só os seus documentos.
            </p>
          </div>
          <div className="text-right">
            <p className="text-2xl font-semibold tabular-nums text-neutral-100">{enviados}/{envio.documentos.length}</p>
            <p className="text-xs text-neutral-500">documentos enviados</p>
          </div>
        </div>
        <div className="mt-4 h-2 overflow-hidden rounded-full bg-neutral-800">
          <div className="h-full bg-blue-500 transition-all" style={{ width: `${(enviados / envio.documentos.length) * 100}%` }} />
        </div>
      </Cartao>

      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      {recusados > 0 && <Aviso tipo="atencao">{recusados} documento(s) recusado(s) na análise. Veja o motivo e envie de novo só esse(s).</Aviso>}
      {tudoEnviado && !recusados && (
        <Aviso tipo="sucesso">
          Tudo enviado. {antesDaAnalise
            ? "Quando todas as partes enviarem, o processo vai para análise."
            : "Os documentos estão com a análise; se algum for recusado, você recebe outro e-mail."}
        </Aviso>
      )}
      {MODO === "falso" && <p className="text-xs text-neutral-500">Modo simulado: os arquivos ficam só na memória do navegador e somem ao recarregar a página.</p>}

      {[...grupos].map(([grupo, docs]) => (
        <Cartao key={grupo} titulo={grupo}>
          <ul className="divide-y divide-neutral-800">
            {docs.map((d) => (
              <li key={d.id} className="grid gap-3 py-3 first:pt-0 last:pb-0 sm:grid-cols-[1fr_auto_220px] sm:items-center">
                <div className="flex items-start gap-3">
                  {SITUACOES_DO_DOCUMENTO[d.situacao].aceito ? <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-emerald-400" aria-hidden /> : <FileText className="mt-0.5 size-5 shrink-0 text-neutral-500" aria-hidden />}
                  <div>
                    <p className="text-sm font-medium text-neutral-100">{d.texto}</p>
                    {d.arquivo && <p className="text-xs text-neutral-500">{d.arquivo}{d.enviadoEm ? ` · ${dataHora(d.enviadoEm)}` : ""}</p>}
                    {d.motivoRecusa && <p className="mt-1 text-xs text-red-300">Recusado: {d.motivoRecusa}</p>}
                  </div>
                </div>
                <span className="justify-self-start"><Etiqueta tom={SITUACOES_DO_DOCUMENTO[d.situacao].tom}>{SITUACOES_DO_DOCUMENTO[d.situacao].texto}</Etiqueta></span>
                {podeEnviar(d)
                  ? <EnvioDeArquivo rotulo={`${SITUACOES_DO_DOCUMENTO[d.situacao].verboDoEnvio} ${minuscula(d.texto)}`}
                      desabilitado={enviando === d.id} aoEscolher={(a) => enviar(d, a)} />
                  : <span />}
              </li>
            ))}
          </ul>
        </Cartao>
      ))}
      <BotaoLink href="#/documentos">Voltar à caixa de e-mail</BotaoLink>
    </>
  );
}
