-- #126 게시글 신고 중복 정리 + unique 제약 추가
--
-- 적용 시점: 앱 배포 전후 어느 쪽이든 데이터는 안전하다. 새 앱과 이전 앱 모두 제약이 있든 없든 동작하기 때문이다.
--   - 권장은 앱 배포 후 적용.
--   - SQL을 먼저 적용하면, 이전 앱은 동시에 들어온 중복 신고를 unique 위반 때문에 서버 오류(INTERNAL_SERVER_ERROR + Slack 알림)로
--     응답한다. 순차 중복 신고는 지금처럼 ALREADY_REPORTED다. 데이터는 맞게 남는다.
-- 반복 실행해도 안전하다. 어느 단계든 실패하면(Duplicate entry, 잠금 대기 시간 초과 1205, 교착 상태 1213 등)
-- 원인을 확인한 뒤 스크립트를 처음부터 다시 실행한다.
--
-- [실행 방법]
--   - mariadb CLI에서 autocommit을 켠 채 실행한다. 예: mariadb ... < 126-post-report-unique.sql
--     이렇게 파일을 넣으면 첫 오류에서 멈춘다. --force는 쓰지 않는다.
--     수동 커밋 모드의 GUI 도구에서 실행하면 커밋할 때까지 잠금이 유지된다.
--   - 실행 전에 SHOW CREATE TABLE post_reports 결과를 저장해 둔다.
--
-- [실행 전 확인]
-- 1) 지울 중복 행 수 (같은 신고자·게시글·사유마다 id가 가장 작은 행만 남긴다)
--   SELECT COUNT(*) FROM post_reports b WHERE EXISTS (SELECT 1 FROM post_reports k WHERE k.reporter_id = b.reporter_id AND k.post_id = b.post_id AND k.report_reason = b.report_reason AND k.id < b.id);
-- 2) 테이블 크기. 1단계는 잠금 없이 테이블 전체를 읽고, 2단계는 지울 행과 남길 행만 잠깐 잠근다.
--   SELECT COUNT(*) FROM post_reports;
-- 3) 테이블 형식. unique 키에 report_reason VARCHAR(255)(utf8mb4 1020바이트)가 들어가므로 ROW_FORMAT이 Dynamic 또는 Compressed여야 한다.
--    Compact·Redundant면 3단계가 1709 오류로 실패한다(정리는 이미 끝난 뒤지만 지운 행은 백업에 있다).
--   SELECT ROW_FORMAT, TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post_reports';
--
-- 참고: 중복 신고 때문에 잘못 자동 삭제된 게시글은 이 테이블로 찾을 수 없다.
-- 자동 삭제(엔티티 삭제)가 cascade로 그 게시글의 신고 행까지 하드 삭제하기 때문이다(기존 동작).

-- 1·2단계는 READ COMMITTED로 실행해서 원본 테이블에 읽기 잠금을 걸지 않는다(앱의 쓰기를 막지 않는다).
-- binlog_format=STATEMENT 서버에서는 READ COMMITTED의 INSERT ... SELECT가 거부된다.
-- 그때는 아래 SET 두 줄을 빼고 실행한다. 정리하는 동안 원본 테이블 쓰기가 잠긴다.
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 1. 지울 중복 행 백업 (같은 신고자·게시글·사유마다 id가 가장 작은 행을 뺀 나머지)
--    제약을 만든 뒤 백업 테이블을 지우고 다시 실행하면 LIKE가 unique 키까지 복사한다.
--    그러면 같은 조합의 백업이 조용히 빠지므로 백업 테이블에서는 unique 키를 지운다.
CREATE TABLE IF NOT EXISTS backup_126_post_reports LIKE post_reports;
ALTER TABLE backup_126_post_reports DROP INDEX IF EXISTS uk_post_reports_reporter_post_reason;
INSERT IGNORE INTO backup_126_post_reports
SELECT b.* FROM post_reports b
JOIN (SELECT reporter_id, post_id, report_reason, MIN(id) AS keep_id
      FROM post_reports
      GROUP BY reporter_id, post_id, report_reason
      HAVING COUNT(*) > 1) g
  ON g.reporter_id = b.reporter_id AND g.post_id = b.post_id AND g.report_reason = b.report_reason
 AND b.id > g.keep_id;

-- 2. 백업한 행만 지운다. 지우기 직전에 더 작은 id가 아직 있는지 다시 확인한다.
--    백업 뒤에 생긴 중복은 지우지 않으므로, 지운 행은 항상 백업 테이블에 있다.
--    그런 중복이 남으면 3단계가 실패하고, 다시 실행하면 정리된다.
DELETE b FROM backup_126_post_reports x
JOIN post_reports b ON b.id = x.id
JOIN post_reports k ON k.reporter_id = b.reporter_id AND k.post_id = b.post_id
 AND k.report_reason = b.report_reason AND k.id < b.id;

SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- 3. unique 제약 (온라인 DDL)
--    메타데이터 잠금을 기다리는 동안에는 이 테이블의 다른 쿼리도 멈춘다(ALTER 시작과 끝에 각각 최대 5초).
--    WAIT로 실패(1205)하면 트래픽이 적은 시간에 다시 실행한다.
ALTER TABLE post_reports WAIT 5
    ADD UNIQUE KEY IF NOT EXISTS uk_post_reports_reporter_post_reason (reporter_id, post_id, report_reason),
    ALGORITHM=INPLACE, LOCK=NONE;

-- [적용 후 확인]
--   SHOW INDEX FROM post_reports WHERE Key_name = 'uk_post_reports_reporter_post_reason';
--   SELECT COUNT(*) FROM backup_126_post_reports;  -- 실행 전 확인 1)과 거의 같다(차이는 그 사이 앱 활동이나 재실행 때문)
