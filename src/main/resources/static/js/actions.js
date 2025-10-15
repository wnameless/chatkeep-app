/**
 * ChatKeep - Actions JavaScript
 * Simplified for HTMX - handles only clipboard operations and file operations
 */

// ==================== Share Actions (Clipboard) ====================

function copyShareLink(noteId) {
    const shareUrl = `${window.location.origin}/share/${noteId}`;

    // Copy to clipboard
    navigator.clipboard.writeText(shareUrl)
        .then(() => {
            showToast('Share link copied to clipboard!', 'success');
        })
        .catch(err => {
            console.error('Error copying to clipboard:', err);
            // Fallback: show prompt with link
            prompt('Copy this share link:', shareUrl);
        });
}

// ==================== Download Actions (File Operations) ====================

function downloadChatNote(noteId) {
    // Use the dedicated download endpoint that returns markdown with proper headers
    const downloadUrl = `/api/v1/chat-notes/${noteId}/download`;

    // Create a temporary anchor element to trigger download
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    showToast('Download started', 'success');
}

// ==================== More Menu Toggle ====================

function toggleMoreMenu(noteId) {
    const menu = document.getElementById(`more-menu-${noteId}`);
    if (!menu) return;

    // Close all other menus first
    document.querySelectorAll('.more-menu').forEach(m => {
        if (m !== menu) m.classList.add('hidden');
    });

    // Toggle current menu
    menu.classList.toggle('hidden');

    // Close menu when clicking outside
    if (!menu.classList.contains('hidden')) {
        setTimeout(() => {
            document.addEventListener('click', function closeMenu(e) {
                if (!menu.contains(e.target)) {
                    menu.classList.add('hidden');
                    document.removeEventListener('click', closeMenu);
                }
            });
        }, 0);
    }
}

// ==================== Edit/Duplicate Actions ====================

function editChatNote(noteId) {
    // Open modal and enter edit mode
    openChatNoteModal(noteId);
    setTimeout(() => enterEditMode(noteId), 300);
}

function duplicateChatNote(noteId) {
    if (!confirm('Duplicate this ChatNote?')) return;

    // Fetch original note
    fetch(`/api/v1/chat-notes/${noteId}`)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            const note = data.data;
            const markdown = note.fullMarkdown || note.conversationContent || '';

            // Upload as new note
            return fetch('/api/v1/chat-notes', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    markdownContent: markdown
                })
            });
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('ChatNote duplicated', 'success');
            // Reload via HTMX
            htmx.ajax('GET', '/fragments/chat-notes', {
                target: '#notes-grid',
                swap: 'innerHTML'
            });
            // Trigger count update
            htmx.trigger(document.body, 'updateCounts');
        } else {
            showToast('Error duplicating ChatNote', 'error');
        }
    })
    .catch(err => {
        console.error('Error duplicating ChatNote:', err);
        showToast('Failed to duplicate', 'error');
    });
}

// ==================== Export Functions ====================

// Export to window for inline onclick handlers
window.copyShareLink = copyShareLink;
window.downloadChatNote = downloadChatNote;
window.toggleMoreMenu = toggleMoreMenu;
window.editChatNote = editChatNote;
window.duplicateChatNote = duplicateChatNote;
