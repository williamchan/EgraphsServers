# Add table for Usernames

# --- !Ups

create table Usernames (
    isRemoved boolean not null,
    isPermanent boolean not null,
    customerId bigint not null,
    username varchar(255) primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
-- indexes on Usernames
create index idx54400800 on Usernames (customerId);

alter table Usernames add constraint UsernamesFK11 foreign key (customerId) references Customer(id) on delete cascade;

# --- !Downs

DROP index idx54400800;

DROP TABLE Usernames;