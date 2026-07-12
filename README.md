# Attendance Management System (EduPresence)

Spring Boot web application for school attendance, grades, timetables, and reports. Shares authentication and a Supabase PostgreSQL database with the [Smart Library](../LIBRARY%20MANAGEMENT/smart-library) app (port 8080).

## Stack

Java 17 · Spring Boot 3.2 · PostgreSQL (Supabase) · Thymeleaf · Spring Security · BCrypt

## Quick start

**Prerequisites:** JDK 17+, Maven (or use `run.ps1`)

1. Copy `application-local.properties.example` → `src/main/resources/application-local.properties`
2. Set Supabase URL, username, and password (same credentials as Library)
3. Run:

```powershell
.\run.ps1
```

Open **http://localhost:8081**

Or: `mvn spring-boot:run "-Djava.net.preferIPv4Stack=true"`

## Configuration

| Variable | Purpose |
|----------|---------|
| `SUPABASE_DB_URL` | JDBC URL (Session Pooler) |
| `SUPABASE_DB_USER` | `postgres.<project-ref>` |
| `SUPABASE_DB_PASSWORD` | Database password |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail SMTP for OTP (optional) |
| `SUPER_ADMIN_SSO_SECRET` | Shared SSO secret (must match Library app) |
| `LIBRARY_APP_URL` | Library app base URL (default `http://localhost:8080`) |

## Demo accounts

| Role | Username | Password | Login page |
|------|----------|----------|------------|
| Super Admin | `superadmin` | `SuperAdmin@123` | `/super-admin/login` (System Control Center) |
| Admin | `admin` | `admin123` | `/login` |
| Teacher | `teacher1` | `teacher123` | `/login` |
| Student | `student1` | `student123` | `/login` |

Student credentials also work in the Library app (shared `public.users`).

## Super Admin (System Control Center)

The **Super Admin** role has its own login at `/super-admin/login` and a centralized dashboard at `/super-admin` that links to every admin-only feature in **both** Attendance and Library. No student, teacher, or other end-user screens are exposed from the Super Admin portal.

- Attendance admin pages open natively at `/admin/*` (Super Admin is allowed by RBAC).
- Library admin pages open via SSO bridge at `/super-admin/bridge/library?path=/admin/...`.
- Regular `/login` rejects Super Admin accounts and directs them to the System Control Center.
- Existing **Admin** roles and workflows are unchanged.

## Project layout

```
attendance-management/
├── database/          # SQL schemas and migrations
├── scripts/           # Maintenance utilities
├── src/               # Application source
├── uploads/           # Profile photos (gitignored)
├── run.ps1 / run.bat  # Startup scripts
└── pom.xml
```

## Build

```bash
mvn clean package -DskipTests
java -jar target/attendance-management-system-1.0.0.jar
```

## Related

**Smart Library** — `../LIBRARY MANAGEMENT/smart-library` on port **8080**

Both apps use one Supabase database: `public` schema (Attendance) and `library` schema (Library).
