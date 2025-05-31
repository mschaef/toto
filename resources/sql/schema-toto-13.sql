INSERT INTO user_role(user_id, role_id)
  SELECT user.user_id, role.role_id
   FROM user, role
  WHERE user.email_addr = 'toto@mschaef.com'
    AND role.role_name = 'verified'
