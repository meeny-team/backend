-- 플레이 지도 뷰 지원을 위해 pins 에 위/경도 좌표 추가.
-- 기존 핀들은 좌표가 없어 지도 마커에서 제외되며, 신규/편집 시 클라이언트가 채운다.
alter table pins add column latitude double precision;
alter table pins add column longitude double precision;
