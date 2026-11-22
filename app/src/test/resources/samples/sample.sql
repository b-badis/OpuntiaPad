-- Rapport mensuel
CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100) NOT NULL);
SELECT u.id, COUNT(*) AS total FROM users u JOIN orders o ON o.user_id = u.id WHERE o.created_at > '2026-01-01' GROUP BY u.id;
/* bloc */ INSERT INTO users VALUES (1, 'Alice');
