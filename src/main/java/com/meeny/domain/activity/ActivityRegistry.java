package com.meeny.domain.activity;

import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.pin.PinRepository;
import com.meeny.domain.activity.play.PlayRepository;

public interface ActivityRegistry {
    CrewRepository crewRepository();
    PlayRepository playRepository();
    PinRepository pinRepository();
}
