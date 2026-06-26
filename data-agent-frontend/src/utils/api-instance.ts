import { Api, OpenAPI } from '@/apis/__generated'

OpenAPI.BASE = import.meta.env.VITE_API_PREFIX ?? ''

export const api = new Api()
