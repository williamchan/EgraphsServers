# --- !Ups

DROP INDEX idx512107dd;

ALTER TABLE celebrity DROP COLUMN isFeatured;

# --- !Downs

-- as seen in 10.sql
ALTER TABLE celebrity ADD COLUMN isFeatured boolean;
UPDATE celebrity SET isFeatured = false;
ALTER TABLE celebrity ALTER COLUMN isFeatured SET NOT NULL;

CREATE INDEX idx512107dd ON Celebrity (isFeatured);