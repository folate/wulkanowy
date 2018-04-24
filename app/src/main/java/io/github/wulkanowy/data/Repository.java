package io.github.wulkanowy.data;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.github.wulkanowy.api.VulcanException;
import io.github.wulkanowy.data.db.dao.entities.Account;
import io.github.wulkanowy.data.db.dao.entities.AttendanceLesson;
import io.github.wulkanowy.data.db.dao.entities.DaoSession;
import io.github.wulkanowy.data.db.dao.entities.DiaryDao;
import io.github.wulkanowy.data.db.dao.entities.Grade;
import io.github.wulkanowy.data.db.dao.entities.GradeDao;
import io.github.wulkanowy.data.db.dao.entities.SemesterDao;
import io.github.wulkanowy.data.db.dao.entities.StudentDao;
import io.github.wulkanowy.data.db.dao.entities.Subject;
import io.github.wulkanowy.data.db.dao.entities.SymbolDao;
import io.github.wulkanowy.data.db.dao.entities.Week;
import io.github.wulkanowy.data.db.dao.entities.WeekDao;
import io.github.wulkanowy.data.db.resources.ResourcesContract;
import io.github.wulkanowy.data.db.shared.SharedPrefContract;
import io.github.wulkanowy.data.sync.SyncContract;
import io.github.wulkanowy.data.sync.account.AccountSyncContract;
import io.github.wulkanowy.data.sync.attendance.AttendanceSyncContract;
import io.github.wulkanowy.data.sync.timetable.TimetableSyncContract;
import io.github.wulkanowy.di.annotations.SyncGrades;
import io.github.wulkanowy.di.annotations.SyncSubjects;
import io.github.wulkanowy.utils.security.CryptoException;

@Singleton
public class Repository implements RepositoryContract {

    private final SharedPrefContract sharedPref;

    private final ResourcesContract resources;

    private final DaoSession daoSession;

    private final AccountSyncContract accountSync;

    private final AttendanceSyncContract attendanceSync;

    private final TimetableSyncContract timetableSync;

    private final SyncContract gradeSync;

    private final SyncContract subjectSync;

    @Inject
    Repository(SharedPrefContract sharedPref,
               ResourcesContract resources,
               DaoSession daoSession,
               AccountSyncContract accountSync,
               AttendanceSyncContract attendanceSync,
               TimetableSyncContract timetableSync,
               @SyncGrades SyncContract gradeSync,
               @SyncSubjects SyncContract subjectSync) {
        this.sharedPref = sharedPref;
        this.resources = resources;
        this.daoSession = daoSession;
        this.accountSync = accountSync;
        this.attendanceSync = attendanceSync;
        this.timetableSync = timetableSync;
        this.gradeSync = gradeSync;
        this.subjectSync = subjectSync;
    }

    @Override
    public long getCurrentUserId() {
        return sharedPref.getCurrentUserId();
    }

    @Override
    public void setTimetableWidgetState(boolean nextDay) {
        sharedPref.setTimetableWidgetState(nextDay);
    }

    @Override
    public boolean getTimetableWidgetState() {
        return sharedPref.getTimetableWidgetState();
    }

    @Override
    public int getStartupTab() {
        return sharedPref.getStartupTab();
    }

    @Override
    public int getServicesInterval() {
        return sharedPref.getServicesInterval();
    }

    @Override
    public boolean isServicesEnable() {
        return sharedPref.isServicesEnable();
    }

    @Override
    public boolean isNotifyEnable() {
        return sharedPref.isNotifyEnable();
    }

    @Override
    public boolean isMobileDisable() {
        return sharedPref.isMobileDisable();
    }

    @Override
    public String[] getSymbolsKeysArray() {
        return resources.getSymbolsKeysArray();
    }

    @Override
    public String[] getSymbolsValuesArray() {
        return resources.getSymbolsValuesArray();
    }

    @Override
    public String getErrorLoginMessage(Exception e) {
        return resources.getErrorLoginMessage(e);
    }

    @Override
    public String getAttendanceLessonDescription(AttendanceLesson lesson) {
        return resources.getAttendanceLessonDescription(lesson);
    }

    @Override
    public void registerUser(String email, String password, String symbol) throws VulcanException,
            IOException, CryptoException {
        accountSync.registerUser(email, password, symbol);
    }

    @Override
    public void initLastUser() throws VulcanException, IOException, CryptoException {
        accountSync.initLastUser();
    }

    @Override
    public void syncGrades() throws VulcanException, IOException, ParseException {
        gradeSync.sync(getCurrentSemesterId());
    }

    @Override
    public void syncSubjects() throws VulcanException, IOException, ParseException {
        subjectSync.sync(getCurrentSemesterId());
    }

    @Override
    public void syncAttendance() throws ParseException, IOException, VulcanException {
        attendanceSync.syncAttendance(getCurrentDiaryId());
    }

    @Override
    public void syncAttendance(String date) throws ParseException, IOException, VulcanException {
        attendanceSync.syncAttendance(getCurrentDiaryId(), date);
    }

    @Override
    public void syncTimetable() throws VulcanException, IOException, ParseException {
        timetableSync.syncTimetable(getCurrentDiaryId());
    }

    @Override
    public void syncTimetable(String date) throws VulcanException, IOException, ParseException {
        timetableSync.syncTimetable(getCurrentDiaryId(), date);
    }

    @Override
    public void syncAll() throws VulcanException, IOException, ParseException {
        syncSubjects();
        syncGrades();
        syncAttendance();
        syncTimetable();
    }

    @Override
    public Account getCurrentUser() {
        return daoSession.getAccountDao().load(sharedPref.getCurrentUserId());
    }

    @Override
    public Week getWeek(String date) {
        return daoSession.getWeekDao().queryBuilder().where(
                WeekDao.Properties.StartDayDate.eq(date),
                WeekDao.Properties.DiaryId.eq(getCurrentDiaryId())
        ).unique();
    }

    public List<Subject> getSubjectList() {
        return daoSession.getSemesterDao().load(getCurrentSemesterId()).getSubjectList();
    }

    @Override
    public List<Grade> getNewGrades() {
        return daoSession.getGradeDao().queryBuilder().where(
                GradeDao.Properties.IsNew.eq(1),
                GradeDao.Properties.SemesterId.eq(getCurrentSemesterId())
        ).list();
    }

    @Override
    public long getCurrentSymbolId() {
        return daoSession.getSymbolDao().queryBuilder().where(
                SymbolDao.Properties.UserId.eq(getCurrentUserId())
        ).unique().getId();
    }

    @Override
    public long getCurrentStudentId() {
        return daoSession.getStudentDao().queryBuilder().where(
                StudentDao.Properties.SymbolId.eq(getCurrentSymbolId()),
                StudentDao.Properties.Current.eq(true)
        ).unique().getId();
    }

    @Override
    public long getCurrentDiaryId() {
        return daoSession.getDiaryDao().queryBuilder().where(
                DiaryDao.Properties.StudentId.eq(getCurrentStudentId()),
                DiaryDao.Properties.Current.eq(true)
        ).unique().getId();
    }

    @Override
    public long getCurrentSemesterId() {
        return daoSession.getSemesterDao().queryBuilder().where(
                SemesterDao.Properties.DiaryId.eq(getCurrentDiaryId()),
                SemesterDao.Properties.Current.eq(true)
        ).unique().getId();
    }
}
