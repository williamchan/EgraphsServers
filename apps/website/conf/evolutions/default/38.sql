# --- !Ups
ALTER TABLE celebrity ADD COLUMN expectedOrderDelayInMinutes integer;
UPDATE celebrity SET expectedOrderDelayInMinutes = 43200; -- 30 days
ALTER TABLE celebrity ALTER COLUMN expectedOrderDelayInMinutes SET NOT NULL;

UPDATE orders SET expectedDate = (now + 30) WHERE expectedDate is NULL;
ALTER TABLE orders ALTER COLUMN expectedDate SET NOT NULL;

# --- !Downs
ALTER TABLE orders ALTER COLUMN expectedDate DROP NOT NULL;

ALTER TABLE celebrity DROP COLUMN expectedOrderDelayInMinutes;