-- #124 북마크·사용자 차단 중복 방지: 기존 중복 정리 + unique 제약 추가
--
-- 적용 시점: 앱 배포 전후 어느 쪽이든 데이터는 안전하다. 새 앱과 이전 앱 모두 제약이 있든 없든 동작하기 때문이다.
--   - 권장은 앱 배포 후 적용.
--   - SQL을 먼저 적용하면, 이전 앱은 북마크 연타와 "모든" 재차단 요청(이전 앱의 차단에는 존재 확인이 없다)을
--     unique 위반 때문에 서버 오류(INTERNAL_SERVER_ERROR + Slack 알림)로 응답한다. 데이터는 맞게 남는다.
-- 반복 실행해도 안전하다. 어느 단계든 실패하면(Duplicate entry, 잠금 대기 시간 초과 1205, 교착 상태 1213 등)
-- 원인을 확인한 뒤 스크립트를 처음부터 다시 실행한다. 제약이 생기기 전에는 앱이 새 중복을 만들 수 있어서
-- 3단계가 Duplicate entry로 실패할 수 있다. 다시 실행하면 그 중복도 백업하고 정리한다.
--
-- [실행 방법]
--   - mariadb CLI에서 autocommit을 켠 채 실행한다. 예: mariadb ... < 124-bookmark-block-unique.sql
--     이렇게 파일을 넣으면 첫 오류에서 멈춘다. --force는 쓰지 않는다.
--     수동 커밋 모드의 GUI 도구에서 실행하면 커밋할 때까지 잠금이 유지된다.
--   - 실행 전에 두 테이블의 SHOW CREATE TABLE 결과를 저장해 둔다.
--
-- [실행 전 확인]
-- 1) 지울 중복 행 수 (같은 사용자·대상마다 id가 가장 작은 행만 남긴다)
--   SELECT COUNT(*) FROM post_bookmarks b WHERE EXISTS (SELECT 1 FROM post_bookmarks k WHERE k.user_id = b.user_id AND k.post_id = b.post_id AND k.id < b.id);
--   SELECT COUNT(*) FROM user_blocks b WHERE EXISTS (SELECT 1 FROM user_blocks k WHERE k.user_id = b.user_id AND k.blocked_user_id = b.blocked_user_id AND k.id < b.id);
-- 2) soft delete 표시 행 수. 둘 다 0이어야 한다.
--    남길 행을 고를 때 deleted 값을 보지 않으므로, 0이 아니면 삭제 표시된 행이 남고 정상 행이 지워질 수 있다.
--    0이 아니면 실행하지 말고 원인을 확인한다.
--   SELECT COUNT(*) FROM post_bookmarks WHERE deleted = 1;
--   SELECT COUNT(*) FROM user_blocks WHERE deleted = 1;
-- 3) 테이블 크기. 1단계는 잠금 없이 테이블 전체를 읽고, 2단계는 지울 행과 남길 행만 잠깐 잠근다.
--    시간은 크기에 비례한다(검증: 100만 행에 제약 추가까지 약 5초).
--   SELECT COUNT(*) FROM post_bookmarks;
--   SELECT COUNT(*) FROM user_blocks;

-- 1·2단계는 READ COMMITTED로 실행해서 원본 테이블에 읽기 잠금을 걸지 않는다(앱의 쓰기를 막지 않는다).
-- binlog_format=STATEMENT 서버에서는 READ COMMITTED의 INSERT ... SELECT가 거부된다.
-- 그때는 아래 SET 두 줄을 빼고 실행한다. 정리하는 동안 원본 테이블 쓰기가 잠긴다.
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 1. 지울 중복 행 백업 (같은 사용자·대상마다 id가 가장 작은 행을 뺀 나머지)
--    제약을 만든 뒤 백업 테이블을 지우고 다시 실행하면 LIKE가 unique 키까지 복사한다.
--    그러면 같은 쌍의 백업이 조용히 빠지므로 백업 테이블에서는 unique 키를 지운다.
CREATE TABLE IF NOT EXISTS backup_124_post_bookmarks LIKE post_bookmarks;
ALTER TABLE backup_124_post_bookmarks DROP INDEX IF EXISTS uk_post_bookmarks_user_post;
INSERT IGNORE INTO backup_124_post_bookmarks
SELECT b.* FROM post_bookmarks b
JOIN (SELECT user_id, post_id, MIN(id) AS keep_id
      FROM post_bookmarks
      GROUP BY user_id, post_id
      HAVING COUNT(*) > 1) g
  ON g.user_id = b.user_id AND g.post_id = b.post_id AND b.id > g.keep_id;

CREATE TABLE IF NOT EXISTS backup_124_user_blocks LIKE user_blocks;
ALTER TABLE backup_124_user_blocks DROP INDEX IF EXISTS uk_user_blocks_user_blocked;
INSERT IGNORE INTO backup_124_user_blocks
SELECT b.* FROM user_blocks b
JOIN (SELECT user_id, blocked_user_id, MIN(id) AS keep_id
      FROM user_blocks
      GROUP BY user_id, blocked_user_id
      HAVING COUNT(*) > 1) g
  ON g.user_id = b.user_id AND g.blocked_user_id = b.blocked_user_id AND b.id > g.keep_id;

-- 2. 백업한 행만 지운다. 지우기 직전에 더 작은 id가 아직 있는지 다시 확인한다.
--    백업 뒤에 생긴 중복은 지우지 않으므로, 지운 행은 항상 백업 테이블에 있다.
--    그런 중복이 남으면 3단계가 실패하고, 다시 실행하면 정리된다.
DELETE b FROM backup_124_post_bookmarks x
JOIN post_bookmarks b ON b.id = x.id
JOIN post_bookmarks k ON k.user_id = b.user_id AND k.post_id = b.post_id AND k.id < b.id;

DELETE b FROM backup_124_user_blocks x
JOIN user_blocks b ON b.id = x.id
JOIN user_blocks k ON k.user_id = b.user_id AND k.blocked_user_id = b.blocked_user_id AND k.id < b.id;

SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- 3. unique 제약 (온라인 DDL)
--    메타데이터 잠금을 기다리는 동안에는 이 테이블의 다른 쿼리도 멈춘다(ALTER 시작과 끝에 각각 최대 5초).
--    WAIT로 실패(1205)하면 트래픽이 적은 시간에 다시 실행한다.
ALTER TABLE post_bookmarks WAIT 5
    ADD UNIQUE KEY IF NOT EXISTS uk_post_bookmarks_user_post (user_id, post_id),
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE user_blocks WAIT 5
    ADD UNIQUE KEY IF NOT EXISTS uk_user_blocks_user_blocked (user_id, blocked_user_id),
    ALGORITHM=INPLACE, LOCK=NONE;

-- [적용 후 확인]
--   SHOW INDEX FROM post_bookmarks WHERE Key_name = 'uk_post_bookmarks_user_post';
--   SHOW INDEX FROM user_blocks WHERE Key_name = 'uk_user_blocks_user_blocked';
--   SELECT COUNT(*) FROM backup_124_post_bookmarks;  -- 실행 전 확인 1)과 거의 같다(차이는 그 사이 앱 활동이나 재실행 때문)
--   SELECT COUNT(*) FROM backup_124_user_blocks;
