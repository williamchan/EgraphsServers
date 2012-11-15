# Initial Coupon schema

# --- !Ups

create table Coupon (
    name varchar(128) not null,
    endDate timestamp not null,
    _couponType varchar(128) not null,
    restrictions varchar(255) not null,
    code varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    _usageType varchar(128) not null,
    isActive boolean not null,
    _discountType varchar(128) not null,
    startDate timestamp not null,
    discountAmount numeric(20,16) not null,
    created timestamp not null
  );
create sequence s_Coupon_id;
create index idx17c60e5d on Coupon (code,startDate,endDate,isActive);
create index idx7cfa10d8 on Coupon (_usageType,startDate,endDate,isActive);

# --- !Downs
