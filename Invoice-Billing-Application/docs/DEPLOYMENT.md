# SwiftLab Cloud Deployment Guide (Render & Supabase)

## 1. Cloud Architecture

```
                       Internet / Clients
          (Web Browser, Windows Desktop Sync, Android App)
                                |
                                v (HTTPS / TLS 1.3)
                  +---------------------------+
                  |     Render Web Service    |
                  |     (Docker Container)    |
                  |   Spring Boot 3 + Java 21 |
                  +---------------------------+
                                |
                                v (Encrypted PostgreSQL Pooler)
                  +---------------------------+
                  |    Supabase PostgreSQL   |
                  |     (Database + RLS)      |
                  +---------------------------+
```

---

## 2. Step-by-Step Render Deployment

### Prerequisites:
1. A GitHub or GitLab repository containing this SwiftLab codebase.
2. An active Supabase PostgreSQL database initialized using `/database/schema.sql`.

### Deployment Steps:
1. **Log in to Render:** Go to [dashboard.render.com](https://dashboard.render.com).
2. **Create New Web Service:**
   - Click **New +** $\rightarrow$ **Web Service**.
   - Connect your Git repository.
3. **Configure Build Settings:**
   - **Name:** `swiftlab-platform`
   - **Language:** `Docker`
   - **Region:** Select region closest to your Supabase database.
   - **Branch:** `main` (or active branch)
   - **Plan:** Starter or Standard
4. **Configure Environment Variables:**
   Under **Environment Variables**, configure the following:

   | Variable Name | Example / Format | Purpose |
   |---|---|---|
   | `SPRING_PROFILES_ACTIVE` | `cloud` | Activates PostgreSQL datasource |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db.<supabase-ref>.supabase.co:5432/postgres?sslmode=require` | Supabase DB URL |
   | `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
   | `SPRING_DATASOURCE_PASSWORD` | `<secure-db-password>` | Database password |
   | `PORT` | `8080` | Container HTTP port |
   | `JAVA_OPTS` | `-Xms256m -Xmx512m -XX:+UseG1GC` | JVM memory limits |

5. **Deploy:** Click **Create Web Service**. Render will execute the multi-stage Docker build, compile the Spring Boot application, and expose the live HTTPS URL (e.g. `https://swiftlab-platform.onrender.com`).

---

## 3. CORS & Security Verification for External Clients

The backend is configured with Spring Security and CORS enabled for:
- Android mobile client requests
- External web clients
- Windows desktop synchronization push/pull

Verify deployment health:
```bash
curl -I https://swiftlab-platform.onrender.com/api/auth/profile
```
(Should return HTTP 401 Unauthorized, verifying that the security filter chain is actively protecting endpoints).
