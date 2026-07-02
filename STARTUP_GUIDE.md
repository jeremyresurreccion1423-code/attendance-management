# Attendance Management System — Startup Guide

## Quick start

```powershell
cd "C:\Users\Jeremy\Downloads\ATTENDANCE MANAGEMENT SYSTEM"
.\run.ps1
```

Or double-click `run.bat` from File Explorer.

Open **http://localhost:8081**

## Login

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |

## Configuration

1. Copy `application-local.properties.example` → `src/main/resources/application-local.properties`
2. Add your Supabase PostgreSQL URL, username, and password
3. Restart the app

Default port: **8081** (change in `application.properties` if needed)

## Database

- PostgreSQL via Supabase (Session Pooler)
- Schema reference: `database/schema_v2.sql`
- Maintenance: `database/fix_uuid_schema.sql`, `scripts/fix_database.py`

## Troubleshooting

**Port in use** — set `server.port=8082` in `application.properties`

**Database errors** — verify Supabase project is active and credentials in `application-local.properties`

**Won't start** — need Java 17+; `run.ps1` can download Maven if missing

## Related

Smart Library: `C:\Users\Jeremy\Downloads\LIBRARY MANAGEMENT\smart-library` (port 8080)
