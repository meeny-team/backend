package com.meeny.domain.crew;

import java.util.List;
import java.util.Optional;

public interface CrewRepository {
    Crew save(Crew crew);
    Optional<Crew> findById(Long id);
    Optional<Crew> findByInviteCode(String inviteCode);
    List<Crew> findAllByMemberId(Long memberId);
    boolean existsByInviteCode(String inviteCode);
    void delete(Crew crew);
}
