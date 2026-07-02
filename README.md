# Attendance Management System (EduPresence)

Spring Boot web app for school attendance, grades, timetables, and reports.

## Features

| Module | Capabilities |
|--------|-------------|
| **Authentication** | Login, logout, forgot password (OTP), role-based access (Admin / Teacher / Student) |
| **Student Management** | Add, update, delete, view profiles |
| **Teacher Management** | Add, update, delete, assign subjects |
| **Subject & Class** | Create subjects, assign teachers and students, manage sections |
| **Smart Attendance** | Manual entry, QR code sessions, geo-location validation |
| **Marks** | Quiz / assignment / exam scores, weighted grade computation |
| **Timetable** | Create schedules, assign rooms, publish for students |
| **Reports** | Attendance and grade reports, export PDF and Excel |
| **Dashboards** | Role-specific analytics |

## Tech stack

- Java 17, Spring Boot 3.2
- PostgreSQL (Supabase)
- Thymeleaf, Spring Security
- ZXing (QR), Apache POI (Excel), iText (PDF)

## Project structure

```
ATTENDANCE MANAGEMENT SYSTEM/
├── database/          # SQL schemas and maintenance scripts
├── docs/              # Architecture notes
├── scripts/           # Utility scripts (e.g. fix_database.py)
├── src/main/java/com/attendance/
│   ├── config/        # Security, data initialization
│   ├── controller/    # Web and REST controllers
│   ├── dto/           # Data transfer objects
│   ├── model/         # JPA entities and enums
│   ├── repository/    # Spring Data JPA
│   ├── security/      # UserDetailsService
│   ├── service/       # Business logic
│   └── util/          # Helpers
├── src/main/resources/
│   ├── application.properties
│   ├── application-local.properties   # gitignored — your secrets
│   ├── static/        # CSS, JS, images
│   └── templates/     # Thymeleaf pages (admin, auth, student, teacher, profile)
├── run.ps1            # Start app (PowerShell)
├── run.bat            # Start app (CMD)
└── pom.xml
```

## Configuration

1. Copy `application-local.properties.example` to  
   `src/main/resources/application-local.properties`
2. Set your Supabase PostgreSQL URL, username, and password
3. Optionally set `MAIL_USERNAME` and `MAIL_PASSWORD` for OTP email

## Run

```powershell
cd "C:\Users\Jeremy\Downloads\ATTENDANCE MANAGEMENT SYSTEM"
.\run.ps1
```

Open **http://localhost:8081**

Or with Maven directly:

```bash
mvn spring-boot:run
```

## Demo accounts

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Teacher | `teacher1` | `teacher123` |
| Student | `student1` | `student123` |

## Build JAR

```bash
mvn clean package -DskipTests
java -jar target/attendance-management-system-1.0.0.jar
```

## Related project

Library system: `C:\Users\Jeremy\Downloads\LIBRARY MANAGEMENT\smart-library` (port **8080**)
