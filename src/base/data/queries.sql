-- name: get-user-by-email
SELECT user.*, count(login_failure.failed_on) AS login_failure_count
    FROM user LEFT JOIN login_failure ON user.user_id = login_failure.user_id
   WHERE user.email_addr = :email_addr
GROUP BY user.user_id

-- name: get-user-by-id
SELECT user.*, count(login_failure.failed_on) AS login_failure_count
    FROM user LEFT JOIN login_failure ON user.user_id = login_failure.user_id
   WHERE user.user_id = :user_id
GROUP BY user.user_id

-- name: get-user-roles
SELECT role_name
  FROM user u, role r, user_role ur
  WHERE u.user_id = ur.user_id
    AND ur.role_id = r.role_id
    AND u.user_id = :user_id

-- name: get-role-id
SELECT role_id
  FROM role
 WHERE role_name = :role_name

-- name: get-verification-link-by-user-id
SELECT *
  FROM verification_link
 WHERE verifies_user_id=:user_id

-- name: get-verification-link-by-id
SELECT *
  FROM verification_link
 WHERE verification_link_id=:link_id

-- name: get-verification-link-by-uuid
SELECT *
  FROM verification_link
 WHERE link_uuid=:link_uuid

-- name: delete-old-verification-links!
DELETE FROM verification_link
  WHERE created_on < DATEADD('hour', -1, CURRENT_TIMESTAMP)
