-- Fix qr_code_url column to support LONGTEXT for Base64 QR code images
-- Run this script manually on your MySQL database

USE bankhoahoc;

-- Change qr_code_url column from VARCHAR to LONGTEXT
ALTER TABLE orders MODIFY COLUMN qr_code_url LONGTEXT;

-- Verify the change
DESCRIBE orders;
