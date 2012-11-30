# --- !Ups

-- Add order type column to orders table
ALTER TABLE Orders ADD COLUMN _orderType text NOT NULL DEFAULT 'Normal';

-- Set existing pregraphs' _orderType to Promotional
-- UPDATE TABLE Orders 
-- SET _orderType='Promotional' WHERE buyid=(
--   SELECT id FROM accounts WHERE email='whoeverbought the pregraphs'
--     and (exclude any he/she bought for him/herself)
-- );

-- No downs required because _orderType can safely be ignored