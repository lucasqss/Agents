import { Inbox, MailOpen } from "lucide-react";
import { useEffect, useState } from "react";
import { dataHora } from "../../apoio/formato";
import { Aviso, BotaoLink, Cartao, Etiqueta, Vazio } from "../../componentes";
import { desenvolvimento, type Email } from "../../jornada/api";
import { TIPOS_DE_EMAIL } from "../../jornada/vocabulario";

/**
 * A caixa de entrada das partes, simulada: o que o serviço "enviou". Imita o e-mail de um provedor de coleta e
 * assinatura de documentos (o protótipo não usa marca de nenhum provedor real).
 */
export function CaixaDeEmail() {
  const [emails, setEmails] = useState<Email[] | null>(null);
  const [aberto, setAberto] = useState<Email | null>(null);
  const [falha, setFalha] = useState<string | null>(null);

  useEffect(() => {
    desenvolvimento.emails().then((r) => {
      if (!r.ok) return setFalha(r.erro.mensagem);
      setEmails(r.dados);
      setAberto(r.dados[0] ?? null);
    });
  }, []);

  if (falha) return <Aviso tipo="erro">{falha}</Aviso>;
  if (emails === null) return <Cartao><Vazio>Carregando...</Vazio></Cartao>;
  if (!emails.length) return <Cartao><Vazio>Nenhum e-mail ainda. Crie um processo na jornada 1: cada parte recebe um e-mail.</Vazio></Cartao>;

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(260px,340px)_1fr]">
      <Cartao titulo={<span className="flex items-center gap-2"><Inbox className="size-4" aria-hidden />Entrada</span>}>
        <ul className="-mx-5 divide-y divide-neutral-800">
          {emails.map((e) => (
            <li key={e.id}>
              <button type="button" onClick={() => setAberto(e)}
                className={`w-full px-5 py-3 text-left text-sm ${aberto?.id === e.id ? "bg-blue-500/10" : "hover:bg-neutral-800/40"}`}>
                <span className="flex items-center justify-between gap-2">
                  <span className="truncate font-medium text-neutral-100">{e.nome}</span>
                  {TIPOS_DE_EMAIL[e.tipo].etiqueta && <Etiqueta tom="erro">{TIPOS_DE_EMAIL[e.tipo].etiqueta}</Etiqueta>}
                </span>
                <span className="block truncate text-neutral-400">{e.assunto}</span>
                <span className="block text-xs text-neutral-500">{e.para} · {dataHora(e.quando)}</span>
              </button>
            </li>
          ))}
        </ul>
      </Cartao>

      {aberto && (
        <article aria-label="E-mail aberto" className="overflow-hidden rounded-xl border border-neutral-800 bg-white text-neutral-800">
          <header className="flex items-center gap-3 bg-emerald-700 px-6 py-4 text-white">
            <MailOpen className="size-5" aria-hidden />
            <div>
              <p className="text-sm font-semibold">Coleta de documentos</p>
              <p className="text-xs opacity-80">simulação do e-mail do provedor de assinatura</p>
            </div>
          </header>
          <div className="space-y-4 px-6 py-5 text-sm">
            <p className="text-xs text-neutral-500">Para: {aberto.nome} &lt;{aberto.para}&gt; · {dataHora(aberto.quando)}</p>
            <h2 className="text-lg font-semibold text-neutral-900">{aberto.assunto}</h2>
            <p className="leading-relaxed">{aberto.texto}</p>
            <BotaoLink href={`#/documentos/${aberto.link}`} variante="principal">Enviar documentos</BotaoLink>
            <p className="text-xs text-neutral-500">Este link é pessoal. Não o encaminhe: por ele, só os seus documentos são vistos e enviados.</p>
          </div>
        </article>
      )}
    </div>
  );
}
