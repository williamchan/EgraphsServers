# --- !Ups

-- Add order type column to orders table
ALTER TABLE VideoAsset ADD COLUMN urlKey text NOT NULL DEFAULT '';
ALTER TABLE VideoAsset MODIFY COLUMN urlKey VARCHAR(255);

# --- !Downs