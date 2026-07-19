-- Demo-only supplemental seed (loaded by the `demo` profile after data.sql).
-- The base data.sql inserts app users without a role, which leaves them unable to
-- reach the role-gated screens. Here we promote the primary demo user to
-- DEPARTMENT_HEAD and link them to a member so features like secretary assignment
-- and meeting creation are exercisable end-to-end.
UPDATE app_users SET role = 'DEPARTMENT_HEAD', linked_member_id = 1 WHERE username = 'username';

-- Give one committee a custom opening-paragraph template so the per-committee minute
-- template feature is demoable (committee 1 shows a custom intro; others use the default).
UPDATE committees
SET committee_minute_opening_template =
'आज मिति {date} ({day}) {partOfDay} {time} बजे {place} मा {committeeName} समितिको बैठक संयोजक {coordinator} ज्यूको अध्यक्षतामा बसी देहाय बमोजिम छलफल तथा निर्णय गरियोः',
    committee_minute_header_template =
'त्रिभुवन विश्वविद्यालय
इन्जिनियरिङ अध्ययन संस्थान
{committeeName}'
WHERE committee_id = 1;
