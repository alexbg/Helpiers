# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table category (
  id                        bigint not null,
  category_name             varchar(255),
  creation_date             timestamp,
  constraint pk_category primary key (id))
;

create table chat (
  id                        bigint not null,
  USERHOST_ID               bigint,
  USEROWNER_ID              bigint,
  CHATREQUEST_ID            bigint,
  start_time                timestamp,
  end_time                  timestamp,
  constraint pk_chat primary key (id))
;

create table chat_request (
  id                        bigint not null,
  USERHOST_ID               bigint,
  USEROWNER_ID              bigint,
  CHAT_ID                   bigint,
  status                    varchar(8),
  creation_date             timestamp,
  status_update_date        timestamp,
  constraint ck_chat_request_status check (status in ('ACCEPTED','REJECTED','CANCELED')),
  constraint pk_chat_request primary key (id))
;

create table topic (
  id                        bigint not null,
  topic_text                varchar(255),
  creation_date             timestamp,
  ID_CATEGORY               bigint not null,
  constraint pk_topic primary key (id))
;

create table user (
  id                        bigint not null,
  email                     varchar(255),
  username                  varchar(255),
  password                  varchar(255),
  user_description          varchar(255),
  sex                       varchar(7),
  born_date                 timestamp,
  register_date             timestamp,
  TOPIC_ID                  bigint,
  constraint ck_user_sex check (sex in ('MALE','FEMALE','UNKNOWN')),
  constraint pk_user primary key (id))
;

create sequence category_seq;

create sequence chat_seq;

create sequence chat_request_seq;

create sequence topic_seq;

create sequence user_seq;

alter table chat add constraint fk_chat_userHost_1 foreign key (USERHOST_ID) references user (id) on delete restrict on update restrict;
create index ix_chat_userHost_1 on chat (USERHOST_ID);
alter table chat add constraint fk_chat_userOwner_2 foreign key (USEROWNER_ID) references user (id) on delete restrict on update restrict;
create index ix_chat_userOwner_2 on chat (USEROWNER_ID);
alter table chat add constraint fk_chat_chatRequest_3 foreign key (CHATREQUEST_ID) references chat_request (id) on delete restrict on update restrict;
create index ix_chat_chatRequest_3 on chat (CHATREQUEST_ID);
alter table chat_request add constraint fk_chat_request_userHost_4 foreign key (USERHOST_ID) references user (id) on delete restrict on update restrict;
create index ix_chat_request_userHost_4 on chat_request (USERHOST_ID);
alter table chat_request add constraint fk_chat_request_userOwner_5 foreign key (USEROWNER_ID) references user (id) on delete restrict on update restrict;
create index ix_chat_request_userOwner_5 on chat_request (USEROWNER_ID);
alter table chat_request add constraint fk_chat_request_chat_6 foreign key (CHAT_ID) references chat (id) on delete restrict on update restrict;
create index ix_chat_request_chat_6 on chat_request (CHAT_ID);
alter table topic add constraint fk_topic_category_7 foreign key (ID_CATEGORY) references category (id) on delete restrict on update restrict;
create index ix_topic_category_7 on topic (ID_CATEGORY);
alter table user add constraint fk_user_topic_8 foreign key (TOPIC_ID) references topic (id) on delete restrict on update restrict;
create index ix_user_topic_8 on user (TOPIC_ID);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists category;

drop table if exists chat;

drop table if exists chat_request;

drop table if exists topic;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists category_seq;

drop sequence if exists chat_seq;

drop sequence if exists chat_request_seq;

drop sequence if exists topic_seq;

drop sequence if exists user_seq;

