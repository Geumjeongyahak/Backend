package geumjeongyahak.domain.channel.service;

import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelManagementMode;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.department.entity.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemChannelService {

    private final ChannelRepository channelRepository;

    @Transactional
    public Channel ensureDepartmentChannel(Department department) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.DEPARTMENT, department.getId())
                .orElseGet(() -> createSystemChannel(
                        department.getName(),
                        department.getName() + " 부서 전용 채널",
                        ChannelType.DEPARTMENT,
                        department.getId(),
                        false,
                        ChannelAccessLevel.READ_WRITE
                ));
    }

    @Transactional
    public Channel ensureClassroomChannel(Classroom classroom) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.CLASSROOM, classroom.getId())
                .orElseGet(() -> createSystemChannel(
                        classroom.getName(),
                        classroom.getName() + " 분반 전용 채널",
                        ChannelType.CLASSROOM,
                        classroom.getId(),
                        false,
                        ChannelAccessLevel.READ_WRITE
                ));
    }

    @Transactional
    public Channel ensureNoticeChannel(String name, String description, boolean isDefault) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.NOTICE, null)
                .orElseGet(() -> createSystemChannel(
                        name,
                        description,
                        ChannelType.NOTICE,
                        null,
                        isDefault,
                        ChannelAccessLevel.READ_ONLY
                ));
    }

    @Transactional
    public Channel ensureEventChannel(String name, String description, boolean isDefault) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.NOTICE, null)
                .orElseGet(() -> createSystemChannel(
                        name,
                        description,
                        ChannelType.EVENT,
                        null,
                        isDefault,
                        ChannelAccessLevel.READ_ONLY
                ));
    }

    private Channel createSystemChannel(
            String name,
            String description,
            ChannelType channelType,
            Long refId,
            boolean isDefault,
            ChannelAccessLevel accessLevel
    ) {
        Channel channel = channelRepository.save(Channel.builder()
                .name(name)
                .description(description)
                .channelType(channelType)
                .managementMode(ChannelManagementMode.SYSTEM_MANAGED)
                .refId(refId)
                .accessLevel(accessLevel)
                .isDefault(isDefault)
                .isActive(true)
                .build());

        log.info("시스템 채널 생성 완료 - type: {}, refId: {}, channelId: {}", channelType, refId, channel.getId());
        return channel;
    }
}
