package com.meeny.member.infrastructure;

import com.meeny.member.domain.Member;
import com.meeny.member.domain.MemberRepository;
import com.meeny.member.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMemberRepository implements MemberRepository {

    private final MemberJpaRepository jpa;

    @Override
    public Optional<Member> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Member> findByProviderAndProviderId(SocialProvider provider, String providerId) {
        return jpa.findByProviderAndProviderId(provider, providerId);
    }

    @Override
    public Member save(Member member) {
        return jpa.save(member);
    }
}
