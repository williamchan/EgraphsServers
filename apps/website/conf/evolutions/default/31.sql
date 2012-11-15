# Add videoasset and videoassetcelebrity tables for video upload

# --- !Ups
create table VideoAsset (
    url varchar(255) not null,
    id bigint primary key not null,
    updated timestamp not null,
    _videoStatus varchar(128) not null,
    created timestamp not null
  );
create sequence s_VideoAsset_id;
create table VideoAssetCelebrity (
    videoId bigint not null,
    id bigint primary key not null,
    celebrityId bigint not null
  );
create sequence s_VideoAssetCelebrity_id;

# --- !Downs