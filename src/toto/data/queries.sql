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

-- name: add-user<!
INSERT INTO user(email_addr, password, account_created_on, password_created_on, friendly_name)
  VALUES(:email_addr, :password, :account_created_on, :password_created_on, :friendly_name)

-- name: set-user-password!
UPDATE user
   SET password = :password,
       password_created_on = :password_created_on
 WHERE email_addr = :email_addr

-- name: set-user-name!
UPDATE user
   SET friendly_name = :friendly_name
 WHERE email_addr = :email_addr

-- name: record-user-login!
UPDATE user
   SET last_login_on = :last_login_on,
       last_login_ip = :last_login_ip
 WHERE email_addr = :email_addr

-- name: record-user-login-failure!
INSERT INTO login_failure(user_id, failed_on, request_ip)
  VALUES(:user_id, :failed_on, :request_ip)

-- name: reset-login-failures!
DELETE FROM login_failure
   WHERE user_id = :user_id

-- name: create-verification-link<!
INSERT INTO verification_link(link_uuid, verifies_user_id, created_on)
  VALUES(:link_uuid, :verifies_user_id, :created_on)

-- name: get-user-roles
SELECT role_name
  FROM user u, role r, user_role ur
  WHERE u.user_id = ur.user_id
    AND ur.role_id = r.role_id
    AND u.user_id = :user_id

-- name: delete-user-role!
DELETE FROM user_role
 WHERE user_id = :user_id

-- name: insert-user-role!
INSERT INTO user_role(user_id, role_id)
  VALUES(:user_id, :role_id)

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
