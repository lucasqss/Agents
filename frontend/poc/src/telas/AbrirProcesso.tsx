// TELA DE REFERÊNCIA (formulário). Valida aqui só o FORMATO que a API não consegue receber (texto no lugar de
// número); todo o resto (obrigatório, tamanho, regra) é a API que diz, e o erro aparece no campo que ela aponta.
import { useState } from "react";
import { processos, type Tipo } from "@/api/processos";
import { Aviso, Campo, Cartao, Opcoes } from "@/componentes";
import { Button } from "@/componentes/ui/button";
import { Input } from "@/componentes/ui/input";
import { TEXTOS, TIPOS } from "@/jornadas/processo-pagamento";
import { lerReais } from "@/nucleo/formato";
import { ir } from "@/nucleo/rotas";
import { lembrar } from "./contexto";

export function AbrirProcesso() {
  const [tipo, setTipo] = useState<Tipo>("SERVICO");
  const [numeroCota, setNumeroCota] = useState("");
  const [valor, setValor] = useState("");
  const [erros, setErros] = useState<Record<string, string>>({});
  const [mensagem, setMensagem] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function confirmar() {
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
      <form noValidate className="max-w-2xl space-y-5" onSubmit={(e) => { e.preventDefault(); void confirmar(); }}>
        <fieldset className="space-y-1.5">
          <legend className="mb-1.5 text-sm font-medium">Tipo do processo</legend>
          <Opcoes rotulo="Tipo do processo" valor={tipo} aoMudar={setTipo}
            opcoes={(Object.keys(TIPOS) as Tipo[]).map((t) => ({ valor: t, titulo: TIPOS[t].texto, explicacao: TIPOS[t].explicacao }))} />
          {erros.tipo && <p className="text-xs text-destructive">{erros.tipo}</p>}
        </fieldset>
        <Campo rotulo="Número da cota" erro={erros.numeroCota}>
          <Input name="numeroCota" maxLength={30} placeholder="ex.: COTA-123" autoComplete="off" value={numeroCota} onChange={(e) => setNumeroCota(e.target.value)} />
        </Campo>
        <Campo rotulo="Valor a pagar (R$)" erro={erros.valor} ajuda={TEXTOS.ajudaDoValor}>
          <Input name="valor" inputMode="decimal" placeholder="ex.: 80.000,00" value={valor} onChange={(e) => setValor(e.target.value)} />
        </Campo>
        {mensagem && <Aviso tipo="erro">{mensagem}</Aviso>}
        <div className="flex gap-2">
          <Button type="submit" disabled={enviando}>Abrir processo</Button>
          <Button asChild variant="outline"><a href="#/">Cancelar</a></Button>
        </div>
      </form>
    </Cartao>
  );
}
