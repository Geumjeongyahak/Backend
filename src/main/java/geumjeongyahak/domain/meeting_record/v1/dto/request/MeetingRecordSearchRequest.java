package geumjeongyahak.domain.meeting_record.v1.dto.request;

import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class MeetingRecordSearchRequest extends BasePaginationRequest {
    private String keyword;
    private Boolean mineOnly;
    private MeetingRecordStatus status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getMineOnly() {
        return mineOnly;
    }

    public void setMineOnly(Boolean mineOnly) {
        this.mineOnly = mineOnly;
    }

    public MeetingRecordStatus getStatus() {
        return status;
    }

    public void setStatus(MeetingRecordStatus status) {
        this.status = status;
    }

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(getPage(), getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
