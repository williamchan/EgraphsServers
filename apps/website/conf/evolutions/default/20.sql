# Adds Shipping Info
# --- !Ups

ALTER TABLE Orders ADD COLUMN billingPostalCode varchar(20);

ALTER TABLE Orders ADD COLUMN shippingAddress varchar(255);

ALTER TABLE Orders ADD COLUMN _printingOption varchar(128);
UPDATE Orders SET _printingOption = 'DoNotPrint';
ALTER TABLE Orders ALTER COLUMN _printingOption SET NOT NULL;

# --- !Downs
