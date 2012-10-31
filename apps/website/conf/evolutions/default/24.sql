# Adds columns for PrintOrder and starting to move stripe columns from Orders to CashTransaction

# --- !Ups

ALTER TABLE PrintOrder ADD COLUMN amountPaidInCurrency decimal(21, 6);
UPDATE PrintOrder SET amountPaidInCurrency = 45;
ALTER TABLE PrintOrder ALTER COLUMN amountPaidInCurrency SET NOT NULL;
ALTER TABLE PrintOrder ADD COLUMN pngUrl varchar(255);

CREATE index idx632308aa on PrintOrder (isFulfilled);

ALTER TABLE CashTransaction ADD COLUMN printOrderId bigint;
ALTER TABLE CashTransaction ADD COLUMN billingPostalCode varchar(20);
ALTER TABLE CashTransaction ADD COLUMN stripeChargeId varchar(128);
ALTER TABLE CashTransaction ADD COLUMN stripeCardTokenId varchar(128);

UPDATE CashTransaction SET billingPostalCode = (SELECT billingPostalCode FROM orders WHERE orders.id = CashTransaction.orderid);
UPDATE CashTransaction SET stripeChargeId    = (SELECT stripeChargeId    FROM orders WHERE orders.id = CashTransaction.orderid);
UPDATE CashTransaction SET stripeCardTokenId = (SELECT stripeCardTokenId FROM orders WHERE orders.id = CashTransaction.orderid);

ALTER TABLE CashTransaction RENAME COLUMN typeString TO _cashTransactionType;

# --- !Downs

