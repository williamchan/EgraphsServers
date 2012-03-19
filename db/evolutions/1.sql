# Initial schema
# --- !Ups

-- table declarations :
create table Customer (
    name varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_Customer_id;
create table Celebrity (
    publicName varchar(128),
    urlSlug varchar(128),
    description varchar(255),
    apiKey varchar(128),
    profilePhotoUpdated varchar(128),
    lastName varchar(128),
    firstName varchar(128),
    id bigint primary key not null,
    updated timestamp not null,
    isLeftHanded boolean not null,
    enrollmentStatusValue varchar(128) not null,
    created timestamp not null
  );
create sequence s_Celebrity_id;
-- indexes on Celebrity
create unique index idx3b9006bf on Celebrity (urlSlug);
create table Administrator (
    role varchar(128),
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_Administrator_id;
create table CashTransaction (
    accountId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    currencyCode varchar(128) not null,
    typeString varchar(128) not null,
    amountInCurrency decimal(21, 6) not null,
    created timestamp not null
  );
create sequence s_CashTransaction_id;
create table Account (
    email varchar(128) not null,
    passwordSalt varchar(128),
    administratorId bigint,
    passwordHash varchar(128),
    customerId bigint,
    id bigint primary key not null,
    updated timestamp not null,
    celebrityId bigint,
    created timestamp not null
  );
create sequence s_Account_id;
-- indexes on Account
create unique index idx226a0503 on Account (email);
create unique index idx6bf50929 on Account (administratorId);
create unique index idx429d071a on Account (customerId);
create unique index idx48fd076b on Account (celebrityId);
create table Product (
    priceInCurrency decimal(21, 6) not null,
    name varchar(128) not null,
    urlSlug varchar(128) not null,
    description varchar(128) not null,
    photoKey varchar(128),
    id bigint primary key not null,
    updated timestamp not null,
    celebrityId bigint not null,
    created timestamp not null
  );
create sequence s_Product_id;
create table Orders (
    recipientId bigint not null,
    recipientName varchar(128) not null,
    buyerId bigint not null,
    id bigint primary key not null,
    amountPaidInCurrency decimal(21, 6) not null,
    updated timestamp not null,
    stripeChargeId varchar(128),
    stripeCardTokenId varchar(128),
    requestedMessage varchar(128),
    productId bigint not null,
    messageToCelebrity varchar(128),
    transactionId bigint,
    created timestamp not null,
    paymentStateString varchar(128) not null
  );
create sequence s_Orders_id;
create table Egraph (
    orderId bigint not null,
    stateValue varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_Egraph_id;
create table EnrollmentBatch (
    isBatchComplete boolean not null,
    id bigint primary key not null,
    isSuccessfulEnrollment boolean,
    updated timestamp not null,
    celebrityId bigint not null,
    created timestamp not null
  );
create sequence s_EnrollmentBatch_id;
create table EnrollmentSample (
    enrollmentBatchId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_EnrollmentSample_id;
create table VBGAudioCheck (
    vbgTransactionId bigint not null,
    enrollmentBatchId bigint not null,
    errorCode varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    usableTime double precision,
    created timestamp not null
  );
create sequence s_VBGAudioCheck_id;
create table VBGEnrollUser (
    success boolean,
    vbgTransactionId bigint not null,
    enrollmentBatchId bigint not null,
    errorCode varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_VBGEnrollUser_id;
create table VBGFinishEnrollTransaction (
    vbgTransactionId bigint not null,
    enrollmentBatchId bigint not null,
    errorCode varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_VBGFinishEnrollTransaction_id;
create table VBGStartEnrollment (
    vbgTransactionId bigint,
    enrollmentBatchId bigint not null,
    errorCode varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_VBGStartEnrollment_id;
create table VBGFinishVerifyTransaction (
    vbgTransactionId bigint not null,
    egraphId bigint not null,
    errorCode varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_VBGFinishVerifyTransaction_id;
create table VBGStartVerification (
    vbgTransactionId bigint,
    egraphId bigint not null,
    errorCode varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    created timestamp not null
  );
create sequence s_VBGStartVerification_id;
create table VBGVerifySample (
    success boolean,
    vbgTransactionId bigint not null,
    score bigint,
    egraphId bigint not null,
    errorCode varchar(128) not null,
    id bigint primary key not null,
    updated timestamp not null,
    usableTime double precision,
    created timestamp not null
  );
create sequence s_VBGVerifySample_id;
create table XyzmoDeleteUser (
    errorMsg varchar(255),
    enrollmentBatchId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    error varchar(128),
    baseResult varchar(128) not null,
    created timestamp not null
  );
create sequence s_XyzmoDeleteUser_id;
create table XyzmoAddUser (
    errorMsg varchar(255),
    enrollmentBatchId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    error varchar(128),
    baseResult varchar(128) not null,
    created timestamp not null
  );
create sequence s_XyzmoAddUser_id;
create table XyzmoAddProfile (
    xyzmoProfileId varchar(128),
    errorMsg varchar(255),
    enrollmentBatchId bigint not null,
    id bigint primary key not null,
    updated timestamp not null,
    error varchar(128),
    baseResult varchar(128) not null,
    created timestamp not null
  );
create sequence s_XyzmoAddProfile_id;
create table XyzmoEnrollDynamicProfile (
    xyzmoProfileId varchar(128),
    errorMsg varchar(255),
    enrollmentBatchId bigint not null,
    nrEnrolled integer,
    id bigint primary key not null,
    updated timestamp not null,
    error varchar(128),
    baseResult varchar(128) not null,
    rejectedSignaturesSummary varchar(255),
    enrollResult varchar(128),
    enrollmentSampleIds varchar(255),
    created timestamp not null
  );
create sequence s_XyzmoEnrollDynamicProfile_id;
create table XyzmoVerifyUser (
    errorMsg varchar(255),
    score integer,
    egraphId bigint not null,
    id bigint primary key not null,
    isMatch boolean,
    updated timestamp not null,
    error varchar(128),
    baseResult varchar(128) not null,
    created timestamp not null
  );
create sequence s_XyzmoVerifyUser_id;
-- foreign key constraints :
alter table Account add constraint AccountFK1 foreign key (customerId) references Customer(id) on delete set null;
alter table Account add constraint AccountFK2 foreign key (administratorId) references Administrator(id) on delete set null;
alter table Account add constraint AccountFK3 foreign key (celebrityId) references Celebrity(id) on delete set null;
alter table CashTransaction add constraint CashTransactionFK4 foreign key (accountId) references Account(id);
alter table Product add constraint ProductFK5 foreign key (celebrityId) references Celebrity(id);
alter table Orders add constraint OrdersFK6 foreign key (productId) references Product(id) on delete cascade;
alter table Orders add constraint OrdersFK7 foreign key (buyerId) references Customer(id) on delete cascade;
alter table Orders add constraint OrdersFK8 foreign key (recipientId) references Customer(id) on delete cascade;
alter table Egraph add constraint EgraphFK9 foreign key (orderId) references Orders(id) on delete cascade;
alter table EnrollmentBatch add constraint EnrollmentBatchFK10 foreign key (celebrityId) references Celebrity(id) on delete cascade;
alter table EnrollmentSample add constraint EnrollmentSampleFK11 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table VBGAudioCheck add constraint VBGAudioCheckFK12 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table VBGEnrollUser add constraint VBGEnrollUserFK13 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table VBGFinishEnrollTransaction add constraint VBGFinishEnrollTransactionFK14 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table VBGStartEnrollment add constraint VBGStartEnrollmentFK15 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table VBGFinishVerifyTransaction add constraint VBGFinishVerifyTransactionFK16 foreign key (egraphId) references Egraph(id) on delete cascade;
alter table VBGStartVerification add constraint VBGStartVerificationFK17 foreign key (egraphId) references Egraph(id) on delete cascade;
alter table VBGVerifySample add constraint VBGVerifySampleFK18 foreign key (egraphId) references Egraph(id) on delete cascade;
alter table XyzmoDeleteUser add constraint XyzmoDeleteUserFK19 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table XyzmoAddUser add constraint XyzmoAddUserFK20 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table XyzmoAddProfile add constraint XyzmoAddProfileFK21 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table XyzmoEnrollDynamicProfile add constraint XyzmoEnrollDynamicProfileFK22 foreign key (enrollmentBatchId) references EnrollmentBatch(id) on delete cascade;
alter table XyzmoVerifyUser add constraint XyzmoVerifyUserFK23 foreign key (egraphId) references Egraph(id) on delete cascade;
-- column group indexes :
create unique index idx93e60a9a on Product (celebrityId,urlSlug);
create index idx7abd0999 on Egraph (orderId,stateValue);
create index idx5e5c1a0d on EnrollmentBatch (celebrityId,isBatchComplete,isSuccessfulEnrollment);
