# Identified Issues

## 1. Unused Service: `EmailAuthService`
- **File**: `service.auth.domain.sonmoeum.EmailAuthService`
- **Description**: This service class appears to be unused in the current codebase. The `AuthController` directly uses `AuthenticationManager` and `UserCrudService` for login and user data retrieval. The `signUp` and `login` methods in `EmailAuthService` are not called by any controller or other service.
- **Recommendation**: Validate if this service is intended for future use or legacy code. If not needed, it should be removed to reduce technical debt.

## 2. Event Handling Complexity
- **File**: `handler.lesson.domain.sonmoeum.LessonEventHandler`
- **Description**: The system uses `EventPublisher` and `@EventListener` for decoupling logic. For example, `SubjectCreatedEvent` triggers `createLessonsFromSubject`. 
- **Recommendation**: Ensure that asynchronous event handling (`@Async`) is properly configured with a task executor to prevent thread exhaustion if event volume increases.
