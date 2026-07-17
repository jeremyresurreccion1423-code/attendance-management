# LU Centralized System — Final Check Demo Cheat Sheet

**Print this page.** Fill in `________` before the check.

---

## Before the check (Laptop 1 = Server)

1. Same WiFi as Laptop 2.
2. Get Server IP: open PowerShell → `ipconfig` → **IPv4 Address**: `________________`
3. Start **both** apps (two terminals):

   ```powershell
   cd smart-library
   .\run.ps1                    # Library → port 8080

   cd attendance-management
   .\run.ps1                    # Attendance → port 8081
   ```

4. Both apps use the **same Supabase credentials** in `application-local.properties`.
5. Test on Server first: `http://localhost:8080` and `http://localhost:8081`.
6. Allow Windows Firewall for ports **8080** and **8081** (if Laptop 2 cannot connect).

---

## URLs (Laptop 2 = Client)

Replace `SERVER_IP` with Laptop 1's IPv4 address.

| System | URL (Client laptop) |
|--------|---------------------|
| Smart Library (EduLibrary) | `http://SERVER_IP:8080` |
| Library Admin login | `http://SERVER_IP:8080/admin/login` |
| Attendance (EduPresence) | `http://SERVER_IP:8081` |
| Attendance Admin login | `http://SERVER_IP:8081/login` |

**Production backup (if LAN fails):** use your Railway URLs instead.

---

## Demo accounts

| Role | Username | Password | Where to log in |
|------|----------|----------|-----------------|
| Student (shared) | `student1` | `student123` | Library `/login` **and** Attendance `/login` |
| Library Admin | `admin` | `admin123` | Library `/admin/login` |
| Attendance Admin | `admin` | `admin123` | Attendance `/login` |
| Teacher | `teacher1` | `teacher123` | Attendance `/login` |

---

## What to say (opening, ~30 sec)

> "This is the **LU Centralized System** — Smart Library and Attendance Management merged into **one platform** with **one Supabase PostgreSQL database**, **shared authentication** (`public.users`), and **profile sync** between the `public` and `library` schemas. Laptop 1 runs both Spring Boot services; Laptop 2 accesses them over the local network."

---

## Demo script (follow in order)

### Step 1 — Prove merged system + one database (~2 min)

**Prof requirement:** *Merge two systems; one centralized backend; one database.*

| Do | Show |
|----|------|
| Open Library on **Laptop 2** | `http://SERVER_IP:8080` |
| Open Attendance on **Laptop 2** | `http://SERVER_IP:8081` |
| Optional: Supabase dashboard | One project → `public` + `library` schemas |

**Say:** "Two modules, one database, one auth source — not two separate systems anymore."

---

### Step 2 — Shared login (~2 min)

**Prof requirement:** *All original functions; client accesses server.*

| Do | Expected result |
|----|-----------------|
| On Laptop 2 → Library → `/login` | Log in as `student1` / `student123` |
| Same browser or new tab → Attendance → `/login` | **Same credentials work** |
| Open student dashboard on both | Same student identity |

**Say:** "Shared `public.users` — register or log in once, access both systems."

---

### Step 3 — Library features (~3 min)

**Prof requirement:** *All features from original Library system operational.*

| Do | Feature |
|----|---------|
| Log out student → `/admin/login` as `admin` / `admin123` | Library Admin |
| Admin dashboard | Books, categories, analytics |
| Issue / return a book (or show QR scan page) | Borrowing workflow |
| Student portal | Search books, reservations |

---

### Step 4 — Attendance features (~3 min)

**Prof requirement:** *All features from original Attendance system operational.*

| Do | Feature |
|----|---------|
| Attendance `/login` as `admin` / `admin123` | Admin dashboard |
| Show Students, Departments, Subjects | Admin management |
| Log in as `teacher1` / `teacher123` | Teacher portal |
| Show attendance session / QR (if ready) | Attendance recording |
| Reports page | Export / reports |

---

### Step 5 — Data consistency (~3 min) ★ Most important

**Prof requirement:** *Data entered on Client is saved in central database on Server.*

**Option A — Shared student (quick):**
1. Laptop 2: log in as `student1` on Library.
2. Laptop 2: log in as `student1` on Attendance.
3. **Say:** "Same user row in `public.users`; profiles linked across schemas."

**Option B — New data (stronger proof):**
1. Laptop 2 → Library → `/register` → create a new student (note username).
2. Laptop 2 → Attendance Admin → Students list → **new student appears** (sync).
3. **Or:** Record attendance for a student → refresh Admin → data persists.

**Say:** "Client laptop writes to the same central database both apps use — not local storage."

---

### Step 6 — Security (optional, +1 min)

| Do | Feature |
|----|---------|
| Forgot password flow | OTP email (Brevo) |
| Wrong password 5× | Account lockout |
| Admin → audit logs (if visible) | Shared security |

---

## Quick answers if prof asks

| Question | Answer |
|----------|--------|
| Why two apps, not one? | "One **centralized platform** — two Spring Boot **modules** on one shared database. Common in enterprise architecture." |
| Where is the database? | "Central **Supabase PostgreSQL** — single instance. Both apps connect via JDBC." |
| Why not Netlify/Vercel? | "Java Spring Boot backend — deployed on **Railway** for production; **local IP** for this final check." |
| How do systems integrate? | "Shared auth + JDBC profile sync — **not** separate databases or REST APIs between apps." |

---

## Troubleshooting (quietly, before prof arrives)

| Problem | Fix |
|---------|-----|
| Laptop 2 cannot open `SERVER_IP:8080` | Same WiFi? Firewall allows 8080/8081? Apps running on Laptop 1? |
| Login fails | Check `application-local.properties` on **both** apps; same Supabase creds |
| Student not in Attendance after Library register | Wait a few seconds; refresh list; check sync ran on registration |
| Port already in use | Close other Java processes; restart `run.ps1` |
| Email OTP not sending | OK for demo — skip Step 6; mention Brevo config on Railway |

---

## Pre-check checklist

- [ ] Laptop 1 IP written above
- [ ] Library running (8080)
- [ ] Attendance running (8081)
- [ ] Laptop 2 can open both URLs via IP
- [ ] `student1` works on both apps
- [ ] `admin` works on both admin portals
- [ ] Supabase dashboard login ready (optional, for "one DB" proof)

---

**Good luck sa final check.**
