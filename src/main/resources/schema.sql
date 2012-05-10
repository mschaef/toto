
drop table if exists user;
create table user (
   user_id bigint identity,
   name varchar(255) unique,
   password varchar(255),
   email_addr varchar(255)
);

insert into user(name, password, email_addr)
  values('mschaef', '14d5b8f25f499c041a12508a9be7b87e52db818e3a06bf6fe970a7fe7d39a1e5', 'schaeffer.michael.a@gmail.com');

drop table if exists todo_item;
create table todo_item (
   item_id identity,
   user_id bigint references user(user_id),
   desc varchar(255),
   completed boolean
);

exit;