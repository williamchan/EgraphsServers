# Rename marketplace-related schema to avoid confusion with http filters. First, drop 28.sql in reverse order, then create new schema.

# --- !Ups

ALTER TABLE InventoryBatchProduct DROP CONSTRAINT InventoryBatchProductFK6;
ALTER TABLE InventoryBatchProduct DROP CONSTRAINT InventoryBatchProductFK5;
ALTER TABLE CelebrityFilterValue DROP CONSTRAINT CelebrityFilterValueFK4;
ALTER TABLE CelebrityFilterValue DROP CONSTRAINT CelebrityFilterValueFK3;
ALTER TABLE FilterValueRelationship DROP CONSTRAINT FilterValueRelationshipFK2;
ALTER TABLE FilterValueRelationship DROP CONSTRAINT FilterValueRelationshipFK1;

DROP SEQUENCE s_CelebrityFilterValue_id;
DROP TABLE CelebrityFilterValue;

DROP SEQUENCE s_FilterValueRelationship_id;
DROP TABLE FilterValueRelationship;

DROP INDEX idx34b60632;
DROP SEQUENCE s_FilterValue_id;
DROP TABLE FilterValue;

DROP INDEX idx18fd0435;
DROP SEQUENCE s_Filter_id;
DROP TABLE Filter;


create table Category (
    name varchar(128) not null,
    publicName varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_Category_id;
-- indexes on Category
create unique index idx22f4050d on Category (name);

create table CategoryValue (
    name varchar(128) not null,
    categoryId bigint not null,
    publicName varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_CategoryValue_id;
-- indexes on CategoryValue
create unique index idx42e5070a on CategoryValue (name);

create table CategoryValueRelationship (
    categoryId bigint not null,
    id bigint primary key not null,
    categoryValueId bigint not null
  );
create sequence s_CategoryValueRelationship_id;

create table CelebrityCategoryValue (
    id bigint primary key not null,
    celebrityId bigint not null,
    categoryValueId bigint not null
  );
create sequence s_CelebrityCategoryValue_id;

alter table CategoryValueRelationship add constraint CategoryValueRelationshipFK1 foreign key (categoryValueId) references CategoryValue(id);
alter table CategoryValueRelationship add constraint CategoryValueRelationshipFK2 foreign key (categoryId) references Category(id);
alter table CelebrityCategoryValue add constraint CelebrityCategoryValueFK3 foreign key (celebrityId) references Celebrity(id);
alter table CelebrityCategoryValue add constraint CelebrityCategoryValueFK4 foreign key (categoryValueId) references CategoryValue(id);

# --- !Downs

