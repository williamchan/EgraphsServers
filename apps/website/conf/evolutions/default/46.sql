-- Change celebritysecureinfo table to have 2 address line fields instead of 1 street address field.
-- Also change country to countryCode since that is what it is.
-- And allow longer email addresses, since 256 is the real limit

# --- !Ups
ALTER TABLE EncryptedCelebritySecureInfo DROP COLUMN streetAddress;
ALTER TABLE EncryptedCelebritySecureInfo ADD COLUMN addressLine1 text;
ALTER TABLE EncryptedCelebritySecureInfo ADD COLUMN addressLine2 text;

ALTER TABLE EncryptedCelebritySecureInfo DROP COLUMN country;
ALTER TABLE EncryptedCelebritySecureInfo ADD COLUMN countryCode varchar(16);

ALTER TABLE EncryptedCelebritySecureInfo DROP COLUMN contactEmail;
ALTER TABLE EncryptedCelebritySecureInfo ADD COLUMN contactEmail varchar(256);

# --- !Downs