package com.meeny.application.crew;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.crew.InviteCode;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinRepository;
import com.meeny.domain.activity.pin.PlaySettlementResult;
import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayRepository;
import com.meeny.domain.identity.Member;
import com.meeny.domain.identity.MemberRepository;
import com.meeny.infrastructure.aws.S3UrlSigner;
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
    private final PlayRepository playRepository;
    private final PinRepository pinRepository;
    private final MemberRepository memberRepository;
    private final S3UrlSigner imageSigner;

    // 크루 생성: 중복 없는 초대 코드를 발급하고 크루를 저장
    @Transactional
    public CrewResponse create(Long creatorId, CreateCrewRequest request) {
        InviteCode inviteCode = generateUniqueInviteCode();
        Crew crew = Crew.create(request.name(), request.coverImage(), creatorId, inviteCode);
        return toResponse(crewRepository.save(crew));
    }

    // 내가 속한 크루 목록 조회
    public List<CrewResponse> getMyCrews(Long memberId) {
        return crewRepository.findAllByMemberId(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    // 크루 상세 조회: 멤버인지 검증한 뒤 반환
    public CrewResponse getCrewDetail(Long crewId, Long memberId) {
        Crew crew = findCrew(crewId);
        crew.verifyMember(memberId);
        return toResponse(crew);
    }

    // 초대 코드로 크루 가입
    @Transactional
    public CrewResponse joinByCode(Long memberId, String inviteCode) {
        Crew crew = crewRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));
        crew.join(memberId);
        return toResponse(crew);
    }

    // 크루 탈퇴: 크루 안 어떤 Play에서도 미정산 잔액(받을/낼 돈)이 남아있으면 탈퇴 차단
    @Transactional
    public void leave(Long crewId, Long memberId) {
        Crew crew = findCrew(crewId);
        verifyNoOutstandingBalance(crewId, memberId);
        crew.leave(memberId);
    }

    private void verifyNoOutstandingBalance(Long crewId, Long memberId) {
        for (Play play : playRepository.findAllByCrewId(crewId)) {
            List<Pin> pins = pinRepository.findAllByPlayId(play.getId());
            if (PlaySettlementResult.memberBalance(memberId, pins) != 0L) {
                throw new BusinessException(ErrorCode.OUTSTANDING_SETTLEMENT_BALANCE);
            }
        }
    }

    // 크루 정보 수정 (권한 검증은 도메인에서 수행)
    @Transactional
    public CrewResponse update(Long crewId, Long memberId, UpdateCrewRequest request) {
        Crew crew = findCrew(crewId);
        crew.updateBy(memberId, request.name(), request.coverImage());
        return toResponse(crew);
    }

    // 응답 빌드: 크루의 멤버 ID 묶음으로 회원 정보를 한 번에 조회해 닉네임/프로필을 같이 내려준다.
    private CrewResponse toResponse(Crew crew) {
        List<Member> members = memberRepository.findAllById(crew.getMemberIds());
        return CrewResponse.from(crew, members, imageSigner);
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
        throw new BusinessException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }
}
