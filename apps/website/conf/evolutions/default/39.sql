# --- !Ups

-- new tables
CREATE TABLE Checkout (
    id bigint primary key NOT NULL,
    customerId bigint NOT NULL,
    created timestamp NOT NULL,
    updated timestamp NOT NULL
);
CREATE SEQUENCE s_Checkout_id;

CREATE TABLE LineItem (
    id bigint primary key NOT NULL,
    _checkoutId bigint NOT NULL,
    _itemTypeId bigint NOT NULL,
    _amountInCurrency float NOT NULL,
    notes text,
    created timestamp NOT NULL,
    updated timestamp NOT NULLh
);
CREATE SEQUENCE s_LineItem_id;

CREATE TABLE LineItemType (
    id bigint primary key NOT NULL,
    _desc text,
    _nature text,
    _codeType text,
    created timestamp NOT NULL,
    updated timestamp NOT NULL
);
CREATE SEQUENCE s_LineItemType_id;

CREATE TABLE GiftCertificate (
    id bigint primary key NOT NULL,
    _couponId bigint NOT NULL,
    _lineItemId bigint NOT NULL,
    recipient text,
    created timestamp NOT NULL,
    updated timestamp NOT NULL
);
CREATE SEQUENCE s_GiftCertificate_id;


-- these should be nullable until fully transitioned into new checkout and gift certificates
ALTER TABLE CashTransaction ADD COLUMN lineItemId bigint;   
ALTER TABLE Coupon ADD COLUMN lineItemTypeId bigint;

-- fk constraints
ALTER TABLE Checkout ADD CONSTRAINT CheckoutFK1 FOREIGN KEY (customerId) REFERENCES Customer(id);
ALTER TABLE LineItem ADD CONSTRAINT LineItemFK1 FOREIGN KEY (_checkoutId) REFERENCES Checkout(id);
ALTER TABLE LineItem ADD CONSTRAINT LineItemFK2 FOREIGN KEY (_itemTypeId) REFERENCES LineItemType(id);
ALTER TABLE GiftCertificate ADD CONSTRAINT GiftCertificateFK1 FOREIGN KEY (_couponId) REFERENCES Coupon(id);
ALTER TABLE GiftCertificate ADD CONSTRAINT GiftCertificateFK2 FOREIGN KEY (_lineItemId) REFERENCES LineItem(id);
ALTER TABLE CashTransaction ADD CONSTRAINT GiftCertificateFK27 FOREIGN KEY (lineItemTypeId) REFERENCES LineItemType(id);
ALTER TABLE Coupon ADD CONSTRAINT CouponFK1 FOREIGN KEY (lineItemTypeId) REFERENCES LineItemType(id);


# --- !Downs

-- fk constraints on Coupon and CashTransaction can stay since they are nullable for now.
