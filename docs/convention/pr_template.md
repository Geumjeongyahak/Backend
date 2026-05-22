# Pull Request 템플릿

GitHub PR 생성 시 사용하는 템플릿입니다.

---

## 템플릿

```markdown
## 개요

<!-- 이 PR에서 변경한 내용을 간단히 설명해주세요 -->

## 변경 유형

- [ ] 새 기능 (feat)
- [ ] 버그 수정 (fix)
- [ ] 리팩토링 (refactor)
- [ ] 문서 수정 (docs)
- [ ] 테스트 (test)
- [ ] 기타 (chore)

## 변경 내용

<!-- 상세 변경 내용을 작성해주세요 -->

-
-
-

## 관련 이슈

<!-- 관련 이슈 번호를 입력해주세요 -->
Closes #

## 스크린샷 (선택)

<!-- API 응답, UI 변경 등 시각적 확인이 필요한 경우 -->

## 체크리스트

- [ ] 코드 컨벤션을 준수했습니다
- [ ] 테스트 코드를 작성/수정했습니다
- [ ] 머지 전 `./gradlew test`가 통과합니다
- [ ] 문서를 업데이트했습니다 (필요한 경우)

## 리뷰어에게

<!-- 리뷰 시 중점적으로 봐주었으면 하는 부분이 있다면 작성해주세요 -->

```

---

## GitHub 설정 방법

`.github/pull_request_template.md` 파일로 저장하면 PR 생성 시 자동 적용됩니다.

```bash
mkdir -p .github
cp docs/convention/pr_template.md .github/pull_request_template.md
```
