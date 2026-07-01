#!/usr/bin/env python3
import psycopg2
from psycopg2 import sql

# Database connection parameters
db_config = {
    'host': 'aws-1-ap-southeast-1.pooler.supabase.com',
    'database': 'postgres',
    'user': 'postgres.zszwwwpzyepuduielccq',
    'password': 'Carlomercado@123',
    'port': 5432,
    'sslmode': 'require'
}

def clean_database():
    """Clean corrupt data from attendance table"""
    try:
        # Connect to database
        conn = psycopg2.connect(
            host=db_config['host'],
            database=db_config['database'],
            user=db_config['user'],
            password=db_config['password'],
            port=db_config['port'],
            sslmode=db_config['sslmode']
        )
        
        cursor = conn.cursor()
        
        # Delete all attendance records (they have corrupt timestamps)
        cursor.execute("TRUNCATE TABLE attendance CASCADE;")
        cursor.execute("ALTER SEQUENCE attendance_id_seq RESTART WITH 1;")
        
        # Commit changes
        conn.commit()
        
        print("✓ Attendance table cleaned successfully")
        print("✓ Sequence reset")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"✗ Error: {e}")
        return False
    
    return True

if __name__ == "__main__":
    print("Cleaning corrupt attendance data...")
    success = clean_database()
    if success:
        print("\nDatabase cleanup completed!")
    else:
        print("\nDatabase cleanup failed!")
