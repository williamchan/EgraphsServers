# --- !Ups
ALTER TABLE Celebrity ADD COLUMN facebookUrl varchar(255);
ALTER TABLE Celebrity ADD COLUMN websiteUrl varchar(255);

# --- !Downs
