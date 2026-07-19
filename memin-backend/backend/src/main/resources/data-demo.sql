-- Demo-only supplemental seed (loaded by the `demo` profile after data.sql).
-- The base data.sql inserts app users without a role, which leaves them unable to
-- reach the role-gated screens. Here we promote the primary demo user to
-- DEPARTMENT_HEAD and link them to a member so features like secretary assignment
-- and meeting creation are exercisable end-to-end.
UPDATE app_users SET role = 'DEPARTMENT_HEAD', linked_member_id = 1 WHERE username = 'username';
