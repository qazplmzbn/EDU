package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.modules.competency.mapper.SkillMapper;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentResumeDocument;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentResumeEvidence;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentEvidenceMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentResumeMapper;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.modules.source.entity.FileAsset;
import com.xyz.question_bank_management_system.modules.source.mapper.FileAssetMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeEvidenceServiceImplTest {
    @Mock StudentResumeMapper resumeMapper;
    @Mock SkillMapper skillMapper;
    @Mock FileAssetMapper assetMapper;
    @Mock StudentEvidenceMapper evidenceMapper;
    @Mock StudentProfileService profileService;
    @TempDir Path tempDir;

    @Test void upload_rejectsUnsupportedExtensionBeforeAnyWrite() {
        ResumeEvidenceServiceImpl service = service();
        MockMultipartFile file = new MockMultipartFile("file", "resume.exe", "application/octet-stream", "bad".getBytes(StandardCharsets.UTF_8));
        assertThrows(BizException.class, () -> service.upload(file, "v1", 7L));
        verifyNoInteractions(resumeMapper, assetMapper);
    }

    @Test void analyze_extractsPdfText() throws Exception {
        Path pdf = tempDir.resolve("resume.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
                content.beginText(); content.setFont(PDType1Font.HELVETICA, 12); content.newLineAtOffset(20, 700); content.showText("Skills: Java"); content.endText();
            }
            document.save(pdf.toFile());
        }
        analyzeAndAssertText(pdf, "Skills: Java");
    }

    @Test void analyze_extractsDocxText() throws Exception {
        Path docx = tempDir.resolve("resume.docx");
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(docx))) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<?xml version=\"1.0\"?><w:document xmlns:w=\"urn:w\"><w:body><w:p><w:r><w:t>Skills: Kotlin</w:t></w:r></w:p></w:body></w:document>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        analyzeAndAssertText(docx, "Skills: Kotlin");
    }

    @Test void analyze_doesNotRecreateConfirmedManualUnresolvedCandidate() throws Exception {
        Path text = tempDir.resolve("resume.txt");
        java.nio.file.Files.writeString(text, "Skills: UnknownRehearsalTool", StandardCharsets.UTF_8);
        StudentResumeDocument resume = new StudentResumeDocument(); resume.setId(1L); resume.setUserId(7L); resume.setFileAssetId(2L); resume.setParseStatus("PARSED");
        FileAsset asset = new FileAsset(); asset.setId(2L); asset.setStoragePath(text.toString()); asset.setFileName("resume.txt");
        StudentResumeEvidence resolved = new StudentResumeEvidence(); resolved.setRawName("UnknownRehearsalTool"); resolved.setAppliedStatus("CONFIRMED"); resolved.setMatchStatus("MATCHED_MANUAL");
        when(resumeMapper.document(1L, 7L)).thenReturn(resume);
        when(assetMapper.selectById(2L)).thenReturn(asset);
        when(resumeMapper.evidence(1L)).thenReturn(List.of(resolved));
        when(skillMapper.selectPage(isNull(), eq(0L), eq(1000))).thenReturn(List.of());
        when(resumeMapper.markParsed(eq(1L), eq(7L), anyString(), eq("PARSED"), eq("resume_document_parser_v2"))).thenReturn(1);

        service().analyze(1L, 7L);
        verify(resumeMapper).deleteCandidates(1L);
        verify(resumeMapper, never()).upsertEvidence(any());
    }

    private void analyzeAndAssertText(Path file, String expected) {
        StudentResumeDocument resume = new StudentResumeDocument(); resume.setId(1L); resume.setUserId(7L); resume.setFileAssetId(2L); resume.setParseStatus("UPLOADED");
        FileAsset asset = new FileAsset(); asset.setId(2L); asset.setStoragePath(file.toString()); asset.setFileName(file.getFileName().toString());
        when(resumeMapper.document(1L, 7L)).thenReturn(resume);
        when(assetMapper.selectById(2L)).thenReturn(asset);
        when(skillMapper.selectPage(isNull(), eq(0L), eq(1000))).thenReturn(List.of());
        when(resumeMapper.markParsed(eq(1L), eq(7L), anyString(), eq("PARSED"), eq("resume_document_parser_v2"))).thenReturn(1);
        assertDoesNotThrow(() -> service().analyze(1L, 7L));
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(resumeMapper).markParsed(eq(1L), eq(7L), text.capture(), eq("PARSED"), eq("resume_document_parser_v2"));
        assertTrue(text.getValue().contains(expected));
    }

    private ResumeEvidenceServiceImpl service() {
        ResumeEvidenceServiceImpl service = new ResumeEvidenceServiceImpl(resumeMapper, skillMapper, assetMapper, evidenceMapper, profileService, new ObjectMapper());
        ReflectionTestUtils.setField(service, "storageDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "maxSizeBytes", 1024L * 1024L);
        return service;
    }
}
