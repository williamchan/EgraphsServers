# --- !Ups

create table Masthead (
    _landingPageImageKey varchar(128),
    headline text not null,
    subtitle text,
    name varchar(128) not null,
    keyBase varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    _publishedStatus varchar(128) not null,
    created timestamp not null
  );
create sequence s_Masthead_id;

# --- !Downs
