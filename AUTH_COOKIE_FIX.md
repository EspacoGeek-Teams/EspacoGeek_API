# Authentication Cookie Fix

## Problem
The login GraphQL query was returning the JWT in the response body but not setting the `Set-Cookie` header, causing the browser to never store an auth cookie. This resulted in subsequent `isLogged` queries returning UNAUTHORIZED.

## Root Cause
- Spring Boot 3.3 with virtual threads enabled (`spring.threads.virtual.enabled=true`)
- GraphQL operations execute on virtual threads via Spring GraphQL's reactive execution model
- `RequestContextHolder.getRequestAttributes()` returns `null` in virtual thread context
- The code in `UserController.login` that attempted to set cookies via `HttpServletResponse` was never executed

## Solution
Removed the `RequestContextHolder` usage from `UserController.login` and `UserController.doLogoutUser`. 

The application now relies solely on `GraphQlCookieInterceptor` which:
- Operates at the web layer (not within virtual thread context)
- Intercepts all GraphQL responses
- For `login` operations: extracts the JWT token from response data and adds `Set-Cookie` header
- For `logout` operations: adds a clear cookie header
- Properly handles cross-origin requests with correct `SameSite` and `Secure` attributes

## Changes Made
- **src/main/java/com/espacogeek/geek/controllers/UserController.java**
  - Removed `RequestContextHolder` usage from `doLoginUser` method (GraphQL operation: `login`)
  - Removed `RequestContextHolder` usage from `doLogoutUser` method (GraphQL operation: `logout`)
  - Removed unused imports: `HttpHeaders`, `ResponseCookie`, `RequestContextHolder`, `ServletRequestAttributes`, `HttpServletRequest`, `HttpServletResponse`

## How It Works Now
1. User calls `login` GraphQL query with credentials
2. `UserController.doLoginUser` authenticates and returns JWT token in response data
3. `GraphQlCookieInterceptor` intercepts the response
4. Interceptor extracts token from `response.getData().get("login")`
5. Interceptor adds `Set-Cookie` header to `response.getResponseHeaders()`
6. Browser receives both token in body and Set-Cookie header

## Testing
All existing tests pass, including:
- `LoginQueryTest` - validates login functionality
- All user-related tests

## Configuration
Cookie behavior is configured in `application.properties`:
```properties
security.jwt.cookie-name=EG_AUTH
security.jwt.cookie-path=/
security.jwt.same-site-when-same-site=Lax
security.jwt.expiration-ms=604800000
```

CORS must allow credentials:
```properties
spring.mvc.cors.allowed-origins=http://localhost:3000
```

Frontend must use `credentials: 'include'` in fetch requests.
