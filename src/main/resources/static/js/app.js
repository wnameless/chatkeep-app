/**
 * ChatKeep - Main Application JavaScript
 * Simplified for HTMX - handles only UI interactions and event listeners
 */

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    initializeSidebar();
    initializeHeader();
    initializeViewToggle();
    initializeTags();
    initializeTheme();
    setupHTMXListeners();
});

// ==================== Sidebar Management ====================

function initializeSidebar() {
    const sidebarToggle = document.getElementById('sidebar-toggle');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    // Mobile sidebar toggle
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('-translate-x-full');
            sidebarOverlay.classList.toggle('hidden');
        });
    }

    // Close sidebar when overlay is clicked
    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', function() {
            sidebar.classList.add('-translate-x-full');
            sidebarOverlay.classList.add('hidden');
        });
    }

    // Sidebar navigation active state
    const navItems = document.querySelectorAll('.sidebar-nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', function() {
            navItems.forEach(nav => {
                nav.classList.remove('active', 'bg-primary-50', 'dark:bg-primary-900', 'text-primary-600', 'dark:text-primary-400');
            });
            this.classList.add('active', 'bg-primary-50', 'dark:bg-primary-900', 'text-primary-600', 'dark:text-primary-400');
        });
    });
}

// ==================== Tags Management ====================

function initializeTags() {
    // Tags toggle
    const tagsToggle = document.getElementById('tags-toggle');
    const tagsList = document.getElementById('tags-list');
    const tagsChevron = document.getElementById('tags-chevron');

    if (tagsToggle && tagsList) {
        tagsToggle.addEventListener('click', function() {
            const isHidden = tagsList.classList.contains('hidden');
            tagsList.classList.toggle('hidden');
            tagsChevron.classList.toggle('rotate-180');

            // Load tags on first expand
            if (isHidden && tagsList.querySelector('#tags-container').children.length === 0) {
                loadTagsViaHTMX();
            }
        });
    }

    // Clear filters button handler
    const clearFiltersBtn = document.getElementById('clear-filters-btn');
    if (clearFiltersBtn) {
        clearFiltersBtn.addEventListener('click', function() {
            // Uncheck all tag checkboxes
            document.querySelectorAll('.tag-checkbox:checked').forEach(cb => cb.checked = false);
            // Hide clear button
            document.getElementById('clear-filters-container')?.classList.add('hidden');
        });
    }
}

function loadTagsViaHTMX() {
    // Fetch tags via HTMX fragment (uses SecurityUtils.getCurrentUserId())
    htmx.ajax('GET', '/fragments/tags', {
        target: '#tags-container',
        swap: 'innerHTML'
    }).then(() => {
        // Hide loading message after tags are loaded
        const tagsLoading = document.getElementById('tags-loading');
        if (tagsLoading) tagsLoading.classList.add('hidden');

        // Attach event listeners to newly loaded checkboxes
        document.querySelectorAll('.tag-checkbox').forEach(checkbox => {
            checkbox.addEventListener('change', handleTagFilterChange);
        });
    });
}

function renderTagsInSidebar(tagsMap) {
    const tagsContainer = document.getElementById('tags-container');
    const tagsLoading = document.getElementById('tags-loading');

    if (tagsLoading) tagsLoading.classList.add('hidden');
    if (!tagsContainer) return;

    tagsContainer.innerHTML = '';

    // Sort tags by count (descending) then alphabetically
    const sortedTags = Array.from(tagsMap.entries())
        .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));

    sortedTags.forEach(([tag, count]) => {
        const label = document.createElement('label');
        label.className = 'flex items-center py-1.5 px-2 ml-6 hover:bg-gray-50 dark:hover:bg-gray-700 rounded cursor-pointer';
        label.innerHTML = `
            <input type="checkbox" value="${tag}" class="tag-checkbox rounded text-primary-500 focus:ring-primary-400 mr-2">
            <span class="text-sm text-gray-700 dark:text-gray-300">${tag}</span>
            <span class="ml-auto text-xs text-gray-500 dark:text-gray-400">${count}</span>
        `;

        const checkbox = label.querySelector('.tag-checkbox');
        checkbox.addEventListener('change', handleTagFilterChange);

        tagsContainer.appendChild(label);
    });
}

function handleTagFilterChange() {
    const selectedTags = Array.from(document.querySelectorAll('.tag-checkbox:checked')).map(cb => cb.value);
    const clearFiltersContainer = document.getElementById('clear-filters-container');

    if (selectedTags.length > 0) {
        clearFiltersContainer?.classList.remove('hidden');
        // Trigger HTMX request with selected tags (OR operation for multiple tags)
        const tagsParam = selectedTags.join(',');
        htmx.ajax('GET', `/fragments/filter/tags?tags=${tagsParam}&operator=OR`, {
            target: '#notes-grid',
            swap: 'innerHTML'
        });
    } else {
        clearFiltersContainer?.classList.add('hidden');
        // Reload default view
        htmx.ajax('GET', '/fragments/chat-notes', {
            target: '#notes-grid',
            swap: 'innerHTML'
        });
    }
}

// ==================== Header Management ====================

function initializeHeader() {
    // Settings dropdown
    const settingsBtn = document.getElementById('settings-btn');
    const settingsDropdown = document.getElementById('settings-dropdown');

    if (settingsBtn && settingsDropdown) {
        settingsBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            settingsDropdown.classList.toggle('hidden');
        });
    }

    // User menu dropdown
    const userMenuBtn = document.getElementById('user-menu-btn');
    const userDropdown = document.getElementById('user-dropdown');

    if (userMenuBtn && userDropdown) {
        userMenuBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            userDropdown.classList.toggle('hidden');
        });
    }

    // Close dropdowns when clicking outside
    document.addEventListener('click', function(e) {
        if (settingsDropdown && !settingsDropdown.contains(e.target) && e.target !== settingsBtn) {
            settingsDropdown.classList.add('hidden');
        }
        if (userDropdown && !userDropdown.contains(e.target) && e.target !== userMenuBtn) {
            userDropdown.classList.add('hidden');
        }
    });

    // Refresh button - add spin animation
    const refreshBtn = document.getElementById('refresh-btn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            const icon = this.querySelector('i');
            icon.classList.add('fa-spin');
            setTimeout(() => icon.classList.remove('fa-spin'), 1000);
        });
    }

    // Import archive button
    const importBtn = document.getElementById('import-archive-btn');
    if (importBtn) {
        importBtn.addEventListener('click', openImportDialog);
    }

    // Copy template button
    const copyTemplateBtn = document.getElementById('copy-template-btn');
    if (copyTemplateBtn) {
        copyTemplateBtn.addEventListener('click', copyArchiveTemplate);
    }

    // Logout button
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', logout);
    }
}

// ==================== View Toggle (Grid/List) ====================

function initializeViewToggle() {
    const gridViewBtn = document.getElementById('grid-view-btn');
    const listViewBtn = document.getElementById('list-view-btn');
    const notesGrid = document.getElementById('notes-grid');

    if (gridViewBtn && listViewBtn && notesGrid) {
        gridViewBtn.addEventListener('click', function() {
            // Update active state
            gridViewBtn.classList.add('active', 'bg-white', 'dark:bg-gray-600');
            listViewBtn.classList.remove('active', 'bg-white', 'dark:bg-gray-600');

            // Update grid CSS classes
            notesGrid.classList.remove('space-y-0');
            notesGrid.classList.add('masonry-grid');
        });

        listViewBtn.addEventListener('click', function() {
            // Update active state
            listViewBtn.classList.add('active', 'bg-white', 'dark:bg-gray-600');
            gridViewBtn.classList.remove('active', 'bg-white', 'dark:bg-gray-600');

            // Update grid CSS classes
            notesGrid.classList.remove('masonry-grid');
            notesGrid.classList.add('space-y-0');
        });
    }
}

// ==================== Theme Management ====================

function initializeTheme() {
    // Load saved theme or default to auto
    const savedTheme = localStorage.getItem('theme') || 'auto';
    applyTheme(savedTheme);

    // Theme option buttons
    const themeOptions = document.querySelectorAll('.theme-option');
    themeOptions.forEach(option => {
        option.addEventListener('click', function() {
            const theme = this.dataset.theme;
            localStorage.setItem('theme', theme);
            applyTheme(theme);
            document.getElementById('settings-dropdown')?.classList.add('hidden');
        });
    });
}

function applyTheme(theme) {
    const html = document.documentElement;

    if (theme === 'dark') {
        html.classList.add('dark');
    } else if (theme === 'light') {
        html.classList.remove('dark');
    } else {
        // Auto: follow system preference
        if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
            html.classList.add('dark');
        } else {
            html.classList.remove('dark');
        }
    }
}

// ==================== HTMX Event Listeners ====================

function setupHTMXListeners() {
    // Listen for showToast events triggered by backend
    document.body.addEventListener('showToast', function(evt) {
        if (evt.detail) {
            showToast(evt.detail.message, evt.detail.type || 'info');
        }
    });

    // Listen for HTMX before request to show loading indicators
    document.body.addEventListener('htmx:beforeRequest', function(evt) {
        // Add loading spinner or progress indicator if needed
    });

    // Listen for HTMX after request to hide loading indicators
    document.body.addEventListener('htmx:afterRequest', function(evt) {
        // Hide loading indicators
    });
}

// ==================== Toast Notifications ====================

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `fixed bottom-4 right-4 px-6 py-3 rounded-lg shadow-lg text-white z-50 animate-slide-up`;

    // Set color based on type
    switch(type) {
        case 'success':
            toast.classList.add('bg-green-500');
            break;
        case 'error':
            toast.classList.add('bg-red-500');
            break;
        case 'warning':
            toast.classList.add('bg-yellow-500');
            break;
        default:
            toast.classList.add('bg-blue-500');
    }

    toast.textContent = message;
    document.body.appendChild(toast);

    // Auto remove after 3 seconds
    setTimeout(() => {
        toast.classList.add('opacity-0', 'transition-opacity');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ==================== Utility Functions ====================

function getUserId() {
    // Get user ID from auth system
    // For now, this could be from a meta tag or global variable
    return document.querySelector('meta[name="user-id"]')?.content || null;
}

function openImportDialog() {
    // Create file input element
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = '.md,.markdown';
    fileInput.onchange = handleFileImport;
    fileInput.click();
}

function handleFileImport(event) {
    const file = event.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    fetch('/api/v1/chat-notes/upload', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('Archive imported successfully', 'success');
            // Reload current view
            htmx.ajax('GET', '/fragments/chat-notes', {
                target: '#notes-grid',
                swap: 'innerHTML'
            });
            // Trigger count update
            htmx.trigger(document.body, 'updateCounts');
        } else {
            showToast(data.message || 'Import failed', 'error');
        }
    })
    .catch(err => {
        console.error('Import error:', err);
        showToast('Import failed', 'error');
    });
}

function copyArchiveTemplate() {
    // Fetch template and copy to clipboard
    fetch('/api/v1/templates/archive')
        .then(response => response.text())
        .then(template => {
            navigator.clipboard.writeText(template)
                .then(() => showToast('Template copied to clipboard', 'success'))
                .catch(() => showToast('Failed to copy template', 'error'));
        })
        .catch(err => {
            console.error('Error fetching template:', err);
            showToast('Failed to fetch template', 'error');
        });
}

function logout() {
    window.location.href = '/logout';
}
