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




--New columns and foreing keys (partially complete list, need to check FK's)
--NOTE: tables for things that don't use the new purchase flow should have nullable lineItem/lineItemType ids

--ALTER TABLE Product ADD COLUMN _itemTypeId bigint NOT NULL;
--ALTER TABLE Product ADD CONSTRAINT ProductFK1 FOREIGN KEY (_itemTypeId)
--    REFERENCES LineItemType(id) NOT NULL;
--
--ALTER TABLE CashTransaction ADD COLUMN lineItemId bigint NOT NULL;
--ALTER TABLE CashTransaction ADD CONSTRAINT CashTransactionFK1 FOREIGN KEY (lineItemId)
--    REFERENCES LineItem(id) NOT NULL;
--
--ALTER TABLE Coupon ADD COLUMN _itemTypeId bigint NOT NULL;
--ALTER TABLE Coupon ADD COLUMN lineItemId bigint NOT NULL;
--ALTER TABLE Coupon ADD CONSTRAINT CouponFK1 FOREIGN KEY (_itemTypeId)
--    REFERENCES LineItemType(id) NOT NULL;
--ALTER TABLE Coupon ADD CONSTRAINT CouponFK2 FOREIGN KEY (lineItemId)
--    REFERENCES LineItem(id) NOT NULL;
--
--ALTER TABLE Orders ADD COLUMN lineItemId bigint NOT NULL;
--ALTER TABLE Orders ADD CONSTRAINT OrdersFK1 FOREIGN KEY (lineItemId) REFERENCES LineItem(id) NOT NULL;
--
--ALTER TABLE PrintOrder ADD COLUMN lineItemId bigint NOT NULL;
--ALTER TABLE PrintOrder ADD CONSTRAINT PrintOrderFK1 FOREIGN KEY (lineItemId) REFERENCES LineItem(id) NOT NULL;



--ALTER TABLE _ ADD COLUMN lineItem Id bigint NOT NULL
-- ALTER TABLE Table ADD CONSTRAINT TableFK1 FOREIGN KEY (col) REFERENCES Table(col);