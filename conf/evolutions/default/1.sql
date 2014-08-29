# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table category (
  id                        bigint auto_increment not null,
  category_name             varchar(255),
  creation_date             datetime,
  constraint pk_category primary key (id))
;

create table chat (
  id                        bigint auto_increment not null,
  USERHOST_ID               bigint,
  USEROWNER_ID              bigint,
  CHATREQUEST_ID            bigint,
  start_time                datetime,
  end_time                  datetime,
  constraint pk_chat primary key (id))
;

create table chat_request (
  id                        bigint auto_increment not null,
  USERHOST_ID               bigint,
  USEROWNER_ID              bigint,
  CHAT_ID                   bigint,
  status                    varchar(8),
  creation_date             datetime,
  status_update_date        datetime,
  constraint ck_chat_request_status check (status in ('ACCEPTED','REJECTED','CANCELED','ONHOLD')),
  constraint pk_chat_request primary key (id))
;

create table rating (
  id                        bigint auto_increment not null,
  ID_USER                   bigint not null,
  CHAT_ID                   bigint,
  ID_CATEGORY               bigint not null,
  value                     integer,
  creation_date             datetime,
  constraint pk_rating primary key (id))
;

create table topic (
  id                        bigint auto_increment not null,
  topic_text                varchar(255),
  creation_date             datetime,
  ID_CATEGORY               bigint not null,
  constraint pk_topic primary key (id))
;

create table user (
  id                        bigint auto_increment not null,
  email                     varchar(255),
  username                  varchar(255),
  password                  varchar(255),
  user_description          varchar(255),
  sex                       varchar(7),
  born_date                 datetime,
  register_date             datetime,
  TOPIC_ID                  bigint,
  constraint ck_user_sex check (sex in ('MALE','FEMALE','UNKNOWN')),
  constraint pk_user primary key (id))
;

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
alter table rating add constraint fk_rating_user_7 foreign key (ID_USER) references user (id) on delete restrict on update restrict;
create index ix_rating_user_7 on rating (ID_USER);
alter table rating add constraint fk_rating_chat_8 foreign key (CHAT_ID) references chat (id) on delete restrict on update restrict;
create index ix_rating_chat_8 on rating (CHAT_ID);
alter table rating add constraint fk_rating_category_9 foreign key (ID_CATEGORY) references category (id) on delete restrict on update restrict;
create index ix_rating_category_9 on rating (ID_CATEGORY);
alter table topic add constraint fk_topic_category_10 foreign key (ID_CATEGORY) references category (id) on delete restrict on update restrict;
create index ix_topic_category_10 on topic (ID_CATEGORY);
alter table user add constraint fk_user_topic_11 foreign key (TOPIC_ID) references topic (id) on delete restrict on update restrict;
create index ix_user_topic_11 on user (TOPIC_ID);



# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table category;

drop table chat;

drop table chat_request;

drop table rating;

drop table topic;

drop table user;

SET FOREIGN_KEY_CHECKS=1;

