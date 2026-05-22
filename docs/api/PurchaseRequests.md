# Purchase Request API

구입 요청, 구매 완료 거래, 거래처 잔액 차감 흐름을 정리한 문서입니다.

이 문서는 `PurchaseRequestController`, `PurchaseRequestAdminController`, `VendorAdminController`, `FileController`, `PurchaseRequestService`, `VendorService`, `FileCleanupScheduler` 구현 기준으로 작성했습니다.

## 1. 역할과 범위

- 일반 사용자는 본인이 작성한 구입 요청을 생성, 조회, 삭제, 구매 완료 보고할 수 있습니다.
- 관리자 또는 `purchase-request:read:*` 권한자는 전체 구입 요청을 조회할 수 있습니다.
- 관리자 또는 `purchase-request:review:*` 권한자는 구입 요청을 승인/반려할 수 있습니다.
- 관리자 또는 `purchase-request:manage:*` 권한자는 처리 전 요청 삭제와 최종 결재 확인을 수행할 수 있습니다.
- 거래처 생성/수정/삭제/충전은 관리자 또는 `vendor:manage:*` 권한자가 수행합니다.
- 거래처 상세/잔액 이력 조회는 관리자 또는 `vendor:read:*` 권한자가 수행합니다.

## 2. 핵심 규칙

### 2.1 구입 요청 상태

| 값 | 의미 |
|---|---|
| `PENDING` | 결재 신청 |
| `APPROVED` | 결재 승인 |
| `PURCHASED` | 구매 완료 보고 |
| `CONFIRMED` | 최종 결재 확인 |
| `REJECTED` | 반려 |

### 2.2 결제 유형

| 값 | 의미 |
|---|---|
| `PREPAID` | 선 결제 |
| `ACTUAL` | 실 결제 |

- 결제 유형은 요청 품목별 `paymentType`으로 저장합니다.
- 요청 생성 시 예상 금액, 거래처, 영수증은 받지 않습니다.
- 구매 완료 보고 시 거래처별 거래 라인(`transactions[]`)에 실제 결제 금액을 입력합니다.
- `CONFIRMED` 전환 시 거래처별 총 결제 금액만큼 잔액을 차감하고 `DEDUCT` 이력을 저장합니다.
- 거래처 잔액이 결제 금액보다 적으면 결재 확인 요청은 `409 CONFLICT`로 실패하며 요청 상태와 거래처 잔액은 변경되지 않습니다.
- 거래처 잔액 차감은 같은 거래처에 대한 동시 승인 요청을 고려해 `PESSIMISTIC_WRITE` lock으로 거래처를 다시 조회한 뒤 수행합니다.

### 2.3 영수증 정책

- 영수증은 구매 완료 거래 라인 단위로 선택 첨부합니다.
- 구매 완료 보고 API는 `transactions[].receiptFileId`를 받습니다.
- soft delete된 파일은 영수증으로 재연결할 수 없습니다.

### 2.4 임시 업로드 파일 정리

- 품목 영수증 이미지는 `POST /api/v1/files/images/purchase-items`로 먼저 업로드합니다.
- 업로드된 파일은 `documents/purchase-items/` 경로와 `files` 메타데이터로 저장됩니다.
- 파일이 어떤 구매 완료 거래 라인의 `receiptFile`에도 연결되지 않은 상태로 `app.file.cleanup.temporary-retention-hours`를 초과하면 `FileCleanupScheduler`가 soft delete 처리합니다.
- soft delete 파일은 기존 파일 정리 정책에 따라 `app.file.cleanup.retention-days`가 지난 뒤 storage 삭제에 성공한 경우에만 DB에서 hard delete 됩니다.
- storage 삭제 실패 시 DB 레코드는 유지되어 다음 스케줄러 실행 때 재시도됩니다.

## 3. 대표 플로우

### 3.1 결재 확인 시 거래처 잔액 차감

```mermaid
sequenceDiagram
    actor Admin as 관리자
    participant API as PurchaseRequestAdminController
    participant PR as PurchaseRequestService
    participant VendorSvc as VendorService
    participant VendorRepo as VendorRepository
    participant HistoryRepo as VendorBalanceHistoryRepository
    participant Push as EventPublisher

    Admin->>API: PATCH /api/v1/admin/purchase-requests/{id}/confirm
    API->>PR: confirmPurchase(confirmerId, requestId)
    PR->>PR: PURCHASED 상태와 거래 라인 필수값 검증
    loop 거래처별 합산 금액
        PR->>VendorSvc: deductForPurchaseRequest(vendor, request, amount, confirmer)
        VendorSvc->>VendorRepo: findByIdForUpdate(vendorId)
        VendorSvc->>VendorSvc: 활성/잔액 검증 및 차감
        VendorSvc->>HistoryRepo: DEDUCT 이력 저장
    end
    PR->>PR: request.confirm()
```

### 3.2 영수증 업로드와 구매 완료 보고

```mermaid
sequenceDiagram
    actor User as 요청자
    participant FileAPI as FileController
    participant Storage as StorageService
    participant PRAPI as PurchaseRequestController
    participant PR as PurchaseRequestService
    participant FileProxy as FileProxyService

    User->>FileAPI: POST /api/v1/files/images/purchase-items
    FileAPI->>Storage: documents/purchase-items/ 업로드
    FileAPI-->>User: receiptFileId 반환
    User->>PRAPI: POST /api/v1/purchase-requests/{id}/report
    PRAPI->>PR: reportPurchase(requestId, transactions[])
    PR->>PR: APPROVED 상태와 승인 후 7일 이내 검증
    loop transactions[]
        PR->>FileProxy: getActiveById(receiptFileId)
        PR->>PR: 거래처/품목명/금액/선택 영수증 저장
    end
    PR->>PR: request.reportPurchase()
```

### 3.3 미연동 품목 영수증 파일 정리

```mermaid
sequenceDiagram
    participant Scheduler as FileCleanupScheduler
    participant FileRepo as FileRepository
    participant Storage as StorageService
    participant PostFile as PostFileRepository
    participant PostAttachment as PostAttachmentRepository
    participant TxRepo as PurchaseRequestPaymentTransactionRepository

    Scheduler->>FileRepo: findUnlinkedPurchaseItemFilesBefore(prefix, threshold)
    Scheduler->>Scheduler: 연결 없는 purchase-items 파일 soft delete
    Scheduler->>FileRepo: findByIsDeletedTrueAndDeletedAtBefore(retentionThreshold)
    loop 삭제 후보
        Scheduler->>Storage: delete(storageKey)
        alt storage 삭제 성공
            Scheduler->>PostFile: deleteByFileId(fileId)
            Scheduler->>PostAttachment: deleteByFileId(fileId)
            Scheduler->>TxRepo: clearReceiptFileByFileId(fileId)
            Scheduler->>FileRepo: delete(file)
        else storage 삭제 실패
            Scheduler->>Scheduler: DB 레코드 유지
        end
    end
```

## 4. 주요 API

### 4.1 구입 요청 생성

- **URL**: `/api/v1/purchase-requests`
- **Method**: `POST`
- **권한**: 인증 사용자

```json
{
  "title": "교재 구입",
  "content": "수업에 필요한 교재를 구입합니다.",
  "classroomId": 1,
  "items": [
    {
      "name": "국어 교재",
      "reason": "수업 교재 부족",
      "quantity": 2,
      "paymentType": "PREPAID"
    }
  ]
}
```

### 4.2 구입 요청 승인

- **URL**: `/api/v1/admin/purchase-requests/{requestId}/approve`
- **Method**: `PATCH`
- **권한**: `ADMIN` 또는 `purchase-request:review:*`

```json
{
  "note": "승인합니다."
}
```

### 4.3 구매 완료 보고

- **URL**: `/api/v1/purchase-requests/{requestId}/report`
- **Method**: `POST`
- **권한**: 인증 사용자 + 요청자 본인 조건

```json
{
  "transactions": [
    {
      "vendorId": 1,
      "itemNames": ["국어 교재", "복사용지"],
      "amount": 15000,
      "receiptFileId": "9e20d3e8-d4f2-42df-bf73-6dd97cc6fc2d"
    }
  ]
}
```

### 4.4 거래처 관리

| 기능 | URL | Method | 권한 |
|---|---|---|---|
| 거래처 목록 | `/api/v1/admin/vendors` | `GET` | 인증 사용자 |
| 거래처 생성 | `/api/v1/admin/vendors` | `POST` | `ADMIN` 또는 `vendor:manage:*` |
| 거래처 상세 | `/api/v1/admin/vendors/{vendorId}` | `GET` | `ADMIN` 또는 `vendor:read:*` |
| 거래처 수정 | `/api/v1/admin/vendors/{vendorId}` | `PATCH` | `ADMIN` 또는 `vendor:manage:*` |
| 거래처 삭제 | `/api/v1/admin/vendors/{vendorId}` | `DELETE` | `ADMIN` 또는 `vendor:manage:*` |
| 거래처 충전 | `/api/v1/admin/vendors/{vendorId}/charges` | `POST` | `ADMIN` 또는 `vendor:manage:*` |
| 거래처 잔액 이력 | `/api/v1/admin/vendors/{vendorId}/histories` | `GET` | `ADMIN` 또는 `vendor:read:*` |

## 5. 주요 실패 케이스

| 상황 | HTTP | 코드 |
|---|---|---|
| 구입 요청 없음 | 404 | `PR-001` |
| 요청 접근 권한 없음 | 403 | `PR-002` |
| 이미 처리된 요청 승인/반려 | 409 | `PR-003` |
| 존재하지 않는 품목 보고 | 404 | `PR-004` |
| 승인 후 7일 초과 구매 보고 | 409 | `PR-005` |
| 구매 완료 거래 입력 오류 | 400/409 | `PR-006`, `PR-007` |
| 거래처 없음 | 404 | `VEN-001` |
| 비활성 거래처 사용 | 409 | `VEN-002` |
| 거래처 잔액 부족 | 409 | `VEN-003` |

## 6. 사이드 이펙트

- 결재 확인 성공 시 거래처 잔액과 거래처 잔액 이력이 함께 변경됩니다.
- 잔액 부족, 거래처 비활성, 상태 오류가 발생하면 요청 상태와 거래처 잔액은 변경되지 않습니다.
- 구매 완료 보고 성공 시 요청 상태가 `PURCHASED`로 변경됩니다.
- 결재 확인 성공 시 요청 상태가 `CONFIRMED`로 변경됩니다.
- 미연동 품목 영수증 파일은 스케줄러에 의해 soft delete될 수 있으므로, 업로드 직후 보고 payload에 연결해야 합니다.
