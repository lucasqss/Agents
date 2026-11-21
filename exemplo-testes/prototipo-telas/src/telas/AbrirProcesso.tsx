import { useState } from "react";
import { ir } from "../apoio/rotas";
import { lerReais, reais } from "../apoio/formato";
import { Aviso, Botao, BotaoLink, Campo, Cartao, Entrada, Opcoes } from "../componentes";
import { processos } from "../jornada/api";
import { lembrar } from "../jornada/estado";
import { ALCADA, TIPOS, type Tipo } from "../jornada/vocabulario";

export function AbrirProcesso() {
  const [tipo, setTipo] = useState<Tipo>("SERVICO");
  const [numeroCota, setNumeroCota] = useState("");
  const [valor, setValor] = useState("");
  const [erros, setErros] = useState<Record<string, string>>({});
  const [mensagem, setMensagem] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function confirmar(e: React.FormEvent) {
    e.preventDefault();
    setErros({});
    setMensagem(null);
    const numero = lerReais(valor);
    if (Number.isNaN(numero)) {
      setErros({ valor: "Use o formato 80.000,00" });
      return;
    }
    setEnviando(true);
    const r = await processos.abrir({ tipo, numeroCota: numeroCota.trim(), valor: numero });
    setEnviando(false);
    if (r.ok) {
      lembrar(r.dados.id);
      ir(`/processo/${r.dados.id}?aberto=1`);
      return;
    }
    setErros(Object.fromEntries(r.erro.campos.map((c) => [c.campo ?? "", c.erro])));
    setMensagem(r.erro.mensagem);
  }

  return (
    <Cartao>
      <form noValidate onSubmit={confirmar} className="max-w-2xl space-y-5">
        <fieldset className="space-y-1.5">
          <legend className="mb-1.5 text-sm font-medium text-neutral-300">Tipo do processo</legend>
          <Opcoes nome="tipo" valor={tipo} aoMudar={setTipo}
            opcoes={Object.entries(TIPOS).map(([v, t]) => ({ valor: v as Tipo, titulo: t.texto, explicacao: t.explicacao }))} />
          {erros.tipo && <p className="text-xs text-red-400">{erros.tipo}</p>}
        </fieldset>
        <Campo rotulo="Número da cota" erro={erros.numeroCota}>
          <Entrada name="numeroCota" maxLength={30} placeholder="ex.: COTA-123" autoComplete="off"
            value={numeroCota} onChange={(e) => setNumeroCota(e.target.value)} />
        </Campo>
        <Campo rotulo="Valor a pagar (R$)" erro={erros.valor}
          ajuda={`Até ${reais(ALCADA)} o processo é aprovado na análise (ou na vistoria); acima, precisa de quem tem alçada.`}>
          <Entrada name="valor" inputMode="decimal" placeholder="ex.: 80.000,00" value={valor} onChange={(e) => setValor(e.target.value)} />
        </Campo>
        {mensagem && <Aviso tipo="erro">{mensagem}</Aviso>}
        <div className="flex gap-2">
          <Botao type="submit" variante="principal" disabled={enviando}>Abrir processo</Botao>
          <BotaoLink href="#/">Cancelar</BotaoLink>
        </div>
      </form>
    </Cartao>
  );
}
