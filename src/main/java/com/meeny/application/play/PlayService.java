package com.meeny.application.play;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayRepository;
import com.meeny.presentation.play.dto.CreatePlayRequest;
import com.meeny.presentation.play.dto.PlayResponse;
import com.meeny.presentation.play.dto.UpdatePlayRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayService {

    private final PlayRepository playRepository;
    private final CrewRepository crewRepository;

    @Transactional
    public PlayResponse create(Long creatorId, CreatePlayRequest request) {
        Crew crew = findCrew(request.crewId());
        crew.verifyMember(creatorId);

        Set<Long> requestedMembers = request.memberIds() == null ? new HashSet<>() : new HashSet<>(request.memberIds());
        requestedMembers.add(creatorId);
        verifyMembersAreSubsetOf(crew, requestedMembers);

        Play play = Play.create(
                crew.getId(),
                creatorId,
                request.title(),
                request.type(),
                request.dateRange().toDomain(),
                requestedMembers,
                request.regions(),
                request.tags(),
                request.coverImage()
        );
        return PlayResponse.from(playRepository.save(play));
    }

    public List<PlayResponse> getPlaysByCrew(Long crewId, Long memberId) {
        Crew crew = findCrew(crewId);
        crew.verifyMember(memberId);
        return playRepository.findAllByCrewId(crewId).stream()
                .map(PlayResponse::from)
                .toList();
    }

    public PlayResponse getPlay(Long playId, Long memberId) {
        Play play = findPlay(playId);
        Crew crew = findCrew(play.getCrewId());
        crew.verifyMember(memberId);
        return PlayResponse.from(play);
    }

    @Transactional
    public PlayResponse update(Long playId, Long memberId, UpdatePlayRequest request) {
        Play play = findPlay(playId);
        Crew crew = findCrew(play.getCrewId());

        Set<Long> updatedMembers = request.memberIds() == null ? null : new HashSet<>(request.memberIds());
        if (updatedMembers != null) {
            updatedMembers.add(play.getCreatedBy());
            verifyMembersAreSubsetOf(crew, updatedMembers);
        }

        play.updateBy(
                memberId,
                request.title(),
                request.type(),
                request.dateRange() == null ? null : request.dateRange().toDomain(),
                updatedMembers,
                request.regions(),
                request.tags(),
                request.coverImage()
        );
        return PlayResponse.from(play);
    }

    @Transactional
    public void delete(Long playId, Long memberId) {
        Play play = findPlay(playId);
        play.verifyAuthor(memberId);
        playRepository.delete(play);
    }

    private Play findPlay(Long playId) {
        return playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
    }

    private Crew findCrew(Long crewId) {
        return crewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
    }

    private void verifyMembersAreSubsetOf(Crew crew, Set<Long> memberIds) {
        if (!crew.getMemberIds().containsAll(memberIds)) {
            throw new BusinessException(ErrorCode.INVALID_PLAY_MEMBERS);
        }
    }
}
