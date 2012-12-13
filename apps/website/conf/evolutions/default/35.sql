# --- !Ups

ALTER TABLE VideoAsset ADD COLUMN _urlKey text NOT NULL DEFAULT '';
ALTER TABLE VideoAsset ALTER COLUMN url DROP NOT NULL;

# --- !Downs