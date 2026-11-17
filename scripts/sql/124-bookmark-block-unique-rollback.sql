-- #124 롤백: unique 제약 제거
-- 애플리케이션만 되돌릴 때는 실행하지 않아도 된다. 이전 앱도 제약이 있는 테이블에서 동작하고 데이터는 맞게 남는다.
-- 다만 그 상태에서는 북마크 연타와 모든 재차단 요청(이전 앱의 차단에는 존재 확인이 없다)이 unique 위반으로 서버 오류를 받는다.
-- 제약 자체를 없애야 할 때만 실행한다.
--
-- 운영 테이블에서는 unique 인덱스가 user_id 외래 키의 인덱스 역할을 한다.
-- 제약을 추가할 때 MariaDB가 외래 키용 user_id 인덱스를 지우기 때문이다.
-- 그래서 user_id 인덱스를 먼저 만든 뒤 unique 인덱스를 지운다(오류 1553 방지).
-- 롤백 뒤에는 idx_*_user_id 인덱스가 남는다. 적용 스크립트를 다시 실행하면 이 인덱스는 중복이 되므로, 필요하면 지운다.
-- 정리 단계에서 지운 중복 행은 backup_124_* 테이블에 남아 있다. 필요 없으면 확인 후 DROP TABLE한다.

ALTER TABLE user_blocks WAIT 5
    ADD INDEX IF NOT EXISTS idx_user_blocks_user_id (user_id),
    DROP INDEX IF EXISTS uk_user_blocks_user_blocked,
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE post_bookmarks WAIT 5
    ADD INDEX IF NOT EXISTS idx_post_bookmarks_user_id (user_id),
    DROP INDEX IF EXISTS uk_post_bookmarks_user_post,
    ALGORITHM=INPLACE, LOCK=NONE;
