INSERT INTO role(role_name) values('admin');

CREATE CACHED TABLE login_failure (
  user_id BIGINT REFERENCES user(user_id),
  failed_on TIMESTAMP NOT NULL,
  request_ip VARCHAR(48) NULL
);

ALTER TABLE user
  ADD last_login_ip VARCHAR(48) NULL;
