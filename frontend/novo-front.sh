#!/usr/bin/env bash
# Cria um front novo a partir da POC (frontend/poc), pronto para a primeira jornada:
#
#   frontend/novo-front.sh <pasta-nova> <contrato> [nome]
#
#   <contrato>  o OpenAPI do serviço: um arquivo (ex.: ../meu-servico/docs/api/openapi-base.json) ou uma URL
#               (ex.: http://localhost:8080/q/openapi?format=json)
#   [nome]      o nome que aparece na barra lateral (padrão: "Protótipo")
#
# O que sai:
#   - src/: o núcleo, o catálogo de componentes e a moldura (iguais aos da POC), e um esqueleto do projeto
#     (jornada, simulado, telas Início e "Página não encontrada") que já passa no `npm run verificar`;
#   - modelo/: as telas, a jornada, o simulado e os specs da POC, SÓ PARA CONSULTA (fora do build e do lint):
#     é o "vizinho" que o agente copia;
#   - .claude/skills (ui-jornada e react-jornada), AGENTS.md e CLAUDE.md: o front funciona com
#     agentes mesmo fora deste repositório.
set -euo pipefail

DESTINO=${1:?"uso: novo-front.sh <pasta-nova> <contrato> [nome]"}
CONTRATO=${2:?"uso: novo-front.sh <pasta-nova> <contrato> [nome]"}
NOME=${3:-Protótipo}
AQUI=$(cd "$(dirname "$0")" && pwd)
POC="$AQUI/poc"

if [ -e "$DESTINO" ] && [ -n "$(ls -A "$DESTINO" 2>/dev/null)" ]; then
  echo "A pasta $DESTINO já existe e não está vazia." >&2
  exit 1
fi
mkdir -p "$DESTINO"
DESTINO=$(cd "$DESTINO" && pwd)

# O contrato, como o contrato.config.json do projeto novo vai citá-lo: URL como está; arquivo, relativo ao projeto.
if [[ "$CONTRATO" =~ ^https?:// ]]; then
  ORIGEM="$CONTRATO"
else
  [ -f "$CONTRATO" ] || { echo "Contrato não encontrado: $CONTRATO" >&2; exit 1; }
  ORIGEM=$(python3 -c 'import os,sys; print(os.path.relpath(os.path.abspath(sys.argv[1]), sys.argv[2]))' "$CONTRATO" "$DESTINO")
fi

# 1. A POC, sem o que é da jornada de exemplo e sem o que é gerado.
(cd "$POC" && tar cf - --exclude=node_modules --exclude=dist --exclude=test-results --exclude=playwright-report \
  --exclude=./src/telas --exclude=./src/jornadas --exclude=./src/simulado --exclude=./src/api \
  --exclude='./e2e/j*.spec.ts' --exclude=./e2e/passos-processo.ts --exclude=./contrato --exclude=./README.md .) | (cd "$DESTINO" && tar xf -)

# 2. modelo/: o vizinho para copiar (só leitura).
mkdir -p "$DESTINO/modelo/src" "$DESTINO/modelo/e2e" "$DESTINO/modelo/contrato"
cp -r "$POC/src/telas" "$POC/src/jornadas" "$POC/src/simulado" "$DESTINO/modelo/src/"
mkdir -p "$DESTINO/modelo/src/api" && cp "$POC/src/api/processos.ts" "$DESTINO/modelo/src/api/"
cp "$POC"/e2e/j*.spec.ts "$POC/e2e/passos-processo.ts" "$DESTINO/modelo/e2e/"
cp "$POC/contrato/proposto.yaml" "$DESTINO/modelo/contrato/"
cat > "$DESTINO/modelo/README.md" <<'EOF'
# Modelo (só para consulta)

As telas, a jornada, o serviço simulado e os specs da POC de referência (o processo de pagamento). **Não fazem parte
deste front**: estão fora do build, do typecheck e do lint. Servem de vizinho para copiar, como manda a skill
`react-jornada`: lista (`src/telas/Inicio.tsx`, `src/telas/ListaProcessos.tsx`), formulário (`AbrirProcesso.tsx`),
detalhe com ações (`Processo.tsx`), assistente com envio de arquivo (`AnexarDocumento.tsx`), jornada
(`src/jornadas/processo-pagamento.ts`), simulado (`src/simulado/servicoFalso.ts`), api (`src/api/processos.ts`),
contrato proposto (`contrato/proposto.yaml`) e specs (`e2e/`).

Copie a estrutura, não os textos nem os nomes do negócio de lá.
EOF

# 3. O esqueleto do projeto.
mkdir -p "$DESTINO/src/telas" "$DESTINO/src/jornadas" "$DESTINO/src/simulado" "$DESTINO/src/api" "$DESTINO/contrato"
cat > "$DESTINO/contrato.config.json" <<EOF
{
  "\$comentario": "De onde vem o contrato da API. 'servico': o OpenAPI publicado pelo backend (arquivo ou URL). 'proposto': o que as telas usam e o backend ainda não tem (telas primeiro).",
  "servico": "$ORIGEM",
  "proposto": "contrato/proposto.yaml"
}
EOF
cat > "$DESTINO/contrato/proposto.yaml" <<'EOF'
# CONTRATO PROPOSTO: as operações que as telas usam e o serviço ainda NÃO tem ("telas primeiro").
# Siga as regras do contrato real (skill arquiteto-padrao, contrato-openapi.md). Exemplo completo: modelo/contrato/proposto.yaml.
openapi: 3.1.0
info:
  title: Proposta das telas
  version: proposta
paths: {}
components:
  schemas: {}
EOF
cat > "$DESTINO/src/api/cliente.ts" <<'EOF'
// O cliente do serviço, tipado pelo contrato (esquema.d.ts, gerado por `npm run gerar:api`). Crie uma função por
// endpoint em src/api/<recurso>.ts, como em modelo/src/api/processos.ts. É o único lugar com caminho de endpoint.
import { criarCliente } from "@/nucleo/http";
import type { paths } from "./esquema";
import { PROPOSTAS_PENDENTES } from "./propostas.gerado";

export const api = criarCliente<paths>();

/** A operação do contrato proposto ainda não foi publicada pelo serviço? (use com proposta() do núcleo) */
export const pendente = (operacao: string) => PROPOSTAS_PENDENTES.has(operacao);
EOF
cat > "$DESTINO/src/jornadas/jornada.ts" <<'EOF'
// O desenho da jornada: termos do negócio para cada código da API, caminhos, personas e textos. É DADO: sem condição,
// sem valor de regra. Exemplo completo: modelo/src/jornadas/processo-pagamento.ts.

/** Quem usa o protótipo (troque pelas pessoas da jornada). O `usuario` é o que vai para o serviço como identidade. */
export const PERSONAS = [
  { usuario: "ana", nome: "Ana", papel: "analista" },
] as const;

/** Textos de ajuda da jornada. */
export const TEXTOS = {
  boasVindas: "Comece pela primeira jornada: descreva-a, escreva o spec e monte a tela (skill react-jornada).",
};
EOF
cat > "$DESTINO/src/simulado/servicoFalso.ts" <<'EOF'
// O serviço simulado (modo `npm run dev:falso` e os e2e): as mesmas rotas, status, mensagens e regras do serviço, em
// memória. É CÓPIA: quem está certo é o serviço (`npm run test:e2e:real` acusa a divergência).
// Exemplo completo: modelo/src/simulado/servicoFalso.ts.
import type { RotaSimulada } from "@/nucleo/simulado";

export const rotasDoServicoFalso: RotaSimulada[] = [];
EOF
cat > "$DESTINO/src/telas/contexto.ts" <<'EOF'
// O que o navegador lembra entre telas: quem sou eu (a persona). Estado de apresentação, não desenho nem regra.
import { PERSONAS } from "@/jornadas/jornada";
import { guardado } from "@/nucleo/guardado";

export const persona = guardado<string>("persona", PERSONAS[0].usuario);

/** A persona escolhida em "Quem sou eu" (e a tela se redesenha quando ela muda). */
export function useEu() {
  const usuario = persona.useValor();
  return PERSONAS.find((p) => p.usuario === usuario) ?? PERSONAS[0];
}
EOF
cat > "$DESTINO/src/telas/Inicio.tsx" <<'EOF'
import { Cartao } from "@/componentes";
import { TEXTOS } from "@/jornadas/jornada";

/** A tela inicial. Troque pelo começo da primeira jornada. */
export function Inicio() {
  return (
    <Cartao titulo="Comece por aqui">
      <p className="text-sm text-muted-foreground">{TEXTOS.boasVindas}</p>
    </Cartao>
  );
}
EOF
cat > "$DESTINO/src/App.tsx" <<EOF
import { Home } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { Cartao, Moldura, SeletorNativo } from "@/componentes";
import { PERSONAS } from "@/jornadas/jornada";
import { useRota } from "@/nucleo/rotas";
import { persona, useEu } from "@/telas/contexto";
import { Inicio } from "@/telas/Inicio";

/** Rotas e moldura. Tela nova: um caso aqui e, se for de primeiro nível, um item no menu (como em modelo/). */
export function App() {
  const { partes } = useRota();
  const eu = useEu();

  let tela: { titulo: string; trilha: { texto: string; href?: string }[]; corpo: ReactNode };
  if (partes.length === 0) {
    tela = { titulo: "Início", trilha: [{ texto: "Início" }], corpo: <Inicio /> };
  } else {
    tela = { titulo: "Página não encontrada", trilha: [{ texto: "Início", href: "#/" }, { texto: "Página não encontrada" }], corpo: <NaoEncontrada /> };
  }
  const titulo = tela.titulo;
  useEffect(() => {
    document.title = \`\${titulo} · $NOME\`;
  }, [titulo]);

  return (
    <Moldura sigla="$(echo "$NOME" | cut -c1-2 | tr '[:lower:]' '[:upper:]')" nome="$NOME" titulo={tela.titulo} trilha={tela.trilha}
      menu={[{ texto: "Início", href: "#/", ativo: partes.length === 0, icone: <Home className="size-4" aria-hidden /> }]}
      rodape={
        <label className="flex items-center gap-3 rounded-lg border bg-background p-2.5">
          <span aria-hidden className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold">
            {eu.nome.slice(0, 2).toUpperCase()}
          </span>
          <span className="min-w-0 flex-1">
            <span className="block text-xs text-muted-foreground">Quem sou eu</span>
            <SeletorNativo aria-label="Quem sou eu" value={eu.usuario} onChange={(e) => persona.gravar(e.target.value)} className="border-0 px-0">
              {PERSONAS.map((p) => <option key={p.usuario} value={p.usuario} className="bg-popover">{p.nome} · {p.papel}</option>)}
            </SeletorNativo>
          </span>
        </label>
      }>
      {tela.corpo}
    </Moldura>
  );
}

function NaoEncontrada() {
  return (
    <Cartao>
      <p className="text-sm text-muted-foreground">Esta página não existe. O endereço pode estar errado ou ter mudado.</p>
      <a href="#/" className="mt-3 inline-block text-sm font-medium text-andamento underline-offset-4 hover:underline">Voltar ao início</a>
    </Cartao>
  );
}
EOF
cat > "$DESTINO/e2e/j00-inicio.spec.ts" <<'EOF'
// O esqueleto no ar: a tela inicial, a página que não existe, e a acessibilidade. Cada jornada ganha o seu
// e2e/jNN-<jornada>.spec.ts, escrito a partir da especificação ANTES da tela (modelo: modelo/e2e/).
import { expect, semProblemasDeAcessibilidade, test } from "./apoio";

test("[I-01] A tela inicial abre, sem problema de acessibilidade", async ({ page }) => {
  await expect(page.getByRole("heading", { name: "Início", level: 1 })).toBeVisible();
  await semProblemasDeAcessibilidade(page);
});

test("[I-02] Endereço que não existe mostra 'Página não encontrada'", async ({ page }) => {
  await page.goto("./#/nao-existe");
  await expect(page.getByRole("heading", { name: "Página não encontrada", level: 1 })).toBeVisible();
  await page.getByRole("link", { name: "Voltar ao início" }).click();
  await expect(page.getByRole("heading", { name: "Início", level: 1 })).toBeVisible();
});
EOF

# 4. O projeto fica de pé sozinho: a verificação de arquitetura vem junto, e modelo/ fica fora de tudo.
cp "$AQUI/../.claude/skills/ui-jornada/scripts/verificar.sh" "$DESTINO/scripts/verificar-arquitetura.sh"
python3 - "$DESTINO" "$(basename "$DESTINO")" <<'EOF'
import json, re, sys
destino, nome = sys.argv[1], sys.argv[2]
p = f"{destino}/package.json"; pkg = json.load(open(p))
pkg["name"] = re.sub(r"[^a-z0-9-]", "-", nome.lower())
pkg["scripts"]["arquitetura"] = 'UI_TELAS="src/telas src/App.tsx" bash scripts/verificar-arquitetura.sh .'
json.dump(pkg, open(p, "w"), indent=2)
p = f"{destino}/tsconfig.json"; s = open(p).read()
s = s.replace('"include": [', '"exclude": ["modelo"],\n  "include": [', 1)
open(p, "w").write(s)
p = f"{destino}/eslint.config.js"; s = open(p).read()
s = s.replace('{ ignores: ["dist",', '{ ignores: ["modelo", ".github", ".claude", "dist",', 1)
open(p, "w").write(s)
EOF

# 5. As skills e as instruções para agentes.
# O front novo é um projeto avulso: leva as suas cópias das skills (no repositório, elas ficam em Agents/.claude/skills).
mkdir -p "$DESTINO/.claude/skills"
cp -r "$AQUI/../.claude/skills/ui-jornada" "$AQUI/../.claude/skills/react-jornada" "$DESTINO/.claude/skills/"
sed -e 's|A referência de tudo é a POC em `poc/`.|O modelo para copiar está em `modelo/` (as telas, a jornada, o simulado e os specs da POC de referência), fora do build.|' \
    -e 's|abra o modelo na POC (a `react-jornada` diz qual)|abra o modelo em `modelo/` (a `react-jornada` diz qual; onde ela diz `frontend/poc`, leia `modelo/`)|' \
    -e 's|ficam em `../.claude/skills/`, na raiz do repositório: abra a sessão do agente lá|ficam em `.claude/skills/` deste projeto|' \
    "$AQUI/AGENTS.md" > "$DESTINO/AGENTS.md"
echo '@AGENTS.md' > "$DESTINO/CLAUDE.md"

# 6. O harness do agente (fim de turno com a verificação, arquivos protegidos), com os caminhos do front novo.
mkdir -p "$DESTINO/.github/hooks"
cp "$AQUI"/.github/hooks/*.sh "$AQUI/.github/hooks/harness.json" "$AQUI/.github/hooks/README.md" "$DESTINO/.github/hooks/"
sed -e 's|^VERIFICACAO="cd poc \&\& npm run -s verificar"|VERIFICACAO="npm run -s verificar"|' \
    -e 's|\^poc/|^|g' \
    -e 's|^\^\\.github/hooks/$|&\n^scripts/verificar-arquitetura\\.sh$\n^\\.claude/skills/|' \
    "$AQUI/.github/hooks/harness.conf" > "$DESTINO/.github/hooks/harness.conf"
cp "$AQUI/.claude/settings.json" "$DESTINO/.claude/settings.json"
cat > "$DESTINO/README.md" <<EOF
# $NOME

Front de rascunho e POC das jornadas, criado a partir da POC de referência (\`frontend/poc\`) com \`novo-front.sh\`.
Contrato do serviço: \`$ORIGEM\` (em \`contrato.config.json\`).

\`\`\`bash
npm ci
npm run gerar:api     # os tipos do contrato
npm run verificar     # contrato, typecheck, lint, arquitetura, testes de unidade
npm run dev:falso     # as telas com o serviço simulado
npm run test:e2e      # os percursos no navegador
\`\`\`

Próximo passo: a primeira jornada (skill \`react-jornada\`; o modelo está em \`modelo/\`).
EOF

echo "Front criado em $DESTINO (contrato: $ORIGEM)."
echo "Agora: cd $DESTINO && npm ci && npm run gerar:api && npm run verificar"
