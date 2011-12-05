# --- !Ups

create table "Customer" (
    "name" varchar(128) not null,
    "id" bigint primary key not null,
    "updated" timestamp not null,
    "created" timestamp not null
  );
create sequence "s_Customer_id";
create table "Celebrity" (
    "publicName" varchar(128),
    "urlSlug" varchar(128),
    "description" varchar(255),
    "apiKey" varchar(128),
    "profilePhotoUpdated" timestamp,
    "lastName" varchar(128),
    "firstName" varchar(128),
    "id" bigint primary key not null,
    "updated" timestamp not null,
    "created" timestamp not null
  );
create sequence "s_Celebrity_id";
create unique index "idx3b9006bf" on "Celebrity" ("urlSlug");
create table "Administrator" (
    "role" varchar(128),
    "id" bigint primary key not null,
    "updated" timestamp not null,
    "created" timestamp not null
  );
create sequence "s_Administrator_id";
create table "Account" (
    "email" varchar(128) not null,
    "passwordSalt" varchar(128),
    "administratorId" bigint,
    "passwordHash" varchar(128),
    "customerId" bigint,
    "id" bigint primary key not null,
    "updated" timestamp not null,
    "celebrityId" bigint,
    "created" timestamp not null
  );
create sequence "s_Account_id";
create unique index "idx226a0503" on "Account" ("email");
create unique index "idx6bf50929" on "Account" ("administratorId");
create unique index "idx429d071a" on "Account" ("customerId");
create unique index "idx48fd076b" on "Account" ("celebrityId");
create table "Product" (
    "name" varchar(128) not null,
    "urlSlug" varchar(128) not null,
    "description" varchar(128) not null,
    "id" bigint primary key not null,
    "updated" timestamp not null,
    "celebrityId" bigint not null,
    "priceInCents" integer not null,
    "created" timestamp not null
  );
create sequence "s_Product_id";
create table "Orders" (
    "recipientId" bigint not null,
    "buyerId" bigint not null,
    "amountPaidInCents" integer not null,
    "id" bigint primary key not null,
    "updated" timestamp not null,
    "requestedMessage" varchar(128),
    "productId" bigint not null,
    "messageToCelebrity" varchar(128),
    "created" timestamp not null
  );
create sequence "s_Orders_id";
create table "Egraph" (
    "orderId" bigint not null,
    "stateValue" varchar(128) not null,
    "id" bigint primary key not null,
    "updated" timestamp not null,
    "created" timestamp not null
  );
create sequence "s_Egraph_id";
alter table "Account" add constraint "AccountFK1" foreign key ("customerId") references "Customer"("id") on delete set null;
alter table "Account" add constraint "AccountFK2" foreign key ("administratorId") references "Administrator"("id") on delete set null;
alter table "Account" add constraint "AccountFK3" foreign key ("celebrityId") references "Celebrity"("id") on delete set null;
alter table "Product" add constraint "ProductFK4" foreign key ("celebrityId") references "Celebrity"("id");
alter table "Orders" add constraint "OrdersFK5" foreign key ("productId") references "Product"("id") on delete set null;
alter table "Orders" add constraint "OrdersFK6" foreign key ("buyerId") references "Customer"("id") on delete set null;
alter table "Orders" add constraint "OrdersFK7" foreign key ("recipientId") references "Customer"("id") on delete set null;
alter table "Egraph" add constraint "EgraphFK8" foreign key ("orderId") references "Orders"("id") on delete set null;
create unique index "idx93e60a9a" on "Product" ("celebrityId","urlSlug");
create unique index "idx7abd0999" on "Egraph" ("orderId","stateValue");

# --- !Downs

DROP TABLE "Account";
DROP TABLE "Administrator";
DROP TABLE "Celebrity";
DROP TABLE "Customer";
DROP TABLE "Egraph";
DROP TABLE "Orders";
DROP TABLE "Product";
DROP sequence "s_Account_id";
DROP sequence "s_Administrator_id";
DROP sequence "s_Account_id";
DROP sequence "s_Celebrity_id";
DROP sequence "s_Customer_id";
DROP sequence "s_Egraph_id";
DROP sequence "s_Orders_id";
DROP sequence "s_Product_id";
