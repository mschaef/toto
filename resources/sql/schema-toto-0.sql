
CREATE CACHED TABLE user (
  user_id BIGINT IDENTITY,
  email_addr VARCHAR(255) UNIQUE,
  password VARCHAR(255)
);

CREATE CACHED TABLE todo_list (
  todo_list_id BIGINT IDENTITY,
  desc VARCHAR(32)
);

CREATE CACHED TABLE todo_view (
  view_id BIGINT IDENTITY,
  user_id BIGINT REFERENCES user(user_id),
  view_name VARCHAR(32) NOT NULL
);

CREATE CACHED TABLE todo_list_owners (
  todo_list_id BIGINT NOT NULL REFERENCES todo_list(todo_list_id),
  user_id BIGINT NOT NULL REFERENCES user(user_id),

  PRIMARY KEY(todo_list_id, user_id)
);

CREATE CACHED TABLE todo_item (
  item_id BIGINT IDENTITY,
  todo_list_id BIGINT NOT NULL REFERENCES todo_list(todo_list_id),
  desc VARCHAR(1024) NOT NULL,
  priority TINYINT NOT NULL,
  created_on TIMESTAMP NOT NULL
);

CREATE CACHED TABLE todo_item_completion (
  item_id BIGINT REFERENCES todo_item(item_id),
  user_id BIGINT REFERENCES user(user_id),
  completed_on TIMESTAMP NOT NULL,
  is_delete BOOLEAN NOT NULL
);
