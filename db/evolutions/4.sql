# Makes orders.inventoryBatchId non-nullable and adds FK constraint
# --- !Ups

ALTER TABLE orders ALTER COLUMN inventoryBatchId SET NOT NULL;

alter table Orders add constraint OrdersFK13 foreign key (inventoryBatchId) references InventoryBatch(id);

# --- !Downs

alter table Orders drop constraint OrdersFK13;

ALTER TABLE orders ALTER COLUMN inventoryBatchId DROP NOT NULL;
