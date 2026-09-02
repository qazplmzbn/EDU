package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.competency.entity.Skill;
import com.xyz.question_bank_management_system.modules.competency.mapper.SkillMapper;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentEvidence;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentResumeDocument;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentResumeEvidence;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentEvidenceMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentResumeMapper;
import com.xyz.question_bank_management_system.modules.profile.service.ResumeEvidenceService;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.modules.source.entity.FileAsset;
import com.xyz.question_bank_management_system.modules.source.mapper.FileAssetMapper;
import com.xyz.question_bank_management_system.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resume handling deliberately separates upload, extraction and profile
 * application. Uploaded text is private file evidence; only user-confirmed
 * matches are copied into the shared student evidence stream.
 */
@Service
@RequiredArgsConstructor
public class ResumeEvidenceServiceImpl implements ResumeEvidenceService {
    private static final String PARSER_VERSION = "resume_document_parser_v2";
    private static final BigDecimal CANDIDATE_CONFIDENCE = new BigDecimal("0.3500");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "pdf", "docx");

    private final StudentResumeMapper mapper;
    private final SkillMapper skillMapper;
    private final FileAssetMapper assetMapper;
    private final StudentEvidenceMapper evidenceMapper;
    private final StudentProfileService studentProfileService;
    private final ObjectMapper objectMapper;

    @Value("${app.file.storage-dir:uploads}")
    private String storageDir;
    @Value("${app.resume.max-size-bytes:10485760}")
    private long maxSizeBytes;

    @Override
    @Transactional
    public StudentResumeDocument upload(MultipartFile file, String consentVersion, Long userId) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "resume file is required");
        }
        String originalName = Objects.toString(file.getOriginalFilename(), "resume");
        String extension = extension(originalName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "resume type must be TXT, PDF, or DOCX");
        }
        if (file.getSize() > maxSizeBytes) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "resume exceeds the configured size limit");
        }
        try {
            byte[] bytes = file.getBytes();
            validateSignature(extension, bytes);
            String hash = HashUtil.sha256(bytes);
            StudentResumeDocument existing = mapper.byHash(userId, hash);
            if (existing != null) {
                return existing;
            }

            Path directory = Paths.get(storageDir).toAbsolutePath().normalize().resolve("student-resumes");
            Files.createDirectories(directory);
            Path target = directory.resolve(hash + ".bin");
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            FileAsset asset = new FileAsset();
            asset.setBizType("student_resume");
            asset.setFileName(originalName);
            asset.setFileExt(extension);
            asset.setMimeType(file.getContentType());
            asset.setStorageType("local");
            asset.setStoragePath(target.toString());
            asset.setFileSize(file.getSize());
            asset.setFileHash(hash);
            asset.setUploadedBy(userId);
            assetMapper.insert(asset);

            StudentResumeDocument document = new StudentResumeDocument();
            document.setUserId(userId);
            document.setFileAssetId(asset.getId());
            document.setFileName(asset.getFileName());
            document.setFileHash(hash);
            document.setParseStatus("UPLOADED");
            document.setParserVersion(PARSER_VERSION);
            document.setConsentVersion(consentVersion);
            mapper.insertDocument(document);
            asset.setBizId(document.getId());
            assetMapper.updateBizId(asset);
            return document;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "resume upload failed: " + shortMessage(ex));
        }
    }

    @Override
    @Transactional
    public StudentResumeDocument analyze(Long resumeId, Long userId) {
        StudentResumeDocument document = requireDocument(resumeId, userId);
        FileAsset asset = document.getFileAssetId() == null ? null : assetMapper.selectById(document.getFileAssetId());
        if (asset == null || asset.getStoragePath() == null || asset.getStoragePath().isBlank()) {
            throw BizException.of(ErrorCode.NOT_FOUND, "resume file asset not found");
        }
        try {
            String text = parse(asset, Files.readAllBytes(Paths.get(asset.getStoragePath()))).trim();
            if (text.isEmpty()) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "resume contains no UTF-8 text available for analysis");
            }
            extract(document, text);
            if (mapper.markParsed(document.getId(), userId, text, "PARSED", PARSER_VERSION) != 1) {
                throw BizException.of(ErrorCode.CONFLICT, "resume changed during analysis");
            }
            return requireDocument(resumeId, userId);
        } catch (BizException ex) {
            mapper.markFailed(document.getId(), userId, shortMessage(ex));
            throw ex;
        } catch (Exception ex) {
            mapper.markFailed(document.getId(), userId, shortMessage(ex));
            throw BizException.of(ErrorCode.BIZ_ERROR, "resume analysis failed: " + shortMessage(ex));
        }
    }

    @Override
    @Transactional
    public int applyToProfile(Long resumeId, Long userId) {
        StudentResumeDocument document = requireDocument(resumeId, userId);
        if (!"PARSED".equals(document.getParseStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "resume must be parsed before it can be applied");
        }
        int applied = 0;
        for (StudentResumeEvidence candidate : mapper.confirmedEvidence(resumeId)) {
            if (candidate.getTargetId() == null || !"SKILL".equals(candidate.getTargetType())) {
                continue;
            }
            StudentEvidence evidence = new StudentEvidence();
            evidence.setUserId(userId);
            evidence.setEvidenceType("RESUME_CONFIRMED");
            evidence.setSourceEntityType("RESUME");
            evidence.setSourceEntityId(resumeId);
            evidence.setTargetType(candidate.getTargetType());
            evidence.setTargetId(candidate.getTargetId());
            evidence.setEvidenceValue(candidate.getEvidenceValue());
            evidence.setEvidenceDirection(0);
            evidence.setConfidence(candidate.getConfidence());
            evidence.setEvidenceText(candidate.getEvidenceText());
            evidence.setOccurredAt(LocalDateTime.now());
            evidence.setExtractVersion(PARSER_VERSION);
            applied += evidenceMapper.insertIgnore(evidence);
        }
        if (applied > 0) studentProfileService.refreshAssessment(userId);
        return applied;
    }

    @Override
    public List<StudentResumeDocument> documents(Long userId) {
        return mapper.documents(userId);
    }

    @Override
    public List<StudentResumeEvidence> evidence(Long resumeId, Long userId) {
        requireDocument(resumeId, userId);
        return mapper.evidence(resumeId);
    }

    @Override
    @Transactional
    public void confirm(Long resumeId, Long evidenceId, boolean accepted, Long targetSkillId, Long userId) {
        StudentResumeDocument document = requireDocument(resumeId, userId);
        if (!"PARSED".equals(document.getParseStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "resume must be parsed before evidence can be confirmed");
        }
        StudentResumeEvidence evidence = mapper.evidence(resumeId).stream()
                .filter(value -> Objects.equals(value.getId(), evidenceId))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.NOT_FOUND, "resume evidence not found"));
        int changed;
        if (accepted && "UNRESOLVED".equals(evidence.getMatchStatus())) {
            Skill target = targetSkillId == null ? null : skillMapper.selectById(targetSkillId);
            if (target == null) throw BizException.of(ErrorCode.PARAM_ERROR, "an unresolved resume candidate requires a valid targetSkillId");
            changed = mapper.confirmUnresolved(evidenceId, resumeId, target.getId(), target.getNameZh());
        } else {
            String status = accepted && ("MATCHED".equals(evidence.getMatchStatus()) || "MATCHED_MANUAL".equals(evidence.getMatchStatus())) ? "CONFIRMED" : "REJECTED";
            changed = mapper.confirm(evidenceId, resumeId, status);
        }
        if (changed != 1) {
            throw BizException.of(ErrorCode.CONFLICT, "resume evidence changed during confirmation");
        }
        if (accepted) {
            applyToProfile(resumeId, userId);
        }
    }

    private StudentResumeDocument requireDocument(Long resumeId, Long userId) {
        StudentResumeDocument document = mapper.document(resumeId, userId);
        if (document == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "resume not found");
        }
        return document;
    }

    private void extract(StudentResumeDocument document, String text) {
        Set<String> manuallyResolvedNames = mapper.evidence(document.getId()).stream()
                .filter(value -> "CONFIRMED".equals(value.getAppliedStatus()) && "MATCHED_MANUAL".equals(value.getMatchStatus()))
                .map(value -> normalize(value.getRawName()))
                .collect(java.util.stream.Collectors.toSet());
        mapper.deleteCandidates(document.getId());
        String normalized = text.toLowerCase(Locale.ROOT);
        Set<String> matchedNames = new LinkedHashSet<>();
        for (Skill skill : skillMapper.selectPage(null, 0, 1000)) {
            String name = skill.getNameZh();
            if (name == null || name.isBlank() || !normalized.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            matchedNames.add(normalize(name));
            StudentResumeEvidence evidence = new StudentResumeEvidence();
            evidence.setResumeId(document.getId());
            evidence.setTargetType("SKILL");
            evidence.setTargetId(skill.getId());
            evidence.setRawName(name);
            evidence.setNormalizedName(name);
            evidence.setEvidenceValue(BigDecimal.ONE);
            evidence.setConfidence(CANDIDATE_CONFIDENCE);
            evidence.setEvidenceText(sentence(text, name));
            evidence.setMatchStatus("MATCHED");
            evidence.setModelVersion(PARSER_VERSION);
            evidence.setSourceSpanJson(json(Map.of("matchedText", name)));
            evidence.setAppliedStatus("CANDIDATE");
            mapper.upsertEvidence(evidence);
        }
        for (String raw : declaredSkillCandidates(text)) {
            if (matchedNames.contains(normalize(raw)) || manuallyResolvedNames.contains(normalize(raw))) continue;
            StudentResumeEvidence evidence = new StudentResumeEvidence();
            evidence.setResumeId(document.getId());
            evidence.setTargetType("SKILL");
            evidence.setTargetId(null);
            evidence.setRawName(raw);
            evidence.setNormalizedName(normalize(raw));
            evidence.setEvidenceValue(BigDecimal.ONE);
            evidence.setConfidence(CANDIDATE_CONFIDENCE);
            evidence.setEvidenceText(sentence(text, raw));
            evidence.setMatchStatus("UNRESOLVED");
            evidence.setModelVersion(PARSER_VERSION);
            evidence.setSourceSpanJson(json(Map.of("matchedText", raw, "resolutionRequired", true)));
            evidence.setAppliedStatus("CANDIDATE");
            mapper.upsertEvidence(evidence);
        }
    }

    private String sentence(String text, String key) {
        int index = text.toLowerCase(Locale.ROOT).indexOf(key.toLowerCase(Locale.ROOT));
        int start = Math.max(0, Math.max(text.lastIndexOf('\n', index), text.lastIndexOf('。', index)) + 1);
        int lineEnd = text.indexOf('\n', index);
        int sentenceEnd = text.indexOf('。', index);
        int end = lineEnd < 0 ? sentenceEnd : sentenceEnd < 0 ? lineEnd : Math.min(lineEnd, sentenceEnd);
        return text.substring(start, end < 0 ? Math.min(text.length(), index + 200) : end).trim();
    }

    private String parse(FileAsset asset, byte[] bytes) throws Exception {
        String extension = extension(asset.getFileName());
        if ("txt".equals(extension)) return new String(bytes, StandardCharsets.UTF_8);
        if ("pdf".equals(extension)) {
            try (PDDocument pdf = PDDocument.load(bytes)) { return new PDFTextStripper().getText(pdf); }
        }
        if ("docx".equals(extension)) return extractDocx(bytes);
        throw BizException.of(ErrorCode.PARAM_ERROR, "unsupported resume document type");
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!"word/document.xml".equals(entry.getName())) continue;
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setExpandEntityReferences(false);
                org.w3c.dom.Document xml = factory.newDocumentBuilder().parse(zip);
                NodeList nodes = xml.getElementsByTagNameNS("*", "t");
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i); text.append(node.getTextContent()).append(' ');
                }
                return text.toString();
            }
        }
        throw BizException.of(ErrorCode.PARAM_ERROR, "DOCX document.xml is missing");
    }

    private void validateSignature(String extension, byte[] bytes) {
        if (bytes.length == 0) throw BizException.of(ErrorCode.PARAM_ERROR, "resume file is empty");
        if ("pdf".equals(extension) && (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "invalid PDF signature");
        }
        if ("docx".equals(extension) && (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K')) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "invalid DOCX signature");
        }
        if ("txt".equals(extension) && new String(bytes, StandardCharsets.UTF_8).indexOf('\u0000') >= 0) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "TXT resume contains binary content");
        }
    }

    private List<String> declaredSkillCandidates(String text) {
        Pattern heading = Pattern.compile("(?im)^(?:skills?|technical\\s+skills?|技能|技术栈)\\s*[:：]\\s*(.+)$");
        Matcher matcher = heading.matcher(text);
        Set<String> values = new LinkedHashSet<>();
        while (matcher.find()) {
            for (String part : matcher.group(1).split("[,，;；|/\\n]+")) {
                String value = part.replaceFirst("^[\\s•*\\-]+", "").trim();
                if (!value.isBlank() && value.length() <= 255) values.add(value);
            }
        }
        return new ArrayList<>(values);
    }

    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }

    private String extension(String filename) {
        int index = filename == null ? -1 : filename.lastIndexOf('.');
        return index < 0 ? null : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String shortMessage(Throwable error) {
        String message = Objects.toString(error.getMessage(), error.getClass().getSimpleName());
        return message.length() > 900 ? message.substring(0, 900) : message;
    }
}
