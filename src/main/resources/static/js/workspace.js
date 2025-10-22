/**
 * ChatKeep - Workspace Management
 * Handles automatic card refresh and event-driven updates
 */

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    setupWorkspaceListeners();
});

// ==================== Event Listeners ====================

function setupWorkspaceListeners() {
    // Listen for label assignment/removal - refresh specific card
    document.body.addEventListener('labelAssigned', function(event) {
        refreshCardInGrid();
    });

    document.body.addEventListener('labelRemoved', function(event) {
        refreshCardInGrid();
    });

    // Listen for note updates (flags, visibility)
    // After HTMX swaps the card, check if it still belongs in current filter
    document.body.addEventListener('htmx:afterSwap', function(event) {
        // Check if this is a card swap
        const target = event.detail.target;

        if (target && target.classList && target.classList.contains('chat-note-card')) {
            // Get note ID from the OLD element (before it's replaced)
            const noteId = target.getAttribute('data-note-id');

            // Wait a tiny bit for DOM to settle, then find the NEW card element
            setTimeout(() => {
                const newCard = document.getElementById('card-' + noteId);
                if (newCard) {
                    checkCardMatchesFilter(newCard);
                }
            }, 50);
        }
    });

    // Listen for label deletion - refresh entire grid
    document.body.addEventListener('labelDeleted', function(event) {
        refreshWorkspaceGrid();
    });
}

// ==================== Card Refresh ====================

/**
 * Refresh a specific card in the workspace grid
 * Finds the card by note ID from the currently open modal
 */
function refreshCardInGrid() {
    // Get note ID from modal (if modal is open)
    const modal = document.getElementById('chat-note-modal');
    if (!modal) {
        return;
    }

    // Extract note ID from modal - it's passed to various elements
    // Try multiple strategies to find the note ID
    const noteId = extractNoteIdFromModal(modal);

    if (!noteId) {
        return;
    }

    // Find the card in the grid
    const card = document.getElementById('card-' + noteId);
    if (!card) {
        return;
    }

    // Use HTMX to fetch and swap the updated card
    htmx.ajax('GET', '/fragments/chat-note-card-single?id=' + noteId, {
        target: '#card-' + noteId,
        swap: 'outerHTML'
    }).catch(function(error) {
        // Fallback: refresh entire grid
        refreshWorkspaceGrid();
    });
}

/**
 * Extract note ID from modal using multiple strategies
 */
function extractNoteIdFromModal(modal) {
    // Strategy 1: Check data attributes
    if (modal.dataset.noteId) {
        return modal.dataset.noteId;
    }

    // Strategy 2: Look for elements with onclick handlers containing note ID
    const editBtn = modal.querySelector('[id="edit-mode-btn"]');
    if (editBtn && editBtn.getAttribute('onclick')) {
        const match = editBtn.getAttribute('onclick').match(/['"]([a-f0-9]{24})['"]/);
        if (match) return match[1];
    }

    // Strategy 3: Check label selector data attribute
    const labelSelector = modal.querySelector('[data-note-id]');
    if (labelSelector) {
        return labelSelector.dataset.noteId;
    }

    // Strategy 4: Extract from URLs in HTMX attributes
    const elementsWithUrls = modal.querySelectorAll('[hx-get], [hx-post], [hx-delete]');
    for (const el of elementsWithUrls) {
        const url = el.getAttribute('hx-get') || el.getAttribute('hx-post') || el.getAttribute('hx-delete');
        if (url) {
            const match = url.match(/chat-notes\/([a-f0-9]{24})/);
            if (match) return match[1];
        }
    }

    return null;
}

/**
 * Refresh the entire workspace grid
 * Used when many cards might be affected (e.g., label deletion)
 */
function refreshWorkspaceGrid() {
    const grid = document.getElementById('notes-grid');
    if (!grid) {
        return;
    }

    // Get current filter from URL or grid state
    const currentFilter = getCurrentFilter();
    const currentLabelIds = getCurrentLabelIds();
    const currentSearch = getCurrentSearch();

    // Build refresh URL with current filters
    let refreshUrl = '/fragments/chat-notes';
    const params = new URLSearchParams();

    if (currentFilter) params.append('filter', currentFilter);
    if (currentLabelIds) params.append('labelIds', currentLabelIds);
    if (currentSearch) params.append('search', currentSearch);

    if (params.toString()) {
        refreshUrl += '?' + params.toString();
    }

    // Use HTMX to refresh
    htmx.ajax('GET', refreshUrl, {
        target: '#notes-grid',
        swap: 'innerHTML'
    });
}

/**
 * Get current filter from URL or sidebar active state
 */
function getCurrentFilter() {
    // Check URL params
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('filter')) {
        return urlParams.get('filter');
    }

    // Check active sidebar item - extract filter from hx-get attribute
    const activeSidebarItem = document.querySelector('.sidebar-nav-item.active');
    if (activeSidebarItem) {
        const hxGet = activeSidebarItem.getAttribute('hx-get') || activeSidebarItem.getAttribute('hx:get');

        if (hxGet) {
            // Extract filter parameter from URL like "/fragments/chat-notes?filter=favorites"
            const match = hxGet.match(/filter[=']([^'&)]+)/);
            if (match) {
                return match[1];
            }
        }
    }

    // Default to 'chatnotes'
    return 'chatnotes';
}

/**
 * Get current label filter from URL
 */
function getCurrentLabelIds() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('labelIds') || '';
}

/**
 * Get current search query from URL or search input
 */
function getCurrentSearch() {
    // Check URL params first
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('search')) {
        return urlParams.get('search');
    }

    // Check search input
    const searchInput = document.querySelector('input[name="search"]');
    if (searchInput && searchInput.value) {
        return searchInput.value;
    }

    return '';
}

// ==================== Card Filter Matching ====================

/**
 * Check if a card matches the current filter view
 * If not, remove it with animation
 */
function checkCardMatchesFilter(cardElement) {
    if (!cardElement) {
        return;
    }

    const currentFilter = getCurrentFilter();

    // Get card state from data attributes
    const isFavorite = cardElement.getAttribute('data-is-favorite') === 'true';
    const isArchived = cardElement.getAttribute('data-is-archived') === 'true';
    const isTrashed = cardElement.getAttribute('data-is-trashed') === 'true';
    const isPublic = cardElement.getAttribute('data-is-public') === 'true';

    let shouldRemove = false;

    // Check if card matches current filter
    switch(currentFilter) {
        case 'favorites':
            // In favorites view, card should be favorited
            shouldRemove = !isFavorite;
            break;
        case 'archive':
            // In archive view, card should be archived (and not trashed)
            shouldRemove = !isArchived || isTrashed;
            break;
        case 'trash':
            // In trash view, card should be trashed
            shouldRemove = !isTrashed;
            break;
        case 'shared':
            // In shared view, card should be public
            shouldRemove = !isPublic;
            break;
        case 'chatnotes':
        default:
            // In active view, card should NOT be archived or trashed
            shouldRemove = isArchived || isTrashed;
            break;
    }

    if (shouldRemove) {
        removeCardWithAnimation(cardElement);
    }
}

/**
 * Remove a card with smooth fade-out animation
 */
function removeCardWithAnimation(cardElement) {
    // Add fade-out animation class
    cardElement.classList.add('card-fade-out');

    // Remove after animation completes (300ms)
    setTimeout(() => {
        cardElement.remove();
    }, 300);
}

// ==================== Export Functions ====================

// Export to window for inline usage if needed
window.refreshCardInGrid = refreshCardInGrid;
window.refreshWorkspaceGrid = refreshWorkspaceGrid;
window.setupWorkspaceListeners = setupWorkspaceListeners;
window.checkCardMatchesFilter = checkCardMatchesFilter;
