# --- !Ups

-- Drop old constraints that will be changed.
alter table EnrollmentBatch drop constraint EnrollmentBatchFK10;
alter table Usernames drop constraint UsernamesFK11;
alter table Orders drop constraint OrdersFK7;
alter table Orders drop constraint OrdersFK8;
alter table EnrollmentSample drop constraint EnrollmentSampleFK11;
alter table Egraph drop constraint EgraphFK9;
alter table PrintOrder drop constraint PrintOrderFK10;
alter table Orders drop constraint OrdersFK6;
alter table VBGAudioCheck drop constraint VBGAudioCheckFK12;
alter table VBGEnrollUser drop constraint VBGEnrollUserFK13;
alter table VBGFinishEnrollTransaction drop constraint VBGFinishEnrollTransactionFK14;
alter table VBGFinishVerifyTransaction drop constraint VBGFinishVerifyTransactionFK16;
alter table VBGStartEnrollment drop constraint VBGStartEnrollmentFK15;
alter table VBGStartVerification drop constraint VBGStartVerificationFK17;
alter table VBGVerifySample drop constraint VBGVerifySampleFK18;
alter table XyzmoAddProfile drop constraint XyzmodropProfileFK21;
alter table XyzmoAddUser drop constraint XyzmodropUserFK20;
alter table XyzmoDeleteUser drop constraint XyzmoDeleteUserFK19;
alter table XyzmoEnrollDynamicProfile drop constraint XyzmoEnrollDynamicProfileFK22;
alter table XyzmoVerifyUser drop constraint XyzmoVerifyUserFK23;
alter table CategoryValueRelationship drop constraint CategoryValueRelationshipFK1;
alter table CategoryValueRelationship drop constraint CategoryValueRelationshipFK2;
alter table CelebrityCategoryValue drop constraint CelebrityCategoryValueFK3;
alter table CelebrityCategoryValue drop constraint CelebrityCategoryValueFK4;

-- Add new constaints without delete cascade
alter table EnrollmentBatch add constraint EnrollmentBatchFK15 foreign key (celebrityId) references Celebrity(id);
alter table Usernames add constraint UsernamesFK18 foreign key (customerId) references Customer(id);
alter table Orders add constraint OrdersFK19 foreign key (buyerId) references Customer(id);
alter table Orders add constraint OrdersFK20 foreign key (recipientId) references Customer(id);
alter table EnrollmentSample add constraint EnrollmentSampleFK21 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table Egraph add constraint EgraphFK23 foreign key (orderId) references Orders(id);
alter table PrintOrder add constraint PrintOrderFK24 foreign key (orderId) references Orders(id);
alter table CashTransaction add constraint CashTransactionFK25 foreign key (orderId) references Orders(id);
alter table CashTransaction add constraint CashTransactionFK26 foreign key (printOrderId) references PrintOrder(id);
alter table Orders add constraint OrdersFK27 foreign key (productId) references Product(id);
alter table VBGAudioCheck add constraint VBGAudioCheckFK28 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table VBGEnrollUser add constraint VBGEnrollUserFK29 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table VBGFinishEnrollTransaction add constraint VBGFinishEnrollTransactionFK30 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table VBGFinishVerifyTransaction add constraint VBGFinishVerifyTransactionFK31 foreign key (egraphId) references Egraph(id);
alter table VBGStartEnrollment add constraint VBGStartEnrollmentFK32 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table VBGStartVerification add constraint VBGStartVerificationFK33 foreign key (egraphId) references Egraph(id);
alter table VBGVerifySample add constraint VBGVerifySampleFK34 foreign key (egraphId) references Egraph(id);
alter table XyzmoAddProfile add constraint XyzmoAddProfileFK35 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table XyzmoAddUser add constraint XyzmoAddUserFK36 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table XyzmoDeleteUser add constraint XyzmoDeleteUserFK37 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table XyzmoEnrollDynamicProfile add constraint XyzmoEnrollDynamicProfileFK38 foreign key (enrollmentBatchId) references EnrollmentBatch(id);
alter table XyzmoVerifyUser add constraint XyzmoVerifyUserFK39 foreign key (egraphId) references Egraph(id);

-- It was a mistake that CategoryValue didn't have a foreign key constraint on category before.
alter table CategoryValue add constraint CategoryValueFK14 foreign key (categoryId) references Category(id);

-- Deletions of a Category or CategoryValue should delete a join value if it is there.
alter table CategoryValueRelationship add constraint CategoryValueRelationshipFK1 foreign key (categoryValueId) references CategoryValue(id) on delete cascade;
alter table CategoryValueRelationship add constraint CategoryValueRelationshipFK2 foreign key (categoryId) references Category(id) on delete cascade;

-- Deletions of a Celebrity or CategoryValue should delete a join value if it is there.
alter table CelebrityCategoryValue add constraint CelebrityCategoryValueFK3 foreign key (celebrityId) references Celebrity(id) on delete cascade;
alter table CelebrityCategoryValue add constraint CelebrityCategoryValueFK4 foreign key (categoryValueId) references CategoryValue(id) on delete cascade;


# --- !Downs

-- Drop all the new constraints.
alter table EnrollmentBatch drop constraint EnrollmentBatchFK15;
alter table Usernames drop constraint UsernamesFK18;
alter table Orders drop constraint OrdersFK19;
alter table Orders drop constraint OrdersFK20;
alter table EnrollmentSample drop constraint EnrollmentSampleFK21;
alter table Egraph drop constraint EgraphFK23;
alter table PrintOrder drop constraint PrintOrderFK24;
alter table CashTransaction drop constraint CashTransactionFK25;
alter table CashTransaction drop constraint CashTransactionFK26;
alter table Orders drop constraint OrdersFK27;
alter table VBGAudioCheck drop constraint VBGAudioCheckFK28;
alter table VBGEnrollUser drop constraint VBGEnrollUserFK29;
alter table VBGFinishEnrollTransaction drop constraint VBGFinishEnrollTransactionFK30;
alter table VBGFinishVerifyTransaction drop constraint VBGFinishVerifyTransactionFK31;
alter table VBGStartEnrollment drop constraint VBGStartEnrollmentFK32;
alter table VBGStartVerification drop constraint VBGStartVerificationFK33;
alter table VBGVerifySample drop constraint VBGVerifySampleFK34;
alter table XyzmoAddProfile drop constraint XyzmoAddProfileFK35;
alter table XyzmoAddUser drop constraint XyzmoAddUserFK36;
alter table XyzmoDeleteUser drop constraint XyzmoDeleteUserFK37;
alter table XyzmoEnrollDynamicProfile drop constraint XyzmoEnrollDynamicProfileFK38
alter table XyzmoVerifyUser drop constraint XyzmoVerifyUserFK39;

-- Add all the old constraints.

-- Just remove the new constraints
alter table CategoryValue drop constraint CategoryValueFK14;
alter table CategoryValueRelationship drop constraint CategoryValueRelationshipFK1;
alter table CategoryValueRelationship drop constraint CategoryValueRelationshipFK2;
alter table CelebrityCategoryValue drop constraint CelebrityCategoryValueFK3;
alter table CelebrityCategoryValue drop constraint CelebrityCategoryValueFK4;