package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.AppealCreateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.AppealHandleRequest;
import com.xyz.question_bank_management_system.modules.bank.vo.AppealMyItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAppealItemVO;

public interface AppealService {

    Long submitAppeal(AppealCreateRequest request, Long userId);
    //student appeal
    PageResponse<AppealMyItemVO> pageMyAppeals(Long userId, Integer status, long page, long size);
    //appeal pages in teachers' view
    PageResponse<TeacherAppealItemVO> pageTeacherAppeals(Integer status,
                                                         long page,
                                                         long size,
                                                         Long actorId,
                                                         boolean isAdmin);

    void handleAppeal(Long appealId, AppealHandleRequest request, Long handlerId, boolean isAdmin);
}
