import type { Dependencies, Session, Tier } from "./handler.js";

const tokens = new Map<string, Session>([
  ["demo-free", { uid: "demo-free" }],
  ["demo-pro", { uid: "demo-pro" }],
  ["demo-byok", { uid: "demo-byok" }],
  ["demo-unverified", "unverified"],
  ["demo-expired", "invalid"],
  ["demo-revoked", "invalid"],
]);

const tiers = new Map<string, Tier>([
  ["demo-pro", "PRO"],
  ["demo-byok", "BYOK"],
]);

export function mockDependencies(now = () => new Date()): Dependencies {
  const usage = new Map<string, { month: string; used: number }>();
  return {
    now,
    async verify(token) {
      return tokens.get(token) ?? "invalid";
    },
    async entitlement(uid, consume) {
      const date = new Date(now().getTime());
      const month = date.toISOString().slice(0, 7);
      date.setUTCDate(1);
      date.setUTCMonth(date.getUTCMonth() + 1);
      date.setUTCHours(0, 0, 0, 0);
      const resetsAt = date.toISOString().replace(".000Z", "Z");
      const tier = tiers.get(uid) ?? "FREE";
      const monthlyLimit = tier === "PRO" ? 100 : 0;
      const record = usage.get(uid);
      let used = record?.month === month ? record.used : 0;
      const granted = consume && used < monthlyLimit;
      if (granted) {
        used++;
        usage.set(uid, { month, used });
      }
      return { tier, monthlyLimit, remaining: monthlyLimit - used, resetsAt, granted };
    },
    async analyze() {
      return {
        difficultyScore: 4,
        estimatedMinutes: 150,
        subtasks: [
          "Catat hasil yang diminta pada instruksi tugas",
          "Buat draf untuk setiap bagian tugas",
          "Periksa draf dengan kriteria penilaian",
        ],
      };
    },
  };
}
