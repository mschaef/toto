ALTER TABLE todo_list
  ADD max_item_age SMALLINT NULL;

ALTER TABLE user
   ALTER COLUMN EMAIL_ADDR SET NOT NULL;

INSERT INTO user(
   email_addr,
   account_created_on,
   password_created_on,
   friendly_name
) VALUES(
  'toto@mschaef.com',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  'Todo List Automation'
);
