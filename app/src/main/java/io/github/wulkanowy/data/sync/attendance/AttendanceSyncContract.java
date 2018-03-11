package io.github.wulkanowy.data.sync.attendance;

import java.io.IOException;
import java.text.ParseException;

import io.github.wulkanowy.api.VulcanException;

public interface AttendanceSyncContract {

    void syncAttendance(String date) throws IOException, ParseException, VulcanException;

    void syncAttendance() throws IOException, ParseException, VulcanException;
}
