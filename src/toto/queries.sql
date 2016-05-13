-- name: get-list-id-by-item-id
SELECT todo_list_id
 FROM todo_item
WHERE item_id=:item_id
