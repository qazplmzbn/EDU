package com.xyz.question_bank_management_system.modules.org.service;

import com.xyz.question_bank_management_system.modules.org.dto.ClassCreateRequest;
import com.xyz.question_bank_management_system.modules.org.dto.JoinClassRequest;
import com.xyz.question_bank_management_system.modules.org.vo.ClassStudentItemVO;
import com.xyz.question_bank_management_system.modules.org.vo.StudentClassItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherOptionVO;
import com.xyz.question_bank_management_system.modules.org.vo.TeacherClassItemVO;

import java.util.List;

public interface ClassService {

    Long create(ClassCreateRequest request, Long currentUserId, boolean isAdmin);

    void update(Long classId, ClassCreateRequest request, Long currentUserId, boolean isAdmin);

    void delete(Long classId, Long currentUserId, boolean isAdmin);

    List<TeacherClassItemVO> listManageable(Long currentUserId, boolean isAdmin);

    List<ClassStudentItemVO> listStudents(Long classId, Long currentUserId, boolean isAdmin);

    void removeStudent(Long classId, Long studentId, Long currentUserId, boolean isAdmin);

    void joinByCode(JoinClassRequest request, Long studentId);

    List<StudentClassItemVO> listMyClasses(Long studentId);

    List<TeacherOptionVO> listTeacherOptions();
}
