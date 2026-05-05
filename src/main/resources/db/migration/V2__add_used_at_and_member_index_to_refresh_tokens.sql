-- 토큰 회전(rotation) + 탈취 감지(reuse detection)를 위한 used_at 컬럼.
-- 토큰이 갱신에 한 번 사용되면 시간이 기록되고, 같은 토큰이 또 들어오면 탈취로 간주.
alter table refresh_tokens add column used_at timestamp(6);

-- 탈취 감지 시 해당 회원의 모든 refresh 토큰을 일괄 삭제(deleteByMemberId)하므로
-- member_id 인덱스가 필요. 단일 회원의 토큰 검색에도 도움.
create index idx_refresh_tokens_member_id on refresh_tokens(member_id);
