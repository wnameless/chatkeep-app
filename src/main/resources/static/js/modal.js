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

    // Load split-pane editor
    loadSplitPaneEditor(noteId);
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

function loadSplitPaneEditor(noteId) {
    // Fetch full markdown content for editing
    fetch(`/api/v1/chat-notes/${noteId}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                renderSplitPaneEditor(data.data);
            }
        })
        .catch(err => {
            console.error('Error loading editor:', err);
            showToast('Failed to load editor', 'error');
        });
}

function renderSplitPaneEditor(note) {
    const editModeContainer = document.getElementById('content-edit-mode');
    if (!editModeContainer) return;

    // Create split-pane layout with markdown editor and preview
    editModeContainer.innerHTML = `
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 h-[600px]">
            <!-- Editor Pane -->
            <div class="flex flex-col">
                <div class="flex items-center justify-between mb-2">
                    <label class="text-sm font-medium text-gray-700 dark:text-gray-300">Markdown Editor</label>
                    <span class="text-xs text-amber-600 dark:text-amber-400">
                        <i class="fas fa-lock mr-1"></i>
                        YAML frontmatter is locked
                    </span>
                </div>
                <textarea id="markdown-editor"
                          class="flex-1 w-full p-4 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 resize-none">${escapeHtml(note.fullMarkdown || '')}</textarea>
            </div>

            <!-- Preview Pane -->
            <div class="flex flex-col">
                <label class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Preview</label>
                <div id="markdown-preview"
                     class="flex-1 p-4 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 overflow-y-auto prose dark:prose-invert max-w-none">
                </div>
            </div>
        </div>
    `;

    // Initialize real-time preview
    const editor = document.getElementById('markdown-editor');
    const preview = document.getElementById('markdown-preview');

    if (editor && preview) {
        // Check if marked.js is loaded
        if (typeof marked !== 'undefined') {
            // Initial preview
            updatePreview(editor.value, preview);

            // Real-time preview update
            editor.addEventListener('input', function() {
                updatePreview(this.value, preview);
            });
        } else {
            preview.innerHTML = '<p class="text-gray-500">Preview unavailable (marked.js not loaded)</p>';
        }

        // Lock YAML frontmatter
        lockYAMLFrontmatter(editor);
    }
}

function updatePreview(markdown, previewElement) {
    if (typeof marked !== 'undefined') {
        const html = marked.parse(markdown);
        previewElement.innerHTML = html;
    }
}

function lockYAMLFrontmatter(editor) {
    // Detect YAML frontmatter (between --- lines)
    const yamlRegex = /^---\s*\n(.*?)\n---/s;
    const originalContent = editor.value;
    const match = originalContent.match(yamlRegex);

    if (match) {
        const yamlFrontmatter = match[0];
        const yamlEnd = yamlFrontmatter.length;

        editor.addEventListener('input', function() {
            const currentContent = this.value;
            const currentMatch = currentContent.match(yamlRegex);

            // If YAML frontmatter was modified, restore it
            if (!currentMatch || currentMatch[0] !== yamlFrontmatter) {
                showToast('YAML frontmatter cannot be modified', 'warning');
                // Restore original frontmatter
                const contentAfterYaml = currentContent.substring(currentContent.indexOf('---', 3) + 3);
                this.value = yamlFrontmatter + contentAfterYaml;
            }
        });
    }
}

function saveChatNote(noteId) {
    const editor = document.getElementById('markdown-editor');
    if (!editor) {
        showToast('Editor not found', 'error');
        return;
    }

    const markdownContent = editor.value;

    // Send update request to API
    fetch(`/api/v1/chat-notes/${noteId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            markdownContent: markdownContent
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('ChatNote saved successfully', 'success');
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
            showToast('Error saving ChatNote: ' + (data.message || 'Unknown error'), 'error');
        }
    })
    .catch(err => {
        console.error('Error saving ChatNote:', err);
        showToast('Failed to save ChatNote', 'error');
    });
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
