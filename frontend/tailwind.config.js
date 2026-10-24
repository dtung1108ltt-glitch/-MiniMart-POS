/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'Segoe UI', 'Roboto', 'system-ui', 'sans-serif'],
      },
      colors: {
        primary: {
          DEFAULT: 'var(--color-primary)',
          hover: 'var(--color-primary-hover)',
          soft: 'var(--color-primary-soft)',
        },
        surface: 'var(--color-surface)',
        appbg: 'var(--color-bg)',
        bordercol: 'var(--color-border)',
        textmain: 'var(--color-text)',
        textmuted: 'var(--color-text-muted)',
        sidebar: 'var(--color-sidebar)',
        sidebartext: 'var(--color-sidebar-text)',
        success: '#16A34A',
        warning: '#F59E0B',
        danger: '#DC2626',
        info: '#0891B2',
      },
      borderRadius: {
        card: '12px',
        control: '8px',
        badge: '999px',
      },
    },
  },
  plugins: [],
};
