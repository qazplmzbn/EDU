package com.xyz.question_bank_management_system.modules.course.service.impl;

import com.xyz.question_bank_management_system.modules.course.dto.CoursePathGenerateRequest;
import com.xyz.question_bank_management_system.modules.course.entity.Course;
import com.xyz.question_bank_management_system.modules.course.entity.CourseKnowledge;
import com.xyz.question_bank_management_system.modules.course.mapper.CourseKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.course.mapper.CourseMapper;
import com.xyz.question_bank_management_system.modules.course.mapper.StudentCourseProgressMapper;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPath;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathItem;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathItemMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningBehaviorMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentKnowledgeStateMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentProfileSnapshotMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysRoleMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseLearningPathServiceImplTest {
    @Mock private CourseMapper courseMapper;
    @Mock private CourseKnowledgeMapper courseKnowledgeMapper;
    @Mock private StudentCourseProgressMapper progressMapper;
    @Mock private LearningPathMapper learningPathMapper;
    @Mock private LearningPathItemMapper itemMapper;
    @Mock private QbKnowledgePointMapper knowledgePointMapper;
    @Mock private QbLearningResourceMapper resourceMapper;
    @Mock private QbLearningBehaviorMapper behaviorMapper;
    @Mock private StudentKnowledgeStateMapper knowledgeStateMapper;
    @Mock private StudentProfileSnapshotMapper profileSnapshotMapper;
    @Mock private QbClassMemberMapper classMemberMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SysRoleMapper roleMapper;

    private CourseLearningPathServiceImpl service() {
        return new CourseLearningPathServiceImpl(courseMapper, courseKnowledgeMapper, progressMapper, learningPathMapper,
                itemMapper, knowledgePointMapper, resourceMapper, behaviorMapper, knowledgeStateMapper,
                profileSnapshotMapper, classMemberMapper, userMapper, roleMapper);
    }

    @Test
    void generateCoursePath_obsoletesCurrentPathAndCreatesOrderedItems() {
        Course course = new Course(); course.setId(11L); course.setCourseName("C language"); course.setStatus("active");
        CourseKnowledge first = relation(11L, 101L, 1); CourseKnowledge second = relation(11L, 102L, 2);
        LearningPath previous = new LearningPath(); previous.setVersion(2L);
        CoursePathGenerateRequest request = new CoursePathGenerateRequest(); request.setPlanningDays(20);

        when(courseMapper.listVisibleForStudent(1001L)).thenReturn(List.of(course));
        when(courseMapper.selectByIdForUpdate(11L)).thenReturn(course);
        when(courseKnowledgeMapper.selectByCourseId(11L)).thenReturn(List.of(first, second));
        when(learningPathMapper.selectActiveByUserAndCourse(1001L, 11L)).thenReturn(previous);
        when(profileSnapshotMapper.selectRecent(1001L, 1)).thenReturn(List.of());
        when(knowledgeStateMapper.selectByUserId(1001L)).thenReturn(List.of());
        when(knowledgePointMapper.selectById(101L)).thenReturn(point(101L));
        when(knowledgePointMapper.selectById(102L)).thenReturn(point(102L));
        when(resourceMapper.selectList(null, 101L, 1)).thenReturn(List.of());
        when(resourceMapper.selectList(null, 102L, 1)).thenReturn(List.of());
        when(courseKnowledgeMapper.countByCourseId(11L)).thenReturn(2);
        when(itemMapper.countCompletedCourseKnowledge(anyLong(), anyLong())).thenReturn(0);
        doAnswer(invocation -> { ((LearningPath) invocation.getArgument(0)).setId(88L); return 1; }).when(learningPathMapper).insert(any());

        Long pathId = service().generateCoursePath(11L, request, 1001L);

        assertEquals(88L, pathId);
        ArgumentCaptor<LearningPath> pathCaptor = ArgumentCaptor.forClass(LearningPath.class);
        verify(learningPathMapper).insert(pathCaptor.capture());
        assertEquals(3L, pathCaptor.getValue().getVersion());
        assertEquals("active", pathCaptor.getValue().getStatus());
        ArgumentCaptor<List<LearningPathItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(itemMapper).batchInsert(itemCaptor.capture());
        assertEquals(2, itemCaptor.getValue().size());
        assertEquals(1, itemCaptor.getValue().get(0).getOrderNo());
        assertEquals(2, itemCaptor.getValue().get(1).getOrderNo());
        verify(learningPathMapper).obsoleteActiveByUserAndCourse(1001L, 11L);
        verify(progressMapper).upsert(any());
    }

    @Test
    void completeKnowledgeItem_recordsBehaviorAndRecalculatesProgress() {
        LearningPath path = new LearningPath(); path.setId(88L); path.setUserId(1001L); path.setCourseId(11L); path.setStatus("active");
        LearningPathItem item = new LearningPathItem(); item.setId(301L); item.setKnowledgePointId(101L); item.setItemType("knowledge");
        when(learningPathMapper.selectOwnedById(88L, 1001L)).thenReturn(path);
        when(itemMapper.selectByIdAndPathId(301L, 88L)).thenReturn(item);
        when(itemMapper.complete(88L, 301L)).thenReturn(1);
        when(courseKnowledgeMapper.countByCourseId(11L)).thenReturn(1);
        when(itemMapper.countCompletedCourseKnowledge(88L, 11L)).thenReturn(1);
        when(itemMapper.selectByPathId(88L)).thenReturn(List.of(item));

        var detail = service().completePathItem(88L, 301L, 1001L);

        assertNotNull(detail);
        verify(behaviorMapper).insert(any());
        ArgumentCaptor<com.xyz.question_bank_management_system.modules.course.entity.StudentCourseProgress> progressCaptor = ArgumentCaptor.forClass(com.xyz.question_bank_management_system.modules.course.entity.StudentCourseProgress.class);
        verify(progressMapper).upsert(progressCaptor.capture());
        assertEquals("completed", progressCaptor.getValue().getStatus());
        assertEquals(1, progressCaptor.getValue().getCompletedKnowledgeCount());
    }

    @Test
    void completeAlreadyCompletedItem_isIdempotentAndDoesNotDuplicateBehavior() {
        LearningPath path = new LearningPath(); path.setId(88L); path.setUserId(1001L); path.setCourseId(11L); path.setStatus("active");
        LearningPathItem item = new LearningPathItem(); item.setId(301L); item.setKnowledgePointId(101L); item.setItemType("knowledge");
        when(learningPathMapper.selectOwnedById(88L, 1001L)).thenReturn(path);
        when(itemMapper.selectByIdAndPathId(301L, 88L)).thenReturn(item);
        when(itemMapper.complete(88L, 301L)).thenReturn(0);
        when(itemMapper.selectByPathId(88L)).thenReturn(List.of(item));

        service().completePathItem(88L, 301L, 1001L);

        verify(behaviorMapper, never()).insert(any());
        verify(progressMapper, never()).upsert(any());
    }

    @Test
    void teacherPath_rejectsStudentOutsideTeachersClasses() {
        LearningPath path = new LearningPath(); path.setId(88L); path.setCourseId(11L); path.setUserId(1001L);
        Course course = new Course(); course.setId(11L); course.setTeacherId(7001L);
        when(learningPathMapper.selectById(88L)).thenReturn(path);
        when(courseMapper.selectById(11L)).thenReturn(course);
        when(classMemberMapper.listStudentIdsByTeacherId(7001L)).thenReturn(List.of(1002L));

        assertThrows(BizException.class, () -> service().teacherPath(88L, 7001L, false));
    }

    private CourseKnowledge relation(Long courseId, Long knowledgePointId, int sequenceNo) {
        CourseKnowledge relation = new CourseKnowledge(); relation.setCourseId(courseId); relation.setKnowledgePointId(knowledgePointId); relation.setSequenceNo(sequenceNo); relation.setIsCore(1); return relation;
    }

    private QbKnowledgePoint point(Long id) {
        QbKnowledgePoint point = new QbKnowledgePoint(); point.setId(id); point.setName("kp-" + id); return point;
    }
}
