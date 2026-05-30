# Docs Guide

금정야학 백엔드 문서는 역할별로 아래처럼 나눕니다.

## 1. 제품/설계 문서

- [PRD](./prd.md): 제품 요구사항과 기능 우선순위
- [기술 명세서](./tech_spec.md): 아키텍처, 기술 스택, 구현 방향
- [데이터 모델](./data_model.md): 엔티티와 관계, DB 구조
- [Channel/Post 변경안](./channel_post_redesign_plan.md): 자료실 채널, 썸네일, `post_type` 제거, 임시 게시판 플로우까지 포함한 변경 기준서
- [Channel/Post 인수인계서](./channel_post_handover_2026_05_04.md): 실제 구현 완료 범위, 남은 작업, E2E 영향 정리

## 2. API 문서

- [API 안내 문서](./api-spec/api_spec.md): API 문서 구조, 공통 응답 규칙, 참조 순서
- [도메인별 API 문서](./api/README.md): 각 도메인의 엔드포인트, 권한, side effect, 실패 케이스, 시퀀스 다이어그램
- [Site Content 도메인](./Domain/SiteContent/README.md): 기관 정보 정적 페이지 표시 콘텐츠 설계와 작업 순서

## 3. 에러 코드 문서

- [Error Codes](./error_codes.md): 공통/인증/도메인 에러 코드 규칙과 코드표

## 4. 운영성 문서

- [E2E Testing Pipeline](./e2e-testing-pipeline.md): 테스트 파이프라인 설계 및 운영 메모
- [Convention](./convention/convention.md): 개발 컨벤션

## 5. 작업 기록성 문서

- [PR 문서](./pr/): 개별 PR 단위 변경 요약
- [Issue 문서](./issue/): 개별 이슈 관련 메모

## 6. 문서 원칙

- `docs/api-spec/api_spec.md`는 API 문서 안내와 공통 규칙만 다룹니다.
- `docs/api/*.md`는 도메인별 REST 엔드포인트와 운영 규칙을 다룹니다.
- 에러 코드는 `docs/error_codes.md`를 단일 기준 문서로 사용합니다.
- PR/Issue 문서는 제품 명세 문서 대신 작업 기록으로만 사용합니다.
