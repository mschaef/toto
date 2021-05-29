INSERT INTO role(role_name) values('admin');

CREATE CACHED TABLE login_failure (
  user_id BIGINT REFERENCES user(user_id),
  failed_on TIMESTAMP NOT NULL,
);
