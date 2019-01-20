CREATE CACHED TABLE verification_link (
  verification_link_id INTEGER IDENTITY,
  link_uuid VARCHAR(36) NOT NULL UNIQUE,
  created_on TIMESTAMP NOT NULL,
  verifies_user_id INTEGER NOT NULL REFERENCES user(user_id)    
);

CREATE CACHED TABLE role (
  role_id INTEGER IDENTITY,
  role_name VARCHAR(32) NOT NULL
);

INSERT INTO role(role_name) values('verified');

CREATE CACHED TABLE user_role (
  user_id INTEGER NOT NULL REFERENCES user(user_id),
  role_id INTEGER NOT NULL REFERENCES role(role_id)
);

-- All legacy users are considered verified

INSERT INTO user_role
  SELECT user.user_id, role.role_id
   FROM user, role
  WHERE role.role_name = 'verified';
  
  
