package com.meeny.domain.activity.log;

public enum ActivityType {
    CREW_JOINED,                // 새 멤버가 초대 코드로 참여
    MEMBER_LEFT,                // 멤버가 자발적으로 탈퇴
    CREW_OWNERSHIP_TRANSFERRED, // 크루 소유권 양도
    PLAY_CREATED,               // 새 플레이 생성
    PLAY_SETTLED,               // 플레이 정산 마감
    PLAY_FORCE_SETTLED,         // 미수신 송금이 남아있는 상태에서 작성자가 강제 마감
    PIN_ADDED,                  // 새 핀 추가
    PIN_UPDATED,                // 핀 수정
    PIN_DELETED,                // 핀 삭제
    TRANSFER_SENT,              // 송신자가 "보냈음" 표시
    TRANSFER_RECEIVED,          // 수신자가 "받았음" 확인
    TRANSFER_RECEIVE_CANCELED,  // 수신자가 "받음"을 잘못 눌렀음을 되돌림
}
