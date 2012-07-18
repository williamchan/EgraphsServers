# Adds PrintOrder table
# --- !Ups

create table PrintOrder (
    quantity integer not null,
    orderId bigint not null,
    isFulfilled boolean not null,
    id bigint primary key not null,
    updated timestamp not null,
    shippingAddress varchar(255) not null,
    created timestamp not null
  );
create sequence s_PrintOrder_id;
alter table PrintOrder add constraint PrintOrderFK10 foreign key (orderId) references Orders(id) on delete cascade;

# --- !Downs

DROP sequence s_PrintOrder_id;
DROP TABLE PrintOrder;
