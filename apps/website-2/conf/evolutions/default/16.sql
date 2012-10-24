# Adds egraph.signedAt
# --- !Ups

ALTER TABLE Egraph ADD COLUMN signedAt timestamp;

# --- !Downs

ALTER TABLE Egraph DROP COLUMN signedAt;
