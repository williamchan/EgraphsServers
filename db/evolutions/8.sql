# Adds _publishedStatus to celebrity and product

# --- !Ups

ALTER TABLE celebrity ADD COLUMN _publishedStatus varchar(128) not null;
ALTER TABLE product ADD COLUMN _publishedStatus varchar(128) not null;

# --- !Downs

ALTER TABLE product DROP COLUMN _publishedStatus;
ALTER TABLE celebrity DROP COLUMN _publishedStatus;