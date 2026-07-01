-- Check what data exists in the database
    SELECT 'Users' as table_name, COUNT(*) as count FROM users
    UNION ALL
    SELECT 'Sections', COUNT(*) FROM sections
    UNION ALL
    SELECT 'Teachers', COUNT(*) FROM teachers
    UNION ALL
    SELECT 'Students', COUNT(*) FROM students
    UNION ALL
    SELECT 'Subjects', COUNT(*) FROM subjects;
