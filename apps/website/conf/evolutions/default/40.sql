# --- !Ups

ALTER TABLE Celebrity ADD COLUMN _gender text NOT NULL DEFAULT 'Male';

# --- !Downs