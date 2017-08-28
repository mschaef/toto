-- name: get-user-by-email
SELECT *
  FROM user
 WHERE email_addr = :email_addr

-- name: get-user-by-id
SELECT *
  FROM user
 WHERE user_id = :user_id

-- name: get-list-id-by-item-id
SELECT todo_list_id
  FROM todo_item
 WHERE item_id = :item_id

-- name: get-todo-list-by-id
SELECT *
  FROM todo_list
 WHERE todo_list_id = :todo_list_id

-- name: get-friendly-users-by-id
SELECT DISTINCT b.user_id, u.email_addr
    FROM todo_list_owners a, todo_list_owners b, user u
   WHERE a.todo_list_id = b.todo_list_id
     AND u.user_id = b.user_id
     AND a.user_id = :user_id
ORDER BY u.email_addr

-- name: get-todo-list-owners-by-list-id
SELECT user_id
  FROM todo_list_owners
 WHERE todo_list_id = :todo_list_id

-- name: get-todo-list-ids-by-user
SELECT DISTINCT todo_list_id
  FROM todo_list_owners
 WHERE user_id = :user_id

-- name: get-todo-lists-by-user
SELECT DISTINCT todo_list.todo_list_id,
                todo_list.desc,
                (SELECT count(item.item_id)
                   FROM todo_item item 
                  WHERE item.todo_list_id=todo_list.todo_list_id
                    AND NOT EXISTS (SELECT 1
                                     FROM todo_item_completion
                                    WHERE item_id=item.item_id))
                   AS item_count
  FROM todo_list, todo_list_owners
 WHERE todo_list.todo_list_id=todo_list_owners.todo_list_id
   AND todo_list_owners.user_id = :user_id
 ORDER BY todo_list.desc

-- name: list-owned-by-user-id?
SELECT COUNT(*)
  FROM todo_list_owners
 WHERE todo_list_id = :list_id
   AND user_id = :user_id
   
-- name: item-owned-by-user-id?
SELECT COUNT(*)
 FROM todo_list_owners lo, todo_item item
WHERE item.item_id = :item_id
  AND lo.todo_list_id=item.todo_list_id
  AND lo.user_id = :user_id

-- name: get-pending-items
SELECT item.item_id,
       item.todo_list_id,
       item.desc,
       item.created_on,
       item.priority,
       completion.completed_on,
       completion.is_delete,
       DATEDIFF('day', item.created_on, CURRENT_TIMESTAMP) as age_in_days
   FROM todo_item item 
      LEFT JOIN todo_item_completion completion
        ON item.item_id = completion.item_id
   WHERE item.todo_list_id = :list_id
   AND (completion.completed_on IS NULL 
        OR completion.completed_on >
               DATEADD('day', :completed_within_days, CURRENT_TIMESTAMP))
   ORDER BY item.priority DESC,
            item.item_id

-- name: get-pending-item-count
SELECT count(item.item_id)
  FROM todo_item item 
 WHERE todo_list_id= :list_id
   AND NOT EXISTS (SELECT 1 FROM todo_item_completion WHERE item_id=item.item_id)

-- name: get-item-by-id
SELECT item.item_id, item.todo_list_id, item.desc, item.created_on
  FROM todo_item item 
 WHERE item_id = :item_id

-- name: item-completion-count
SELECT COUNT(*)
  FROM todo_item_completion
 WHERE item_id = :item_id

-- name: set-item-completion!
MERGE INTO todo_item_completion
   USING (VALUES(:item_id, :user_id, :completed_on, :is_delete))
     AS vals(item_id, user_id, completed_on, is_delete)
     ON todo_item_completion.item_id = vals.item_id
   WHEN MATCHED THEN
      UPDATE SET todo_item_completion.is_delete = vals.is_delete
   WHEN NOT MATCHED THEN
      INSERT VALUES :item_id, :user_id, :completed_on, :is_delete