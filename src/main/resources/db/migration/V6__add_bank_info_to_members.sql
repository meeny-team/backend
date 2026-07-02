-- 정산 결과 화면에서 "받을 사람" 의 계좌를 노출하기 위해 members 에 은행/계좌/예금주 3개 컬럼 추가.
-- 세 컬럼 모두 optional. 미등록 멤버는 계좌 미등록 상태로 표시된다.
alter table members add column bank_code varchar(20);
alter table members add column account_number varchar(30);
alter table members add column account_holder_name varchar(50);
