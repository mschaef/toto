-- name: get-user-by-email
SELECT *
  FROM user
 WHERE email_addr = :email_addr

-- name: get-user-by-id
SELECT *
  FROM user
 WHERE user_id = :user_id

