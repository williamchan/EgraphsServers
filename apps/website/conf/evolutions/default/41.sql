# --- !Ups

-- add new columns
ALTER TABLE Orders ADD COLUMN lineItemId bigint;
ALTER TABLE PrintOrder ADD COLUMN lineItemId bigint;
ALTER TABLE Product ADD COLUMN lineItemTypeId bigint;

-- add new constraints
alter table Orders add constraint OrdersFK28 foreign key (lineItemId) references LineItem(id);
alter table PrintOrder add constraint PrintOrderFK29 foreign key (lineItemId) references LineItem(id);
alter table Product add constraint ProductFK32 foreign key (lineItemTypeId) references LineItemType(id);

# --- !Downs