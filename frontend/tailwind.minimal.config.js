import tailwindcssAnimate from 'tailwindcss-animate';

export default {
  content: ['./src/**/*.{html,js,svelte,ts}'],
  theme: {
    extend: {
      colors: {
        primary: 'hsl(var(--primary) / <alpha-value>)'
      }
    }
  },
  plugins: [tailwindcssAnimate]
};
