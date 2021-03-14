CREATE CACHED TABLE user (
  user_id BIGINT IDENTITY,
  email_addr VARCHAR(255) UNIQUE,
  password VARCHAR(255),
  last_login_on TIMESTAMP NULL,
  account_created_on TIMESTAMP NOT NULL,
  password_created_on TIMESTAMP NOT NULL,
  password_expires_on TIMESTAMP NULL,
  friendly_name VARCHAR(255) NOT NULL
);

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
  

