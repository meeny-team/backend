-- 게스트 로그인(둘러보기) 도입에 따라 members.provider CHECK 제약에 GUEST 추가.
-- V1 의 원본 제약은 ('GOOGLE','KAKAO','APPLE') 만 허용했어서 GUEST insert 가 제약 위반으로 실패한다.
alter table members drop constraint if exists members_provider_check;
alter table members add constraint members_provider_check
    check (provider in ('GOOGLE','KAKAO','APPLE','GUEST'));
