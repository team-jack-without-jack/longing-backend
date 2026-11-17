-- #126 댓글 수 재계산 (선택 실행)
-- comment_count가 실제 댓글 수와 다른 게시글을 바로잡는다. 주로 댓글 삭제 연타로 작아지거나 음수가 된 경우다(삭제된 게시글은 제외).
-- 새 앱을 배포한 뒤에 실행한다(이전 앱이 돌고 있으면 다시 틀어질 수 있다). 반복 실행해도 안전하다.
--
-- [실행 방법]
--   - mariadb CLI에서 autocommit을 켠 채 파일로 넣어 실행한다(파일로 넣으면 첫 오류에서 멈춘다. --force는 쓰지 않는다).
--   - binlog_format=STATEMENT 서버에서는 1665 오류로 아무것도 바꾸지 않고 실패한다.
--     이때 REPEATABLE READ로 바꿔 실행하지 않는다(posts 전체가 잠긴다). binlog 형식을 먼저 확인한다.
--   - 드물게 게시글 삭제·회원 탈퇴와 겹치면 교착 상태(1213)가 날 수 있다. 그때는 다시 실행한다.
--
-- [실행 전·후 확인] 실제 댓글 수(삭제되지 않은 댓글)와 다른, 삭제되지 않은 게시글. 실행 후에는 0행이어야 한다.
--   실행 중에 새로 어긋난 게시글이 생기면(드묾) 다시 실행한다.
--   SELECT p.id, p.comment_count, COUNT(c.id) AS actual
--   FROM posts p LEFT JOIN comments c ON c.post_id = p.id AND c.deleted = 0
--   WHERE p.deleted = 0
--   GROUP BY p.id, p.comment_count
--   HAVING p.comment_count <> COUNT(c.id);
--
-- 방식: (1) 잠금 없이 어긋난 게시글을 고르고, (2) 그 게시글 행만 잠근 뒤, (3) 새 문장으로 다시 센다.
-- 댓글을 쓰는 모든 경로(댓글 생성·삭제, 신고 자동 삭제)는 게시글 행을 먼저 잠근다.
-- 그래서 (2)는 진행 중인 쓰기가 끝나길 기다리고, (3)은 그 뒤의 최신 커밋 상태로 센다.
-- UPDATE 한 문장으로 바로 세면 안 된다. READ COMMITTED에서도 하위 쿼리는 문장 시작 시점의 스냅샷을 쓴다.
-- 그러면 실행 중에 댓글이 생긴 게시글을 옛 값으로 덮어쓴다(리뷰에서 재현).
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

CREATE TEMPORARY TABLE IF NOT EXISTS recount_126_targets (id BIGINT PRIMARY KEY);
DELETE FROM recount_126_targets;

-- 1. 후보: 일관된 읽기, 잠금 없음
INSERT INTO recount_126_targets (id)
SELECT p.id FROM posts p
WHERE p.deleted = 0
  AND p.comment_count <> (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id AND c.deleted = 0);

START TRANSACTION;

-- 2. 후보 게시글 행만 잠근다(진행 중인 댓글 쓰기를 기다린다)
SELECT STRAIGHT_JOIN COUNT(*) AS locked_posts
FROM recount_126_targets t JOIN posts p ON p.id = t.id
FOR UPDATE;

-- 3. 잠금 뒤의 새 문장이라 새 스냅샷으로 정확히 센다
UPDATE posts p
SET p.comment_count = (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id AND c.deleted = 0)
WHERE p.id IN (SELECT t.id FROM recount_126_targets t)
  AND p.deleted = 0
  AND p.comment_count <> (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id AND c.deleted = 0);

COMMIT;

DROP TEMPORARY TABLE recount_126_targets;
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
