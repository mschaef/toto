-- name: get-list-id-by-item-id
SELECT todo_list_id
  FROM todo_item
 WHERE item_id = :item_id

-- name: get-todo-list-by-id
SELECT *
  FROM todo_list
 WHERE todo_list_id = :todo_list_id

-- name: get-view-sublists
SELECT todo_view_sublist.sublist_id, todo_list.desc
  FROM todo_view_sublist,
       todo_list
 WHERE todo_view_sublist.todo_list_id = :todo_list_id
   AND todo_list.todo_list_id = todo_view_sublist.sublist_id
   AND NOT todo_list.is_deleted
 ORDER BY todo_list.desc

-- name: get-views-with-sublist
SELECT todo_list.todo_list_id
  FROM todo_list, todo_list_owners, todo_view_sublist
 WHERE todo_list.is_view
   AND todo_list_owners.todo_list_id = todo_list.todo_list_id
   AND todo_view_sublist.todo_list_id = todo_list.todo_list_id
   AND todo_list_owners.user_id = :user_id
   AND todo_view_sublist.sublist_id = :sublist_id

-- name: get-todo-list-is-public-by-id
SELECT is_public
  FROM todo_list
 WHERE todo_list_id = :todo_list_id
   AND NOT(todo_list.is_deleted)

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
SELECT DISTINCT todo_list_owners.todo_list_id
  FROM todo_list_owners, todo_list
 WHERE NOT(todo_list.is_deleted)
   AND todo_list.todo_list_id=todo_list_owners.todo_list_id
   AND user_id = :user_id

-- name: get-todo-list-item-count
SELECT count(item.item_id)
  FROM todo_item item
 WHERE (item.todo_list_id=:todo_list_id
        OR item.todo_list_id in (SELECT todo_view_sublist.sublist_id
                                   FROM todo_view_sublist
                                  WHERE todo_view_sublist.todo_list_id=:todo_list_id))
    AND NOT item.is_deleted
    AND NOT item.is_complete
    AND (CURRENT_TIMESTAMP >= NVL(item.snoozed_until, CURRENT_TIMESTAMP))

-- name: get-todo-lists-by-user
SELECT todo_list.todo_list_id,
       todo_list.desc,
       todo_list.is_public,
       todo_list.is_view,
       todo_list.is_deleted,
       todo_list_owners.priority,
       (SELECT count(list_owners.user_id)
          FROM todo_list_owners list_owners
         WHERE list_owners.todo_list_id = todo_list.todo_list_id) AS list_owner_count
  FROM todo_list, todo_list_owners
 WHERE (:include_deleted
        OR NOT(todo_list.is_deleted))
   AND todo_list.todo_list_id=todo_list_owners.todo_list_id
   AND todo_list_owners.user_id = :user_id
 ORDER BY todo_list.is_view DESC,
          todo_list_owners.priority DESC,
          todo_list.desc

-- name: get-todo-lists-with-item-age-limit
SELECT todo_list.todo_list_id, todo_list.max_item_age
  FROM todo_list
 WHERE todo_list.max_item_age IS NOT NULL

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

-- name: get-item-count
SELECT count(item.item_id)
   FROM todo_item item
   WHERE item.todo_list_id = :list_id

-- name: get-pending-items
SELECT item.item_id,
       item.todo_list_id,
       item.desc,
       item.created_on,
       user.user_id as created_by_id,
       user.email_addr as created_by_email,
       user.friendly_name as created_by_name,
       item.priority,
       item.updated_on,
       item.is_deleted,
       item.is_complete,
       DATEDIFF('day', item.created_on, CURRENT_TIMESTAMP) as age_in_days,
       DATEDIFF('day', item.updated_on, CURRENT_TIMESTAMP) as sunset_age_in_days,
       item.snoozed_until,
       CURRENT_TIMESTAMP < NVL(item.snoozed_until, CURRENT_TIMESTAMP) AS currently_snoozed,
       DATEADD('day', :snoozed_within_days, CURRENT_TIMESTAMP) < NVL(item.snoozed_until, CURRENT_TIMESTAMP) AS visibly_snoozed,
       item_ordinal
   FROM todo_item item, user
   WHERE item.todo_list_id = :list_id
   AND user.user_id = item.created_by
   AND (NOT(item.is_deleted OR item.is_complete)
        OR item.updated_on >
               DATEADD('day', :completed_within_days, CURRENT_TIMESTAMP))
   AND item.priority >= :min_item_priority
   ORDER BY item.priority DESC,
            item.item_ordinal,
            item.created_on

-- name: get-completed-items
SELECT item.item_id,
       item.todo_list_id,
       item.desc,
       item.created_on,
       user.user_id as created_by_id,
       user.friendly_name as created_by_name,
       item.priority,
       item.updated_on,
       item.is_deleted,
       item.is_complete,
       DATEDIFF('day', item.created_on, CURRENT_TIMESTAMP) as age_in_days,
       DATEDIFF('day', item.updated_on, CURRENT_TIMESTAMP) as sunset_age_in_days,
       item_ordinal
   FROM todo_item item, user
   WHERE item.todo_list_id = :list_id
     AND user.user_id = item.created_by
     AND item.is_complete
     AND item.updated_on > DATEADD('day', :completed_within_days, CURRENT_TIMESTAMP)
   ORDER BY item.updated_on DESC

-- name: get-sunset-items-by-id
SELECT item_id
  FROM todo_item
 WHERE todo_list_id = :todo_list_id
   AND DATEDIFF('day', updated_on, CURRENT_TIMESTAMP) > :age_limit
   AND NOT is_deleted
   AND NOT is_complete

-- name: get-pending-item-order-by-description
SELECT item.item_id,
       item.todo_list_id
   FROM todo_item item
   WHERE item.todo_list_id = :list_id
     AND NOT(item.is_deleted OR item.is_complete)
   ORDER BY UPPER(item.desc)

-- name: get-pending-item-order-by-created-on
SELECT item.item_id,
       item.todo_list_id
   FROM todo_item item
   WHERE item.todo_list_id = :list_id
     AND NOT(item.is_deleted OR item.is_complete)
   ORDER BY created_on

-- name: get-pending-item-order-by-updated-on
SELECT item.item_id,
       item.todo_list_id
   FROM todo_item item
   WHERE item.todo_list_id = :list_id
     AND NOT(item.is_deleted OR item.is_complete)
   ORDER BY updated_on

-- name: get-pending-item-order-by-snoozed-until
SELECT item.item_id,
       item.todo_list_id
   FROM todo_item item
   WHERE item.todo_list_id = :list_id
     AND NOT(item.is_deleted OR item.is_complete)
   ORDER BY NVL(item.snoozed_until, CURRENT_TIMESTAMP),
            item_ordinal

-- name: list-items-tail
SELECT item_id, item_ordinal
  FROM todo_item
 WHERE todo_list_id = :todo_list_id
   AND item_ordinal >= :begin_ordinal
 ORDER BY item_ordinal

-- name: get-pending-item-count
SELECT count(item.item_id)
  FROM todo_item item
 WHERE todo_list_id= :list_id
   AND NOT (item.is_deleted OR item.is_complete)

-- name: get-item-by-id
SELECT item.item_id,
       item.todo_list_id,
       item.desc,
       item.created_on,
       item.priority,
       item.updated_on,
       item.is_deleted,
       item.is_complete,
       DATEDIFF('day', item.created_on, CURRENT_TIMESTAMP) as age_in_days,
       DATEDIFF('day', item.updated_on, CURRENT_TIMESTAMP) as sunset_age_in_days,
       item.snoozed_until,
       CURRENT_TIMESTAMP < NVL(item.snoozed_until, CURRENT_TIMESTAMP) AS currently_snoozed,
       item_ordinal
  FROM todo_item item
 WHERE item_id = :item_id

-- name: get-max-ordinal-by-list
SELECT MAX(item_ordinal)
  FROM todo_item item
 WHERE todo_list_id = :list_id
   AND NOT (item.is_deleted OR item.is_complete)

-- name: get-min-ordinal-by-list
SELECT MIN(item_ordinal)
  FROM todo_item item
 WHERE todo_list_id = :list_id
   AND NOT (item.is_deleted OR item.is_complete)

-- name: set-item-completion!
MERGE INTO todo_item_completion
   USING (VALUES(:item_id, :user_id, :completed_on, :is_delete))
     AS vals(item_id, user_id, completed_on, is_delete)
     ON todo_item_completion.item_id = vals.item_id
   WHEN MATCHED THEN
      UPDATE SET todo_item_completion.is_delete = vals.is_delete
   WHEN NOT MATCHED THEN
      INSERT VALUES :item_id, :user_id, :completed_on, :is_delete
