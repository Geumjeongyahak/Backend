ALTER TABLE purchase_request_payment_transactions
    ADD COLUMN payment_method VARCHAR(20);

ALTER TABLE purchase_request_payment_transactions
    ADD CONSTRAINT ck_purchase_request_payment_method
        CHECK (payment_method IN ('CASH', 'CARD', 'TRANSFER', 'AUTO_TRANSFER', 'OTHER'));
