# ChatKeep REST API Documentation

Base URL: `/api/v1`

## Archive Management Endpoints

### 1. Upload Archive
**POST** `/api/v1/archives`

Upload and process a new conversation archive from markdown format.

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
    "archiveCompleteness": "COMPLETE",
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
- `400 Bad Request` - Invalid archive format or validation failed
- `500 Internal Server Error` - Server processing error

---

### 2. Get Archive by ID (Lightweight)
**GET** `/api/v1/archives/{id}`

Retrieve a specific archive by ID with **lightweight response** - includes all metadata, conversation summary, and workarounds, but **excludes** artifact and attachment content (metadata only). Increments view count.

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
    "archiveCompleteness": "COMPLETE",
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
**GET** `/api/v1/archives/{id}/artifacts/{index}`

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
**GET** `/api/v1/archives/{id}/attachments/{index}`

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
**GET** `/api/v1/archives?page=0&size=20&sort=createdAt,desc`

Retrieve all archives with pagination and sorting.

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
        "archiveCompleteness": "COMPLETE",
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

### 6. Get Archives by User
**GET** `/api/v1/archives/user/{userId}`

Retrieve all archives created by a specific user.

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "67890abcdef",
      "title": "Conversation Title",
      // ... archive summary
    }
  ]
}
```

---

### 7. Search Archives
**GET** `/api/v1/archives/search?title=keyword`
**GET** `/api/v1/archives/search?tag=tagname`

Search archives by title or tag. Requires at least one parameter.

**Query Parameters:**
- `title` (optional) - Search in archive titles (case-insensitive)
- `tag` (optional) - Filter by tag

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "67890abcdef",
      "title": "Conversation Title",
      // ... archive summary
    }
  ]
}
```

**Error Responses:**
- `400 Bad Request` - Neither title nor tag parameter provided

---

### 8. Get Public Archives
**GET** `/api/v1/archives/public?page=0&size=20`

Retrieve public archives (paginated).

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

### 9. Update Archive Visibility
**PATCH** `/api/v1/archives/{id}/visibility?isPublic=true`

Update whether an archive is public or private.

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
    // ... full archive details
  }
}
```

**Error Responses:**
- `404 Not Found` - Archive does not exist

---

### 10. Delete Archive
**DELETE** `/api/v1/archives/{id}`

Permanently delete an archive.

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
When viewing an archive, call `GET /api/v1/archives/{id}` to receive:
- ✅ All metadata
- ✅ Conversation summary
- ✅ Artifact/attachment metadata (titles, filenames, types)
- ✅ Workarounds
- ❌ NO artifact/attachment content

### 2. On-Demand Content Loading
When user clicks to view a specific artifact or attachment:
- Call `GET /api/v1/archives/{id}/artifacts/{index}` for artifact content
- Call `GET /api/v1/archives/{id}/attachments/{index}` for attachment content

### 3. Benefits
- **Bandwidth**: 90%+ reduction when browsing
- **Performance**: Faster initial page loads
- **Mobile-friendly**: Lower data usage
- **Progressive**: Only pay for what you view

### Usage Example Flow
```javascript
// 1. Load archive (lightweight)
const archive = await fetch('/api/v1/archives/abc123');
// archive.artifacts = [{ title: "Script", type: "code" }]  // NO content

// 2. User clicks on first artifact
const artifact = await fetch('/api/v1/archives/abc123/artifacts/0');
// artifact.content = "def process():\n..."  // Full content loaded

// 3. User clicks on second attachment
const attachment = await fetch('/api/v1/archives/abc123/attachments/1');
// attachment.content = "base64encodedimage..."  // Full content loaded
```

---

## Archive Models

### ArchiveResponse (Summary)
Used in list endpoints:
```json
{
  "id": "string",
  "title": "string",
  "conversationDate": "date",
  "tags": ["string"],
  "originalPlatform": "string",
  "archiveCompleteness": "COMPLETE|PARTIAL|SUMMARIZED",
  "attachmentCount": "number",
  "artifactCount": "number",
  "viewCount": "number",
  "isPublic": "boolean",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### ArchiveDetailResponse
Full archive details including summary, artifacts, attachments, and workarounds.

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
   - Export multiple archives

4. **Analytics**
   - View statistics
   - Popular archives
   - User activity tracking

---

## Testing the API

### Example: Upload an Archive
```bash
curl -X POST http://localhost:8080/api/v1/archives \
  -H "Content-Type: application/json" \
  -d '{
    "markdownContent": "---\nARCHIVE_FORMAT_VERSION: 1.0\n...",
    "userId": "testuser"
  }'
```

### Example: Get Archive (Lightweight)
```bash
# Returns metadata only (no artifact/attachment content)
curl http://localhost:8080/api/v1/archives/abc123
```

### Example: Get Artifact Content
```bash
# Get first artifact's content (index 0)
curl http://localhost:8080/api/v1/archives/abc123/artifacts/0
```

### Example: Get Attachment Content
```bash
# Get second attachment's content (index 1)
curl http://localhost:8080/api/v1/archives/abc123/attachments/1
```

### Example: Get All Archives
```bash
curl http://localhost:8080/api/v1/archives?page=0&size=10
```

### Example: Search by Title
```bash
curl http://localhost:8080/api/v1/archives/search?title=python
```
