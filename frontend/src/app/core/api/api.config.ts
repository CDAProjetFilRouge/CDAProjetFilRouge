interface AppConfig {
  apiUrl?: string;
}

const config = (globalThis as { APP_CONFIG?: AppConfig }).APP_CONFIG ?? {};

export const URL_BACKEND = config.apiUrl || 'http://localhost:8080';
export const URL_IMAGES = URL_BACKEND;
