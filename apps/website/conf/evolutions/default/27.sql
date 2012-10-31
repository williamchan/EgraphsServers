# Makes Orders._printingOption nullable since this column is no longer used

# --- !Ups
ALTER TABLE Orders ALTER COLUMN _printingOption DROP NOT NULL;

# --- !Downs
