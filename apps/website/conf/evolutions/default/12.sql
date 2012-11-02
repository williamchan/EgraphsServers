# Adds Address table and new columns, Order.expectedDate, and Customer columns username, isGalleryVisible, and notice_stars
# --- !Ups

ALTER TABLE orders ADD COLUMN expectedDate date;

create table Address (
    addressLine1 varchar(128) not null,
    city varchar(128) not null,
    _state varchar(2) not null,
    accountId bigint not null,
    postalCode varchar(20) not null,
    id bigint primary key not null,
    updated timestamp not null,
    addressLine2 varchar(128) not null,
    created timestamp not null
  );
create sequence s_Address_id;
alter table Address add constraint AddressFK5 foreign key (accountId) references Account(id);

ALTER TABLE customer ADD COLUMN username varchar(128);
UPDATE customer SET username = 'user' || id;
ALTER TABLE customer ALTER COLUMN username SET NOT NULL;
create unique index idx3d8706e0 on Customer (username);

ALTER TABLE customer ADD COLUMN isGalleryVisible boolean;
UPDATE customer SET isGalleryVisible = true;
ALTER TABLE customer ALTER COLUMN isGalleryVisible SET NOT NULL;

ALTER TABLE customer ADD COLUMN notice_stars boolean;
UPDATE customer SET notice_stars = false;
ALTER TABLE customer ALTER COLUMN notice_stars SET NOT NULL;

# --- !Downs

