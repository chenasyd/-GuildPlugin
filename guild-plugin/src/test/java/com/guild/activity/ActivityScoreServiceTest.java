package com.guild.activity;

import com.guild.models.GuildMember;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityScoreServiceTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void calculateActivityPts_nullRecordOnline_capsAt100() {
        double pts = ActivityScoreService.calculateActivityPts(null, true);
        assertEquals(15.0, pts);
    }

    @Test
    void calculateActivityPts_fullOnlineDay_scoresHigh() {
        MemberActivityRecord record = baseRecord();
        record.setTodayDate(ActivityTracker.today());
        record.setWeekStartDate(ActivityTracker.weekStart());
        record.setOnlineMinutesToday(80);
        record.setActiveDaysWeek(6);
        record.setLastLoginDate(ActivityTracker.today());
        record.setLastSeen(System.currentTimeMillis());

        double pts = ActivityScoreService.calculateActivityPts(record, true);

        assertEquals(100.0, pts);
    }

    @Test
    void calculateActivityPts_staleWeekResetsActiveDays() {
        MemberActivityRecord record = baseRecord();
        record.setTodayDate(ActivityTracker.today());
        record.setWeekStartDate("2000-01-01");
        record.setActiveDaysWeek(99);
        record.setOnlineMinutesToday(10);

        double pts = ActivityScoreService.calculateActivityPts(record, false);

        assertTrue(pts < 50.0);
    }

    @Test
    void calculateActivityPts_offlineRecently_addsRecencyBonus() {
        MemberActivityRecord record = baseRecord();
        record.setLastSeen(System.currentTimeMillis() - 30 * 60 * 1000L);

        double pts = ActivityScoreService.calculateActivityPts(record, false);

        assertEquals(12.0, pts);
    }

    @Test
    void calculateActivityPts_weightAppliedInHybridTotal() {
        MemberActivityRecord record = baseRecord();
        record.setOnlineMinutesToday(20);

        double activity = ActivityScoreService.calculateActivityPts(record, false);
        double economy = 50.0;
        double weight = 2.0;

        MemberActivityScore score = new MemberActivityScore(
                PLAYER, "Tester", economy, activity, economy + activity * weight, 1, false);

        assertEquals(economy + activity * weight, score.getTotalScore(), 0.001);
    }

    private static MemberActivityRecord baseRecord() {
        return new MemberActivityRecord(1, PLAYER, "Tester");
    }
}
