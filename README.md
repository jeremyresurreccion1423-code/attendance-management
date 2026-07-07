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

## Demo accounts

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Teacher | `teacher1` | `teacher123` |
| Student | `student1` | `student123` |

Student credentials also work in the Library app (shared `public.users`).

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
