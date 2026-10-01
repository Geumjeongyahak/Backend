-- 만료 스케줄러와 승인·반려의 동시 수정을 낙관적 락으로 막는다 (#240)
ALTER TABLE absence_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE lesson_exchange_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
