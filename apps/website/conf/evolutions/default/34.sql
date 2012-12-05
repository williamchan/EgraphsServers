# --- !Ups

-- Add order type column to orders table
ALTER TABLE Orders ADD COLUMN _orderType text NOT NULL DEFAULT 'Normal';

-- No downs required because _orderType can safely be ignored