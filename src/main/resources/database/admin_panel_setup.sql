-- Add active status column to users table if it doesn't exist
ALTER TABLE AutoTech.users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create a view for user management in the admin panel
CREATE OR REPLACE VIEW AutoTech.user_management_view AS
SELECT 
    u.id,
    u.username,
    u.email,
    r.name AS role_name,
    u.active
FROM 
    AutoTech.users u
JOIN 
    AutoTech.roles r ON u.role_id = r.id;

-- Create stored procedure to toggle user active status
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS AutoTech.toggle_user_status(IN user_id INT, IN new_status BOOLEAN)
BEGIN
    UPDATE AutoTech.users SET active = new_status WHERE id = user_id;
END //
DELIMITER ;

-- Create stored procedure to delete a user
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS AutoTech.delete_user(IN user_id INT)
BEGIN
    -- Check if user exists
    DECLARE user_exists INT;
    SELECT COUNT(*) INTO user_exists FROM AutoTech.users WHERE id = user_id;
    
    IF user_exists > 0 THEN
        -- Delete any foreign key references first
        DELETE FROM AutoTech.mechanics WHERE user_id = user_id;
        
        -- Then delete the user
        DELETE FROM AutoTech.users WHERE id = user_id;
    END IF;
END //
DELIMITER ;

-- Ensure all default users have the active status set
UPDATE AutoTech.users SET active = TRUE WHERE active IS NULL;

-- Add an index on username for faster searching
CREATE INDEX IF NOT EXISTS idx_username ON AutoTech.users(username);
CREATE INDEX IF NOT EXISTS idx_user_email ON AutoTech.users(email);
