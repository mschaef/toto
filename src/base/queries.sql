
-- name: query-sql-session
SELECT session_value, accessed_on_day
  FROM web_session
 WHERE session_key = :session_key

-- name: delete-session!
DELETE FROM web_session
  WHERE session_key = :session_key

-- name: create-session!
INSERT INTO web_session(session_key, session_value, updated_on, accessed_on_day)
  VALUES(:session_key, :session_value, :updated_on, :accessed_on_day)

-- name: update-session!
UPDATE web_session
   SET session_value=:session_value,
       updated_on=:updated_on,
       accessed_on_day=:accessed_on_day
 WHERE session_key=:session_key

-- name: set-session-accessed-on!
UPDATE web_session
  SET accessed_on_day = :accessed_on_day
 WHERE session_key = :session_key

-- name: get-stale-sessions
SELECT session_key
  FROM web_session
 WHERE accessed_on_day < DATEADD('month', -1, CURRENT_TIMESTAMP)
