# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table category (
  id                        bigint not null,
  category_name             varchar(255),
  creation_date             timestamp,
  constraint pk_category primary key (id))
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

create sequence topic_seq;

create sequence user_seq;

alter table topic add constraint fk_topic_category_1 foreign key (ID_CATEGORY) references category (id) on delete restrict on update restrict;
create index ix_topic_category_1 on topic (ID_CATEGORY);
alter table user add constraint fk_user_topic_2 foreign key (TOPIC_ID) references topic (id) on delete restrict on update restrict;
create index ix_user_topic_2 on user (TOPIC_ID);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists category;

drop table if exists topic;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists category_seq;

drop sequence if exists topic_seq;

drop sequence if exists user_seq;

