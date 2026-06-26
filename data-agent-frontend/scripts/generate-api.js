/* eslint-env node */
import { mkdtemp, readdir, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

import { generate } from 'openapi-typescript-codegen'

const DEFAULT_OPENAPI_URL = 'http://localhost:9933/v3/api-docs'
const GENERATED_PATH = path.resolve('src/apis/__generated')
const AGENT_METHOD_NAME = 'agentJson'
const AGENT_CARD_PATH = '/.well-known/agent-card.json'

export async function resolveOpenApiInput(openApiUrl) {
  if (!/^https?:\/\//i.test(openApiUrl)) {
    return {
      input: openApiUrl,
      cleanup: async () => {},
    }
  }

  let response
  try {
    response = await fetch(openApiUrl)
  } catch (error) {
    throw new Error(`Failed to download OpenAPI document from ${openApiUrl}`, { cause: error })
  }
  if (!response.ok) {
    throw new Error(`Failed to download OpenAPI document: ${response.status} ${response.statusText}`)
  }

  const tempPath = await mkdtemp(path.join(tmpdir(), 'springdoc-openapi-'))
  const input = path.join(tempPath, 'openapi.json')
  await writeFile(input, await response.text(), 'utf8')

  return {
    input,
    cleanup: async () => {
      await rm(tempPath, { force: true, recursive: true })
    },
  }
}

export async function findServiceWithMethod(generatedPath, methodName) {
  const servicesPath = path.join(generatedPath, 'services')
  const entries = await readdir(servicesPath, { withFileTypes: true })

  for (const entry of entries) {
    if (!entry.isFile() || !entry.name.endsWith('.ts')) {
      continue
    }

    const servicePath = path.join(servicesPath, entry.name)
    const source = await readFile(servicePath, 'utf8')
    const methodPattern = new RegExp(`\\bstatic\\s+${methodName}\\s*\\(`)

    if (!methodPattern.test(source)) {
      continue
    }

    const classMatch = source.match(/export\s+class\s+([A-Za-z0-9_]+)/)
    if (!classMatch) {
      throw new Error(`Found ${methodName} in ${servicePath}, but could not find its service class`)
    }

    return {
      className: classMatch[1],
      importPath: `./services/${path.basename(entry.name, '.ts')}`,
    }
  }

  throw new Error(`Could not find generated service method "${methodName}" in ${servicesPath}`)
}

export function buildApiCompatSource() {
  return [
    "import { OpenAPI } from './core/OpenAPI'",
    "import { request as __request } from './core/request'",
    '',
    'type LegacyRequestExecutor = (request: {',
    '  uri: string',
    '  method: string',
    '  body?: unknown',
    '}) => unknown',
    '',
    'export class Api {',
    '  constructor(_request?: LegacyRequestExecutor) {}',
    '',
    '  readonly a2acontroller = {',
    `    ${AGENT_METHOD_NAME}: () => __request(OpenAPI, { method: 'GET', url: '${AGENT_CARD_PATH}' }),`,
    '  }',
    '}',
    '',
  ].join('\n')
}

async function writeApiCompat(generatedPath) {
  await findServiceWithMethod(generatedPath, AGENT_METHOD_NAME)
  const compatPath = path.join(generatedPath, 'Api.ts')
  const indexPath = path.join(generatedPath, 'index.ts')
  const indexSource = await readFile(indexPath, 'utf8')

  await writeFile(compatPath, buildApiCompatSource(), 'utf8')

  if (!indexSource.includes("export { Api } from './Api'")) {
    await writeFile(indexPath, `${indexSource.trimEnd()}\nexport { Api } from './Api'\n`, 'utf8')
  }
}

export async function generateApi() {
  const openApiUrl = process.env.OPENAPI_URL || DEFAULT_OPENAPI_URL
  const openApiInput = await resolveOpenApiInput(openApiUrl)

  console.log(`Generating API client from ${openApiUrl}...`)
  try {
    await rm(GENERATED_PATH, { force: true, recursive: true })
    await generate({
      input: openApiInput.input,
      output: GENERATED_PATH,
      httpClient: 'fetch',
      useOptions: true,
      useUnionTypes: true,
    })
    await writeApiCompat(GENERATED_PATH)
    console.log(`API client generated at ${GENERATED_PATH}`)
  } finally {
    await openApiInput.cleanup()
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  generateApi().catch((error) => {
    console.error(error)
    process.exitCode = 1
  })
}
