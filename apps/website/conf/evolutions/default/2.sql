# Adds Order.reviewstatus
# --- !Ups
ALTER TABLE orders ADD COLUMN reviewStatus varchar(128);
UPDATE orders SET reviewStatus = 'ApprovedByAdmin';
ALTER TABLE orders ALTER COLUMN reviewStatus SET NOT NULL;
CREATE INDEX idx4b0107b3 ON orders (reviewStatus);
ALTER TABLE orders ADD COLUMN rejectionReason varchar(128);

# --- !Downs
