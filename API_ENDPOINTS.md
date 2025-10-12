# ChatKeep REST API Documentation

Base URL: `/api/v1`

## Chat Note Management Endpoints

### 1. Upload Chat Note
**POST** `/api/v1/chat-notes`

Upload and process a new conversation chat note from markdown format.

**Request Body:**
```json
{
  "markdownContent": "---\nARCHIVE_FORMAT_VERSION: 1.0\n...",
  "userId": "user123"  // Optional, defaults to "anonymous"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Archive uploaded successfully",
  "data": {
    "id": "67890abcdef",
    "archiveVersion": "1.0",
    "archiveType": "conversation_summary",
    "title": "Conversation Title",
    "conversationDate": "2025-10-12",
    "tags": ["ai", "development"],
    "originalPlatform": "Claude",
    "chatNoteCompleteness": "COMPLETE",
    "attachmentCount": 2,
    "artifactCount": 3,
    "summary": { ... },
    "artifacts": [ ... ],
    "attachments": [ ... ],
    "workarounds": [ ... ],
    "userId": "user123",
    "isPublic": false,
    "viewCount": 0,
    "createdAt": "2025-10-12T08:45:30Z",
    "updatedAt": "2025-10-12T08:45:30Z"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid chat note format or validation failed
- `500 Internal Server Error` - Server processing error

---

### 2. Get Chat Note by ID (Lightweight)
**GET** `/api/v1/chat-notes/{id}`

Retrieve a specific chat note by ID with **lightweight response** - includes all metadata, conversation summary, and workarounds, but **excludes** artifact and attachment content (metadata only). Increments view count.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "67890abcdef",
    "archiveVersion": "1.0",
    "archiveType": "conversation_summary",
    "title": "Conversation Title",
    "conversationDate": "2025-10-12",
    "tags": ["ai", "development"],
    "originalPlatform": "Claude",
    "chatNoteCompleteness": "COMPLETE",
    "attachmentCount": 2,
    "artifactCount": 3,
    "summary": {
      "initialQuery": { ... },
      "keyInsights": { ... },
      "followUpExplorations": { ... },
      "references": [ ... ]
    },
    "artifacts": [
      {
        "type": "code",
        "title": "Data Processing Script",
        "language": "python",
        "version": "final",
        "iterations": "3",
        "evolutionNotes": "..."
        // Note: "content" field is NOT included
      }
    ],
    "attachments": [
      {
        "filename": "document1.pdf",
        "isSummarized": false,
        "originalSize": "2.5 MB",
        "summarizationLevel": null,
        "contentPreserved": null,
        "processingLimitation": null
        // Note: "content" field is NOT included
      }
    ],
    "workarounds": [ ... ],
    "userId": "user123",
    "isPublic": false,
    "viewCount": 15,
    "createdAt": "2025-10-12T08:45:30Z",
    "updatedAt": "2025-10-12T08:45:30Z"
  }
}
```

**Benefits of Lightweight Response:**
- ✅ Includes conversation summary (essential context)
- ✅ Includes artifact/attachment metadata (titles, filenames, types)
- ✅ Includes workarounds (processing context)
- ⚡ Excludes large content fields (90%+ bandwidth reduction)
- 📱 Perfect for browsing/listing views
- 🔄 Load content on-demand via separate endpoints

**Error Responses:**
- `404 Not Found` - Archive does not exist
- `500 Internal Server Error` - Server error

---

### 3. Get Artifact Content
**GET** `/api/v1/chat-notes/{id}/artifacts/{index}`

Retrieve the full content of a specific artifact by its index (0-based).

**Path Parameters:**
- `id` (required) - Archive ID
- `index` (required) - Artifact index (0-based, order matches metadata list)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "type": "code",
    "title": "Data Processing Script",
    "language": "python",
    "version": "final",
    "iterations": "3",
    "evolutionNotes": "Final version after debugging...",
    "content": "def process_data(input_file):\n    # Full code here..."
  }
}
```

**Error Responses:**
- `404 Not Found` - Archive does not exist
- `400 Bad Request` - Invalid artifact index
- `500 Internal Server Error` - Server error

---

### 4. Get Attachment Content
**GET** `/api/v1/chat-notes/{id}/attachments/{index}`

Retrieve the full content of a specific attachment by its index (0-based).

**Path Parameters:**
- `id` (required) - Archive ID
- `index` (required) - Attachment index (0-based, order matches metadata list)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "filename": "diagram.png",
    "content": "iVBORw0KGgoAAAANSUhEUgAAAA...",  // Base64 encoded or markdown
    "isSummarized": false,
    "originalSize": "125 KB",
    "summarizationLevel": null,
    "contentPreserved": null,
    "processingLimitation": null
  }
}
```

**Error Responses:**
- `404 Not Found` - Archive does not exist
- `400 Bad Request` - Invalid attachment index
- `500 Internal Server Error` - Server error

---

### 5. Get All Archives (Paginated)
**GET** `/api/v1/chat-notes?page=0&size=20&sort=createdAt,desc`

Retrieve all chat notes with pagination and sorting.

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page
- `sort` (optional, default: createdAt,desc) - Sort field and direction

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "67890abcdef",
        "title": "Conversation Title",
        "conversationDate": "2025-10-12",
        "tags": ["ai", "development"],
        "originalPlatform": "Claude",
        "chatNoteCompleteness": "COMPLETE",
        "attachmentCount": 2,
        "artifactCount": 3,
        "viewCount": 15,
        "isPublic": false,
        "createdAt": "2025-10-12T08:45:30Z",
        "updatedAt": "2025-10-12T08:45:30Z"
      }
    ],
    "pageable": { ... },
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20
  }
}
```

---

### 6. Get Chat Notes by User
**GET** `/api/v1/chat-notes/user/{userId}`

Retrieve all chat notes created by a specific user.

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "67890abcdef",
      "title": "Conversation Title",
      // ... chat note summary
    }
  ]
}
```

---

### 7. Search Archives
**GET** `/api/v1/chat-notes/search?title=keyword`
**GET** `/api/v1/chat-notes/search?tag=tagname`

Search chat notes by title or tag. Requires at least one parameter.

**Query Parameters:**
- `title` (optional) - Search in chat note titles (case-insensitive)
- `tag` (optional) - Filter by tag

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "67890abcdef",
      "title": "Conversation Title",
      // ... chat note summary
    }
  ]
}
```

**Error Responses:**
- `400 Bad Request` - Neither title nor tag parameter provided

---

### 8. Get Public Archives
**GET** `/api/v1/chat-notes/public?page=0&size=20`

Retrieve public chat notes (paginated).

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

### 9. Update Chat Note Visibility
**PATCH** `/api/v1/chat-notes/{id}/visibility?isPublic=true`

Update whether a chat note is public or private.

**Query Parameters:**
- `isPublic` (required) - Boolean flag for visibility

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Visibility updated successfully",
  "data": {
    "id": "67890abcdef",
    "isPublic": true,
    // ... full chat note details
  }
}
```

**Error Responses:**
- `404 Not Found` - Archive does not exist

---

### 10. Delete Chat Note
**DELETE** `/api/v1/chat-notes/{id}`

Permanently delete a chat note.

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Archive deleted successfully",
  "data": null
}
```

**Error Responses:**
- `404 Not Found` - Archive does not exist

---

## Tag-Based Filtering Endpoints

### 10.1. Filter by Multiple Tags (Global)
**GET** `/api/v1/chat-notes/filter/tags?tags=java,spring&operator=AND`

Filter chat notes by multiple tags with AND/OR logic.

**Query Parameters:**
- `tags` (required) - Comma-separated list of tags
- `operator` (optional, default: AND) - Filter operation: `AND` (must have ALL tags) or `OR` (must have ANY tag)
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "67890abcdef",
        "title": "Spring Boot Tutorial",
        "tags": ["java", "spring", "backend"],
        // ... chat note summary
      }
    ],
    "totalElements": 25,
    "totalPages": 2
  }
}
```

**Examples:**
```bash
# Find notes with ALL tags (AND operation)
GET /api/v1/chat-notes/filter/tags?tags=java,spring&operator=AND

# Find notes with ANY tag (OR operation)
GET /api/v1/chat-notes/filter/tags?tags=java,python,go&operator=OR
```

---

### 10.2. Filter User's Notes by Tags
**GET** `/api/v1/chat-notes/user/{userId}/filter/tags?tags=java,spring&operator=AND&lifecycle=active`

Filter a specific user's chat notes by tags with lifecycle support.

**Path Parameters:**
- `userId` (required) - User ID

**Query Parameters:**
- `tags` (required) - Comma-separated list of tags
- `operator` (optional, default: AND) - `AND` or `OR`
- `lifecycle` (optional, default: active) - `active` (not archived/trashed) or `all`
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "67890abcdef",
        "title": "My Java Notes",
        "tags": ["java", "spring"],
        "isArchived": false,
        "isTrashed": false,
        // ... chat note summary
      }
    ]
  }
}
```

**Examples:**
```bash
# User's active notes with ALL tags
GET /api/v1/chat-notes/user/john/filter/tags?tags=java,spring&operator=AND&lifecycle=active

# User's all notes (including archived) with ANY tag
GET /api/v1/chat-notes/user/john/filter/tags?tags=tutorial,guide&operator=OR&lifecycle=all
```

---

### 10.3. Filter Active Notes by Tags (Global)
**GET** `/api/v1/chat-notes/filter/tags/active?tags=java,spring&operator=OR`

Filter active (not archived, not trashed) chat notes by tags across all users.

**Query Parameters:**
- `tags` (required) - Comma-separated list of tags
- `operator` (optional, default: AND) - `AND` or `OR`
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "67890abcdef",
        "title": "Active Tutorial",
        "tags": ["java", "spring"],
        "isArchived": false,
        "isTrashed": false,
        // ... chat note summary
      }
    ]
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid operator or lifecycle parameter
- `500 Internal Server Error` - Server error

---

## Tag Filtering Usage Examples

### Quick Filters
```bash
# Find all Python tutorials
curl "http://localhost:8080/api/v1/chat-notes/filter/tags?tags=python,tutorial&operator=AND"

# Find notes about Java OR Spring OR React
curl "http://localhost:8080/api/v1/chat-notes/filter/tags?tags=java,spring,react&operator=OR"
```

### User-Specific Filters
```bash
# John's active notes about databases
curl "http://localhost:8080/api/v1/chat-notes/user/john/filter/tags?tags=database,sql&lifecycle=active"

# All of Mary's notes with machine-learning tag
curl "http://localhost:8080/api/v1/chat-notes/user/mary/filter/tags?tags=machine-learning&lifecycle=all"
```

### Benefits
- ✅ **Fast Filtering** - MongoDB indexed tag queries
- ✅ **Flexible Logic** - AND/OR operations
- ✅ **Lifecycle-Aware** - Filter active, archived, or all notes
- ✅ **User-Scoped** - Personal tag filtering
- ✅ **Paginated** - Handle large result sets

---

## Favorites Management Endpoints

### 10.4. Toggle Favorite Status
**PATCH** `/api/v1/chat-notes/{id}/favorite?isFavorite=true`

Star or unstar a chat note for quick access. Favorites are independent from lifecycle states.

**Query Parameters:**
- `isFavorite` (required) - Boolean flag (true to favorite, false to unfavorite)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Favorite status updated successfully",
  "data": {
    "id": "67890abcdef",
    "isFavorite": true,
    // ... full chat note details
  }
}
```

**Error Responses:**
- `404 Not Found` - Chat note does not exist

---

### 10.5. Get User's Favorite Notes
**GET** `/api/v1/chat-notes/user/{userId}/favorites?page=0&size=20`

Retrieve all favorited chat notes for a user (paginated). Includes notes in all lifecycle states (active, archived, trashed).

**Path Parameters:**
- `userId` (required) - User ID

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "67890abcdef",
        "title": "Important Reference",
        "isFavorite": true,
        "isArchived": false,
        "isTrashed": false,
        // ... chat note summary
      }
    ],
    "totalElements": 12,
    "totalPages": 1
  }
}
```

---

### 10.6. Get User's Active Favorite Notes
**GET** `/api/v1/chat-notes/user/{userId}/favorites/active?page=0&size=20`

Retrieve only active (not archived, not trashed) favorited notes for a user.

**Path Parameters:**
- `userId` (required) - User ID

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "67890abcdef",
        "title": "Quick Reference Guide",
        "isFavorite": true,
        "isArchived": false,
        "isTrashed": false,
        // ... chat note summary
      }
    ]
  }
}
```

---

## Favorites Usage Examples

### Toggle Favorites
```bash
# Mark as favorite
curl -X PATCH "http://localhost:8080/api/v1/chat-notes/abc123/favorite?isFavorite=true"

# Remove from favorites
curl -X PATCH "http://localhost:8080/api/v1/chat-notes/abc123/favorite?isFavorite=false"
```

### View Favorites
```bash
# All favorites (including archived)
curl "http://localhost:8080/api/v1/chat-notes/user/john/favorites"

# Active favorites only
curl "http://localhost:8080/api/v1/chat-notes/user/john/favorites/active"
```

### Benefits
- ⭐ **Quick Access** - Star important notes for easy retrieval
- 🔄 **Lifecycle Independent** - Favorite notes in any state (active/archived/trashed)
- 📌 **Personal** - Each user maintains their own favorites
- 🎯 **Focused View** - Filter out noise, see only what matters

---

## Lifecycle Management Endpoints

### 11. Update Archive Status (renumbered from previous)
**PATCH** `/api/v1/chat-notes/{id}/archive?isArchived=true`

Archive or unarchive a chat note. Archived notes are hidden from the main/active view but remain accessible.

**Query Parameters:**
- `isArchived` (required) - Boolean flag (true to archive, false to unarchive)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Archive status updated successfully",
  "data": {
    "id": "67890abcdef",
    "isArchived": true,
    "isTrashed": false,
    // ... full chat note details
  }
}
```

**Error Responses:**
- `404 Not Found` - Chat note does not exist

---

### 12. Move to Trash (Soft Delete)
**PATCH** `/api/v1/chat-notes/{id}/trash`

Move a chat note to trash (soft delete). Trashed notes can be restored within 30 days before auto-purge.

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Chat note moved to trash",
  "data": {
    "id": "67890abcdef",
    "isTrashed": true,
    "trashedAt": "2025-10-12T10:30:00Z",
    // ... full chat note details
  }
}
```

**Error Responses:**
- `404 Not Found` - Chat note does not exist

---

### 13. Restore from Trash
**PATCH** `/api/v1/chat-notes/{id}/restore`

Restore a chat note from trash back to active status.

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Chat note restored from trash",
  "data": {
    "id": "67890abcdef",
    "isTrashed": false,
    "trashedAt": null,
    // ... full chat note details
  }
}
```

**Error Responses:**
- `404 Not Found` - Chat note does not exist

---

### 14. Permanently Delete
**DELETE** `/api/v1/chat-notes/{id}/permanent`

Permanently delete a chat note from the database (hard delete). Cannot be undone.

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Chat note permanently deleted",
  "data": null
}
```

**Error Responses:**
- `404 Not Found` - Chat note does not exist

---

### 15. Get Active Chat Notes for User
**GET** `/api/v1/chat-notes/user/{userId}/active?page=0&size=20`

Retrieve active (not archived, not trashed) chat notes for a specific user.

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 45,
    "totalPages": 3
  }
}
```

---

### 16. Get Archived Chat Notes for User
**GET** `/api/v1/chat-notes/user/{userId}/archived`

Retrieve archived (but not trashed) chat notes for a specific user.

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "67890abcdef",
      "title": "Archived Conversation",
      "isArchived": true,
      "isTrashed": false,
      // ... chat note summary
    }
  ]
}
```

---

### 17. Get Trashed Chat Notes for User
**GET** `/api/v1/chat-notes/user/{userId}/trash`

Retrieve trashed chat notes for a specific user (within 30-day recovery window).

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "67890abcdef",
      "title": "Trashed Conversation",
      "isTrashed": true,
      "trashedAt": "2025-10-12T10:30:00Z",
      // ... chat note summary
    }
  ]
}
```

---

### 18. Get All Archived Chat Notes (Admin)
**GET** `/api/v1/chat-notes/archived?page=0&size=20`

Retrieve all archived chat notes across all users (paginated). For admin/global view.

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 150,
    "totalPages": 8
  }
}
```

---

### 19. Get All Trashed Chat Notes (Admin)
**GET** `/api/v1/chat-notes/trash?page=0&size=20`

Retrieve all trashed chat notes across all users (paginated). For admin/global view.

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 25,
    "totalPages": 2
  }
}
```

---

## Lifecycle Management Flow

### Chat Note States
- **Active**: `isArchived=false`, `isTrashed=false` - Normal working notes
- **Archived**: `isArchived=true`, `isTrashed=false` - Old notes, hidden from main view
- **Trashed**: `isTrashed=true` - Soft deleted, 30-day recovery window
- **Permanently Deleted**: Removed from database (hard delete)

### Typical Workflows

#### Archive Old Notes
```bash
# Archive a note
curl -X PATCH http://localhost:8080/api/v1/chat-notes/abc123/archive?isArchived=true

# View archived notes
curl http://localhost:8080/api/v1/chat-notes/user/john/archived

# Unarchive a note
curl -X PATCH http://localhost:8080/api/v1/chat-notes/abc123/archive?isArchived=false
```

#### Soft Delete with Recovery
```bash
# Move to trash
curl -X PATCH http://localhost:8080/api/v1/chat-notes/abc123/trash

# View trashed notes
curl http://localhost:8080/api/v1/chat-notes/user/john/trash

# Restore from trash
curl -X PATCH http://localhost:8080/api/v1/chat-notes/abc123/restore
```

#### Permanent Deletion
```bash
# Permanently delete (cannot be undone)
curl -X DELETE http://localhost:8080/api/v1/chat-notes/abc123/permanent
```

### Auto-Purge
- Trashed notes older than 30 days are automatically purged
- Scheduled job runs daily to clean up old trashed notes
- No recovery possible after auto-purge

---

## Response Format

All API responses follow this structure:

```json
{
  "success": true/false,
  "message": "Optional message",
  "data": { ... }  // Response data or null
}
```

---

## Lazy Loading Pattern

To optimize bandwidth and performance, the API implements a **lazy loading pattern** for artifacts and attachments:

### 1. Initial Browse (Lightweight)
When viewing a chat note, call `GET /api/v1/chat-notes/{id}` to receive:
- ✅ All metadata
- ✅ Conversation summary
- ✅ Artifact/attachment metadata (titles, filenames, types)
- ✅ Workarounds
- ❌ NO artifact/attachment content

### 2. On-Demand Content Loading
When user clicks to view a specific artifact or attachment:
- Call `GET /api/v1/chat-notes/{id}/artifacts/{index}` for artifact content
- Call `GET /api/v1/chat-notes/{id}/attachments/{index}` for attachment content

### 3. Benefits
- **Bandwidth**: 90%+ reduction when browsing
- **Performance**: Faster initial page loads
- **Mobile-friendly**: Lower data usage
- **Progressive**: Only pay for what you view

### Usage Example Flow
```javascript
// 1. Load chat note (lightweight)
const chatNote = await fetch('/api/v1/chat-notes/abc123');
// chatNote.artifacts = [{ title: "Script", type: "code" }]  // NO content

// 2. User clicks on first artifact
const artifact = await fetch('/api/v1/chat-notes/abc123/artifacts/0');
// artifact.content = "def process():\n..."  // Full content loaded

// 3. User clicks on second attachment
const attachment = await fetch('/api/v1/chat-notes/abc123/attachments/1');
// attachment.content = "base64encodedimage..."  // Full content loaded
```

---

## Archive Models

### ChatNoteResponse (Summary)
Used in list endpoints:
```json
{
  "id": "string",
  "title": "string",
  "conversationDate": "date",
  "tags": ["string"],
  "originalPlatform": "string",
  "chatNoteCompleteness": "COMPLETE|PARTIAL|SUMMARIZED",
  "attachmentCount": "number",
  "artifactCount": "number",
  "viewCount": "number",
  "isPublic": "boolean",
  "isArchived": "boolean",
  "isTrashed": "boolean",
  "isFavorite": "boolean",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### ArchiveDetailResponse
Full chat note details including summary, artifacts, attachments, and workarounds.

---

## Future Enhancements

1. **Authentication & Authorization**
   - JWT-based authentication
   - Role-based access control
   - User ownership verification

2. **Advanced Search**
   - Full-text search across conversation content
   - Date range filtering
   - Platform filtering
   - Completeness status filtering

3. **Batch Operations**
   - Bulk upload
   - Bulk delete
   - Export multiple chat notes

4. **Analytics**
   - View statistics
   - Popular chat notes
   - User activity tracking

---

## Testing the API

### Example: Upload an Archive
```bash
curl -X POST http://localhost:8080/api/v1/chat-notes \
  -H "Content-Type: application/json" \
  -d '{
    "markdownContent": "---\nARCHIVE_FORMAT_VERSION: 1.0\n...",
    "userId": "testuser"
  }'
```

### Example: Get Chat Note (Lightweight)
```bash
# Returns metadata only (no artifact/attachment content)
curl http://localhost:8080/api/v1/chat-notes/abc123
```

### Example: Get Artifact Content
```bash
# Get first artifact's content (index 0)
curl http://localhost:8080/api/v1/chat-notes/abc123/artifacts/0
```

### Example: Get Attachment Content
```bash
# Get second attachment's content (index 1)
curl http://localhost:8080/api/v1/chat-notes/abc123/attachments/1
```

### Example: Get All Archives
```bash
curl http://localhost:8080/api/v1/chat-notes?page=0&size=10
```

### Example: Search by Title
```bash
curl http://localhost:8080/api/v1/chat-notes/search?title=python
```
