-- Sub-proyek 2c: username unik (juga menolak duplikat dari register stack lama).
ALTER TABLE user ADD UNIQUE KEY uq_user_username (username);
