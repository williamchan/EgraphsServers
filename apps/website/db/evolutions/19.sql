# Adds BlobKey table
# --- !Ups

create table BlobKey (
    url varchar(255) not null,
    key varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_BlobKey_id;
create unique index idx1810041f on BlobKey (key);

# --- !Downs

DROP sequence s_BlobKey_id;
DROP TABLE BlobKey;
