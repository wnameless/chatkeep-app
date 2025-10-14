/**
 * ChatKeep - Authentication Management
 * Simplified cookie-based authentication (httpOnly cookies managed by server)
 */

// Initialize auth on page load
document.addEventListener('DOMContentLoaded', function() {
    checkAuthStatus();
});

/**
 * Check authentication status by fetching user info from server.
 * Server automatically handles anonymous session via httpOnly cookie.
 */
function checkAuthStatus() {
    // No headers needed - browser automatically sends cookies
    fetch('/api/v1/user/me')
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            throw new Error('Not authenticated');
        })
        .then(data => {
            if (data.success && data.data) {
                const user = data.data;

                if (user.userType === 'AUTHENTICATED') {
                    setAuthenticatedUser(user);
                } else {
                    setAnonymousUser();
                }
            } else {
                setAnonymousUser();
            }
        })
        .catch(err => {
            console.log('User is anonymous');
            setAnonymousUser();
        });
}

/**
 * Update UI for authenticated user.
 */
function setAuthenticatedUser(user) {
    const userAvatar = document.getElementById('user-avatar');
    const anonymousIcon = document.getElementById('anonymous-icon');
    const userInitials = document.getElementById('user-initials');
    const userEmail = document.getElementById('user-email');
    const userUsername = document.getElementById('user-username');
    const authenticatedMenu = document.getElementById('authenticated-user-menu');
    const anonymousMenu = document.getElementById('anonymous-user-menu');

    if (userAvatar && anonymousIcon) {
        userAvatar.classList.remove('hidden');
        anonymousIcon.classList.add('hidden');
    }

    if (userInitials && user.username) {
        const initials = user.username.substring(0, 2).toUpperCase();
        userInitials.textContent = initials;
    }

    if (userEmail) {
        userEmail.textContent = user.email || 'User';
    }

    if (userUsername) {
        userUsername.textContent = user.username ? '@' + user.username : '';
    }

    if (authenticatedMenu && anonymousMenu) {
        authenticatedMenu.classList.remove('hidden');
        anonymousMenu.classList.add('hidden');
    }

    console.log('Authenticated user:', user);
}

/**
 * Update UI for anonymous user.
 */
function setAnonymousUser() {
    const userAvatar = document.getElementById('user-avatar');
    const anonymousIcon = document.getElementById('anonymous-icon');
    const authenticatedMenu = document.getElementById('authenticated-user-menu');
    const anonymousMenu = document.getElementById('anonymous-user-menu');

    if (userAvatar && anonymousIcon) {
        userAvatar.classList.add('hidden');
        anonymousIcon.classList.remove('hidden');
    }

    if (authenticatedMenu && anonymousMenu) {
        authenticatedMenu.classList.add('hidden');
        anonymousMenu.classList.remove('hidden');
    }

    console.log('Anonymous user (cookie-based)');
}

/**
 * Logout user and redirect to logout endpoint.
 * Server will clear both session and anonymous cookie.
 */
function logout() {
    window.location.href = '/logout';
}

// Export functions
window.checkAuthStatus = checkAuthStatus;
window.logout = logout;
