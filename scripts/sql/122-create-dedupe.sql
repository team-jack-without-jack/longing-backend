-- #122 게시글·댓글 생성 중복 방지: Idempotency-Key 저장 컬럼과 사용자별 unique 제약
--
-- 적용 시점: 이 변경이 포함된 애플리케이션을 배포하기 "전"에 실행한다.
--   새 엔티티는 client_request_id 컬럼을 조회하므로, 미적용 상태로 배포하면 게시글·댓글 조회가 실패한다.
--   이전 버전 애플리케이션은 이 컬럼이 있어도 그대로 동작한다(nullable, 미매핑).
-- 영향: 기존 행은 NULL이 되며, unique 인덱스는 NULL 중복을 허용하므로 기존 데이터와 키 없는 요청에 영향이 없다.
-- 인덱스 이름은 PostEntity/CommentEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT와 같아야 한다(중복 키 위반 판별에 사용).
-- 키는 Redis와 같이 대소문자를 구분해야 하므로 바이너리 정렬(ascii_bin)을 쓴다. 허용 문자는 [A-Za-z0-9_-]뿐이다.
-- 잠금: WAIT 5 - 열린 트랜잭션 때문에 메타데이터 잠금을 5초 안에 얻지 못하면 실패(오류 1205)하고 다른 쿼리를 막지 않는다.
--       실패하면 트래픽이 적은 시간에 다시 실행한다. ALGORITHM=INPLACE, LOCK=NONE으로 실행 중 읽기·쓰기를 막지 않는다.
-- 재실행: IF NOT EXISTS로 일부만 적용된 뒤 다시 실행해도 된다. 다른 정의로 이미 있는 컬럼은 건너뛰므로 아래 확인 쿼리로 검증한다.
-- 참고: user_id 외래 키가 자동 생성 인덱스를 쓰고 있으면 새 unique 인덱스(user_id가 첫 컬럼)가 그 역할을 대신하고
--       자동 인덱스는 사라질 수 있다. 롤백은 122-create-dedupe-rollback.sql을 쓴다.
-- 함께 필요한 운영 설정(이슈 #122): spring.redis.* (이 기능의 Redis), management.health.redis.enabled=false (Redis 장애가 /actuator/health를 DOWN·지연시키지 않도록).

ALTER TABLE posts WAIT 5
    ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD UNIQUE KEY IF NOT EXISTS uk_posts_user_client_request (user_id, client_request_id),
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE comments WAIT 5
    ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD UNIQUE KEY IF NOT EXISTS uk_comments_user_client_request (user_id, client_request_id),
    ALGORITHM=INPLACE, LOCK=NONE;

-- 확인 쿼리(기대값: ascii / ascii_bin / YES, 각 인덱스는 user_id, client_request_id 순서의 unique):
-- SELECT TABLE_NAME, CHARACTER_SET_NAME, COLLATION_NAME, IS_NULLABLE FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('posts', 'comments') AND COLUMN_NAME = 'client_request_id';
-- SELECT TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND INDEX_NAME IN ('uk_posts_user_client_request', 'uk_comments_user_client_request');
