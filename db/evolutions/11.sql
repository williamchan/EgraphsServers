# Adds _privacyStatus to order defaulted to 'Public', renames some enum columns, makes administrator._role not null, and drops isLeftHanded from celebrity
# --- !Ups

ALTER TABLE celebrity DROP COLUMN isLeftHanded;

ALTER TABLE orders ADD COLUMN _privacyStatus varchar(128);
UPDATE orders SET _privacyStatus = 'Public';
ALTER TABLE orders ALTER COLUMN _privacyStatus SET NOT NULL;

ALTER TABLE administrator RENAME COLUMN role TO _role;
ALTER TABLE celebrity RENAME COLUMN enrollmentStatusValue TO _enrollmentStatus;
ALTER TABLE egraph RENAME COLUMN stateValue TO _egraphState;
ALTER TABLE orders RENAME COLUMN paymentStateString TO _paymentStatus;
ALTER TABLE orders RENAME COLUMN reviewStatus TO _reviewStatus;

ALTER TABLE administrator ALTER COLUMN _role SET NOT NULL;

# --- !Downs

ALTER TABLE administrator ALTER COLUMN _role DROP NOT NULL;

ALTER TABLE orders RENAME COLUMN _reviewStatus TO reviewStatus;
ALTER TABLE orders RENAME COLUMN _paymentStatus TO paymentStateString;
ALTER TABLE orders RENAME COLUMN _egraphState TO stateValue;
ALTER TABLE celebrity RENAME COLUMN _enrollmentStatus TO enrollmentStatusValue;
ALTER TABLE administrator RENAME COLUMN _role TO role;

ALTER TABLE orders DROP COLUMN _privacyStatus;

ALTER TABLE celebrity ADD COLUMN isLeftHanded boolean;
ALTER TABLE celebrity SET isLeftHanded = true;
ALTER TABLE celebrity ALTER COLUMN isLeftHanded SET NOT NULL;
