#!/usr/bin/env node
// O contrato da API que as telas usam = o OpenAPI do serviço + o contrato proposto (o que o serviço ainda não tem).
//   node scripts/contrato.mjs gerar      junta os dois e gera src/api/esquema.d.ts e src/api/propostas.gerado.ts
//   node scripts/contrato.mjs conferir   falha se os arquivos gerados não conferem com o contrato atual (API-001)
//   node scripts/contrato.mjs propostas  relatório: o que ainda é proposta e o que o serviço já publicou (igual ou não)
// As origens estão em contrato.config.json. Nada aqui é do projeto: em outro front, fica igual.
import { readFile, writeFile, mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import openapiTS, { astToString } from "openapi-typescript";
import ts from "typescript";
import { parse } from "yaml";

const RAIZ = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const METODOS = ["get", "post", "put", "patch", "delete"];

async function ler(origem) {
  const texto = /^https?:\/\//.test(origem) ? await (await fetch(origem)).text() : await readFile(resolve(RAIZ, origem), "utf8");
  return parse(texto); // YAML é superconjunto de JSON: lê os dois
}

async function carregar() {
  const config = JSON.parse(await readFile(join(RAIZ, "contrato.config.json"), "utf8"));
  const servico = await ler(config.servico);
  const proposto = config.proposto ? await ler(config.proposto) : { paths: {}, components: { schemas: {} } };
  return { servico, proposto };
}

/** Junta os dois documentos. Onde os dois definem a mesma operação ou schema, vale o do serviço. */
function juntar(servico, proposto) {
  const junto = structuredClone(servico);
  const pendentes = [];
  junto.paths ??= {};
  for (const [caminho, ops] of Object.entries(proposto.paths ?? {})) {
    junto.paths[caminho] ??= {};
    for (const metodo of METODOS) {
      if (!ops[metodo]) continue;
      if (junto.paths[caminho][metodo]) continue; // o serviço já tem: vale o dele
      junto.paths[caminho][metodo] = { ...ops[metodo], "x-proposta": true };
      pendentes.push(`${metodo.toUpperCase()} ${caminho}`);
    }
  }
  junto.components ??= {};
  junto.components.schemas ??= {};
  for (const [nome, schema] of Object.entries(proposto.components?.schemas ?? {})) junto.components.schemas[nome] ??= schema;
  return { junto, pendentes };
}

async function tipos(documento) {
  const ast = await openapiTS(documento, {
    propertiesRequiredByDefault: true,
    // Arquivo (format: binary) é Blob no navegador, não string.
    transform: (schema) => (schema.format === "binary" ? ts.factory.createTypeReferenceNode("Blob") : undefined),
  });
  return "// GERADO por `npm run gerar:api` a partir de contrato.config.json. Não edite à mão.\n" + astToString(ast);
}

function arquivoDePropostas(pendentes) {
  return `// GERADO por \`npm run gerar:api\`. As operações do contrato proposto que o serviço ainda não publicou.
// No modo real, a camada api não as chama (devolve 501 "ainda é proposta"); no modo simulado, o simulado atende.
export const PROPOSTAS_PENDENTES: ReadonlySet<string> = new Set(${JSON.stringify(pendentes.sort(), null, 2)});
`;
}

async function gerar(destino) {
  const { servico, proposto } = await carregar();
  const { junto, pendentes } = juntar(servico, proposto);
  await writeFile(join(destino, "esquema.d.ts"), await tipos(junto));
  await writeFile(join(destino, "propostas.gerado.ts"), arquivoDePropostas(pendentes));
}

const comando = process.argv[2];
if (comando === "gerar") {
  await gerar(join(RAIZ, "src/api"));
  console.log("Contrato gerado: src/api/esquema.d.ts e src/api/propostas.gerado.ts.");
} else if (comando === "conferir") {
  const temp = await mkdtemp(join(tmpdir(), "contrato-"));
  try {
    await gerar(temp);
    for (const arquivo of ["esquema.d.ts", "propostas.gerado.ts"]) {
      const [novo, atual] = await Promise.all([readFile(join(temp, arquivo), "utf8"), readFile(join(RAIZ, "src/api", arquivo), "utf8").catch(() => "")]);
      if (novo !== atual) {
        console.error(`API-001: src/api/${arquivo} está desatualizado em relação ao contrato (contrato.config.json).`);
        console.error("  Corrija: rode 'npm run gerar:api' e ajuste o que o typecheck apontar. Nunca edite os arquivos gerados à mão.");
        process.exit(1);
      }
    }
    console.log("Contrato da API ok: os arquivos gerados conferem com o serviço e com a proposta.");
  } finally {
    await rm(temp, { recursive: true, force: true });
  }
} else if (comando === "propostas") {
  const { servico, proposto } = await carregar();
  let publicadas = 0;
  for (const [caminho, ops] of Object.entries(proposto.paths ?? {})) {
    for (const metodo of METODOS) {
      if (!ops[metodo]) continue;
      const op = `${metodo.toUpperCase()} ${caminho}`;
      const real = servico.paths?.[caminho]?.[metodo];
      if (!real) { console.log(`PROPOSTA   ${op}  (o serviço ainda não tem)`); continue; }
      publicadas++;
      const status = (o) => Object.keys(o.responses ?? {}).sort().join(",");
      const params = (o) => (o.parameters ?? []).map((p) => `${p.in}:${p.name}`).sort().join(",");
      const iguais = status(real) === status(ops[metodo]) && params(real) === params(ops[metodo]);
      console.log(`${iguais ? "PUBLICADA " : "DIFERENTE "} ${op}  ${iguais ? "(pode sair do proposto)" : `(serviço: status ${status(real)}, parâmetros ${params(real)})`}`);
    }
  }
  if (publicadas) console.log("\nO que o serviço já publicou sai de contrato/proposto.yaml; ajuste a tela se ficou diferente.");
} else {
  console.error("uso: node scripts/contrato.mjs gerar | conferir | propostas");
  process.exit(2);
}
