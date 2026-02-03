# Issue 템플릿 - Bug Report

버그 리포트 시 사용하는 템플릿입니다.

---

## 템플릿

```markdown
---
name: Bug Report
about: 버그를 제보합니다
title: '[BUG] '
labels: bug
assignees: ''
---

## 버그 설명

<!-- 발생한 버그를 간단히 설명해주세요 -->

## 재현 방법

1.
2.
3.

## 예상 동작

<!-- 정상적으로 동작해야 하는 방식을 설명해주세요 -->

## 실제 동작

<!-- 실제로 발생한 동작을 설명해주세요 -->

## 스크린샷 / 로그

<!-- 에러 로그, 스크린샷 등 첨부해주세요 -->

```
에러 로그 내용
```

## 환경 정보

- OS:
- Java 버전:
- Spring Boot 버전:
- 브라우저 (프론트 관련 시):

## 추가 정보

<!-- 기타 참고사항 -->

```

---

## GitHub 설정 방법

`.github/ISSUE_TEMPLATE/bug_report.md` 파일로 저장합니다.

```bash
mkdir -p .github/ISSUE_TEMPLATE
cp docs/convention/issue_bug_template.md .github/ISSUE_TEMPLATE/bug_report.md
```
