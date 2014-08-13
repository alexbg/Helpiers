# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table category (
  id                        bigint auto_increment not null,
  category_name             varchar(255),
  creation_date             datetime,
  constraint pk_category primary key (id))
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

alter table topic add constraint fk_topic_category_1 foreign key (ID_CATEGORY) references category (id) on delete restrict on update restrict;
create index ix_topic_category_1 on topic (ID_CATEGORY);
alter table user add constraint fk_user_topic_2 foreign key (TOPIC_ID) references topic (id) on delete restrict on update restrict;
create index ix_user_topic_2 on user (TOPIC_ID);



# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table category;

drop table topic;

drop table user;

SET FOREIGN_KEY_CHECKS=1;

