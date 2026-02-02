# User API

## 1. 사용자 목록 조회
- **URL**: `/api/v1/users`
- **Method**: `GET`
- **Description**: 페이지네이션된 사용자 목록을 조회합니다.
- **Query Parameters**:
    - `page` (optional): 페이지 번호 (0-based)
    - `size` (optional): 페이지 크기
- **Response**: `ApiResponse<BasePageResponse<UserResponse>>`

## 2. 사용자 상세 조회
- **URL**: `/api/v1/users/{id}`
- **Method**: `GET`
- **Description**: ID로 사용자를 조회합니다.
- **Response**: `ApiResponse<UserResponse>`

## 3. 사용자 생성
- **URL**: `/api/v1/users`
- **Method**: `POST`
- **Description**: 이메일 기반 사용자를 생성합니다.
- **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "name": "User Name",
      "password": "password",
      "role": "USER"
    }
    ```
- **Response**: `ApiResponse<UserResponse>`

## 4. 사용자 수정
- **URL**: `/api/v1/users/{id}`
- **Method**: `PUT`
- **Description**: 사용자 정보를 수정합니다.
- **Request Body**:
    ```json
    {
      "name": "Updated Name",
      "role": "ADMIN"
    }
    ```
- **Response**: `ApiResponse<UserResponse>`

## 5. 사용자 삭제
- **URL**: `/api/v1/users/{id}`
- **Method**: `DELETE`
- **Description**: 사용자를 삭제합니다.
- **Response**: `ApiResponse<Void>`
