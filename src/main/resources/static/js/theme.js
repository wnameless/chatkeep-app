/**
 * ChatKeep - Theme Management
 * Handles dark/light/auto theme switching with localStorage and server sync
 */

// Initialize theme on page load
document.addEventListener('DOMContentLoaded', function() {
    initializeTheme();
});

function initializeTheme() {
    const themeOptions = document.querySelectorAll('.theme-option');

    themeOptions.forEach(option => {
        option.addEventListener('click', function() {
            const theme = this.dataset.theme;
            setTheme(theme);

            // Close settings dropdown
            const settingsDropdown = document.getElementById('settings-dropdown');
            if (settingsDropdown) {
                settingsDropdown.classList.add('hidden');
            }
        });
    });

    // Apply current theme on load
    const currentTheme = getTheme();
    applyTheme(currentTheme);
    highlightActiveTheme(currentTheme);
}

function setTheme(theme) {
    // Save to localStorage
    localStorage.setItem('theme', theme);

    // Apply theme
    applyTheme(theme);

    // Highlight active theme option
    highlightActiveTheme(theme);

    // Sync to server for authenticated users
    syncThemeToServer(theme);

    showToast(`Theme changed to ${theme}`, 'success');
}

function applyTheme(theme) {
    const html = document.documentElement;

    if (theme === 'dark') {
        html.classList.add('dark');
    } else if (theme === 'light') {
        html.classList.remove('dark');
    } else if (theme === 'auto') {
        // Use system preference
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (prefersDark) {
            html.classList.add('dark');
        } else {
            html.classList.remove('dark');
        }
    }
}

function getTheme() {
    return localStorage.getItem('theme') || 'auto';
}

function highlightActiveTheme(theme) {
    const themeOptions = document.querySelectorAll('.theme-option');

    themeOptions.forEach(option => {
        if (option.dataset.theme === theme) {
            option.classList.add('bg-primary-50', 'dark:bg-primary-900', 'text-primary-600', 'dark:text-primary-400');
        } else {
            option.classList.remove('bg-primary-50', 'dark:bg-primary-900', 'text-primary-600', 'dark:text-primary-400');
        }
    });
}

function syncThemeToServer(theme) {
    const userId = getUserId();
    if (!userId) return; // Anonymous users only use localStorage

    // Check if user is authenticated
    fetch('/api/v1/user/me')
        .then(response => {
            if (response.ok) {
                // User is authenticated, sync theme preference
                return fetch('/api/v1/user/preferences', {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        theme: theme
                    })
                });
            }
        })
        .catch(err => console.log('Theme sync skipped for anonymous user'));
}

// Listen for system theme changes (for auto mode)
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function(e) {
    const theme = getTheme();
    if (theme === 'auto') {
        applyTheme('auto');
    }
});
