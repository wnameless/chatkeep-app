/**
 * ChatKeep - Main Application JavaScript
 * Simplified for HTMX - handles only UI interactions and event listeners
 */

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    initializeSidebar();
    initializeHeader();
    initializeViewToggle();
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

// ==================== Label Filtering ====================

// Track selected label IDs
const selectedLabelIds = new Set();

function toggleLabelSelection(button) {
    const labelId = button.dataset.labelId;
    const labelColor = button.dataset.labelColor;
    const indicator = button.querySelector('.label-indicator');
    const colorDot = indicator.querySelector('.color-dot');
    const checkmark = indicator.querySelector('.checkmark-icon');
    const labelName = button.querySelector('.label-name');

    // Toggle selection
    if (selectedLabelIds.has(labelId)) {
        // Deselect
        selectedLabelIds.delete(labelId);

        // Reset to unselected state
        button.style.backgroundColor = '';
        button.classList.remove('selected-label');
        button.classList.add('hover:bg-gray-100', 'dark:hover:bg-gray-700');

        // Show color dot, hide checkmark
        colorDot.classList.remove('hidden');
        checkmark.classList.add('hidden');

        // Reset text color
        labelName.classList.remove('text-white');
        labelName.classList.add('text-gray-700', 'dark:text-gray-300');

    } else {
        // Select
        selectedLabelIds.add(labelId);

        // Apply selected state with label color
        button.style.backgroundColor = labelColor;
        button.classList.add('selected-label');
        button.classList.remove('hover:bg-gray-100', 'dark:hover:bg-gray-700');

        // Hide color dot, show checkmark
        colorDot.classList.add('hidden');
        checkmark.classList.remove('hidden');

        // White text for selected
        labelName.classList.add('text-white');
        labelName.classList.remove('text-gray-700', 'dark:text-gray-300');
    }

    // Update filter and UI
    updateLabelFilter();
}

function updateLabelFilter() {
    const clearButton = document.getElementById('clear-label-filters');

    if (selectedLabelIds.size > 0) {
        // Show clear button
        clearButton.classList.remove('hidden');

        // Build comma-separated labelIds
        const labelIdsParam = Array.from(selectedLabelIds).join(',');

        // Trigger HTMX request to filter notes
        htmx.ajax('GET', `/fragments/chat-notes?labelIds=${labelIdsParam}`, {
            target: '#notes-grid',
            swap: 'innerHTML'
        });
    } else {
        // Hide clear button
        clearButton.classList.add('hidden');

        // Reload all notes (no filter)
        htmx.ajax('GET', '/fragments/chat-notes', {
            target: '#notes-grid',
            swap: 'innerHTML'
        });
    }
}

function clearLabelFilters() {
    // Clear all selections
    selectedLabelIds.clear();

    // Reset all label buttons to unselected state
    document.querySelectorAll('.label-filter-btn').forEach(button => {
        const labelColor = button.dataset.labelColor;
        const indicator = button.querySelector('.label-indicator');
        const colorDot = indicator.querySelector('.color-dot');
        const checkmark = indicator.querySelector('.checkmark-icon');
        const labelName = button.querySelector('.label-name');

        // Reset styling
        button.style.backgroundColor = '';
        button.classList.remove('selected-label');
        button.classList.add('hover:bg-gray-100', 'dark:hover:bg-gray-700');

        // Show color dot, hide checkmark
        colorDot.classList.remove('hidden');
        checkmark.classList.add('hidden');

        // Reset text color
        labelName.classList.remove('text-white');
        labelName.classList.add('text-gray-700', 'dark:text-gray-300');
    });

    // Hide clear button
    document.getElementById('clear-label-filters').classList.add('hidden');

    // Reload all notes
    htmx.ajax('GET', '/fragments/chat-notes', {
        target: '#notes-grid',
        swap: 'innerHTML'
    });
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

    // Archiving prompt dropdown
    const copyTemplateBtn = document.getElementById('copy-template-btn');
    const archivingPromptDropdown = document.getElementById('archiving-prompt-dropdown');

    if (copyTemplateBtn && archivingPromptDropdown) {
        copyTemplateBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            archivingPromptDropdown.classList.toggle('hidden');
            // Reset success message when opening
            const successMessage = document.getElementById('copy-success-message');
            if (successMessage) {
                successMessage.classList.add('hidden');
            }
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
        if (archivingPromptDropdown && !archivingPromptDropdown.contains(e.target) && e.target !== copyTemplateBtn) {
            archivingPromptDropdown.classList.add('hidden');
        }

        // Close import dropdowns
        const importDropdown = document.getElementById('import-dropdown');
        const importBtn = document.getElementById('import-archive-btn');
        if (importDropdown && !importDropdown.contains(e.target) && e.target !== importBtn) {
            importDropdown.classList.add('hidden');
        }

        const mobileImportDropdown = document.getElementById('mobile-import-dropdown');
        const mobileImportBtn = document.getElementById('mobile-import-archive-btn');
        if (mobileImportDropdown && !mobileImportDropdown.contains(e.target) && e.target !== mobileImportBtn) {
            mobileImportDropdown.classList.add('hidden');
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

    // Import archive dropdown toggle (desktop)
    const importBtn = document.getElementById('import-archive-btn');
    const importDropdown = document.getElementById('import-dropdown');
    if (importBtn && importDropdown) {
        importBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            importDropdown.classList.toggle('hidden');
        });
    }

    // Import archive dropdown toggle (mobile)
    const mobileImportBtn = document.getElementById('mobile-import-archive-btn');
    const mobileImportDropdown = document.getElementById('mobile-import-dropdown');
    if (mobileImportBtn && mobileImportDropdown) {
        mobileImportBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            mobileImportDropdown.classList.toggle('hidden');
        });
    }

    // Upload file option (desktop)
    const uploadFileBtn = document.getElementById('upload-file-btn');
    if (uploadFileBtn) {
        uploadFileBtn.addEventListener('click', function() {
            importDropdown?.classList.add('hidden');
            openImportDialog();
        });
    }

    // Upload file option (mobile)
    const mobileUploadFileBtn = document.querySelector('.mobile-upload-file-btn');
    if (mobileUploadFileBtn) {
        mobileUploadFileBtn.addEventListener('click', function() {
            mobileImportDropdown?.classList.add('hidden');
            openImportDialog();
        });
    }

    // Paste content option (desktop)
    const pasteContentBtn = document.getElementById('paste-content-btn');
    if (pasteContentBtn) {
        pasteContentBtn.addEventListener('click', function() {
            importDropdown?.classList.add('hidden');
            openPasteModal();
        });
    }

    // Paste content option (mobile)
    const mobilePasteContentBtn = document.querySelector('.mobile-paste-content-btn');
    if (mobilePasteContentBtn) {
        mobilePasteContentBtn.addEventListener('click', function() {
            mobileImportDropdown?.classList.add('hidden');
            openPasteModal();
        });
    }

    // Copy prompt action button (inside dropdown)
    const copyPromptActionBtn = document.getElementById('copy-prompt-action-btn');
    if (copyPromptActionBtn) {
        copyPromptActionBtn.addEventListener('click', function() {
            copyArchiveTemplate(archivingPromptDropdown);
        });
    }

    // Mobile archiving prompt bottom sheet
    const mobileArchivingBtn = document.getElementById('mobile-archiving-prompt-btn');
    const mobileBottomSheet = document.getElementById('mobile-archiving-bottom-sheet');
    const mobileBackdrop = document.getElementById('mobile-archiving-backdrop');
    const mobileCopyActionBtn = document.getElementById('mobile-copy-prompt-action-btn');

    if (mobileArchivingBtn && mobileBottomSheet && mobileBackdrop) {
        // Open bottom sheet
        mobileArchivingBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            mobileBottomSheet.classList.remove('hidden');
            mobileBackdrop.classList.remove('hidden');
            // Reset success message when opening
            const mobileSuccessMessage = document.getElementById('mobile-copy-success-message');
            if (mobileSuccessMessage) {
                mobileSuccessMessage.classList.add('hidden');
            }
        });

        // Close bottom sheet when clicking backdrop
        mobileBackdrop.addEventListener('click', function() {
            mobileBottomSheet.classList.add('hidden');
            mobileBackdrop.classList.add('hidden');
        });

        // Copy action button inside mobile bottom sheet
        if (mobileCopyActionBtn) {
            mobileCopyActionBtn.addEventListener('click', function() {
                copyArchiveTemplate(mobileBottomSheet);
            });
        }
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
        } else {
            showToast(data.message || 'Import failed', 'error');
        }
    })
    .catch(err => {
        console.error('Import error:', err);
        showToast('Import failed', 'error');
    });
}

function copyArchiveTemplate(dropdown) {
    // iOS Safari workaround: Create textarea immediately (within user gesture)
    // This maintains the security context even after async fetch
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
    let textarea = null;

    if (isIOS) {
        // Create textarea NOW while we still have user gesture context
        textarea = document.createElement('textarea');
        textarea.value = 'Loading...'; // Placeholder
        textarea.style.position = 'fixed';
        textarea.style.top = '-9999px';
        textarea.style.left = '-9999px';
        textarea.setAttribute('readonly', '');
        document.body.appendChild(textarea);
        textarea.select(); // Select immediately to maintain focus
    }

    // Fetch template and copy to clipboard
    fetch('/api/v1/templates/archive')
        .then(response => response.text())
        .then(template => {
            // iOS: Use the pre-created textarea
            if (isIOS && textarea) {
                textarea.value = template;
                textarea.select();
                textarea.setSelectionRange(0, template.length);

                try {
                    const successful = document.execCommand('copy');
                    document.body.removeChild(textarea);

                    if (successful) {
                        if (dropdown) {
                            // Check if it's mobile bottom sheet or desktop dropdown
                            const isMobile = dropdown.id === 'mobile-archiving-bottom-sheet';
                            const successMessageId = isMobile ? 'mobile-copy-success-message' : 'copy-success-message';
                            const successMessage = document.getElementById(successMessageId);

                            if (successMessage) successMessage.classList.remove('hidden');

                            setTimeout(() => {
                                dropdown.classList.add('hidden');
                                // Also hide backdrop for mobile
                                if (isMobile) {
                                    const backdrop = document.getElementById('mobile-archiving-backdrop');
                                    if (backdrop) backdrop.classList.add('hidden');
                                }
                            }, 2000);
                        } else {
                            showToast('Prompt copied to clipboard', 'success');
                        }
                    } else {
                        throw new Error('execCommand failed');
                    }
                } catch (err) {
                    if (textarea.parentNode) document.body.removeChild(textarea);
                    console.error('iOS copy failed:', err);
                    showToast('Copy failed. Please try again.', 'error');
                }
                return;
            }

            // Non-iOS: Try modern Clipboard API first, fallback to legacy method
            copyToClipboard(template)
                .then(() => {
                    if (dropdown) {
                        // Check if it's mobile bottom sheet or desktop dropdown
                        const isMobile = dropdown.id === 'mobile-archiving-bottom-sheet';
                        const successMessageId = isMobile ? 'mobile-copy-success-message' : 'copy-success-message';
                        const successMessage = document.getElementById(successMessageId);

                        if (successMessage) {
                            successMessage.classList.remove('hidden');
                        }

                        // Auto-close dropdown/bottom sheet after 2 seconds
                        setTimeout(() => {
                            dropdown.classList.add('hidden');
                            // Also hide backdrop for mobile
                            if (isMobile) {
                                const backdrop = document.getElementById('mobile-archiving-backdrop');
                                if (backdrop) backdrop.classList.add('hidden');
                            }
                        }, 2000);
                    } else {
                        // Fallback: Show toast notification
                        showToast('Prompt copied to clipboard', 'success');
                    }
                })
                .catch((err) => {
                    console.error('Copy failed:', err);
                    showToast('Copy failed. Please try again.', 'error');
                });
        })
        .catch(err => {
            // Clean up iOS textarea on fetch error
            if (isIOS && textarea && textarea.parentNode) {
                document.body.removeChild(textarea);
            }
            console.error('Error fetching template:', err);
            showToast('Failed to fetch prompt', 'error');
        });
}

/**
 * Copy text to clipboard with fallback support
 * Tries modern Clipboard API first, falls back to legacy execCommand
 * Works with self-signed SSL certificates
 */
function copyToClipboard(text) {
    // Try modern Clipboard API (requires secure context)
    if (navigator.clipboard && navigator.clipboard.writeText) {
        return navigator.clipboard.writeText(text)
            .catch(err => {
                console.warn('Clipboard API failed, trying fallback:', err);
                return copyToClipboardFallback(text);
            });
    }

    // Fallback to legacy method
    return copyToClipboardFallback(text);
}

/**
 * Legacy clipboard copy using execCommand
 * Works in more environments including self-signed SSL
 */
function copyToClipboardFallback(text) {
    return new Promise((resolve, reject) => {
        // Create temporary textarea
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.top = '-9999px';
        textarea.style.left = '-9999px';
        textarea.setAttribute('readonly', '');

        document.body.appendChild(textarea);

        try {
            // Select and copy
            textarea.select();
            textarea.setSelectionRange(0, text.length); // For mobile

            const successful = document.execCommand('copy');
            document.body.removeChild(textarea);

            if (successful) {
                resolve();
            } else {
                reject(new Error('execCommand copy failed'));
            }
        } catch (err) {
            document.body.removeChild(textarea);
            reject(err);
        }
    });
}

function logout() {
    window.location.href = '/logout';
}

// ==================== Paste Archive Modal ====================

function openPasteModal() {
    // Fetch the modal template and insert into modal-container
    fetch('/fragments/paste-archive-modal')
        .then(response => response.text())
        .then(html => {
            const modalContainer = document.getElementById('modal-container');
            if (modalContainer) {
                modalContainer.innerHTML = html;
            }
        })
        .catch(err => {
            console.error('Error loading paste modal:', err);
            showToast('Failed to open paste modal', 'error');
        });
}

function closePasteModal() {
    const modal = document.getElementById('paste-archive-modal');
    if (modal) {
        modal.remove();
    }
}

function handlePasteSubmit(event) {
    event.preventDefault();

    const content = document.getElementById('archive-content').value.trim();
    if (!content) {
        showToast('Please paste archive content', 'warning');
        return;
    }

    // Show loading state
    const submitBtn = document.getElementById('paste-submit-btn');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Importing...';

    // Submit to backend
    fetch('/api/v1/chat-notes', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            markdownContent: content
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('Archive imported successfully', 'success');
            closePasteModal();

            // Reload current view
            htmx.ajax('GET', '/fragments/chat-notes', {
                target: '#notes-grid',
                swap: 'innerHTML'
            });
        } else {
            showToast(data.message || 'Import failed', 'error');
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    })
    .catch(err => {
        console.error('Import error:', err);
        showToast('Import failed. Please check the archive format.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    });
}
