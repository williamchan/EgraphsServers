# Adds InventoryBatch and InventoryBatchProduct tables, and orders.inventoryBatchId as nullable bigint
# --- !Ups
create table InventoryBatch (
    numInventory integer not null,
    endDate date not null,
    id bigint primary key not null,
    updated timestamp not null,
    celebrityId bigint not null,
    startDate date not null,
    created timestamp not null
  );
create sequence s_InventoryBatch_id;
create table InventoryBatchProduct (
    id bigint primary key not null,
    productId bigint not null,
    inventoryBatchId bigint not null
  );
create sequence s_InventoryBatchProduct_id;

ALTER TABLE orders ADD COLUMN inventoryBatchId bigint;

alter table InventoryBatch add constraint InventoryBatchFK10 foreign key (celebrityId) references Celebrity(id);
alter table InventoryBatchProduct add constraint InventoryBatchProductFK11 foreign key (inventoryBatchId) references InventoryBatch(id);
alter table InventoryBatchProduct add constraint InventoryBatchProductFK12 foreign key (productId) references Product(id);

create index idxd0960c6c on InventoryBatch (startDate,endDate);
create index idx84d61109 on InventoryBatch (celebrityId,startDate,endDate);
create unique index idxd8791317 on InventoryBatchProduct (inventoryBatchId,productId);

# --- !Downs

ALTER TABLE orders DROP COLUMN inventoryBatchId;
DROP TABLE InventoryBatchProduct;
DROP TABLE InventoryBatch;
