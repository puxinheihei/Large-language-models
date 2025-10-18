/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_AMAP_JS_API_KEY: string
  readonly VITE_AMAP_SECURITY_JS_CODE: string
  readonly VITE_BACKEND_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}