package com.xyz.question_bank_management_system.modules.course.service.impl;

import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.course.dto.CourseKnowledgeReplaceRequest;
import com.xyz.question_bank_management_system.modules.course.dto.CoursePathGenerateRequest;
import com.xyz.question_bank_management_system.modules.course.dto.CourseUpsertRequest;
import com.xyz.question_bank_management_system.modules.course.entity.Course;
import com.xyz.question_bank_management_system.modules.course.entity.CourseKnowledge;
import com.xyz.question_bank_management_system.modules.course.entity.StudentCourseProgress;
import com.xyz.question_bank_management_system.modules.course.mapper.CourseKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.course.mapper.CourseMapper;
import com.xyz.question_bank_management_system.modules.course.mapper.StudentCourseProgressMapper;
import com.xyz.question_bank_management_system.modules.course.service.CourseLearningPathService;
import com.xyz.question_bank_management_system.modules.course.vo.CourseProgressVO;
import com.xyz.question_bank_management_system.modules.course.vo.CourseStudentProgressVO;
import com.xyz.question_bank_management_system.modules.course.vo.LearningPathDetailVO;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPath;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathItem;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningBehavior;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathItemMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningBehaviorMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSnapshot;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentKnowledgeStateMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentProfileSnapshotMapper;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.modules.user.mapper.SysRoleMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseLearningPathServiceImpl implements CourseLearningPathService {

    private final CourseMapper courseMapper;
    private final CourseKnowledgeMapper courseKnowledgeMapper;
    private final StudentCourseProgressMapper progressMapper;
    private final LearningPathMapper learningPathMapper;
    private final LearningPathItemMapper learningPathItemMapper;
    private final QbKnowledgePointMapper knowledgePointMapper;
    private final QbLearningResourceMapper resourceMapper;
    private final QbLearningBehaviorMapper behaviorMapper;
    private final StudentKnowledgeStateMapper knowledgeStateMapper;
    private final StudentProfileSnapshotMapper profileSnapshotMapper;
    private final QbClassMemberMapper classMemberMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    @Override
    @Transactional
    public Long createCourse(CourseUpsertRequest request, Long operatorId, boolean admin) {
        Course course = new Course();
        course.setCourseCode(normalizeCourseCode(request.getCourseCode()));
        if (course.getCourseCode() != null && courseMapper.selectByCode(course.getCourseCode()) != null) {
            throw BizException.of(ErrorCode.CONFLICT, "课程编码已存在");
        }
        course.setCourseName(requireText(request.getCourseName(), "课程名称不能为空"));
        course.setDescription(trimToNull(request.getDescription()));
        course.setTeacherId(resolveTeacherId(request.getTeacherId(), operatorId, admin));
        course.setStatus(normalizeStatus(request.getStatus(), "draft"));
        courseMapper.insert(course);
        return course.getId();
    }

    @Override
    @Transactional
    public void updateCourse(Long courseId, CourseUpsertRequest request, Long operatorId, boolean admin) {
        Course course = loadManageableCourse(courseId, operatorId, admin);
        String courseCode = normalizeCourseCode(request.getCourseCode());
        if (courseCode != null) {
            Course duplicate = courseMapper.selectByCodeExcludeId(courseCode, courseId);
            if (duplicate != null) {
                throw BizException.of(ErrorCode.CONFLICT, "课程编码已存在");
            }
        }
        course.setCourseCode(courseCode);
        course.setCourseName(requireText(request.getCourseName(), "课程名称不能为空"));
        course.setDescription(trimToNull(request.getDescription()));
        course.setTeacherId(resolveTeacherId(request.getTeacherId(), course.getTeacherId(), admin));
        course.setStatus(normalizeStatus(request.getStatus(), course.getStatus()));
        courseMapper.update(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long courseId, Long operatorId, boolean admin) {
        loadManageableCourse(courseId, operatorId, admin);
        courseMapper.softDelete(courseId);
    }

    @Override
    public List<Course> manageableCourses(Long operatorId, boolean admin) {
        return admin ? courseMapper.listAll() : courseMapper.listByTeacher(operatorId);
    }

    @Override
    public List<CourseKnowledge> courseKnowledge(Long courseId, Long operatorId, boolean admin) {
        loadManageableCourse(courseId, operatorId, admin);
        return courseKnowledgeMapper.selectByCourseId(courseId);
    }

    @Override
    @Transactional
    public void replaceCourseKnowledge(Long courseId, CourseKnowledgeReplaceRequest request, Long operatorId, boolean admin) {
        loadManageableCourse(courseId, operatorId, admin);
        Set<Long> knowledgeIds = new HashSet<>();
        Set<Integer> sequenceNumbers = new HashSet<>();
        List<CourseKnowledge> rows = new ArrayList<>();
        for (CourseKnowledgeReplaceRequest.Item item : request.getItems()) {
            if (item.getSequenceNo() < 0 || !knowledgeIds.add(item.getKnowledgePointId()) || !sequenceNumbers.add(item.getSequenceNo())) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "课程知识点或顺序重复");
            }
            if (knowledgePointMapper.selectById(item.getKnowledgePointId()) == null) {
                throw BizException.of(ErrorCode.NOT_FOUND, "知识点不存在");
            }
            CourseKnowledge row = new CourseKnowledge();
            row.setCourseId(courseId);
            row.setKnowledgePointId(item.getKnowledgePointId());
            row.setSequenceNo(item.getSequenceNo());
            row.setIsCore(Boolean.TRUE.equals(item.getCore()) ? 1 : 0);
            BigDecimal weight = item.getCoverageWeight() == null ? BigDecimal.ONE : item.getCoverageWeight();
            if (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.ONE) > 0) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "课程知识点覆盖权重必须在 0 到 1 之间");
            }
            row.setCoverageWeight(weight);
            rows.add(row);
        }
        courseKnowledgeMapper.deleteByCourseId(courseId);
        courseKnowledgeMapper.batchInsert(rows);
    }

    @Override
    public List<CourseProgressVO> visibleCourses(Long studentId) {
        List<CourseProgressVO> result = new ArrayList<>();
        for (Course course : courseMapper.listVisibleForStudent(studentId)) {
            result.add(toCourseProgress(course, progressMapper.selectByUserAndCourse(studentId, course.getId())));
        }
        return result;
    }

    @Override
    public CourseProgressVO courseProgress(Long courseId, Long studentId) {
        Course course = loadVisibleCourse(courseId, studentId);
        return toCourseProgress(course, progressMapper.selectByUserAndCourse(studentId, courseId));
    }

    @Override
    @Transactional
    public Long generateCoursePath(Long courseId, CoursePathGenerateRequest request, Long studentId) {
        Course course = loadVisibleCourse(courseId, studentId);
        Course lockedCourse = courseMapper.selectByIdForUpdate(courseId);
        if (lockedCourse == null || !"active".equals(lockedCourse.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "课程当前不可用于生成学习路径");
        }
        course = lockedCourse;
        List<CourseKnowledge> courseKnowledge = courseKnowledgeMapper.selectByCourseId(courseId);
        if (courseKnowledge.isEmpty()) {
            throw BizException.of(ErrorCode.CONFLICT, "课程尚未配置知识点，不能生成学习路径");
        }
        LearningPath active = learningPathMapper.selectActiveByUserAndCourse(studentId, courseId);
        long version = active == null || active.getVersion() == null ? 1L : active.getVersion() + 1L;
        learningPathMapper.obsoleteActiveByUserAndCourse(studentId, courseId);

        LearningPath path = new LearningPath();
        path.setUserId(studentId);
        path.setCourseId(courseId);
        path.setTitle(course.getCourseName() + " 学习路径");
        path.setStage(trimToNull(request == null ? null : request.getStage()));
        path.setPlanningDays(normalizePlanningDays(request == null ? null : request.getPlanningDays()));
        path.setVersion(version);
        path.setStatus("active");
        path.setSummaryText("基于课程知识点顺序生成；完成知识点节点后更新课程进度。");
        List<StudentProfileSnapshot> snapshots = profileSnapshotMapper.selectRecent(studentId, 1);
        if (!snapshots.isEmpty()) {
            path.setProfileSnapshotId(snapshots.get(0).getId());
        }
        learningPathMapper.insert(path);
        learningPathItemMapper.batchInsert(buildPathItems(path, courseKnowledge, studentId));
        refreshProgress(path, LocalDateTime.now());
        return path.getId();
    }

    @Override
    public List<LearningPathDetailVO> studentPaths(Long studentId) {
        List<LearningPathDetailVO> result = new ArrayList<>();
        for (LearningPath path : learningPathMapper.listByUserId(studentId)) {
            result.add(toDetail(path));
        }
        return result;
    }

    @Override
    public LearningPathDetailVO studentPath(Long pathId, Long studentId) {
        return toDetail(loadOwnedPath(pathId, studentId));
    }

    @Override
    @Transactional
    public LearningPathDetailVO completePathItem(Long pathId, Long itemId, Long studentId) {
        LearningPath path = loadOwnedPath(pathId, studentId);
        if (!"active".equals(path.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "只有活动学习路径可以完成节点");
        }
        LearningPathItem item = learningPathItemMapper.selectByIdAndPathId(itemId, pathId);
        if (item == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "学习路径节点不存在");
        }
        if (learningPathItemMapper.complete(pathId, itemId) > 0) {
            QbLearningBehavior behavior = new QbLearningBehavior();
            behavior.setUserId(studentId);
            behavior.setBehaviorType("complete");
            behavior.setRefType("path_item");
            behavior.setRefId(itemId);
            behavior.setKnowledgePointId(item.getKnowledgePointId());
            behavior.setEventValue("1");
            behavior.setNote("学习路径节点完成");
            behaviorMapper.insert(behavior);
            refreshProgress(path, LocalDateTime.now());
        }
        return toDetail(path);
    }

    @Override
    public List<CourseStudentProgressVO> courseStudentProgress(Long courseId, Long operatorId, boolean admin) {
        loadManageableCourse(courseId, operatorId, admin);
        return progressMapper.listByCourseId(courseId);
    }

    @Override
    public LearningPathDetailVO teacherPath(Long pathId, Long operatorId, boolean admin) {
        LearningPath path = learningPathMapper.selectById(pathId);
        if (path == null || path.getCourseId() == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "学习路径不存在或为只读历史路径");
        }
        Course course = loadManageableCourse(path.getCourseId(), operatorId, admin);
        if (!admin && !classMemberMapper.listStudentIdsByTeacherId(course.getTeacherId()).contains(path.getUserId())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "学生不属于当前教师的班级");
        }
        return toDetail(path);
    }

    private List<LearningPathItem> buildPathItems(LearningPath path, List<CourseKnowledge> rows, Long studentId) {
        Map<Long, StudentKnowledgeState> states = new HashMap<>();
        for (StudentKnowledgeState state : knowledgeStateMapper.selectByUserId(studentId)) {
            states.put(state.getKnowledgePointId(), state);
        }
        LocalDateTime start = LocalDateTime.now();
        int knowledgeCount = Math.max(rows.size(), 1);
        List<LearningPathItem> items = new ArrayList<>();
        int order = 1;
        for (int index = 0; index < rows.size(); index++) {
            CourseKnowledge relation = rows.get(index);
            QbKnowledgePoint point = knowledgePointMapper.selectById(relation.getKnowledgePointId());
            if (point == null) {
                continue;
            }
            LocalDateTime itemStart = start.plusDays((long) index * path.getPlanningDays() / knowledgeCount);
            LocalDateTime itemEnd = start.plusDays((long) (index + 1) * path.getPlanningDays() / knowledgeCount);
            LearningPathItem knowledgeItem = new LearningPathItem();
            knowledgeItem.setPathId(path.getId());
            knowledgeItem.setOrderNo(order++);
            knowledgeItem.setItemType("knowledge");
            knowledgeItem.setKnowledgePointId(point.getId());
            knowledgeItem.setPlannedStartAt(itemStart);
            knowledgeItem.setPlannedEndAt(itemEnd);
            knowledgeItem.setStatus("pending");
            knowledgeItem.setDecisionReason(buildKnowledgeReason(relation, states.get(point.getId())));
            items.add(knowledgeItem);

            List<QbLearningResource> resources = resourceMapper.selectList(null, point.getId(), 1);
            if (!resources.isEmpty()) {
                LearningPathItem resourceItem = new LearningPathItem();
                resourceItem.setPathId(path.getId());
                resourceItem.setOrderNo(order++);
                resourceItem.setItemType("resource");
                resourceItem.setKnowledgePointId(point.getId());
                resourceItem.setResourceId(resources.get(0).getId());
                resourceItem.setPlannedStartAt(itemStart);
                resourceItem.setPlannedEndAt(itemEnd);
                resourceItem.setStatus("pending");
                resourceItem.setDecisionReason("配套学习资源：" + resources.get(0).getTitle());
                items.add(resourceItem);
            }
        }
        return items;
    }

    private String buildKnowledgeReason(CourseKnowledge relation, StudentKnowledgeState state) {
        String core = relation.getIsCore() != null && relation.getIsCore() == 1 ? "核心知识点" : "课程知识点";
        if (state == null || state.getMasteryValue() == null) {
            return core + "，暂无掌握度记录，按课程顺序学习。";
        }
        return core + "，当前掌握度 " + state.getMasteryValue().setScale(2, RoundingMode.HALF_UP) + "，按课程顺序安排。";
    }

    private void refreshProgress(LearningPath path, LocalDateTime learnedAt) {
        if (path.getCourseId() == null) {
            return;
        }
        int total = courseKnowledgeMapper.countByCourseId(path.getCourseId());
        int completed = total == 0 ? 0 : learningPathItemMapper.countCompletedCourseKnowledge(path.getId(), path.getCourseId());
        StudentCourseProgress progress = new StudentCourseProgress();
        progress.setUserId(path.getUserId());
        progress.setCourseId(path.getCourseId());
        progress.setTotalKnowledgeCount(total);
        progress.setCompletedKnowledgeCount(Math.min(completed, total));
        progress.setProgressRate(total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP));
        progress.setStatus(total > 0 && completed >= total ? "completed" : "in_progress");
        progress.setLastLearningAt(learnedAt);
        progressMapper.upsert(progress);
    }

    private LearningPathDetailVO toDetail(LearningPath path) {
        LearningPathDetailVO detail = new LearningPathDetailVO();
        detail.setPath(path);
        detail.setItems(learningPathItemMapper.selectByPathId(path.getId()));
        return detail;
    }

    private CourseProgressVO toCourseProgress(Course course, StudentCourseProgress progress) {
        CourseProgressVO result = new CourseProgressVO();
        result.setCourse(course);
        result.setProgress(progress);
        return result;
    }

    private Course loadManageableCourse(Long courseId, Long operatorId, boolean admin) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "课程不存在");
        }
        if (!admin && !Objects.equals(course.getTeacherId(), operatorId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权管理该课程");
        }
        return course;
    }

    private Course loadVisibleCourse(Long courseId, Long studentId) {
        for (Course course : courseMapper.listVisibleForStudent(studentId)) {
            if (Objects.equals(course.getId(), courseId)) {
                return course;
            }
        }
        throw BizException.of(ErrorCode.FORBIDDEN, "当前学生无权访问该课程");
    }

    private LearningPath loadOwnedPath(Long pathId, Long userId) {
        LearningPath path = learningPathMapper.selectOwnedById(pathId, userId);
        if (path == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "学习路径不存在");
        }
        return path;
    }

    private Long resolveTeacherId(Long requestedTeacherId, Long fallbackTeacherId, boolean admin) {
        Long teacherId = admin && requestedTeacherId != null ? requestedTeacherId : fallbackTeacherId;
        SysUser teacher = userMapper.selectById(teacherId);
        if (teacher == null || !"TEACHER".equalsIgnoreCase(roleMapper.selectRoleCodeByUserId(teacherId))) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "课程负责人必须是有效教师");
        }
        return teacherId;
    }

    private String normalizeStatus(String value, String fallback) {
        String status = trimToNull(value);
        if (status == null) {
            return fallback;
        }
        status = status.toLowerCase(Locale.ROOT);
        if (!Set.of("draft", "active", "archived").contains(status)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "课程状态必须是 draft、active 或 archived");
        }
        return status;
    }

    private String normalizeCourseCode(String value) {
        String code = trimToNull(value);
        return code == null ? null : code.toUpperCase(Locale.ROOT);
    }

    private int normalizePlanningDays(Integer days) {
        if (days == null) {
            return 14;
        }
        if (days < 1 || days > 365) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "规划天数必须在 1 到 365 之间");
        }
        return days;
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
