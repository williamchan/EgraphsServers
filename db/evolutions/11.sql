# Adds privacyStatus to order defaulted to 'Public'
# --- !Ups

ALTER TABLE orders ADD COLUMN privacyStatus varchar(128);
UPDATE order SET privacyStatus = 'Public';
ALTER TABLE orders ALTER COLUMN privacyStatus SET NOT NULL;

# --- !Downs

ALTER TABLE orders DROP COLUMN privacyStatus;
