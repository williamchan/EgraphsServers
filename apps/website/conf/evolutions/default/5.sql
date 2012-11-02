# Drops orders.transactionId and adds orderId column to CashTransaction
# --- !Ups

ALTER TABLE cashtransaction ADD COLUMN orderId bigint;
ALTER TABLE orders DROP COLUMN transactionId;

# --- !Downs

