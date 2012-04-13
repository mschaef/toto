
drop table if exists user;
create table user (
   user_id bigint identity,
   name varchar(255),
   email_addr varchar(255)
);

drop table if exists todo_list;
create table list (
   list_id bigint identity,
   name varchar(255)
);

drop table if exists user_lists;
create table user_lists (
   user_id bigint,
   list_id bigint
);

drop table if exists item;
create table todo_item (
   item_id bigint identity,
   list_id bigint,
   name varchar(255),
   completed boolean
);

