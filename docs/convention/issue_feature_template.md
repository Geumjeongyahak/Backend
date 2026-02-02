# Issue 템플릿 - Feature Request

새 기능 요청 시 사용하는 템플릿입니다.

---

## 템플릿

```markdown
---
name: Feature Request
about: 새로운 기능을 요청합니다
title: '[FEAT] '
labels: enhancement
assignees: ''
---

## 기능 설명

<!-- 구현하고자 하는 기능을 설명해주세요 -->

## 배경

<!-- 이 기능이 필요한 이유를 설명해주세요 -->

## 상세 요구사항

- [ ] 요구사항 1
- [ ] 요구사항 2
- [ ] 요구사항 3

## 참고 자료

<!-- 관련 문서, 디자인, 참고 링크 등 -->

- PRD:
- API 명세:
- 기타:

## 추가 정보

<!-- 기타 참고사항 -->

```

---

## GitHub 설정 방법

`.github/ISSUE_TEMPLATE/feature_request.md` 파일로 저장합니다.

```bash
mkdir -p .github/ISSUE_TEMPLATE
cp docs/convention/issue_feature_template.md .github/ISSUE_TEMPLATE/feature_request.md
```
