import type { Plugin } from 'vite'
import { defineConfig } from 'vite'
import preact from '@preact/preset-vite'
import type { Adapter } from 'vite-plugin-mix'
import mixPlugin from 'vite-plugin-mix'

interface MixConfig {
  handler: string
  adapter?: Adapter | undefined
}

type MixPlugin = (config: MixConfig) => Plugin

interface Mix {
  default: MixPlugin
}

// Make mix plugin works with latest vite version https://github.com/egoist/vite-plugin-mix/issues/33#issuecomment-1255778587
const mix = (mixPlugin as unknown as Mix).default

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    preact(),
    mix({
      handler: './server/index.ts',
    }),
  ],
})
