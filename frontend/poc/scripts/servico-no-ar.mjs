// Antes do e2e contra o serviço real: ele responde? Se não, diz como subir, em vez de 20 testes falhando por timeout.
const url = process.env.VITE_API ?? "http://localhost:8080";
try {
  await fetch(url, { signal: AbortSignal.timeout(5000) });
  console.log(`Serviço no ar: ${url}`);
} catch {
  console.error(`O serviço não responde em ${url}.`);
  console.error("  Suba-o em modo dev (ex.: cd ../../exemplo-testes && ./mvnw quarkus:dev) ou aponte VITE_API para ele.");
  process.exit(1);
}
