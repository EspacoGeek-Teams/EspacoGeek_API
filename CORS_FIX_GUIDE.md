# CORS Configuration Fix

## Problem
The application was returning HTTP 403 errors for CORS preflight (OPTIONS) requests, preventing the frontend from making API calls.

## Root Causes Identified

1. **JWT Filter Processing OPTIONS Requests**: The `JwtAuthenticationFilter` was processing OPTIONS preflight requests, which should be allowed to pass through without authentication.

2. **Incorrect or Missing ALLOWED_ORIGINS**: The production environment may not have the correct `ALLOWED_ORIGINS` environment variable set.

## Changes Made

### 1. JwtAuthenticationFilter.java
Modified to skip JWT processing for OPTIONS requests:
```java
if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
    filterChain.doFilter(request, response);
    return;
}
```

### 2. CorsConfig.java
- Changed `allowedHeaders` to `*` to accept all headers
- Added `Set-Cookie` to exposed headers
- Added logging to show what origins are loaded at startup

### 3. DiagnosticController.java (NEW)
Created a diagnostic endpoint at `/actuator/diagnostic/cors` to verify CORS configuration in production.

## Production Deployment Steps

### Step 1: Set the Correct Environment Variable

Your production environment **MUST** have the `ALLOWED_ORIGINS` environment variable set correctly:

```bash
ALLOWED_ORIGINS=https://espacogeek.com,https://www.espacogeek.com,http://espacogeek.com,http://www.espacogeek.com
```

**Note**: Based on your error, you're using `api.espacogeek.com` as the backend. Make sure you include ALL the frontend domains that will call this API.

### Step 2: Verify Environment Variable

After deploying, check that the environment variable is loaded:

```bash
# Access the diagnostic endpoint (it's public via /actuator/**)
curl https://api.espacogeek.com/actuator/diagnostic/cors
```

Expected response:
```json
{
  "allowedOrigins": "https://espacogeek.com,https://www.espacogeek.com",
  "message": "CORS configuration loaded from environment"
}
```

### Step 3: Check Application Logs

Look for this log line at startup:
```
CORS Configuration loaded:
Allowed Origins: https://espacogeek.com,https://www.espacogeek.com
  - https://espacogeek.com
  - https://www.espacogeek.com
```

### Step 4: Test CORS Preflight

Test the OPTIONS request:
```bash
curl -X OPTIONS https://api.espacogeek.com/ \
  -H "Origin: https://espacogeek.com" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

You should see these headers in the response:
- `Access-Control-Allow-Origin: https://espacogeek.com`
- `Access-Control-Allow-Credentials: true`
- `Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS`

## Docker/Docker Compose Configuration

If you're using Docker Compose, update your environment variables:

```yaml
environment:
  ALLOWED_ORIGINS: "https://espacogeek.com,https://www.espacogeek.com,http://espacogeek.com,http://www.espacogeek.com"
```

Or in your `.env` file for Docker:
```bash
ALLOWED_ORIGINS=https://espacogeek.com,https://www.espacogeek.com,http://espacogeek.com,http://www.espacogeek.com
```

## GitHub Secrets (for CI/CD)

If you're using GitHub Actions, update the secret:

1. Go to your repository → Settings → Secrets and variables → Actions
2. Update or create the `ALLOWED_ORIGINS` secret with:
   ```
   https://espacogeek.com,https://www.espacogeek.com,http://espacogeek.com,http://www.espacogeek.com
   ```

## Troubleshooting

### Still Getting 403?

1. **Check Reverse Proxy/Load Balancer**: If you have Nginx, Apache, or a cloud load balancer in front of your Spring Boot app, make sure they're not stripping CORS headers or blocking OPTIONS requests.

2. **Check Firewall Rules**: Ensure OPTIONS requests are not being blocked by a WAF (Web Application Firewall).

3. **Browser Cache**: Clear your browser cache and try in incognito mode.

4. **Check Origin**: Make sure the frontend is sending the exact origin that's configured. Check browser DevTools → Network → Request Headers → Origin.

### Nginx Configuration Example

If using Nginx as a reverse proxy:

```nginx
location / {
    if ($request_method = 'OPTIONS') {
        add_header 'Access-Control-Allow-Origin' $http_origin always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Authorization,Content-Type,X-Requested-With,Accept' always;
        add_header 'Access-Control-Max-Age' 86400 always;
        return 204;
    }

    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## Additional Notes

### Multiple Origins
The application supports multiple origins separated by commas. No spaces:
```
ALLOWED_ORIGINS=https://site1.com,https://site2.com,http://localhost:3000
```

### Security Considerations
- Always use HTTPS in production
- Never use `*` as allowed origin when `allowCredentials` is `true`
- Include only the origins you actually need

## Verification Checklist

- [ ] `ALLOWED_ORIGINS` environment variable is set correctly
- [ ] Application logs show the correct origins at startup
- [ ] `/actuator/diagnostic/cors` returns the expected origins
- [ ] OPTIONS preflight requests return 200/204 status
- [ ] Browser DevTools shows CORS headers in response
- [ ] Frontend can successfully make API calls
