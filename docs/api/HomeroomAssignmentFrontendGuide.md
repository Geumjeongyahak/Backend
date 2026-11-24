# Homeroom Assignment Frontend Guide

프론트엔드에서 사용자 업무분장과 담임 지정 화면을 구현할 때 사용하는 백엔드 계약을 정리합니다.

## 핵심 개념

사용자 응답에는 서로 다른 세 종류의 분반 정보가 존재할 수 있습니다.

| 필드 | 의미 | 변경 API |
| --- | --- | --- |
| `classroom`, `classroomId` | 사용자의 대표 분반 | `PUT/DELETE /api/v1/users/{userId}/classroom` |
| `workAssignment.classroomId` | 담임 업무분장의 분반 | `PUT/DELETE /api/v1/users/{userId}/homeroom` |
| `teacherAssignments[].classroomId` | 실제 담당 중인 수업의 분반 | 교사 시간표 배정 API |

세 값은 독립적입니다. 대표 분반이 장미반인 사용자가 벚꽃반 담임이면서 국화반 수업을 담당할 수도 있습니다.
수업 교환으로 다른 교사가 일시적으로 수업을 진행해도 담임 업무분장은 변경되지 않습니다.

## 업무분장 응답

사용자 목록과 상세 응답에는 `workAssignment`가 포함될 수 있습니다.

부서 업무분장:

```json
{
  "type": "DEPARTMENT",
  "name": "교육연구부",
  "departmentId": 2
}
```

담임 업무분장:

```json
{
  "type": "HOMEROOM",
  "name": "담임",
  "classroomId": 1,
  "classroomName": "벚꽃반"
}
```

업무분장이 없으면 `workAssignment`는 JSON 응답에서 생략될 수 있습니다. 따라서 프론트 타입에서는 optional로 선언합니다.

```ts
type WorkAssignment =
  | {
      type: 'DEPARTMENT';
      name: string;
      departmentId: number;
    }
  | {
      type: 'HOMEROOM';
      name: '담임';
      classroomId: number;
      classroomName: string;
    };

type UserResponse = {
  workAssignment?: WorkAssignment;
};
```

목록 화면에서는 `workAssignment.name`을 업무분장으로 표시하면 됩니다. 담임 분반이 필요한 상세 화면에서는
`type === 'HOMEROOM'`일 때 `classroomName`을 함께 표시합니다.

## 담임 지정 및 변경

```http
PUT /api/v1/users/{userId}/homeroom
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "classroomId": 1
}
```

권한은 `ROLE_ADMIN` 또는 `user:manage:*`가 필요합니다. 성공 시 변경된 `UserDetailResponse`를 반환합니다.

지정 시 적용되는 규칙:

- 해당 분반의 기존 담임은 자동 해제됩니다.
- 대상 사용자의 부서 업무분장은 자동 해제됩니다.
- 대표 분반과 담당 수업은 유지됩니다.
- `VOLUNTEER`, `MANAGER`, `ADMIN`만 담임으로 지정할 수 있습니다.
- 같은 사용자와 분반으로 요청을 반복해도 최종 상태는 동일합니다.

담임 선택에 사용할 활성 분반 목록은 다음 API로 조회합니다.

```http
GET /api/v1/classrooms?page=0&size=100
```

## 담임 해제

```http
DELETE /api/v1/users/{userId}/homeroom
Authorization: Bearer {accessToken}
```

성공 시 `204 No Content`를 반환합니다. 해제 후 이전 부서 업무분장은 자동 복원되지 않습니다.

## 오류 처리

| 상태 | 대표 원인 | 프론트 처리 |
| --- | --- | --- |
| `400 Bad Request` | `classroomId` 누락 또는 `GUEST` 담임 지정 | 입력과 대상 사용자 역할 확인 |
| `403 Forbidden` | `ROLE_ADMIN`, `user:manage:*` 권한 없음 | 관리 기능 비노출 또는 권한 안내 |
| `404 Not Found` | 비활성·미존재 사용자 또는 분반 | 목록 재조회 후 선택 상태 초기화 |

기존 담임이 있다는 이유로 충돌 응답을 반환하지는 않습니다. 새 담임 지정이 성공하면 기존 담임은 자동 해제됩니다.

## 부서와 담임 간 전환

두 업무분장은 동시에 가질 수 없습니다.

| 현재 상태 | 요청 | 결과 |
| --- | --- | --- |
| 부서 업무분장 | 담임 지정 | 부서 해제 후 담임 지정 |
| 담임 업무분장 | 사용자 수정으로 `departmentId` 지정 | 담임 해제 후 부서 지정 |
| 담임 업무분장 | 담임 해제 | 업무분장 없음 |
| 기존 담임이 있는 분반 | 다른 사용자 담임 지정 | 기존 담임 해제 후 새 담임 지정 |

부서 업무분장을 명시적으로 해제할 때는 다음 API를 사용합니다.

```http
DELETE /api/v1/users/{userId}/department
```

## 프론트 구현 시 주의사항

- `department`만 보고 업무분장을 판단하지 말고 `workAssignment`를 사용합니다.
- 대표 분반을 담임 분반으로 간주하지 않습니다.
- 시간표의 담당 교사를 담임으로 간주하지 않습니다.
- 담임 변경 성공 후에는 PUT 응답으로 대상 사용자를 갱신하고, 같은 분반의 기존 담임이 화면에 있을 수 있으므로 사용자 목록을 재조회하는 것을 권장합니다.
- `workAssignment`가 없는 사용자를 정상 상태로 처리합니다. 모든 봉사자에게 업무분장이 지정되는 것이 운영 원칙이지만 API는 미지정 상태도 허용합니다.
