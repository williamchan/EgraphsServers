# Database schema for marketplace

# --- !Ups
create table Filter (
    name varchar(128) not null,
    publicName varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_Filter_id;
-- indexes on Filter
create unique index idx18fd0435 on Filter (name);

create table FilterValue (
    name varchar(128) not null,
    publicName varchar(128) not null,
    id bigint primary key not null,
    filterId bigint not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_FilterValue_id;
-- indexes on FilterValue
create unique index idx34b60632 on FilterValue (name);

create table FilterValueRelationship (
    filterValueId bigint not null,
    id bigint primary key not null,
    filterId bigint not null
  );
create sequence s_FilterValueRelationship_id;

create table CelebrityFilterValue (
    filterValueId bigint not null,
    id bigint primary key not null,
    celebrityId bigint not null
  );
create sequence s_CelebrityFilterValue_id;

alter table FilterValueRelationship add constraint FilterValueRelationshipFK1 foreign key (filterValueId) references FilterValue(id);
alter table FilterValueRelationship add constraint FilterValueRelationshipFK2 foreign key (filterId) references Filter(id);
alter table CelebrityFilterValue add constraint CelebrityFilterValueFK3 foreign key (celebrityId) references Celebrity(id);
alter table CelebrityFilterValue add constraint CelebrityFilterValueFK4 foreign key (filterValueId) references FilterValue(id);
alter table InventoryBatchProduct add constraint InventoryBatchProductFK5 foreign key (inventoryBatchId) references InventoryBatch(id);
alter table InventoryBatchProduct add constraint InventoryBatchProductFK6 foreign key (productId) references Product(id);

# --- !Downs

