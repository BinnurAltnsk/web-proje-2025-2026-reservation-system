-- Database Migration for Saved Cards
-- This creates the saved_cards table to store multiple cards per user

CREATE TABLE IF NOT EXISTS saved_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- Card information (masked for security)
    masked_number VARCHAR(20) NOT NULL,     -- Format: "**** **** **** 1234"
    card_holder VARCHAR(100) NOT NULL,
    expiry_month VARCHAR(2) NOT NULL,       -- Format: "01" to "12"
    expiry_year VARCHAR(4) NOT NULL,        -- Format: "2027"
    token VARCHAR(500),                      -- UUID or payment gateway token
    
    -- Default card flag
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Timestamps
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_saved_card_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_user_id (user_id),
    INDEX idx_user_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comments to columns for documentation
ALTER TABLE saved_cards 
    MODIFY COLUMN masked_number VARCHAR(20) 
    COMMENT 'Masked card number showing only last 4 digits: **** **** **** 1234';

ALTER TABLE saved_cards 
    MODIFY COLUMN token VARCHAR(500) 
    COMMENT 'Payment gateway token or UUID for secure card storage';

ALTER TABLE saved_cards 
    MODIFY COLUMN is_default BOOLEAN 
    COMMENT 'True if this is the default card for the user';

-- IMPORTANT SECURITY NOTES:
-- 1. CVV is NEVER stored - it's only validated during card addition and required during payment
-- 2. Full card number is NEVER stored - only masked version showing last 4 digits
-- 3. Token field can store UUID or payment gateway token (iyzico, Stripe, etc.)
-- 4. Multiple cards per user are supported
-- 5. Only one card can be default per user (enforced by application logic)

