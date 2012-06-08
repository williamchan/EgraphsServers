# Adds resetPasswordKey and fbUserId to account
# --- !Ups

ALTER TABLE celebrity ADD COLUMN isFeatured boolean NOT NULL;
ALTER TABLE celebrity ADD COLUMN roleDescription varchar(128);

CREATE index idx512107dd on celebrity (isFeatured);

# --- !Downs

ALTER TABLE celebrity DROP COLUMN isFeatured;
ALTER TABLE celebrity DROP COLUMN roleDescription;

DROP index idx512107dd;