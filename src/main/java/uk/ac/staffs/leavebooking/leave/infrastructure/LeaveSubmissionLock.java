package uk.ac.staffs.leavebooking.leave.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LeaveSubmissionLock {
    private final JdbcTemplate jdbcTemplate;

    public LeaveSubmissionLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire(String staffMemberId) {
        jdbcTemplate.update(
                "MERGE INTO leave_submission_lock (staff_member_id) KEY (staff_member_id) VALUES (?)",
                staffMemberId
        );
    }
}
