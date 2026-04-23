package com.meeny.member.domain;

import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findById(Long id);
    Optional<Member> findByProviderAndProviderId(SocialProvider provider, String providerId);
    Member save(Member member);
}
