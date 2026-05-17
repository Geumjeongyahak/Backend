package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleNotFoundException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyScheduleProxyService {

    private final DailyScheduleRepository dailyScheduleRepository;

    public DailySchedule getActiveById(Long dailyScheduleId) {
        return dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> new DailyScheduleNotFoundException(dailyScheduleId));
    }
}
