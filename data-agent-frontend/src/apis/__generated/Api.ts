import { OpenAPI } from './core/OpenAPI'
import { request as __request } from './core/request'

type LegacyRequestExecutor = (request: {
  uri: string
  method: string
  body?: unknown
}) => unknown

export class Api {
  constructor(_request?: LegacyRequestExecutor) {}

  readonly a2acontroller = {
    agentJson: () => __request(OpenAPI, { method: 'GET', url: '/.well-known/agent-card.json' }),
  }
}
