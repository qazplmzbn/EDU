package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.modules.bank.dto.StudentAssistantChatRequest;
import com.xyz.question_bank_management_system.modules.bank.mapper.*;
import com.xyz.question_bank_management_system.modules.dialogue.entity.*;
import com.xyz.question_bank_management_system.modules.dialogue.mapper.DialogueMapper;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class StudentAssistantServiceImplTest {
 @Mock QbAttemptMapper attempts; @Mock QbAttemptQuestionMapper questions; @Mock LlmService llm; @Mock DialogueMapper dialogue;
 @Test void chat_createsSessionAndPersistsBothMessages(){ doAnswer(i->{((DialogueSession)i.getArgument(0)).setId(8L);return 1;}).when(dialogue).insertSession(any()); doAnswer(i->{DialogueMessage m=i.getArgument(0);m.setId(m.getRole().equals("user")?11L:12L);return 1;}).when(dialogue).insertMessage(any());QbLlmCall call=new QbLlmCall();call.setId(31L);call.setResponseText("assistant reply");when(llm.chatCompletion(anyInt(),anyLong(),anyString(),nullable(String.class),anyLong())).thenReturn(call);StudentAssistantChatRequest req=new StudentAssistantChatRequest();req.setMessage("what is pointer");var vo=new StudentAssistantServiceImpl(attempts,questions,llm,dialogue).chat(5L,req);assertEquals(8L,vo.getSessionId());assertEquals(31L,vo.getLlmCallId());verify(dialogue,times(2)).insertMessage(any());verify(dialogue).touch(8L); }
}
