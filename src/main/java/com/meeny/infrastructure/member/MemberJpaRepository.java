package com.meeny.infrastructure.member;

import com.meeny.domain.member.Member;
import com.meeny.domain.member.MemberRepository;
import com.meeny.domain.member.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long>, MemberRepository {
    Optional<Member> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
