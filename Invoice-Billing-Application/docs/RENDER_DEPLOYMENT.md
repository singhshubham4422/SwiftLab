# SwiftLab Production Deployment Guide - Render & Supabase

This guide provides the exact configuration, environment variables, health checks, port behavior, and post-deployment validation steps for deploying the SwiftLab Spring Boot platform on [Render](https://render.com).

---

## 1. Architecture Overview

```
                                Client Traffic
         (Web Browsers, Windows Desktop Clients, Android Mobile APK)
                                      |
                                      v HTTPS / Port 443
                     +----------------------------------+
                     |        Render Web Service        |
                     |   (Docker Container - Java 21)   |
                     |     Spring Boot + Thymeleaf      |
                     +----------------------------------+
                                      |
                                      v Encrypted TLS Connection (Port 5432)
                     +----------------------------------+
                     |       Supabase PostgreSQL        |
                     |    (Central Database with RLS)   |
                     +----------------------------------+
```

- **Web Server:** Spring Boot 3.2.3 serving both the responsive Thymeleaf web management console and the REST APIs.
- **No Vercel Needed:** Spring Boot directly renders Thymeleaf templates server-side.
- **Port Handling:** Render dynamically assigns the `PORT` environment variable. The Docker entrypoint uses `-Dserver.port=${PORT:-8080}` to bind to Render's allocated port automatically.
- **Database:** Supabase PostgreSQL cluster with Row Level Security (RLS).

---

## 2. Render Web Service Configuration

| Setting | Value / Recommended Configuration |
|---|---|
| **Service Type** | **Web Service** |
| **Runtime Environment** | **Docker** |
| **Repository** | `https://github.com/singhshubham4422/SwiftLab.git` |
| **Branch** | `main` |
| **Root Directory** | Leave blank (root of repository) |
| **Dockerfile Path** | `Dockerfile` |
| **Instance Type / Plan** | `Starter` or `Standard` (minimum 512MB RAM recommended) |
| **Auto-Deploy** | `Yes` (deploys on push to `main`) |
| **Health Check Path** | `/api/auth/profile` (returns HTTP 401 Unauthorized when unauthenticated, confirming security filter is live) |

---

## 3. Required Environment Variables in Render Dashboard

In the Render Dashboard under **Environment** $\rightarrow$ **Add Environment Variable**:

| Variable Name | Required | Example / Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | **YES** | `cloud` (Activates PostgreSQL profile and disables H2) |
| `DATABASE_URL` | **YES** | `jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require` |
| `DATABASE_USERNAME` | **YES** | `postgres` |
| `DATABASE_PASSWORD` | **YES** | `<YOUR_SUPABASE_DATABASE_PASSWORD>` |
| `SUPABASE_URL` | **YES** | `https://<PROJECT_REF>.supabase.co` |
| `SUPABASE_ANON_KEY` | **YES** | `<YOUR_SUPABASE_ANON_KEY>` |
| `SUPABASE_SERVICE_ROLE_KEY` | **YES** | `<YOUR_SUPABASE_SERVICE_ROLE_KEY>` *(Kept server-side in Render only!)* |
| `APP_JWT_SECRET` | **YES** | A random 32+ character string for signing session/API tokens |
| `JAVA_OPTS` | Optional | `-Xms256m -Xmx512m -XX:+UseG1GC` |

> [!CAUTION]
> **Zero Secret Leakage:** Never embed `SUPABASE_SERVICE_ROLE_KEY` in git repositories, the Android APK, or Windows desktop binaries. It must exist solely in Render's environment variable storage.

---

## 4. Step-by-Step Deployment Instructions

1. **Initialize Central Database:**
   - Follow [`docs/SUPABASE_SETUP.md`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/docs/SUPABASE_SETUP.md) to run `schema.sql`, `sync_schema.sql`, `indexes.sql`, `rls.sql`, and `seed.sql` in Supabase SQL Editor.
2. **Push Repository to GitHub:**
   - Commit and push the project to `https://github.com/singhshubham4422/SwiftLab.git`.
3. **Create Render Web Service:**
   - In Render Dashboard, click **New +** $\rightarrow$ **Web Service**.
   - Select your GitHub repository (`SwiftLab`).
   - Choose **Docker** as environment.
4. **Enter Environment Variables:**
   - Add all variables listed in Section 3 above.
5. **Deploy:**
   - Click **Create Web Service**.
   - Render will build the multi-stage Docker image, install dependencies, compile the JAR, and start the service.
   - Note the assigned public URL (e.g. `https://swiftlab-platform.onrender.com`).

---

## 5. Post-Deployment Verification Steps

### Step 1: Health Endpoint Check
Verify that the service is running and protected:
```bash
curl -I https://<your-render-subdomain>.onrender.com/api/auth/profile
```
- **Expected Status:** `HTTP/1.1 401 Unauthorized` (confirms Spring Security filter is active).

### Step 2: Web Console Login Check
1. Open `https://<your-render-subdomain>.onrender.com/login` in your web browser.
2. Log in with the bootstrapped administrator credentials:
   - **Username:** `admin`
   - **Password:** `admin123`
3. Verify that the Dashboard loads with Organization #1 (`SwiftLab Demo Enterprise`).

### Step 3: REST API Authentication & OpenAPI Check
1. Open the interactive OpenAPI documentation:
   `https://<your-render-subdomain>.onrender.com/swagger-ui.html`
2. Test the API login endpoint:
```bash
curl -X POST https://<your-render-subdomain>.onrender.com/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@swiftlab.io","password":"admin123"}'
```
- **Expected Result:** `200 OK` with JSON response containing bearer `token`, `user`, and `organization`.

### Step 4: Connecting Android & Windows Clients to Render
- **Android App:** Open Android app $\rightarrow$ Settings $\rightarrow$ Set Server Base URL to `https://<your-render-subdomain>.onrender.com/` $\rightarrow$ Tap Save.
- **Windows Desktop:** In `application-local.properties` or environment, set `APP_CLOUD_API_URL=https://<your-render-subdomain>.onrender.com`.
