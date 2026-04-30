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

    // Play 생성: 크루 멤버 검증 후, 참여자가 모두 크루에 속하는지 확인하고 저장 (생성자는 자동 포함)
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

    // 특정 크루의 Play 목록 조회 (크루 멤버에게만 노출)
    public List<PlayResponse> getPlaysByCrew(Long crewId, Long memberId) {
        Crew crew = findCrew(crewId);
        crew.verifyMember(memberId);
        return playRepository.findAllByCrewId(crewId).stream()
                .map(PlayResponse::from)
                .toList();
    }

    // Play 단건 조회 (크루 멤버에게만 노출)
    public PlayResponse getPlay(Long playId, Long memberId) {
        Play play = findPlay(playId);
        Crew crew = findCrew(play.getCrewId());
        crew.verifyMember(memberId);
        return PlayResponse.from(play);
    }

    // Play 수정: 멤버 변경 시 생성자는 항상 포함되며, 새 멤버가 크루에 속하는지 검증
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

    // Play 삭제: 생성자 본인만 가능
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
