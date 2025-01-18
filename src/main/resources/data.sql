-- Insert Roles
INSERT INTO role (name) VALUES
    ('ADMIN'),
    ('CUSTOMER')
ON CONFLICT (name) DO NOTHING;

-- Insert Initial Admin User
INSERT INTO ticket_user (username, password, email, role_id) VALUES
    ('admin', '$2a$10$h./.ij.R4rk9ao3L.kmbyeLyhh20pNJlR7U52VJMrubkrWMVmx25a', 'admin@example.com', 1)
ON CONFLICT (username) DO NOTHING;

"$2a$10$zYPJujO6OqP31/3dfExVI.G9EpmHnqFcu.hLTwCcmWlJKI0L9u8ym"
"$2a$10$h./.ij.R4rk9ao3L.kmbyeLyhh20pNJlR7U52VJMrubkrWMVmx25a"