# Make Celebrity.roleDescription non-null

# --- !Ups

UPDATE Celebrity SET roleDescription = '' WHERE roleDescription IS NULL;
ALTER TABLE Celebrity ALTER COLUMN roleDescription SET NOT NULL;

# --- !Downs

ALTER TABLE Celebrity ALTER COLUMN roleDescription DROP NOT NULL;
