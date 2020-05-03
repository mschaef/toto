ALTER TABLE todo_item
   ADD item_ordinal BIGINT NULL;

UPDATE todo_item target
  SET item_ordinal = (SELECT NEW_ORDINAL from
                       (SELECT item_id, ROWNUM() AS NEW_ORDINAL
                          FROM todo_item lookup
                        ORDER BY created_on)
                        WHERE item_id = target.item_id);
  
ALTER TABLE todo_item
  ALTER COLUMN item_ordinal SET NOT NULL;

UPDATE todo_item_history
   SET item_ordinal = 0;

ALTER TABLE todo_item_history
  ALTER COLUMN item_ordinal SET NOT NULL;

