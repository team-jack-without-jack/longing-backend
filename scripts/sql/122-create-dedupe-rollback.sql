-- #122 롤백(선택). 이전 버전 애플리케이션은 컬럼이 있어도 동작하므로 보통은 애플리케이션 롤백만으로 충분하다.
-- 컬럼까지 지워야 할 때만, 애플리케이션을 이전 버전으로 되돌린 "후" 실행한다.
-- unique 인덱스가 user_id 외래 키의 인덱스 역할을 대신하고 있을 수 있으므로(그대로 지우면 오류 1553),
-- user_id 인덱스를 먼저 만든 뒤 지운다.

ALTER TABLE comments WAIT 5
    ADD INDEX IF NOT EXISTS idx_comments_user_id (user_id),
    DROP INDEX IF EXISTS uk_comments_user_client_request,
    DROP COLUMN IF EXISTS client_request_id;

ALTER TABLE posts WAIT 5
    ADD INDEX IF NOT EXISTS idx_posts_user_id (user_id),
    DROP INDEX IF EXISTS uk_posts_user_client_request,
    DROP COLUMN IF EXISTS client_request_id;
