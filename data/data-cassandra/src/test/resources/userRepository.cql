CREATE TABLE users (name text PRIMARY KEY, realname text, password text, algorithm text);
CREATE INDEX ON users(realname);