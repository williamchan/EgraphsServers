# Adds isFeatured and roleDescription to the celebrity table
# --- !Ups

ALTER TABLE celebrity ADD COLUMN isFeatured boolean;
UPDATE celebrity SET isFeatured = false;
ALTER TABLE celebrity ALTER COLUMN isFeatured SET NOT NULL;

ALTER TABLE celebrity ADD COLUMN roleDescription varchar(128);

CREATE index idx512107dd on celebrity (isFeatured);

# --- !Downs

DROP index idx512107dd;

ALTER TABLE celebrity DROP COLUMN roleDescription;
ALTER TABLE celebrity DROP COLUMN isFeatured;

