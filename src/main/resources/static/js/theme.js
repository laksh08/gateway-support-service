const PortalTheme = (() => {
    const KEY = 'portal-theme';

    function apply(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem(KEY, theme);
        const btn = document.getElementById('theme-toggle');
        if (btn) {
            btn.textContent = theme === 'dark' ? 'Light Mode' : 'Dark Mode';
        }
    }

    function init() {
        const saved = localStorage.getItem(KEY) || 'light';
        apply(saved);
        document.getElementById('theme-toggle')?.addEventListener('click', () => {
            const current = document.documentElement.getAttribute('data-theme');
            apply(current === 'dark' ? 'light' : 'dark');
        });
    }

    return { init };
})();
