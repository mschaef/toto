ALTER TABLE todo_item
   ADD updated_by BIGINT NULL;

ALTER TABLE todo_item
   ADD updated_on TIMESTAMP NULL;

UPDATE TODO_ITEM LI
   SET updated_by = (SELECT NVL(MIN(lo.user_id), 0)
                       FROM todo_list_owners lo
                      WHERE li.todo_list_id = lo.todo_list_id),
       updated_on = li.created_on;

ALTER TABLE todo_item
  ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE todo_item
  ALTER COLUMN updated_on SET NOT NULL;

ALTER TABLE todo_item
  ADD FOREIGN KEY (updated_by)
  REFERENCES user(user_id);

ALTER TABLE todo_item
  ADD is_deleted BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE todo_item
  ADD is_complete BOOLEAN DEFAULT FALSE NOT NULL;

UPDATE TODO_ITEM LI
   SET (updated_by, updated_on, is_deleted, is_complete)
      = (SELECT tic.user_id, tic.completed_on, tic.is_delete, not(tic.is_delete)
           FROM todo_item_completion tic
          WHERE li.item_id = tic.item_id)
 WHERE (SELECT count(*)
          FROM todo_item_completion tic2
        WHERE li.item_id = tic2.item_id) > 0;

DROP TABLE todo_item_completion;

CREATE CACHED TABLE todo_item_history (
  todo_item_history_id BIGINT IDENTITY,
  item_id BIGINT NOT NULL REFERENCES todo_item(item_id),
  todo_list_id BIGINT NOT NULL REFERENCES todo_list(todo_list_id),
  desc VARCHAR(1024) NOT NULL,
  created_on TIMESTAMP NOT NULL,
  priority TINYINT NOT NULL,
  snoozed_until TIMESTAMP NULL,
  updated_by BIGINT NOT NULL REFERENCES user(user_id),
  updated_on TIMESTAMP NOT NULL,
  is_deleted BOOLEAN NOT NULL,
  is_complete BOOLEAN NOT NULL
);

