import { Mail } from "lucide-react";
import { useEffect, useState } from "react";
import { Aviso, BotaoLink, Cartao, Tabela, Vazio } from "../../componentes";
import { desenvolvimento, type Email } from "../../jornada/api";

/** Depois de criar: o processo aguarda os documentos, e cada parte recebeu um e-mail. Leva à jornada 2. */
export function EmailsEnviados({ processoId }: { processoId: string }) {
  const [emails, setEmails] = useState<Email[] | null>(null);
  const [falha, setFalha] = useState<string | null>(null);

  useEffect(() => {
    desenvolvimento.emails().then((r) => (r.ok ? setEmails(r.dados.filter((e) => String(e.processoId) === processoId)) : setFalha(r.erro.mensagem)));
  }, [processoId]);

  return (
    <>
      <Aviso tipo="sucesso">Processo {processoId} criado. Ele aguarda os documentos das partes; com tudo enviado, fica pronto para análise.</Aviso>
      <Cartao titulo="E-mails enviados às partes">
        {falha && <Aviso tipo="erro">{falha}</Aviso>}
        {!falha && emails === null && <Vazio>Carregando...</Vazio>}
        {emails && (
          <Tabela<Email> linhas={emails} chave={(e) => e.id} colunas={[
            { titulo: "Para", valor: (e) => e.nome },
            { titulo: "E-mail", valor: (e) => e.para },
            { titulo: "", valor: (e) => <BotaoLink href={`#/documentos/${e.link}`} icone={<Mail className="size-4" aria-hidden />}>Abrir como {e.nome.split(" ")[0]}</BotaoLink> },
          ]} />
        )}
      </Cartao>
      <div className="flex flex-wrap gap-2">
        <BotaoLink href="#/documentos" variante="principal">Ir para a jornada de documentos</BotaoLink>
        <BotaoLink href="#/criacao">Criar outro processo</BotaoLink>
      </div>
    </>
  );
}
