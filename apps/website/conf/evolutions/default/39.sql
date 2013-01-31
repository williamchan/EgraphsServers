# --- !Ups

-- new tables
create table Checkout (
    customerId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_Checkout_id;

create table GiftCertificate (
    recipient varchar(128) not null,
    _lineItemId bigint not null,
    _couponId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_GiftCertificate_id;

create table LineItem (
    _itemTypeId bigint not null,
    _checkoutId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    _amountInCurrency decimal(21, 6) not null,
    notes varchar(128) not null,
    created timestamp not null
  );
create sequence s_LineItem_id;

create table LineItemType (
    _desc varchar(128) not null,
    _codeType varchar(128) not null,
    _nature varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_LineItemType_id;


-- these should be nullable until fully transitioned into new checkout and gift certificates
ALTER TABLE CashTransaction ADD COLUMN lineItemId bigint;
ALTER TABLE Coupon ADD COLUMN lineItemTypeId bigint;


alter table LineItem add constraint LineItemFK18 foreign key (_checkoutId) references Checkout(id);
alter table GiftCertificate add constraint GiftCertificateFK19 foreign key (_couponId) references Coupon(id);
alter table Checkout add constraint CheckoutFK20 foreign key (customerId) references Customer(id);
alter table CashTransaction add constraint CashTransactionFK27 foreign key (lineItemId) references LineItem(id);
alter table GiftCertificate add constraint GiftCertificateFK27 foreign key (_lineItemId) references LineItem(id);
alter table Coupon add constraint CouponFK28 foreign key (lineItemTypeId) references LineItemType(id);
alter table LineItem add constraint LineItemFK29 foreign key (_itemTypeId) references LineItemType(id);



# --- !Downs
>>>>>>> master
