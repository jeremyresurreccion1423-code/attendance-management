"""Apply database/fix_uuid_schema.sql to Supabase (fixes admin 500 errors)."""
import re
import sys
from pathlib import Path

try:
    import psycopg2
except ImportError:
    print("Install dependency: pip install psycopg2-binary")
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
PROPS = ROOT / "src" / "main" / "resources" / "application.properties"
SQL = ROOT / "database" / "fix_uuid_schema.sql"


def load_db_config():
    text = PROPS.read_text(encoding="utf-8")
    url = re.search(r"spring\.datasource\.url=(.+)", text).group(1).strip()
    user = re.search(r"spring\.datasource\.username=(.+)", text).group(1).strip()
    password = re.search(r"spring\.datasource\.password=(.+)", text).group(1).strip()
    host_port_db = url.split("://", 1)[1].split("/", 1)
    host_port = host_port_db[0].split("?")[0]
    host, port = host_port.rsplit(":", 1)
    dbname = host_port_db[1].split("?")[0]
    return dict(host=host, port=int(port), dbname=dbname, user=user, password=password, sslmode="require")


def main():
    sql = SQL.read_text(encoding="utf-8")
    cfg = load_db_config()
    print(f"Connecting to {cfg['host']}...")
    conn = psycopg2.connect(**cfg)
    conn.autocommit = True
    with conn.cursor() as cur:
        cur.execute(sql)
    conn.close()
    print("Schema migration completed successfully.")


if __name__ == "__main__":
    main()
