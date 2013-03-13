-- Change celebritysecureinfo table to allow longer email addresses even when encrypted.
-- Also the last change wrongly made countryCode too small since it is an encrypted value.

# --- !Ups
ALTER TABLE encryptedcelebritysecureinfo DROP COLUMN countryCode;
ALTER TABLE encryptedcelebritysecureinfo ADD COLUMN countryCode varchar(128);

ALTER TABLE encryptedcelebritysecureinfo DROP COLUMN agentEmail;
ALTER TABLE encryptedcelebritysecureinfo ADD COLUMN agentEmail text;

ALTER TABLE encryptedcelebritysecureinfo DROP COLUMN contactEmail;
ALTER TABLE encryptedcelebritysecureinfo ADD COLUMN contactEmail text;

# --- !Downs