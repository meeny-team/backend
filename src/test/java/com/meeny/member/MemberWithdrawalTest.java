package com.meeny.member;

import com.meeny.domain.identity.Member;
import com.meeny.domain.identity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Member.withdraw() 는 스토어 컴플라이언스 (Google Play + iOS 5.1.1(v)) 요구사항으로
 * PII 를 파기해야 한다. 이 테스트가 그 invariant 를 보호한다.
 *
 * 정산 히스토리 무결성을 위해 row 자체는 유지하고 필드만 null 로 정리한다.
 */
class MemberWithdrawalTest {

    private Member createMember() {
        Member m = Member.create(SocialProvider.GOOGLE, "google-uid-1", "user@example.com", "홍길동");
        m.updateProfile("홍길동", "https://cdn.example/avatar.png", "안녕하세요");
        m.updateBankInfo("088", "1234567890", "홍길동");
        return m;
    }

    @Nested
    @DisplayName("withdraw()")
    class Withdraw {

        @Test
        @DisplayName("deletedAt 이 세팅되고 isDeleted true")
        void marksAsDeleted() {
            Member m = createMember();

            m.withdraw();

            assertThat(m.isDeleted()).isTrue();
            assertThat(m.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("PII 파기 — email/profileImage/bio/bank 정보 null")
        void purgesPii() {
            Member m = createMember();

            m.withdraw();

            assertThat(m.getEmail()).isNull();
            assertThat(m.getProfileImage()).isNull();
            assertThat(m.getBio()).isNull();
            assertThat(m.getBankCode()).isNull();
            assertThat(m.getAccountNumber()).isNull();
            assertThat(m.getAccountHolderName()).isNull();
            assertThat(m.hasBankAccount()).isFalse();
        }

        @Test
        @DisplayName("nickname 은 sentinel 로 대체 (NOT NULL 제약)")
        void replacesNicknameWithSentinel() {
            Member m = createMember();

            m.withdraw();

            assertThat(m.getNickname()).isEqualTo("(탈퇴한 사용자)");
            assertThat(m.getDisplayNickname()).isEqualTo("(탈퇴한 사용자)");
        }

        @Test
        @DisplayName("provider + providerId 는 유지 → 재활성화 매칭 가능")
        void keepsProviderKeyForReactivation() {
            Member m = createMember();

            m.withdraw();

            assertThat(m.getProvider()).isEqualTo(SocialProvider.GOOGLE);
            assertThat(m.getProviderId()).isEqualTo("google-uid-1");
        }

        @Test
        @DisplayName("이미 탈퇴 상태면 재호출 no-op (deletedAt 원본 유지)")
        void idempotent() throws InterruptedException {
            Member m = createMember();
            m.withdraw();
            var firstDeletedAt = m.getDeletedAt();
            Thread.sleep(5);

            m.withdraw();

            assertThat(m.getDeletedAt()).isEqualTo(firstDeletedAt);
        }
    }

    @Nested
    @DisplayName("reactivate() after withdraw()")
    class Reactivate {

        @Test
        @DisplayName("새 email + nickname 으로 되살아남 — 계좌 정보는 복구 안 됨 (사용자가 재등록)")
        void reactivatesWithFreshData() {
            Member m = createMember();
            m.withdraw();

            m.reactivate("new@example.com", "새닉네임");

            assertThat(m.isDeleted()).isFalse();
            assertThat(m.getEmail()).isEqualTo("new@example.com");
            assertThat(m.getNickname()).isEqualTo("새닉네임");
            // 파기된 필드는 여전히 null — 사용자가 다시 등록해야
            assertThat(m.getProfileImage()).isNull();
            assertThat(m.getBio()).isNull();
            assertThat(m.getBankCode()).isNull();
        }

        @Test
        @DisplayName("nickname 미제공 시 sentinel 이면 '사용자' 로 복구")
        void restoresDefaultNickname_whenSentinelAndNoInput() {
            Member m = createMember();
            m.withdraw();

            m.reactivate("new@example.com", null);

            assertThat(m.getNickname()).isEqualTo("사용자");
        }
    }
}
