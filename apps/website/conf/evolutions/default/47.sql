-- Change celebritysecureinfo table to allow longer email addresses for agent too, since 256 is the real limit.

# --- !Ups
ALTER TABLE EncryptedCelebritySecureInfo DROP COLUMN agentEmail;
ALTER TABLE EncryptedCelebritySecureInfo ADD COLUMN agentEmail varchar(256);

# --- !Downs