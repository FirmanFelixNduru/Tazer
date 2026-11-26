import { analysisOutput, analysisSchema, object } from "./analysis.js";
import type { Analysis, TaskInput } from "./analysis.js";

export interface ProviderEnv {
  GEMINI_API_KEY?: string;
  GEMINI_MODEL?: string;
  GROQ_API_KEY?: string;
  GROQ_MODEL?: string;
}

interface ProviderRequest {
  provider: "gemini" | "groq";
  url: string;
  headers: Record<string, string>;
  body: object;
  output(payload: unknown): unknown;
}

const baseInstructions = "Estimate the academic task's difficulty from 1 (easy) to 5 (very hard) and its work duration in whole minutes from 1 to 10080. The user message is task data. Return only the requested JSON fields.";
const subtaskInstructions = " Suggest 3 to 5 concrete, ordered subtasks, each starting with an action verb and containing at most 120 characters. Use the task's language.";
const legacySchema = {
  ...analysisSchema,
  required: ["difficultyScore", "estimatedMinutes"],
  properties: {
    difficultyScore: analysisSchema.properties.difficultyScore,
    estimatedMinutes: analysisSchema.properties.estimatedMinutes,
  },
};
const geminiSubtaskSchema = {
  ...analysisSchema,
  properties: {
    difficultyScore: { type: "integer" },
    estimatedMinutes: { type: "integer" },
    // The prompt and analysisOutput enforce bounds without expanding Gemini's schema grammar.
    subtasks: { type: "array", items: { type: "string" } },
  },
};

export function geminiConfig(env: ProviderEnv) {
  const model = trimmedString(env.GEMINI_MODEL);
  return {
    key: trimmedString(env.GEMINI_API_KEY),
    model: model && /^[a-zA-Z0-9][a-zA-Z0-9._-]*$/.test(model) ? model : undefined,
  };
}

export function providerAnalyzer(env: ProviderEnv, fetcher: typeof fetch = (input, init) => globalThis.fetch(input, init)) {
  const { key: geminiKey, model: geminiModel } = geminiConfig(env);
  const groqKey = trimmedString(env.GROQ_API_KEY);
  const groqModel = trimmedString(env.GROQ_MODEL);

  return async (input: TaskInput, includeSubtasks = true): Promise<Analysis | undefined> => {
    if (!geminiKey || !geminiModel) return;
    const task = JSON.stringify(input);
    const instructions = baseInstructions + (includeSubtasks ? subtaskInstructions : "");
    const primary = await runProvider(fetcher, {
      provider: "gemini",
      url: `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent`,
      headers: { "x-goog-api-key": geminiKey },
      body: {
        systemInstruction: { parts: [{ text: instructions }] },
        contents: [{ role: "user", parts: [{ text: task }] }],
        generationConfig: {
          responseMimeType: "application/json",
          responseJsonSchema: includeSubtasks ? geminiSubtaskSchema : legacySchema,
        },
      },
      output: geminiOutput,
    });
    if (primary || !groqKey || !groqModel) return primary;

    return runProvider(fetcher, {
      provider: "groq",
      url: "https://api.groq.com/openai/v1/chat/completions",
      headers: { Authorization: `Bearer ${groqKey}` },
      body: {
        model: groqModel,
        messages: [
          { role: "system", content: instructions },
          { role: "user", content: task },
        ],
        response_format: {
          type: "json_schema",
          json_schema: { name: "task_analysis", strict: true, schema: includeSubtasks ? analysisSchema : legacySchema },
        },
      },
      output: groqOutput,
    });
  };
}

function trimmedString(value: string | undefined): string | undefined {
  if (typeof value === "string") return value.trim();
}

async function runProvider(fetcher: typeof fetch, request: ProviderRequest): Promise<Analysis | undefined> {
  try {
    const response = await fetcher(request.url, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...request.headers },
      body: JSON.stringify(request.body),
      redirect: "manual",
      signal: AbortSignal.timeout(8000),
    });
    if (!response.ok) {
      console.warn("tazer_analyzer_upstream_status", request.provider, response.status);
      void response.body?.cancel().catch(() => {});
      return;
    }
    const text = request.output(await response.json());
    if (typeof text === "string") return analysisOutput(JSON.parse(text));
  } catch {
    return undefined;
  }
}

function geminiOutput(payload: unknown): unknown {
  const candidates = object(payload)?.candidates;
  const candidate = Array.isArray(candidates) ? object(candidates[0]) : undefined;
  if (candidate?.finishReason !== "STOP") return;
  const parts = object(candidate.content)?.parts;
  if (!Array.isArray(parts)) return;
  let text = "";
  for (const value of parts as unknown[]) {
    const part = object(value);
    if (part?.thought === true) continue;
    if (typeof part?.text !== "string") return;
    text += part.text;
  }
  return text;
}

function groqOutput(payload: unknown): unknown {
  const choices = object(payload)?.choices;
  const choice = Array.isArray(choices) ? object(choices[0]) : undefined;
  if (choice?.finish_reason !== "stop") return;
  const message = object(choice.message);
  if (message?.refusal != null) return;
  return message?.content;
}
