-- Database Migration for User Profiles
-- This creates the user_profiles table to store address and card information

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    
    -- Address fields
    street VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Card fields (masked/tokenized for security)
    card_number_masked VARCHAR(20),  -- Format: "**** **** **** 1234"
    card_token VARCHAR(500),          -- Optional: for payment gateway integration
    card_holder VARCHAR(100),
    expiry_month VARCHAR(2),          -- Format: "01" to "12"
    expiry_year VARCHAR(4),           -- Format: "2027"
    
    -- Foreign key constraint
    CONSTRAINT fk_user_profile_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comments to columns for documentation
ALTER TABLE user_profiles 
    MODIFY COLUMN card_number_masked VARCHAR(20) 
    COMMENT 'Masked card number showing only last 4 digits: **** **** **** 1234';

ALTER TABLE user_profiles 
    MODIFY COLUMN card_token VARCHAR(500) 
    COMMENT 'Payment gateway token for secure card storage (iyzico, Stripe, etc.)';

-- IMPORTANT SECURITY NOTES:
-- 1. NEVER store CVV - it should only be used during payment processing
-- 2. NEVER store full card number in plain text - use tokenization via payment gateway
-- 3. Only store masked card number (last 4 digits) for display purposes
-- 4. In production, card_token should be obtained from payment gateway after tokenization

