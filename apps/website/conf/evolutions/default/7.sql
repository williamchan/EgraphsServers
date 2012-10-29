# Adds latitude and longitude to egraph
# --- !Ups

ALTER TABLE egraph ADD COLUMN latitude double precision;
ALTER TABLE egraph ADD COLUMN longitude double precision;

# --- !Downs

ALTER TABLE egraph DROP COLUMN longitude;
ALTER TABLE egraph DROP COLUMN latitude;
