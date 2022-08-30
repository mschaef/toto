ALTER TABLE todo_list
  ADD is_view BOOLEAN DEFAULT FALSE NOT NULL;

CREATE CACHED TABLE todo_view_sublist (
  todo_list_id BIGINT NOT NULL REFERENCES todo_list(todo_list_id),
  sublist_id BIGINT NOT NULL REFERENCES todo_list(todo_list_id),

  PRIMARY KEY(todo_list_id, sublist_id)
);
