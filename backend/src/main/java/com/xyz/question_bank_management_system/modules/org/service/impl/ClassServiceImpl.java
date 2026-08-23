package com.xyz.question_bank_management_system.modules.org.service.impl;

import com.xyz.question_bank_management_system.modules.org.dto.ClassCreateRequest;
import com.xyz.question_bank_management_system.modules.org.dto.JoinClassRequest;
import com.xyz.question_bank_management_system.modules.org.entity.QbClass;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysRoleMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.modules.org.service.ClassService;
import com.xyz.question_bank_management_system.modules.org.vo.ClassStudentItemVO;
import com.xyz.question_bank_management_system.modules.org.vo.StudentClassItemVO;
import com.xyz.question_bank_management_system.modules.org.vo.TeacherClassItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final QbClassMapper classMapper;
    private final QbClassMemberMapper classMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public Long create(ClassCreateRequest request, Long currentUserId, boolean isAdmin) {
        QbClass qbClass = new QbClass();
        qbClass.setClassName(requireClassName(request.getClassName()));
        qbClass.setClassDesc(trimToNull(request.getClassDesc()));
        qbClass.setTeacherId(resolveTeacherId(request, currentUserId, isAdmin));
        qbClass.setClassCode(resolveCreateClassCode(request.getClassCode()));
        classMapper.insert(qbClass);
        return qbClass.getId();
    }

    @Override
    @Transactional
    public void update(Long classId, ClassCreateRequest request, Long currentUserId, boolean isAdmin) {
        QbClass qbClass = loadClassForManage(classId, currentUserId, isAdmin);
        qbClass.setClassName(requireClassName(request.getClassName()));
        qbClass.setClassDesc(trimToNull(request.getClassDesc()));
        qbClass.setTeacherId(resolveTeacherId(request, qbClass.getTeacherId(), isAdmin));

        String classCode = normalizeClassCode(request.getClassCode());
        if (classCode != null) {
            QbClass duplicate = classMapper.selectByClassCodeExcludeId(classCode, qbClass.getId());
            if (duplicate != null) {
                throw BizException.of(ErrorCode.CONFLICT, "班级邀请码已存在");
            }
            qbClass.setClassCode(classCode);
        }

        classMapper.update(qbClass);
    }

    @Override
    @Transactional
    public void delete(Long classId, Long currentUserId, boolean isAdmin) {
        loadClassForManage(classId, currentUserId, isAdmin);
        classMapper.softDelete(classId);
    }

    @Override
    public List<TeacherClassItemVO> listManageable(Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return classMapper.listAll();
        }
        return classMapper.listByTeacher(currentUserId);
    }

    @Override
    public List<ClassStudentItemVO> listStudents(Long classId, Long currentUserId, boolean isAdmin) {
        loadClassForManage(classId, currentUserId, isAdmin);
        return classMemberMapper.listStudentsByClassId(classId);
    }

    @Override
    @Transactional
    public void removeStudent(Long classId, Long studentId, Long currentUserId, boolean isAdmin) {
        if (studentId == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "学生编号不能为空");
        }
        loadClassForManage(classId, currentUserId, isAdmin);
        classMemberMapper.removeByClassAndStudent(classId, studentId);
    }

    @Override
    @Transactional
    public void joinByCode(JoinClassRequest request, Long studentId) {
        String classCode = normalizeClassCode(request.getClassCode());
        if (classCode == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "班级邀请码不能为空");
        }

        QbClass qbClass = classMapper.selectByClassCode(classCode);
        if (qbClass == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "班级不存在");
        }

        long joined = classMemberMapper.countByClassAndStudent(qbClass.getId(), studentId);
        if (joined > 0) {
            return;
        }
        classMemberMapper.insert(qbClass.getId(), studentId);
    }

    @Override
    public List<StudentClassItemVO> listMyClasses(Long studentId) {
        return classMapper.listByStudent(studentId);
    }

    @Override
    public List<TeacherOptionVO> listTeacherOptions() {
        return sysUserMapper.listTeacherOptions();
    }

    private Long resolveTeacherId(ClassCreateRequest request, Long currentTeacherId, boolean isAdmin) {
        if (!isAdmin) {
            return currentTeacherId;
        }
        Long teacherId = request.getTeacherId();
        if (teacherId == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "教师编号不能为空");
        }
        SysUser teacher = sysUserMapper.selectById(teacherId);
        if (teacher == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "教师不存在");
        }
        String roleCode = sysRoleMapper.selectRoleCodeByUserId(teacherId);
        if (!"TEACHER".equalsIgnoreCase(roleCode)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "教师编号必须对应教师账号");
        }
        return teacherId;
    }

    private String requireClassName(String className) {
        String normalized = trimToNull(className);
        if (normalized == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "班级名称不能为空");
        }
        return normalized;
    }

    private String resolveCreateClassCode(String classCode) {
        String normalized = normalizeClassCode(classCode);
        if (normalized == null) {
            return generateUniqueClassCode();
        }
        if (classMapper.selectByClassCode(normalized) != null) {
            throw BizException.of(ErrorCode.CONFLICT, "班级邀请码已存在");
        }
        return normalized;
    }

    private String generateUniqueClassCode() {
        for (int i = 0; i < 10; i++) {
            String classCode = randomClassCode();
            if (classMapper.selectByClassCode(classCode) == null) {
                return classCode;
            }
        }
        throw BizException.of(ErrorCode.CONFLICT, "生成唯一班级邀请码失败，请稍后重试");
    }

    private String randomClassCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int idx = secureRandom.nextInt(CODE_CHARS.length());
            sb.append(CODE_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private String normalizeClassCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private QbClass loadClassForManage(Long classId, Long currentUserId, boolean isAdmin) {
        QbClass qbClass = classMapper.selectById(classId);
        if (qbClass == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "班级不存在");
        }
        if (!isAdmin && !Objects.equals(qbClass.getTeacherId(), currentUserId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权管理该班级");
        }
        return qbClass;
    }
}

