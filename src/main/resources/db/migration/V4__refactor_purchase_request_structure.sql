ALTER TABLE purchase_requests
    ADD COLUMN department_id BIGINT,
    ADD COLUMN payment_type VARCHAR(20),
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM purchase_requests_items
        GROUP BY purchase_request_id
        HAVING COUNT(DISTINCT payment_type) > 1
    ) THEN
        RAISE EXCEPTION
            '동일한 결제 신청에 서로 다른 payment_type이 존재합니다. 데이터 정리 후 마이그레이션을 다시 실행해주세요.';
    END IF;
END $$;

UPDATE purchase_requests purchase_request
SET payment_type = (
        SELECT MIN(item.payment_type)
        FROM purchase_requests_items item
        WHERE item.purchase_request_id = purchase_request.id
    ),
    department_id = (
        SELECT requested_by.department_id
        FROM users requested_by
        WHERE requested_by.id = purchase_request.requested_by
    );

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM purchase_requests
        WHERE payment_type IS NULL
    ) THEN
        RAISE EXCEPTION
            '품목 또는 payment_type이 없는 결제 신청이 존재합니다. 데이터 정리 후 마이그레이션을 다시 실행해주세요.';
    END IF;
END $$;

ALTER TABLE purchase_requests
    ALTER COLUMN payment_type SET NOT NULL,
    ALTER COLUMN content DROP NOT NULL,
    ADD CONSTRAINT fk_purchase_requests_department
        FOREIGN KEY (department_id) REFERENCES departments(id),
    ADD CONSTRAINT ck_purchase_requests_payment_type
        CHECK (payment_type IN ('PREPAID', 'ACTUAL'));

CREATE INDEX idx_purchase_requests_department_id
    ON purchase_requests(department_id);

CREATE INDEX idx_purchase_requests_payment_type
    ON purchase_requests(payment_type);

CREATE INDEX idx_purchase_requests_is_deleted
    ON purchase_requests(is_deleted);

ALTER TABLE purchase_requests_items
    DROP COLUMN payment_type;
