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

### 2. Get Archive by ID
**GET** `/api/v1/archives/{id}`

Retrieve a specific archive by ID. Increments view count.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "67890abcdef",
    "title": "Conversation Title",
    // ... full archive details
  }
}
```

**Error Responses:**
- `404 Not Found` - Archive does not exist
- `500 Internal Server Error` - Server error

---

### 3. Get All Archives (Paginated)
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

### 4. Get Archives by User
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

### 5. Search Archives
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

### 6. Get Public Archives
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

### 7. Update Archive Visibility
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

### 8. Delete Archive
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

### Example: Get All Archives
```bash
curl http://localhost:8080/api/v1/archives?page=0&size=10
```

### Example: Search by Title
```bash
curl http://localhost:8080/api/v1/archives/search?title=python
```
