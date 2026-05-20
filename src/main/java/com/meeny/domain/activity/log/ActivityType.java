package com.meeny.domain.activity.log;

public enum ActivityType {
    CREW_JOINED,        // 새 멤버가 초대 코드로 참여
    MEMBER_LEFT,        // 멤버가 자발적으로 탈퇴
    PLAY_CREATED,       // 새 플레이 생성
    PLAY_SETTLED,       // 플레이 정산 마감
    PIN_ADDED,          // 새 핀 추가
    PIN_UPDATED,        // 핀 수정
    PIN_DELETED,        // 핀 삭제
    TRANSFER_SENT,      // 송신자가 "보냈음" 표시
    TRANSFER_RECEIVED,  // 수신자가 "받았음" 확인
}
