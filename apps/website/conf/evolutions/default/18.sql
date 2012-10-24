# Adds egraph.signedAt
# --- !Ups

ALTER TABLE Account ADD COLUMN emailVerified boolean;
UPDATE Account SET emailVerified = true;
ALTER TABLE Account ALTER COLUMN emailVerified SET NOT NULL;

# --- !Downs

ALTER TABLE Account DROP COLUMN emailVerified;

