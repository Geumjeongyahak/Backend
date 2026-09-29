# Drive 파일 등록을 Drive 조회 결과로 저장

- 이슈: #231
- 브랜치: `fix/231-drive-file-verification`

## 지금 어떻게 쓰이나 (2026-09-29 dev 서버)

| 확인 | 결과 |
|---|---|
| Drive 파일 행 | 1개 (7월 8일). URL 형태가 서버 업로드가 남기는 것과 달라 등록 API로 들어온 것으로 보인다 |
| 서버 업로드(`POST /api/v1/files/drive/{target}`) | 6월 27일에 생겼다. 이쪽은 이미 Drive 응답 값으로 저장한다 |
| 서버의 Drive 인증 | GCP 서비스 계정, `drive` 범위. 서비스 계정에 공유됐거나 공유 드라이브에 있는 파일만 읽힌다 |

등록 API는 드물게 쓰이지만, 등록된 파일은 영수증으로 붙일 수 있고 결의서를 만들 때 **서버 계정으로
그 파일 ID를 내려받는다.** 요청 값을 그대로 믿으면 안 되는 이유가 이것이다.

## 무엇을 바꾸나

| 파일 | 변경 |
|---|---|
| `DriveStorageService` | `StoredDriveFile getMetadata(String fileId)` 추가 |
| `GoogleDriveStorageService` | `GET files/{id}?supportsAllDrives=true&fields=id,name,mimeType,size,webViewLink`. 404 · 403 → 「확인할 수 없는 파일」(400), 그 밖의 실패 → `FILE_UPLOAD_FAILED`(500). 기존 `download`와 같은 모양 |
| `DriveFileService.registerDriveFile` | 링크에서 ID를 뽑고 → Drive에 조회 → 이름 · 형식 · 크기를 **조회 결과로** 저장. 같은 링크가 이미 있으면 **기존 행을 그대로** 돌려준다 |
| `File.updateDriveMetadata` | 쓰는 곳이 없어져 지운다 |
| `RegisterDriveFileRequest` | `originalName` · `mimeType` · `fileSize`는 받되 무시한다. `originalName`의 `@NotBlank`를 푼다(프론트가 안 보내도 되게). Swagger 설명을 고친다 |
| `FileController` | 등록 API 설명의 「백엔드는 Drive 클라이언트를 사용하지 않고」를 고친다 |
| `TestStorageConfig` (테스트) | Drive 대역에 `getMetadata`를 더한다. 모르는 ID는 「확인할 수 없음」 |

요청 항목을 없애지 않는 이유: 없애면 지금 프론트 요청이 400이 된다. 남겨 두고 무시하면 프론트는 그대로
동작하고, 나중에 프론트가 안 보내게 되면 그때 지운다.

## 실패 경로

| 상황 | 결과 |
|---|---|
| Drive 링크가 아니다 / ID를 못 뽑는다 | 400 (지금과 같다) |
| 서버 계정이 못 읽는 파일 (비공개, 없는 ID) | 400 「확인할 수 없는 Google Drive 파일입니다」. Drive는 권한이 없어도 404를 준다 |
| Drive 장애 · 네트워크 오류 | 500 `FILE_UPLOAD_FAILED`, 오류 로그. 사용자 링크 탓으로 돌리지 않는다 |
| 같은 링크를 다시 등록 | 기존 행 그대로. Drive 조회도 하지 않는다 |
| 요청의 이름 · 형식 · 크기가 실제와 다르다 | 무시된다. Drive 값이 저장된다 |

## 안 하는 것

- 공유 드라이브 소속 확인(Should). 서비스 계정은 원래 공유된 파일만 읽는다
- 이미 등록된 행 재검증 (이슈 Out of scope)
- Drive 조회를 트랜잭션 밖으로 빼기. 등록은 dev 전체에 1건이고 조회는 1회다
- 서버 계정이 읽을 수 있는 파일이면 누구의 파일이든 등록된다. 제한하려면 폴더 · 공유 드라이브 소속을 봐야 하고, 그건 위 Should다

## 테스트 (먼저 쓰고 실패를 본다)

| 층 | 테스트 | 고치기 전 |
|---|---|---|
| 단위 (`GoogleDriveStorageServiceTest`) | `getMetadata`가 Drive 응답의 이름 · 형식 · 크기를 돌려준다 / 404면 「확인할 수 없음」 / 500이면 `FILE_UPLOAD_FAILED` | 실패 (메서드 없음) |
| 단위 (`DriveFileService`) | 저장 값이 요청 값이 아니라 Drive 값이다 / Drive가 못 찾으면 거절하고 저장하지 않는다 / 이미 있는 링크는 기존 행을 그대로 돌려준다 | 실패 |
| E2E | 확인할 수 없는 링크 등록 → 400 | 실패 (201) |
| 회귀 | 기존 Drive 등록 → 게시글 첨부 E2E | — |

## 작업

1. 테스트 → 실패 확인 → 커밋 `test(file): …`
2. `getMetadata` + 등록 흐름 + 요청 DTO · API 설명 → 커밋 `fix(file): …`
3. 이슈 본문 「미정」 두 줄에 결정을 적는다
