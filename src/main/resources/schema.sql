
drop table if exists user;
create table user (
   user_id bigint identity,
   name varchar(255) unique,
   password varchar(255),
   email_addr varchar(255)
);

insert into user(name, password, email_addr) values('mschaef', '', 'schaeffer.michael.a@gmail.com');

drop table if exists todo_item;
create table todo_item (
   item_id identity,
   user_id bigint,
   desc varchar(255),
   completed boolean
);

