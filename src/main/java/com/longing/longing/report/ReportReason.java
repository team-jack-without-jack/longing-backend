package com.longing.longing.report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    WRONG_INFORMATION("wrong_information"),
    ADVERTISEMENT("advertisement"),
    HARASSMENT("harassment"),
    THREATENING_VIOLENCE("threatening_violence"),
    OTHERS("others");

    private final String key;
//    private final String title;
}
