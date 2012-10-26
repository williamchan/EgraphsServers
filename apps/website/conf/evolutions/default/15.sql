# Creates FailedPurchaseData table
# --- !Ups

create table FailedPurchaseData (
    purchaseData varchar(1000) not null,
    id bigint primary key not null,
    updated timestamp not null,
    errorDescription varchar(128) not null,
    created timestamp not null
  );
create sequence s_FailedPurchaseData_id;

# --- !Downs

DROP sequence s_FailedPurchaseData_id;
DROP TABLE FailedPurchaseData;
