import { createNodeServer, loopback } from "./node.js";

try {
  const port = Number(process.env.PORT ?? "8787");
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error("Invalid port");
  const server = createNodeServer(process.env);
  server.on("error", () => {
    console.error("Tazer backend could not listen.");
    process.exitCode = 1;
  });
  server.listen(port, loopback, () => console.log(`Tazer backend listening on http://${loopback}:${port}`));
} catch {
  console.error("Tazer backend configuration is invalid.");
  process.exitCode = 1;
}
