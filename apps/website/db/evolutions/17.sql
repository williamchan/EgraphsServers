# Adds more columns to Celebrity
# --- !Ups

ALTER TABLE Celebrity ADD COLUMN bio text;
UPDATE Celebrity SET bio = '';
ALTER TABLE Celebrity ALTER COLUMN bio SET NOT NULL;

ALTER TABLE Celebrity ADD COLUMN casualName varchar(128);

ALTER TABLE Celebrity ADD COLUMN organization varchar(128);
UPDATE Celebrity SET organization = '';
ALTER TABLE Celebrity ALTER COLUMN organization SET NOT NULL;

ALTER TABLE Celebrity ADD COLUMN twitterUsername varchar(128);

ALTER TABLE Celebrity ADD COLUMN _landingPageImageKey varchar(128);

ALTER TABLE Celebrity ADD COLUMN _logoImageKey varchar(128);

# --- !Downs

ALTER TABLE Celebrity DROP COLUMN _logoImageKey;
ALTER TABLE Celebrity DROP COLUMN _landingPageImageKey;
ALTER TABLE Celebrity DROP COLUMN twitterUsername;
ALTER TABLE Celebrity DROP COLUMN organization;
ALTER TABLE Celebrity DROP COLUMN casualName;
ALTER TABLE Celebrity DROP COLUMN bio;
