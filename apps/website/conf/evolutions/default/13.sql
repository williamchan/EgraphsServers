# Adds _orderType to order 'SignatureWithMessage'
# --- !Ups

ALTER TABLE orders ADD COLUMN _orderType varchar(128);
UPDATE orders SET _orderType = 'SignatureWithMessage';
ALTER TABLE orders ALTER COLUMN _orderType SET NOT NULL;

# --- !Downs

