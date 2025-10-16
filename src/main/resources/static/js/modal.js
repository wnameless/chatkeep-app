/**
 * ChatKeep - Modal Management
 * Simplified for HTMX - handles only UI toggles and edit mode
 */

// Global state
let currentNoteId = null;
let isEditMode = false;

// ==================== Modal Functions ====================

function closeChatNoteModal() {
    const modalContainer = document.getElementById('modal-container');
    if (modalContainer) {
        modalContainer.innerHTML = '';
    }
    currentNoteId = null;
    isEditMode = false;
}

// Called after HTMX loads modal content
function initializeModal(noteId) {
    currentNoteId = noteId;
    initializeModalTabs();
}

// ==================== Modal Tabs ====================

function initializeModalTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', function() {
            const targetTab = this.dataset.tab;

            // Update button states
            tabButtons.forEach(btn => {
                btn.classList.remove('active', 'text-primary-600', 'dark:text-primary-400', 'border-primary-600', 'dark:border-primary-400');
                btn.classList.add('text-gray-600', 'dark:text-gray-400', 'border-transparent');
            });

            this.classList.add('active', 'text-primary-600', 'dark:text-primary-400', 'border-primary-600', 'dark:border-primary-400');
            this.classList.remove('text-gray-600', 'dark:text-gray-400', 'border-transparent');

            // Update content visibility
            tabContents.forEach(content => {
                content.classList.add('hidden');
            });

            const targetContent = document.getElementById(`${targetTab}-tab`);
            if (targetContent) {
                targetContent.classList.remove('hidden');
            }
        });
    });
}

// ==================== Artifacts & Attachments Toggle ====================

function toggleArtifact(index) {
    const content = document.getElementById(`artifact-content-${index}`);
    const chevron = document.getElementById(`artifact-chevron-${index}`);

    if (content && chevron) {
        content.classList.toggle('hidden');
        chevron.classList.toggle('rotate-180');
    }
}

function toggleAttachment(index) {
    const content = document.getElementById(`attachment-content-${index}`);
    const chevron = document.getElementById(`attachment-chevron-${index}`);

    if (content && chevron) {
        content.classList.toggle('hidden');
        chevron.classList.toggle('rotate-180');
    }
}

// ==================== Edit Mode ====================

function enterEditMode(noteId) {
    isEditMode = true;

    // Show save/cancel buttons, hide edit button
    document.getElementById('edit-mode-btn')?.classList.add('hidden');
    document.getElementById('save-mode-btn')?.classList.remove('hidden');
    document.getElementById('cancel-mode-btn')?.classList.remove('hidden');

    // Switch to edit view
    document.getElementById('content-view-mode')?.classList.add('hidden');
    document.getElementById('content-edit-mode')?.classList.remove('hidden');

    // No need to load anything - structured forms are already in the template
}

function exitEditMode() {
    isEditMode = false;

    // Show edit button, hide save/cancel buttons
    document.getElementById('edit-mode-btn')?.classList.remove('hidden');
    document.getElementById('save-mode-btn')?.classList.add('hidden');
    document.getElementById('cancel-mode-btn')?.classList.add('hidden');

    // Switch back to view mode
    document.getElementById('content-view-mode')?.classList.remove('hidden');
    document.getElementById('content-edit-mode')?.classList.add('hidden');
}

// This function is replaced by saveStructuredEdits()
// Kept for backward compatibility with header buttons
function saveChatNote(noteId) {
    saveStructuredEdits(noteId);
}

async function saveStructuredEdits(noteId) {
    try {
        // Collect all field values
        const title = document.getElementById('edit-title')?.value;
        const tagsRaw = document.getElementById('edit-tags')?.value;
        const conversationDate = document.getElementById('edit-conversation-date')?.value;
        const initialQuery = document.getElementById('edit-initial-query')?.value;
        const keyInsightsDesc = document.getElementById('edit-key-insights-desc')?.value;
        const keyPointsRaw = document.getElementById('edit-key-points')?.value;
        const followUp = document.getElementById('edit-follow-up')?.value;
        const referencesRaw = document.getElementById('edit-references')?.value;

        // Parse tags (comma-separated to array)
        const tags = tagsRaw ? tagsRaw.split(',').map(t => t.trim()).filter(t => t) : [];

        // Parse key points (line-separated to array)
        const keyPoints = keyPointsRaw ? keyPointsRaw.split('\n').map(p => p.trim()).filter(p => p) : [];

        // Parse references (JSON)
        let references = [];
        try {
            references = referencesRaw ? JSON.parse(referencesRaw) : [];
        } catch (e) {
            showToast('Invalid JSON format for references', 'error');
            return;
        }

        // Send all updates in sequence (granular endpoints)
        const updates = [];

        // 1. Update title
        if (title !== undefined) {
            updates.push(
                fetch(`/api/v1/chat-notes/${noteId}/title`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ title })
                }).then(r => r.json())
            );
        }

        // 2. Update tags
        if (tags !== undefined) {
            updates.push(
                fetch(`/api/v1/chat-notes/${noteId}/tags`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ tags })
                }).then(r => r.json())
            );
        }

        // 3. Update conversation date
        if (conversationDate) {
            updates.push(
                fetch(`/api/v1/chat-notes/${noteId}/conversation-date`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ conversationDate })
                }).then(r => r.json())
            );
        }

        // 4. Update initial query
        if (initialQuery !== undefined) {
            updates.push(
                fetch(`/api/v1/chat-notes/${noteId}/initial-query`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ description: initialQuery })
                }).then(r => r.json())
            );
        }

        // 5. Update key insights
        if (keyInsightsDesc !== undefined) {
            updates.push(
                fetch(`/api/v1/chat-notes/${noteId}/key-insights`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ description: keyInsightsDesc, keyPoints })
                }).then(r => r.json())
            );
        }

        // 6. Update follow-up
        if (followUp !== undefined) {
            updates.push(
                fetch(`/api/v1/chat-notes/${noteId}/follow-up`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ description: followUp })
                }).then(r => r.json())
            );
        }

        // 7. Update references
        if (references !== undefined) {
            updates.push(
                fetch(`/api/v1/chat-notes/${noteId}/references`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ references })
                }).then(r => r.json())
            );
        }

        // Wait for all updates to complete
        const results = await Promise.all(updates);

        // Check if all succeeded
        const allSuccess = results.every(r => r.success);

        if (allSuccess) {
            showToast('All changes saved successfully', 'success');
            exitEditMode();

            // Reload modal via HTMX
            htmx.ajax('GET', `/fragments/chat-note-modal?id=${noteId}`, {
                target: '#modal-container',
                swap: 'innerHTML'
            });

            // Reload grid via HTMX
            htmx.ajax('GET', '/fragments/chat-notes', {
                target: '#notes-grid',
                swap: 'innerHTML'
            });

            // Trigger count update
            htmx.trigger(document.body, 'updateCounts');
        } else {
            const failures = results.filter(r => !r.success);
            showToast(`Some updates failed: ${failures.map(f => f.message).join(', ')}`, 'error');
        }
    } catch (err) {
        console.error('Error saving changes:', err);
        showToast('Failed to save changes', 'error');
    }
}

// ==================== Artifact/Attachment Editing ====================

function enterArtifactEditMode(index) {
    document.getElementById(`artifact-view-${index}`)?.classList.add('hidden');
    document.getElementById(`artifact-edit-${index}`)?.classList.remove('hidden');
    document.getElementById(`artifact-edit-btn-${index}`)?.classList.add('hidden');
}

function cancelArtifactEdit(index) {
    document.getElementById(`artifact-view-${index}`)?.classList.remove('hidden');
    document.getElementById(`artifact-edit-${index}`)?.classList.add('hidden');
    document.getElementById(`artifact-edit-btn-${index}`)?.classList.remove('hidden');
}

async function saveArtifactContent(noteId, index) {
    try {
        const editor = document.getElementById(`artifact-editor-${index}`);
        if (!editor) {
            showToast('Editor not found', 'error');
            return;
        }

        const content = editor.value;

        const response = await fetch(`/api/v1/chat-notes/${noteId}/artifacts/${index}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content })
        });

        const data = await response.json();

        if (data.success) {
            showToast('Artifact content saved successfully', 'success');
            cancelArtifactEdit(index);

            // Reload the artifact content
            htmx.ajax('GET', `/fragments/artifact/${noteId}/${index}`, {
                target: `#artifact-content-${index}`,
                swap: 'innerHTML'
            });
        } else {
            showToast('Error saving artifact: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (err) {
        console.error('Error saving artifact:', err);
        showToast('Failed to save artifact content', 'error');
    }
}

function enterAttachmentEditMode(index) {
    document.getElementById(`attachment-view-${index}`)?.classList.add('hidden');
    document.getElementById(`attachment-edit-${index}`)?.classList.remove('hidden');
    document.getElementById(`attachment-edit-btn-${index}`)?.classList.add('hidden');
}

function cancelAttachmentEdit(index) {
    document.getElementById(`attachment-view-${index}`)?.classList.remove('hidden');
    document.getElementById(`attachment-edit-${index}`)?.classList.add('hidden');
    document.getElementById(`attachment-edit-btn-${index}`)?.classList.remove('hidden');
}

async function saveAttachmentContent(noteId, index) {
    try {
        const editor = document.getElementById(`attachment-editor-${index}`);
        if (!editor) {
            showToast('Editor not found', 'error');
            return;
        }

        const content = editor.value;

        const response = await fetch(`/api/v1/chat-notes/${noteId}/attachments/${index}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content })
        });

        const data = await response.json();

        if (data.success) {
            showToast('Attachment content saved successfully', 'success');
            cancelAttachmentEdit(index);

            // Reload the attachment content
            htmx.ajax('GET', `/fragments/attachment/${noteId}/${index}`, {
                target: `#attachment-content-${index}`,
                swap: 'innerHTML'
            });
        } else {
            showToast('Error saving attachment: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (err) {
        console.error('Error saving attachment:', err);
        showToast('Failed to save attachment content', 'error');
    }
}

// ==================== Helper Functions ====================

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

// ==================== Export Functions ====================

// Export to window for inline onclick handlers
window.closeChatNoteModal = closeChatNoteModal;
window.initializeModal = initializeModal;
window.toggleArtifact = toggleArtifact;
window.toggleAttachment = toggleAttachment;
window.enterEditMode = enterEditMode;
window.exitEditMode = exitEditMode;
window.saveChatNote = saveChatNote;
window.saveStructuredEdits = saveStructuredEdits;
window.enterArtifactEditMode = enterArtifactEditMode;
window.cancelArtifactEdit = cancelArtifactEdit;
window.saveArtifactContent = saveArtifactContent;
window.enterAttachmentEditMode = enterAttachmentEditMode;
window.cancelAttachmentEdit = cancelAttachmentEdit;
window.saveAttachmentContent = saveAttachmentContent;
