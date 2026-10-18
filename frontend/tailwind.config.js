// White "Liquid Glass" palette. The existing slate/red/emerald/amber text classes are remapped to
// light-theme values, so every page keeps working without class-by-class edits.
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#f4f6fb', panel: 'rgba(255,255,255,0.62)', line: 'rgba(20,22,28,0.09)', brand: '#0a84ff', ink: '#14161c',
        slate: { 100: '#14161c', 200: '#262b36', 300: '#4a5160', 400: '#6b7383', 500: '#8a91a1' },
        emerald: { 300: '#157f4a', 400: '#1a9c5b' },
        red: { 300: '#b42323', 400: '#d93a3a' },
        amber: { 200: '#7a4a00', 300: '#a15c00' },
        sky: { 200: '#075985', 300: '#0369a1' },
        indigo: { 300: '#0a6fe0' },
      },
      fontFamily: { sans: ['-apple-system', 'BlinkMacSystemFont', '"SF Pro Display"', '"SF Pro Text"', 'Inter', 'system-ui', 'sans-serif'] },
    },
  },
  plugins: [],
}
