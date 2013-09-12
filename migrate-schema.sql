ALTER TABLE todo_item_completion
      ADD COLUMN is_delete BOOLEAN DEFAULT FALSE NOT NULL;

INSERT INTO todo_item_completion(item_id, user_id, completed_on, is_delete)
  SELECT item_id, 0, deleted_on, TRUE
    FROM todo_item
    WHERE deleted_on IS NOT NULL;

ALTER TABLE todo_item
      DROP COLUMN deleted_on;



       