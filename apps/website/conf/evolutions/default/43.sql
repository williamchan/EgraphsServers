# Add celebrityrequest table for request a star feature

# --- !Ups
create table CelebrityRequest (
    celebrityName varchar(128) not null,
    customerId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    _celebrityRequestStatus varchar(128) not null,
    created timestamp not null
  );
create sequence s_CelebrityRequest_id;

# --- !Downs