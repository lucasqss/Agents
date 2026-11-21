import { Send } from "lucide-react";
import { useState } from "react";
import { lerReais, reais } from "../../apoio/formato";
import { ir } from "../../apoio/rotas";
import { Aviso, Botao, Cartao, Tabela } from "../../componentes";
import { processos } from "../../jornada/api";
import { RASCUNHO_VAZIO, rascunho, type BemNoRascunho, type ParteNoRascunho } from "../../jornada/estado";
import { TIPOS } from "../../jornada/vocabulario";
import type { PropsDoPasso } from "./Criacao";
import { totalDosBens } from "./PassoBens";

export function PassoRevisao({ cota, voltar }: PropsDoPasso) {
  const r = rascunho.usar();
  const tipo = r.tipo ? TIPOS[r.tipo] : null;
  const total = totalDosBens(r.bens);
  const [enviando, setEnviando] = useState(false);
  const [falha, setFalha] = useState<{ mensagem: string; campos: string[] } | null>(null);

  async function criar() {
    setEnviando(true);
    setFalha(null);
    const partes = r.partes.map((p) => ({
      nome: p.nome.trim(), documento: p.documento, papel: p.papel as never, email: p.email.trim(), valor: p.recebe ? lerReais(p.valor) : null,
    }));
    const resposta = await processos.criar({
      cotaId: r.cotaId!, tipo: r.tipo!, valor: total,
      bens: r.bens.map((b) => ({ dados: b.dados, valor: lerReais(b.valor) })),
      partes,
      favorecidos: partes.filter((p) => p.valor !== null).map((p) => ({ nome: p.nome, documento: p.documento, papel: p.papel, valor: p.valor! })),
    });
    setEnviando(false);
    if (!resposta.ok) return setFalha({ mensagem: resposta.erro.mensagem, campos: resposta.erro.campos.map((c) => `${c.campo}: ${c.erro}`) });
    // Limpa o rascunho só depois que a rota trocar: antes, o assistente ainda na tela voltaria ao primeiro passo.
    window.addEventListener("hashchange", () => rascunho.gravar(RASCUNHO_VAZIO), { once: true });
    ir(`/criacao/criado/${resposta.dados.id}`);

  }

  if (!tipo) return null; // o assistente leva de volta ao passo que falta
  return (
    <>
      <Cartao titulo="Confira antes de criar">
        <dl className="grid gap-3 text-sm sm:grid-cols-3">
          <div><dt className="text-neutral-500">Cota</dt><dd className="text-neutral-100">{cota?.numero ?? r.cotaId}</dd></div>
          <div><dt className="text-neutral-500">Tipo</dt><dd className="text-neutral-100">{tipo.texto}</dd></div>
          <div><dt className="text-neutral-500">Valor do processo</dt><dd className="font-semibold tabular-nums text-neutral-100">{reais(total)}</dd></div>
        </dl>
      </Cartao>
      <Cartao titulo={`${tipo.bens[0].toUpperCase()}${tipo.bens.slice(1)} (${r.bens.length})`}>
        <Tabela<BemNoRascunho> linhas={r.bens} chave={(b) => b.chave} colunas={[
          ...tipo.campos.map((c) => ({ titulo: c.rotulo, valor: (b: BemNoRascunho) => b.dados[c.chave] })),
          { titulo: "Valor", valor: (b) => reais(lerReais(b.valor)), numero: true },
        ]} />
      </Cartao>
      <Cartao titulo={`Partes (${r.partes.length})`}>
        <Tabela<ParteNoRascunho> linhas={r.partes} chave={(p) => p.chave} colunas={[
          { titulo: "Nome", valor: (p) => p.nome },
          { titulo: "Papel", valor: (p) => p.papel },
          { titulo: "CPF/CNPJ", valor: (p) => p.documento },
          { titulo: "E-mail", valor: (p) => p.email },
          { titulo: "Recebe", valor: (p) => (p.recebe ? reais(lerReais(p.valor)) : "—"), numero: true },
        ]} />
        <p className="mt-3 text-xs text-neutral-500">Ao criar, cada parte recebe um e-mail com o link para enviar os documentos.</p>
      </Cartao>
      {falha && (
        <Aviso tipo="erro">
          {falha.mensagem}
          {falha.campos.length > 0 && <ul className="mt-1 list-disc pl-5">{falha.campos.map((c) => <li key={c}>{c}</li>)}</ul>}
        </Aviso>
      )}
      <div className="flex justify-between gap-2">
        <Botao onClick={voltar}>Voltar</Botao>
        <Botao variante="principal" onClick={criar} disabled={enviando} icone={<Send className="size-4" aria-hidden />}>Criar processo</Botao>
      </div>
    </>
  );
}
