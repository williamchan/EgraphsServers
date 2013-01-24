# --- !Ups
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
    _domainEntityId bigint,
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
    _lineItemTypeId bigint NOT NULL,
    recipient text,
    created timestamp NOT NULL,
    updated timestamp NOT NULL
);
CREATE SEQUENCE s_GiftCertificate_id;

# --- !Downs
