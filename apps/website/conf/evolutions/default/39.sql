# --- !Ups

ALTER TABLE Celebrity ADD COLUMN gender text NOT NULL DEFAULT 'Male';

# --- !Downs