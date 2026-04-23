package com.meeny.member.infrastructure;

import com.meeny.member.domain.Member;
import com.meeny.member.domain.MemberRepository;
import com.meeny.member.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long>, MemberRepository {
    Optional<Member> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
