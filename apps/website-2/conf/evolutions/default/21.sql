# Changes width of Order.messageToCelebrity
# --- !Ups

ALTER TABLE Orders ALTER COLUMN messageToCelebrity TYPE varchar(140);

# --- !Downs

ALTER TABLE Orders ALTER COLUMN messageToCelebrity TYPE varchar(128);
