# --- !Ups
-- Add tables for mastheads and their relations to categoryvalues
create table Masthead (
    _landingPageImageKey varchar(128),
    headline text not null,
    subtitle text,
    name varchar(128) not null,
    callToActionText varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    callToActionTarget varchar(128) not null,
    _callToActionType varchar(128) not null,
    _publishedStatus varchar(128) not null,
    created timestamp not null
);
create sequence s_Masthead_id;

create table MastheadCategoryValue (
    mastheadId bigint not null,
    id bigint primary key not null,
    categoryValueId bigint not null
  );
create sequence s_MastheadCategoryValue_id;

alter table MastheadCategoryValue add constraint MastheadCategoryValueFK7 foreign key (mastheadId) references Masthead(id) on delete cascade;
alter table MastheadCategoryValue add constraint MastheadCategoryValueFK8 foreign key (categoryValueId) references CategoryValue(id) on delete cascade;

# --- !Downs
