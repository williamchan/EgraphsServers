-- Add celebritysecureinfo table for storing celebrity contact and payment info

# --- !Ups
ALTER TABLE Celebrity ADD COLUMN secureInfoId bigint;

CREATE TABLE EncryptedCelebritySecureInfo (
  city varchar(128),
  _depositAccountRoutingNumber varchar(128),
  agentEmail varchar(128),
  state varchar(128),
  _depositAccountNumber varchar(128),
  streetAddress varchar(128),
  voicePhone varchar(128),
  country varchar(128),
  _depositAccountType varchar(128),
  postalCode varchar(128),
  id bigint primary key not null,
  updated timestamp not null,
  contactEmail varchar(128),
  smsPhone varchar(128),
  created timestamp not null
);
CREATE sequence s_EncryptedCelebritySecureInfo_id;
CREATE UNIQUE INDEX idx57551046 ON EncryptedCelebritySecureInfo (contactEmail);
ALTER TABLE Celebrity ADD CONSTRAINT CelebrityFK17 FOREIGN KEY (secureInfoId) REFERENCES EncryptedCelebritySecureInfo(id) ON DELETE SET NULL;

# --- !Downs