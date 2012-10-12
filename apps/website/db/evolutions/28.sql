-- table declarations

-- !Ups
create table Filters(
	name varchar(128) unique not null,
	publicname varchar(128) not null,
	id bigint primary key not null,
	updated timestamp not null,
  created timestamp not null
);

create table FilterValues(
	name varchar(128) unique not null,
  publicname varchar(128) not null,
	id bigint primary key not null,
	filterId bigint references Filters,
	updated timestamp not null,
  created timestamp not null
);

create table FilterValueRelationships(
  id bigint primary key not null,
  filterId bigint references Filters,
  filterValueId bigint references FilterValues
);

create table CelebrityFilterValues(
  id bigint primary key not null,
  celebrityId bigint references Celebrity,
  filterValueId bigint references FilterValues
);

-- !Downs

DROP TABLE CelebrityFilterValues;
DROP TABLE FilterValueRelationships;
DROP TABLE FilterValues;
DROP TABLE Filters;