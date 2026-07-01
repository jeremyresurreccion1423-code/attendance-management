# Attendance Management System - Startup Guide

## Quick Start (Opening Windsurf Fresh)

### Step 1: Start the Application
Simply run the batch file:
```
run.bat
```

The application will start on **http://localhost:8081**

### Step 2: Login
Use these credentials:
- **Username**: `admin`
- **Password**: `admin123`

## Current Configuration

### Database Connection
- **Type**: Supabase PostgreSQL (Session Pooler)
- **Host**: aws-1-ap-southeast-1.pooler.supabase.com
- **Port**: 5432
- **Database**: postgres
- **Username**: postgres.zszwwwpzyepuduielccq
- **Password**: Carlomercado@123

### Application Settings
- **Port**: 8081
- **DDL Auto**: none (validates schema but doesn't modify)
- **Hibernate Dialect**: PostgreSQL

## Important Notes

### Database Schema
The application uses existing Supabase tables with BIGINT ID columns. The Java entities are configured to match this schema.

### Data Initialization
The DataInitializer is currently disabled to avoid schema conflicts. Users are created manually via SQL scripts.

### Adding New Users
To add new users, run SQL commands in Supabase SQL Editor:
```sql
INSERT INTO users (username, password, role, last_login, enabled, created_at)
VALUES ('newuser', 'password123', 'STUDENT', NULL, true, NOW());
```

## Troubleshooting

### Port Already in Use
If port 8081 is busy, change it in `src/main/resources/application.properties`:
```properties
server.port=8082
```

### Database Connection Issues
1. Check Supabase project status (not paused)
2. Verify credentials in application.properties
3. Test connection using pgAdmin or DBeaver

### Application Won't Start
1. Check if Java 17+ is installed
2. Ensure Maven is available (run.bat will download if needed)
3. Check for compilation errors in IDE

## File Locations
- **Configuration**: `src/main/resources/application.properties`
- **Startup Script**: `run.bat`
- **Database Schema**: `database/schema_postgres.sql`
- **Sample Users SQL**: `database/insert_sample_users.sql`

## Support
For issues, check:
1. Console output when running run.bat
2. Supabase dashboard for database status
3. IDE error logs for compilation issues
