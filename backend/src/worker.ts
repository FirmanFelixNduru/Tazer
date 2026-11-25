import { realHandler } from "./config.js";
import type { Env } from "./config.js";

export default {
  fetch(request: Request, env: Env) {
    return realHandler(env)(request);
  },
};
