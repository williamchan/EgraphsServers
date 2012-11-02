# Adds more columns to Celebrity
# --- !Ups

ALTER TABLE Celebrity RENAME description TO bio;
UPDATE Celebrity SET bio = '' WHERE bio is null;
ALTER TABLE Celebrity ALTER COLUMN bio TYPE text;
ALTER TABLE Celebrity ALTER COLUMN bio SET NOT NULL;

ALTER TABLE Celebrity ADD COLUMN casualName varchar(128);

ALTER TABLE Celebrity ADD COLUMN organization varchar(128);
UPDATE Celebrity SET organization = '';
ALTER TABLE Celebrity ALTER COLUMN organization SET NOT NULL;

ALTER TABLE Celebrity ADD COLUMN twitterUsername varchar(128);

ALTER TABLE Celebrity ADD COLUMN _landingPageImageKey varchar(128);

ALTER TABLE Celebrity ADD COLUMN _logoImageKey varchar(128);

ALTER TABLE Celebrity DROP COLUMN firstname;
ALTER TABLE Celebrity DROP COLUMN lastname;

# --- !Downs

