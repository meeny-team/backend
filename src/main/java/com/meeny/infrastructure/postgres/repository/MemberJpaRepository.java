package com.meeny.infrastructure.postgres.repository;

import com.meeny.domain.identity.Member;
import com.meeny.domain.identity.MemberRepository;
import com.meeny.domain.identity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long>, MemberRepository {
    Optional<Member> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
