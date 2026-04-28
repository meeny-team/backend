package com.meeny.application.crew;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.crew.Crew;
import com.meeny.domain.crew.CrewRepository;
import com.meeny.domain.crew.InviteCode;
import com.meeny.presentation.crew.dto.CreateCrewRequest;
import com.meeny.presentation.crew.dto.CrewResponse;
import com.meeny.presentation.crew.dto.UpdateCrewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewService {

    private static final int MAX_INVITE_CODE_RETRIES = 5;

    private final CrewRepository crewRepository;

    @Transactional
    public CrewResponse create(Long creatorId, CreateCrewRequest request) {
        InviteCode inviteCode = generateUniqueInviteCode();
        Crew crew = Crew.create(request.name(), request.coverImage(), creatorId, inviteCode);
        return CrewResponse.from(crewRepository.save(crew));
    }

    public List<CrewResponse> getMyCrews(Long memberId) {
        return crewRepository.findAllByMemberId(memberId).stream()
                .map(CrewResponse::from)
                .toList();
    }

    public CrewResponse getCrewDetail(Long crewId, Long memberId) {
        Crew crew = findCrew(crewId);
        crew.verifyMember(memberId);
        return CrewResponse.from(crew);
    }

    @Transactional
    public CrewResponse joinByCode(Long memberId, String inviteCode) {
        Crew crew = crewRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));
        crew.join(memberId);
        return CrewResponse.from(crew);
    }

    @Transactional
    public void leave(Long crewId, Long memberId) {
        Crew crew = findCrew(crewId);
        crew.leave(memberId);
    }

    @Transactional
    public CrewResponse update(Long crewId, Long memberId, UpdateCrewRequest request) {
        Crew crew = findCrew(crewId);
        crew.updateBy(memberId, request.name(), request.coverImage());
        return CrewResponse.from(crew);
    }

    private Crew findCrew(Long crewId) {
        return crewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
    }

    private InviteCode generateUniqueInviteCode() {
        for (int i = 0; i < MAX_INVITE_CODE_RETRIES; i++) {
            InviteCode candidate = InviteCode.generate();
            if (!crewRepository.existsByInviteCode(candidate.getValue())) {
                return candidate;
            }
        }
        throw new IllegalStateException("초대 코드 생성에 실패했습니다.");
    }
}
