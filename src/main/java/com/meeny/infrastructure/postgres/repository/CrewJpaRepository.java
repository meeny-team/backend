package com.meeny.infrastructure.postgres.repository;

import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrewJpaRepository extends JpaRepository<Crew, Long>, CrewRepository {

    @Query("select c from Crew c where c.inviteCode.value = :code")
    Optional<Crew> findByInviteCode(String code);

    @Query("select c from Crew c join c.memberIds m where m = :memberId order by c.createdAt desc")
    List<Crew> findAllByMemberId(Long memberId);

    @Query("select case when count(c) > 0 then true else false end from Crew c where c.inviteCode.value = :code")
    boolean existsByInviteCode(String code);
}
