# --- !Ups

-- It was a mistake that CategoryValue didn't have a foreighn key constraint on category before.
alter table CategoryValue add constraint CategoryValueFK14 foreign key (categoryId) references Category(id);

-- Deletions of a Category or CategoryValue should delete a join value if it is there.
alter table CategoryValueRelationship add constraint CategoryValueRelationshipFK1 foreign key (categoryValueId) references CategoryValue(id) on delete cascade;
alter table CategoryValueRelationship add constraint CategoryValueRelationshipFK2 foreign key (categoryId) references Category(id) on delete cascade;

-- Deletions of a Celebrity or CategoryValue should delete a join value if it is there.
alter table CelebrityCategoryValue add constraint CelebrityCategoryValueFK3 foreign key (celebrityId) references Celebrity(id) on delete cascade;
alter table CelebrityCategoryValue add constraint CelebrityCategoryValueFK4 foreign key (categoryValueId) references CategoryValue(id) on delete cascade;

# --- !Downs
-- Just remove the constraints
alter table CategoryValue drop constraint CategoryValueFK14;
alter table CategoryValueRelationship drop constraint CategoryValueRelationshipFK1;
alter table CategoryValueRelationship drop constraint CategoryValueRelationshipFK2;
alter table CelebrityCategoryValue drop constraint CelebrityCategoryValueFK3;
alter table CelebrityCategoryValue drop constraint CelebrityCategoryValueFK4;