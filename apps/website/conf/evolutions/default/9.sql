# Adds resetPasswordKey and fbUserId to account
# --- !Ups

ALTER TABLE account ADD COLUMN resetPasswordKey varchar(128);
ALTER TABLE account ADD COLUMN fbUserId varchar(128);

# --- !Downs

