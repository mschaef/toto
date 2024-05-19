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

