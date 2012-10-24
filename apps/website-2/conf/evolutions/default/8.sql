# Adds _publishedStatus to celebrity and product

# --- !Ups

ALTER TABLE celebrity ADD COLUMN _publishedStatus varchar(128);
UPDATE celebrity SET _publishedStatus = 'Published';
ALTER TABLE celebrity ALTER COLUMN _publishedStatus SET NOT NULL;
ALTER TABLE product ADD COLUMN _publishedStatus varchar(128);
UPDATE product SET _publishedStatus = 'Published';
ALTER TABLE product ALTER COLUMN _publishedStatus SET NOT NULL;

# --- !Downs

ALTER TABLE product DROP COLUMN _publishedStatus;
ALTER TABLE celebrity DROP COLUMN _publishedStatus;