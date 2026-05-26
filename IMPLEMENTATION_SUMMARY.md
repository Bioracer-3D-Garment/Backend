# Backend Implementation Summary: Cloudinary Gallery Support

## Overview
Implemented minimal, focused backend changes to support Cloudinary URL persistence for project cover images and galleries. The frontend uploads images directly to Cloudinary and sends their secure URLs to the backend for storage.

## Changes Made

### 1. **Entity Model (Project.java)**
- Added `List<String> gallery` field with `@ElementCollection` and `@CollectionTable`
- `gallery` persists to a separate `project_gallery` table (reliable DB persistence, not in-memory)
- Added getters/setters for gallery with null-safety defaults
- Updated constructors and equals/hashCode methods to include gallery

**Why**: @ElementCollection ensures the list is stored in the database as a separate table, not as a JSON blob, providing reliable persistence.

### 2. **DTO (ProjectInput.java)**
- Added `List<String> gallery` field to accept gallery URLs from frontend
- Name field marked `@NotBlank` for validation

**Why**: Minimal DTO change; frontend sends gallery as a list of Cloudinary URLs.

### 3. **Service (ProjectService.java)**
- Updated `createProject()` to accept and save gallery
- Enhanced `updateProjectDetails()` to:
  - Accept `Long userId` parameter for authorization
  - Validate user ownership before allowing updates
  - Handle gallery updates alongside name/coverImage
- Fixed comparisons to use `.equals()` instead of `!=` for Long objects

**Why**: Maintains authorization checks and ensures only project owners can modify.

### 4. **Controller (ProjectController.java)**
- Updated `updateProjectDetails()` endpoint to extract userId from Authorization header
- Pass userId to service for ownership validation

**Why**: Prevents unauthorized users from modifying other users' projects.

### 5. **Test Suite (ProjectServiceTest.java)**
- 11 comprehensive test cases covering:
  - Project creation with/without gallery
  - Gallery updates and persistence
  - Gallery clearing on update
  - Authorization checks (user cannot access/update other user's projects)
  - Invalid user handling
  - Database persistence verification
  - All-projects retrieval

**Test Results**: 12/12 passed (including SecurityConfigTest)

### 6. **Configuration Changes**

#### application-local.yaml
- Changed pipeline output-dir from local folder to `cloudinary`
- Added Cloudinary configuration section with environment variables:
  ```yaml
  cloudinary:
    cloud-name: ${NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME:}
    api-key: ${CLOUDINARY_API_KEY:}
    api-secret: ${CLOUDINARY_API_SECRET:}
  ```

#### .env
- Added `CLOUDINARY_API_KEY=614433938861825` for backend use
- Kept existing `NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME`, `NEXT_PUBLIC_CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`

### 7. **Test Data (DbInitializer.java)**
- Updated seed data to include sample Cloudinary URLs in gallery:
  ```java
  new Project("Test Project", admin, 
    "https://cdn.pixabay.com/photo/...",
    new ArrayList<>(List.of(
      "https://res.cloudinary.com/dfuh1mdzq/image/upload/v.../sample1.jpg",
      "https://res.cloudinary.com/dfuh1mdzq/image/upload/v.../sample2.jpg"
    )));
  ```

## API Contracts

### Create Project
```
POST /projects
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "My Project",
  "coverImage": "https://res.cloudinary.com/dfuh1mdzq/...",
  "gallery": [
    "https://res.cloudinary.com/dfuh1mdzq/...",
    "https://res.cloudinary.com/dfuh1mdzq/..."
  ]
}
```

### Update Project
```
PUT /projects/{id}
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "Updated Name",
  "coverImage": "https://res.cloudinary.com/dfuh1mdzq/...",
  "gallery": [...]
}
```

### Response
```json
{
  "id": 1,
  "name": "My Project",
  "coverImage": "https://res.cloudinary.com/dfuh1mdzq/...",
  "gallery": [
    "https://res.cloudinary.com/dfuh1mdzq/...",
    "https://res.cloudinary.com/dfuh1mdzq/..."
  ]
}
```

## Frontend Integration

The frontend should:
1. Upload images directly to Cloudinary using their SDK
2. Receive secure URLs from Cloudinary
3. Send those URLs to backend create/update endpoints
4. Example request body:
```javascript
{
  "name": "My Design",
  "coverImage": "https://res.cloudinary.com/dfuh1mdzq/image/upload/v1234567890/cover.jpg",
  "gallery": [
    "https://res.cloudinary.com/dfuh1mdzq/image/upload/v1234567890/img1.jpg",
    "https://res.cloudinary.com/dfuh1mdzq/image/upload/v1234567890/img2.jpg"
  ]
}
```

## Database Schema

The following tables are created:
- `project` - main project table with cover_image, name, user_id
- `project_gallery` - separate table for gallery URLs (project_id, element)

## What Was NOT Included (Per Requirements)

✅ **Intentionally Excluded**:
- No backend Cloudinary upload logic (frontend owns uploads)
- No additional Cloudinary SDK dependencies added
- No new upload endpoints
- Frontend .env setup is separate (not in Backend/.env path)

**Why**: Frontend uploading directly reduces backend load and matches Cloudinary best practices.

## Build & Test

```bash
cd Backend
API_URL=http://localhost:8000 \
STUDIO_URL=http://localhost:3000 \
JWT_SECRET_KEY=<your-key> \
NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME=dfuh1mdzq \
CLOUDINARY_API_KEY=614433938861825 \
CLOUDINARY_API_SECRET=<secret> \
./mvnw clean test
```

**Result**: BUILD SUCCESS, Tests: 12 passed, 0 failures

## Future Enhancements (Optional)

1. Add batch operations for multiple projects
2. Add image validation/sanitization
3. Add rate limiting on project creation
4. Add soft deletes for projects
5. Add pagination for project listings
6. Add full-text search on project names
7. Add image optimization hooks (Cloudinary transformations)

## Notes

- Gallery is truly persisted in database, not in-memory
- Authorization checks prevent unauthorized access
- Null-safe defaults for gallery (never null, always a list)
- All tests pass; code is production-ready for the feature scope

