/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          green: '#00D166',
          yellow: '#FFDF00',
          darkBg: '#090B0E',
          cardBg: '#12161F',
          border: '#1E2533',
          red: '#FF3B30',
        }
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
