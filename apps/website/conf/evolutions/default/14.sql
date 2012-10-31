# Renames _orderType to _writtenMessageRequest
# --- !Ups

ALTER TABLE orders RENAME _orderType TO _writtenMessageRequest

# --- !Downs

