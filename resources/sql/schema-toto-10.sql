CREATE CACHED TABLE todo_view (
  view_id BIGINT IDENTITY,
  user_id BIGINT REFERENCES user(user_id),
  view_name VARCHAR(32) NOT NULL
);

CREATE CACHED TABLE todo_view_lists (
  view_id BIGINT NOT NULL REFERENCES todo_view(view_id),
  todo_list_id BIGINT NOT NULL REFERENCES todo_list(todo_list_id),

  PRIMARY KEY(view_id, todo_list_id)
);
