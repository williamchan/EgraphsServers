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
