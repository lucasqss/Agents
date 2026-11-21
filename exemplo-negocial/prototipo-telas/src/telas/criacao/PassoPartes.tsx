import { Plus, Trash2 } from "lucide-react";
import { useEffect } from "react";
import { lerReais, reais } from "../../apoio/formato";
import { Aviso, Botao, Campo, Cartao, Entrada, Etiqueta, Interruptor } from "../../componentes";
import { CLIENTE_DA_DEMONSTRACAO } from "../../jornada/demonstracao";
import { novaChave, rascunho, type ParteNoRascunho } from "../../jornada/estado";
import { PAPEIS_DA_PARTE } from "../../jornada/vocabulario";
import type { PropsDoPasso } from "./Criacao";
import { Navegacao } from "./Navegacao";
import { totalDosBens } from "./PassoBens";

const soDigitos = (s: string) => s.replace(/\D/g, "");
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function errosDaParte(p: ParteNoRascunho): Record<string, string> {
  const e: Record<string, string> = {};
  if (!p.nome.trim()) e.nome = "Informe o nome";
  if (![11, 14].includes(soDigitos(p.documento).length)) e.documento = "CPF (11 dígitos) ou CNPJ (14 dígitos)";
  if (!EMAIL.test(p.email.trim())) e.email = "Informe um e-mail válido";
  if (p.recebe && !(lerReais(p.valor) > 0)) e.valor = "Informe quanto recebe, no formato 80.000,00";
  return e;
}

export function PassoPartes({ avancar, voltar }: PropsDoPasso) {
  const r = rascunho.usar();

  // O comprador é a cliente da cota, e vem preenchido.
  useEffect(() => {
    if (!r.partes.length) {
      rascunho.gravar({ ...r, partes: [{
        chave: novaChave(), nome: CLIENTE_DA_DEMONSTRACAO.nome, documento: CLIENTE_DA_DEMONSTRACAO.cpf, papel: "comprador",
        email: CLIENTE_DA_DEMONSTRACAO.email, recebe: false, valor: "",
      }] });
    }
  });

  const mudar = (chave: string, m: Partial<ParteNoRascunho>) =>
    rascunho.gravar({ ...r, partes: r.partes.map((p) => (p.chave === chave ? { ...p, ...m } : p)) });
  const nova = (): ParteNoRascunho => ({ chave: novaChave(), nome: "", documento: "", papel: "vendedor", email: "", recebe: true, valor: "" });

  const total = totalDosBens(r.bens);
  const recebido = Math.round(r.partes.filter((p) => p.recebe).reduce((s, p) => s + (lerReais(p.valor) || 0), 0) * 100) / 100;
  const diferenca = Math.round((total - recebido) * 100) / 100;
  const semErros = r.partes.every((p) => !Object.keys(errosDaParte(p)).length);
  const algumRecebe = r.partes.some((p) => p.recebe);

  return (
    <Cartao titulo="Quem são as partes?">
      <p className="mb-4 text-sm text-neutral-400">
        Cada parte recebe um e-mail para enviar os próprios documentos. Quem recebe valor (o vendedor, por exemplo) é favorecido:
        as partes dos favorecidos somam o valor do processo.
      </p>
      <div className="mb-4 space-y-4">
        {r.partes.map((p, i) => {
          const comprador = i === 0;
          const erros = errosDaParte(p);
          const mostrar = (campo: string, valor: string) => (valor ? erros[campo] : undefined);
          return (
            <fieldset key={p.chave} aria-label={comprador ? "Comprador" : `Parte ${i + 1}`} className="rounded-lg border border-neutral-800 p-4">
              <div className="mb-3 flex items-center justify-between gap-2">
                <span className="flex items-center gap-2 text-sm font-medium text-neutral-200">
                  {comprador ? "Comprador" : `Parte ${i + 1}`}
                  {comprador && <Etiqueta tom="andamento">cliente da cota</Etiqueta>}
                </span>
                {!comprador && (
                  <Botao variante="perigo" onClick={() => rascunho.gravar({ ...r, partes: r.partes.filter((x) => x.chave !== p.chave) })}
                    icone={<Trash2 className="size-4" aria-hidden />}>Remover</Botao>
                )}
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <Campo rotulo="Nome" erro={mostrar("nome", p.nome)}>
                  <Entrada value={p.nome} disabled={comprador} onChange={(e) => mudar(p.chave, { nome: e.target.value })} placeholder="ex.: João Pereira" />
                </Campo>
                <Campo rotulo="CPF ou CNPJ" erro={mostrar("documento", p.documento)}>
                  <Entrada value={p.documento} disabled={comprador} inputMode="numeric" placeholder="só os números"
                    onChange={(e) => mudar(p.chave, { documento: soDigitos(e.target.value) })} />
                </Campo>
                <Campo rotulo="Papel">
                  <select value={p.papel} disabled={comprador} onChange={(e) => mudar(p.chave, { papel: e.target.value })}
                    className="w-full rounded-lg border border-neutral-700 bg-neutral-950 px-3 py-2 text-sm text-neutral-100 disabled:opacity-60">
                    {PAPEIS_DA_PARTE.filter((x) => (comprador ? x === "comprador" : x !== "comprador")).map((x) => <option key={x} value={x}>{x}</option>)}
                  </select>
                </Campo>
                <Campo rotulo="E-mail (para o link dos documentos)" erro={mostrar("email", p.email)}>
                  <Entrada type="email" value={p.email} onChange={(e) => mudar(p.chave, { email: e.target.value })} placeholder="ex.: joao@exemplo.com" />
                </Campo>
              </div>
              <div className="mt-3 flex flex-wrap items-end gap-4">
                <Interruptor rotulo="Recebe valor (favorecido)" ligado={p.recebe} aoMudar={(v) => mudar(p.chave, { recebe: v, valor: v ? p.valor : "" })} />
                {p.recebe && (
                  <div className="w-48">
                    <Campo rotulo="Quanto recebe (R$)" erro={mostrar("valor", p.valor)}>
                      <Entrada inputMode="decimal" value={p.valor} onChange={(e) => mudar(p.chave, { valor: e.target.value })} placeholder="ex.: 80.000,00" />
                    </Campo>
                  </div>
                )}
              </div>
            </fieldset>
          );
        })}
        <Botao onClick={() => rascunho.gravar({ ...r, partes: [...r.partes, nova()] })} icone={<Plus className="size-4" aria-hidden />}>
          Acrescentar parte
        </Botao>
      </div>
      <div className="mb-5 grid gap-1 rounded-lg bg-neutral-950 px-4 py-3 text-sm sm:grid-cols-3">
        <span className="text-neutral-400">Valor do processo: <strong className="tabular-nums text-neutral-100">{reais(total)}</strong></span>
        <span className="text-neutral-400">Para os favorecidos: <strong className="tabular-nums text-neutral-100">{reais(recebido)}</strong></span>
        <span className={diferenca === 0 ? "text-emerald-300" : "text-amber-300"}>
          {diferenca === 0 ? "Fechou o valor do processo" : diferenca > 0 ? `Faltam ${reais(diferenca)}` : `Passou ${reais(-diferenca)}`}
        </span>
      </div>
      {!algumRecebe && <div className="mb-5"><Aviso tipo="atencao">Ao menos uma parte precisa receber o valor.</Aviso></div>}
      <Navegacao voltar={voltar} avancar={avancar} podeAvancar={semErros && algumRecebe && diferenca === 0} />
    </Cartao>
  );
}
