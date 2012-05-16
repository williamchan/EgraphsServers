# Adds signingOriginX and signingOriginY to product
# --- !Ups

ALTER TABLE product ADD COLUMN signingOriginX integer;
ALTER TABLE product ADD COLUMN signingOriginY integer;
UPDATE product SET signingOriginX = 0;
UPDATE product SET signingOriginY = 0;
ALTER TABLE product ALTER COLUMN signingOriginX SET NOT NULL;
ALTER TABLE product ALTER COLUMN signingOriginY SET NOT NULL;

ALTER TABLE product ADD COLUMN signingScaleW integer;
ALTER TABLE product ADD COLUMN signingScaleH integer;
UPDATE product SET signingScaleW = 0;
UPDATE product SET signingScaleH = 0;
ALTER TABLE product ALTER COLUMN signingScaleW SET NOT NULL;
ALTER TABLE product ALTER COLUMN signingScaleH SET NOT NULL;

ALTER TABLE product ADD COLUMN signingAreaW integer;
ALTER TABLE product ADD COLUMN signingAreaH integer;
UPDATE product SET signingAreaW = 1024;
UPDATE product SET signingAreaH = 1024;
ALTER TABLE product ALTER COLUMN signingAreaW SET NOT NULL;
ALTER TABLE product ALTER COLUMN signingAreaH SET NOT NULL;

# --- !Downs

ALTER TABLE product DROP COLUMN signingAreaW;
ALTER TABLE product DROP COLUMN signingAreaH;
ALTER TABLE product DROP COLUMN signingScaleW;
ALTER TABLE product DROP COLUMN signingScaleH;
ALTER TABLE product DROP COLUMN signingOriginX;
ALTER TABLE product DROP COLUMN signingOriginY;
