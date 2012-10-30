#Alters columns to be not null when appropiate for SER-152


# --- !Ups
ALTER TABLE Address ALTER COLUMN addressline2 DROP NOT NULL;

ALTER TABLE Celebrity ALTER COLUMN publicname SET NOT NULL;
ALTER TABLE Celebrity ALTER COLUMN urlslug SET NOT NULL;

# --- !Downs
