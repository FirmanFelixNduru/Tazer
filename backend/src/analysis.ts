export interface TaskInput {
  title: string;
  course: string;
  description: string;
  deadlineAt: string;
}

export interface Analysis {
  difficultyScore: number;
  estimatedMinutes: number;
}

export const analysisSchema = {
  type: "object",
  additionalProperties: false,
  required: ["difficultyScore", "estimatedMinutes"],
  properties: {
    difficultyScore: { type: "integer", minimum: 1, maximum: 5 },
    estimatedMinutes: { type: "integer", minimum: 1, maximum: 10080 },
  },
};

export function object(value: unknown): Record<string, unknown> | undefined {
  return typeof value === "object" && value !== null && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : undefined;
}

export function isUtcTimestamp(value: unknown): value is string {
  if (typeof value !== "string") return false;
  const match = /^(\d{4})-(\d{2})-(\d{2})t(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d+)?(?:z|\+00:00)$/i.exec(value);
  if (!match || match[0] !== value) return false;
  const time = new Date(value);
  return Number.isFinite(time.getTime()) && time.getUTCDate() === Number(match[3]);
}

export function taskInput(value: unknown): TaskInput | undefined {
  const fields = object(value);
  if (!fields || Object.keys(fields).length !== 4) return;
  const { title, course, description, deadlineAt } = fields;
  if (
    typeof title !== "string" || typeof course !== "string" ||
    typeof description !== "string" || !isUtcTimestamp(deadlineAt)
  ) return;
  const trimmedTitle = title.trim();
  const trimmedCourse = course.trim();
  if (
    trimmedTitle.length === 0 || [...trimmedTitle].length > 120 ||
    trimmedCourse.length === 0 || [...trimmedCourse].length > 120 ||
    [...description].length > 4000
  ) return;
  return { title: trimmedTitle, course: trimmedCourse, description, deadlineAt };
}

export function analysisOutput(value: unknown): Analysis | undefined {
  const fields = object(value);
  if (!fields || Object.keys(fields).length !== 2) return;
  const { difficultyScore, estimatedMinutes } = fields;
  if (
    !integerBetween(difficultyScore, 1, 5) ||
    !integerBetween(estimatedMinutes, 1, 10080)
  ) return;
  return { difficultyScore, estimatedMinutes };
}

export function integerBetween(value: unknown, min: number, max: number): value is number {
  return typeof value === "number" && Number.isInteger(value) && value >= min && value <= max;
}
