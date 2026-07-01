# Attendance Management System (AMS)

A full-featured **Java Spring Boot** web application with **MySQL** database for managing school attendance, grades, timetables, and reports.

## Features

| Module | Capabilities |
|--------|-------------|
| **Authentication** | Login, Logout, Forgot Password, Role-Based Access (Admin / Teacher / Student) |
| **Student Management** | Add, Update, Delete, View profiles |
| **Teacher Management** | Add, Update, Delete, Assign subjects |
| **Subject & Class** | Create subjects, assign teachers & students, manage sections |
| **Smart Attendance** | Manual entry, QR code sessions, face verification (demo), geo-location validation |
| **Marks** | Quiz / Assignment / Exam scores, weighted grade computation |
| **Timetable** | Create schedules, assign rooms, publish for students |
| **Reports** | Attendance & grade reports, export PDF & Excel |
| **Dashboards** | Role-specific analytics and widgets |

## Tech Stack

- **Java 17**
- **Spring Boot 3.2** (Web, Security, JPA, Thymeleaf)
- **MySQL 8**
- **ZXing** (QR codes)
- **Apache POI** (Excel export)
- **iText** (PDF export)

## Prerequisites

1. [Java JDK 17+](https://adoptium.net/)
2. [Apache Maven 3.8+](https://maven.apache.org/)
3. [MySQL Server 8](https://dev.mysql.com/downloads/mysql/)

## Database Setup

### Option A: Auto-create (recommended)

Spring Boot will create the `attendance_db` database and tables automatically on first run.

### Option B: Manual setup

```sql
mysql -u root -p < database/schema.sql
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/attendance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

## Run the Application

```bash
cd "ATTENDANCE MANAGEMENT SYSTEM"
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

## Demo Accounts

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Teacher | `teacher1` | `teacher123` |
| Student | `student1` | `student123` |

Sample data includes one teacher, one student, one subject (CS301 Database Systems), enrollments, and a published timetable.

## System Architecture

```
Login → Role Validation → Admin / Teacher / Student Dashboard
                              ↓
                         MySQL Database
                              ↓
                    Analytics & Reports
```

### Database Tables

- `users` — Authentication & roles
- `students`, `teachers`, `sections`
- `subjects`, `enrollments`
- `attendance`, `attendance_qr`, `face_recognition_log`
- `marks`, `timetables`
- `notifications`, `reports`, `audit_logs`

## Usage Guide

### Teacher Attendance Flow
1. Login as teacher → **Attendance**
2. Select subject → choose **Manual Entry** or **Generate QR Code**
3. Students scan QR (with optional face verification & geo-location)

### Student Attendance Flow
1. Login as student → **Scan QR**
2. Paste the session code from teacher's QR display
3. Allow location access → Submit

### Marks Flow
1. Teacher → **Marks** → select subject
2. Enter quiz, assignment, exam scores → **Save**
3. Click **Compute All Final Grades** (20% / 30% / 50% weighting)

## Project Structure

```
src/main/java/com/attendance/
├── config/          # Security, data initialization
├── controller/      # Web controllers (Admin, Teacher, Student)
├── model/           # JPA entities & enums
├── repository/      # Spring Data JPA repositories
├── security/        # UserDetailsService
└── service/         # Business logic
```

## Build JAR

```bash
mvn clean package -DskipTests
java -jar target/attendance-management-system-1.0.0.jar
```

## Notes

- **Face recognition** is simulated for demo purposes. Integrate OpenCV or a cloud API for production.
- **Geo-validation** uses a configurable radius (default 100m) in `application.properties`.
- **CSRF** is enabled by default in Spring Security; forms use standard POST submissions.

## License

Educational project — free to use and modify.
