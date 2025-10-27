/**
 * ChatKeep - Language Management
 * Handles language switching with localStorage and server sync
 */

// Initialize language on page load
document.addEventListener('DOMContentLoaded', function() {
    initializeLanguage();
});

function initializeLanguage() {
    const languageOptions = document.querySelectorAll('.language-option');

    languageOptions.forEach(option => {
        option.addEventListener('click', function() {
            const language = this.dataset.language;
            setLanguage(language);
        });
    });

    // Highlight active language based on current page locale
    const currentLanguage = getCurrentLanguage();
    highlightActiveLanguage(currentLanguage);
}

/**
 * Change language by reloading page with ?lang= parameter
 */
function setLanguage(language) {
    // Save to localStorage
    localStorage.setItem('language', language);

    // Close settings dropdown
    const settingsDropdown = document.getElementById('settings-dropdown');
    if (settingsDropdown) {
        settingsDropdown.classList.add('hidden');
    }

    // Sync to server for authenticated users (async, no need to wait)
    syncLanguageToServer(language);

    // Reload page with lang parameter to change locale
    // This triggers Spring's LocaleChangeInterceptor
    const url = new URL(window.location.href);
    url.searchParams.set('lang', language);
    window.location.href = url.toString();
}

/**
 * Get current language from page's <html lang="..."> attribute
 * Falls back to localStorage, then 'en'
 */
function getCurrentLanguage() {
    // Try to get from HTML lang attribute (set by server)
    const htmlLang = document.documentElement.lang;
    if (htmlLang) {
        // Convert HTML lang format (en, zh-TW, zh-CN) to our format
        return htmlLang.replace('-', '_');
    }

    // Fallback to localStorage
    return localStorage.getItem('language') || 'en';
}

/**
 * Highlight the active language option
 */
function highlightActiveLanguage(language) {
    const languageOptions = document.querySelectorAll('.language-option');

    languageOptions.forEach(option => {
        if (option.dataset.language === language) {
            option.classList.add('bg-primary-50', 'dark:bg-primary-900', 'text-primary-600', 'dark:text-primary-400');

            // Add checkmark icon
            const existingCheck = option.querySelector('.fa-check');
            if (!existingCheck) {
                const checkIcon = document.createElement('i');
                checkIcon.className = 'fas fa-check ml-auto';
                option.appendChild(checkIcon);
            }
        } else {
            option.classList.remove('bg-primary-50', 'dark:bg-primary-900', 'text-primary-600', 'dark:text-primary-400');

            // Remove checkmark icon
            const checkIcon = option.querySelector('.fa-check');
            if (checkIcon) {
                checkIcon.remove();
            }
        }
    });
}

/**
 * Sync language preference to server
 * Server automatically handles both authenticated and anonymous users via cookies
 */
function syncLanguageToServer(language) {
    // Check if user is authenticated
    fetch('/api/v1/user/me')
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            throw new Error('Not authenticated');
        })
        .then(data => {
            if (data.success && data.data && data.data.userType === 'AUTHENTICATED') {
                // User is authenticated, sync language preference to database
                return fetch('/api/v1/user/preferences', {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        preferredLanguage: language
                    })
                });
            }
        })
        .then(response => {
            if (response && response.ok) {
                console.log('Language preference synced to server:', language);
            }
        })
        .catch(err => console.log('Language sync skipped (anonymous user or error):', err.message));
}
