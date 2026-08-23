package com.xyz.question_bank_management_system.modules.competency.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.competency.entity.QbCompetencyJobSnapshot;
import com.xyz.question_bank_management_system.modules.competency.entity.QbCompetencyJobSyncRecord;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.competency.mapper.QbCompetencyJobSnapshotMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.QbCompetencyJobSyncRecordMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetencyLandingService {

    private static final String PLATFORM = "BOSS直聘";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern JOB_URL_PATTERN = Pattern.compile("(https://www\\.zhipin\\.com/job_detail/[A-Za-z0-9~._\\-]+(?:\\.html)?)");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SALARY_PATTERN = Pattern.compile("薪资(?:待遇)?[:：]?\\s*([^<\\n]{2,40})");
    private static final Pattern COMPANY_PATTERN = Pattern.compile("(?:公司名称|公司)[:：]?\\s*([^<\\n]{2,60})");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("(?:工作地点|地点|城市)[:：]?\\s*([^<\\n]{2,40})");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d-\\d年|\\d年以上|经验不限|1年以下|应届生)");
    private static final Pattern EDUCATION_PATTERN = Pattern.compile("(大专|本科|硕士|博士|学历不限)");
    private static final Pattern TAG_PATTERN = Pattern.compile("(Linux|C\\+\\+|C语言|嵌入式|驱动|内核|性能优化|DPDK|测试|稳定性|需求分析|方案设计)");

    private final QbCompetencyJobSnapshotMapper snapshotMapper;
    private final QbCompetencyJobSyncRecordMapper syncRecordMapper;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    @Value("${app.landing.boss.max-visible-jobs:60}")
    private int maxVisibleJobs;

    @Value("${app.landing.boss.search-page-limit:1}")
    private int searchPageLimit;

    private static final List<DimensionSeed> DIMENSIONS = List.of(
            new DimensionSeed("技术能力", "#38bdf8", "聚焦 C 语言核心语法、指针、内存和工程实现能力。", List.of(
                    detail("变量与数据类型", "理解基础数据表示、类型转换与存储方式。"),
                    detail("函数设计", "能够进行函数拆分、参数设计和返回值组织。"),
                    detail("指针基础", "掌握地址、解引用和指针与数组、结构体的联系。"),
                    detail("动态内存管理", "理解 malloc/free 使用和内存生命周期。"),
                    detail("文件操作", "能够完成文本文件读写和基本数据持久化。"),
                    detail("综合实现能力", "把语法、数据结构、调试能力整合到完整程序中。")
            )),
            new DimensionSeed("系统编程", "#0ea5e9", "强调 Linux C、驱动、系统调用和底层运行机制理解。", List.of(
                    detail("Linux 编译运行", "掌握 gcc、make、基本 shell 运行流程。"),
                    detail("系统调用意识", "理解文件、进程、IO 相关基础系统接口。"),
                    detail("进程与线程基础", "理解并发执行与资源隔离的基本概念。"),
                    detail("驱动与设备交互认知", "理解嵌入式和驱动岗位中的设备访问场景。"),
                    detail("内核/用户态边界", "理解用户态程序与系统层之间的职责边界。")
            )),
            new DimensionSeed("数据分析", "#34d399", "围绕数组、结构化记录、统计和结果解释能力构建。", List.of(
                    detail("数组统计", "完成求和、平均值、极值、频次等基础统计。"),
                    detail("结构化记录处理", "能对结构体数组和多字段记录进行筛选汇总。"),
                    detail("规则建模", "把现实规则转为可计算条件。"),
                    detail("结果校验", "通过样例和反例验证结果可信度。"),
                    detail("趋势解释", "不仅算出结果，还能解释其变化原因。")
            )),
            new DimensionSeed("工程质量", "#a78bfa", "强调规范、测试、鲁棒性和交付完整性。", List.of(
                    detail("命名与排版规范", "保持代码可读性和一致性。"),
                    detail("边界测试意识", "主动验证极值、空输入和异常输入。"),
                    detail("异常处理", "对失败路径提供保护和反馈。"),
                    detail("鲁棒性设计", "让程序在非理想条件下仍能稳定运行。"),
                    detail("缺陷复盘", "能总结错误根因和修复策略。")
            )),
            new DimensionSeed("问题求解", "#f59e0b", "关注需求理解、分步拆解和方案优化能力。", List.of(
                    detail("题意提炼", "识别输入、输出、约束与关键条件。"),
                    detail("任务拆解", "把复杂问题拆成可执行步骤。"),
                    detail("算法选择", "从枚举、模拟、查找、排序中选合适策略。"),
                    detail("边界分析", "识别特殊输入和规模变化风险。"),
                    detail("持续纠偏", "发现思路不可行时能及时调整。")
            )),
            new DimensionSeed("性能优化", "#f97316", "聚焦时间复杂度、吞吐、延迟和关键路径优化。", List.of(
                    detail("复杂度意识", "在正确性基础上关注时间和空间开销。"),
                    detail("关键路径识别", "能找出性能瓶颈集中位置。"),
                    detail("数据访问优化", "理解缓存友好、批量处理和少拷贝思路。"),
                    detail("性能验证", "会用样例或日志对优化前后效果做对比。"),
                    detail("吞吐/延迟认知", "理解网络、安全、系统类岗位中的性能指标。")
            )),
            new DimensionSeed("学习迁移", "#fb7185", "把课程知识迁移到项目、岗位和综合任务中。", List.of(
                    detail("知识映射", "把课堂知识点映射到实验和岗位任务。"),
                    detail("项目落地", "把练习能力延伸到综合实验和项目。"),
                    detail("岗位连接", "理解当前学习内容与岗位要求的对应关系。"),
                    detail("任务优先级安排", "根据场景先补基础、再做综合、最后强化。"),
                    detail("能力证据整理", "会整理作业、项目和代码片段作为能力证明。")
            )),
            new DimensionSeed("协作表达", "#22d3ee", "提升讲解、文档、答辩和反馈闭环能力。", List.of(
                    detail("思路口头表达", "清晰说明关键步骤和判断依据。"),
                    detail("技术书面表达", "能写实验说明、程序说明和总结。"),
                    detail("代码讲解", "能向他人解释函数职责和核心逻辑。"),
                    detail("技术答辩", "能说明方案选择与权衡理由。"),
                    detail("反馈闭环", "把建议转化为下一步改进项。")
            )),
            new DimensionSeed("职业工程化", "#10b981", "围绕真实岗位中的需求、协作、交付和维护意识。", List.of(
                    detail("需求确认", "动手前确认输入、输出和验收标准。"),
                    detail("文档协同", "在共享文档中记录问题和方案。"),
                    detail("版本管理基础", "理解基本 Git 协作流程。"),
                    detail("任务推进意识", "能在多人环境下跟进进度和阻塞。"),
                    detail("交付完整性", "代码、说明、测试和结果展示保持一致。")
            ))
    );

    private static final List<String> SEARCH_KEYWORDS = List.of("C语言开发", "Linux C开发", "嵌入式软件工程师", "嵌入式 Linux", "C/C++开发");
    private static final Map<String, String> CITY_CODES = Map.of(
            "北京", "101010100",
            "上海", "101020100",
            "深圳", "101280600",
            "广州", "101280100",
            "杭州", "101210100",
            "东莞", "101281600"
    );

    private static final List<SeedJob> SEED_JOBS = List.of(
            new SeedJob("https://www.zhipin.com/job_detail/d7c7b15630c337eb0X142NS4F1U~.html", "嵌入式软件工程师", "技术能力", "C/C++、Linux 应用开发、数据结构", "深圳", "音频行业岗位"),
            new SeedJob("https://www.zhipin.com/job_detail/aef5e843fbcd4a3303Jz3N25FVZW.html", "Linux C开发工程师", "系统编程", "Linux 内核相关代码、用户态控制程序", "深圳", "轻网科技"),
            new SeedJob("https://m.zhipin.com/job_detail/5ecbda95d79105750XF539u7EVc~.html", "Linux C开发工程师", "性能优化", "DPDK、基础功能稳定性、性能优化", "北京", "天融信"),
            new SeedJob("https://m.zhipin.com/job_detail/ed59a0f9cb555bf13nBz29i7EFI~.html", "Linux嵌入式软件工程师", "学习迁移", "嵌入式 Linux 应用与驱动、项目问题处理", "东莞", "ZCAM"),
            new SeedJob("https://www.zhipin.com/job_detail/47321b217f9dbb3f1XV909W7E1BS.html", "嵌入式软件工程师", "问题求解", "需求到方案设计、独立承担项目开发", "杭州", "锦沃"),
            new SeedJob("https://www.zhipin.com/job_detail/82fba5974dbfd8c00nZ-2t68GFZW.html", "Linux C++ 开发工程师", "数据分析", "系统集成、边缘端程序、数据协同", "上海", "边缘端方向岗位"),
            new SeedJob("https://www.zhipin.com/job_detail/8859bab829cad70d1XR-2N-9F1RX.html", "嵌入式软件开发工程师", "职业工程化", "硬件平台开发、系统设计、接口文档", "广州", "通用嵌入式岗位"),
            new SeedJob("https://www.zhipin.com/job_detail/cad892bd30111fa40nd_2t2_EFZZ", "linux/c++开发工程师", "协作表达", "Linux 原理理解、跨团队沟通", "深圳", "深圳从平")
    );

    public CompetencyLayerResponse getCompetencyLayer() {
        ensureSeedDataIfEmpty();
        List<QbCompetencyJobSnapshot> rows = snapshotMapper.selectActiveSnapshots(Math.max(maxVisibleJobs, 20));
        CompetencyLayerResponse response = new CompetencyLayerResponse();
        response.setDimensions(buildDimensions());
        response.setJobs(rows.stream().map(this::toJobDto).collect(Collectors.toList()));
        response.setSyncMeta(buildSyncMeta());
        return response;
    }

    public SyncResult triggerManualSync(Long operatorId) {
        return doSync("manual", operatorId);
    }

    public void runScheduledSync() {
        doSync("scheduled", null);
    }

    public List<SyncRecordItem> recentSyncRecords(int limit) {
        return syncRecordMapper.selectRecent(Math.max(1, Math.min(limit, 20))).stream()
                .map(this::toSyncRecordItem)
                .collect(Collectors.toList());
    }

    private void ensureSeedDataIfEmpty() {
        if (snapshotMapper.countAll() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SeedJob seed : SEED_JOBS) {
            QbCompetencyJobSnapshot snapshot = new QbCompetencyJobSnapshot();
            snapshot.setSourcePlatform(PLATFORM);
            snapshot.setSourceUrl(seed.sourceUrl());
            snapshot.setSourceJobKey(extractJobKey(seed.sourceUrl()));
            snapshot.setTitle(seed.title());
            snapshot.setDimension(seed.dimension());
            snapshot.setSkill(seed.skill());
            snapshot.setLocation(seed.location());
            snapshot.setSalary("待同步");
            snapshot.setExperience("待同步");
            snapshot.setEducation("待同步");
            snapshot.setCompany(seed.company());
            snapshot.setDescription("初始化岗位快照，等待同步任务更新。");
            snapshot.setTagsJson(writeTags(List.of(seed.dimension(), seed.location())));
            snapshot.setSourceUpdatedAt(now);
            snapshot.setLastSeenAt(now);
            snapshot.setAvailabilityStatus("seeded");
            snapshot.setSyncVersion(0L);
            snapshotMapper.insert(snapshot);
        }
    }

    private SyncResult doSync(String triggerType, Long operatorId) {
        if (!syncRunning.compareAndSet(false, true)) {
            SyncResult result = new SyncResult();
            result.setStatus("running");
            result.setMessage("同步任务正在运行中");
            result.setStartedAt(format(LocalDateTime.now()));
            return result;
        }

        QbCompetencyJobSyncRecord record = new QbCompetencyJobSyncRecord();
        record.setTriggerType(triggerType);
        record.setTriggerBy(operatorId);
        record.setPlatform(PLATFORM);
        record.setStatus("running");
        record.setKeywordCount(SEARCH_KEYWORDS.size());
        record.setCityCount(CITY_CODES.size());
        record.setFetchedCandidateCount(0);
        record.setSuccessCount(0);
        record.setFailureCount(0);
        record.setOfflineCount(0);
        record.setErrorMessage(null);
        record.setStartedAt(LocalDateTime.now());
        record.setFinishedAt(null);
        syncRecordMapper.insert(record);

        int successCount = 0;
        int failureCount = 0;
        int candidateCount = 0;
        LocalDateTime now = LocalDateTime.now();
        try {
            Set<String> candidateUrls = new LinkedHashSet<>();
            for (String keyword : SEARCH_KEYWORDS) {
                for (Map.Entry<String, String> city : CITY_CODES.entrySet()) {
                    candidateUrls.addAll(fetchCandidateUrls(keyword, city.getValue()));
                }
            }
            candidateUrls.addAll(SEED_JOBS.stream().map(SeedJob::sourceUrl).collect(Collectors.toList()));
            candidateCount = candidateUrls.size();

            for (String url : candidateUrls) {
                try {
                    JobDetail detail = fetchJobDetail(url);
                    upsertSnapshot(detail, record.getId(), now);
                    successCount++;
                } catch (Exception ex) {
                    failureCount++;
                    log.warn("Competency landing sync fetch failed, url={}", url, ex);
                }
            }

            int offlineCount = snapshotMapper.markMissingAsOffline(now.minusMinutes(1));
            record.setStatus(successCount > 0 ? (failureCount > 0 ? "degraded" : "success") : "failed");
            record.setFetchedCandidateCount(candidateCount);
            record.setSuccessCount(successCount);
            record.setFailureCount(failureCount);
            record.setOfflineCount(offlineCount);
            record.setFinishedAt(LocalDateTime.now());
            record.setErrorMessage(successCount > 0 ? null : "未能成功解析任何岗位详情");
            syncRecordMapper.updateCompletion(record);

            SyncResult result = new SyncResult();
            result.setStatus(record.getStatus());
            result.setMessage(successCount > 0 ? "同步完成" : "同步失败");
            result.setStartedAt(format(record.getStartedAt()));
            result.setFinishedAt(format(record.getFinishedAt()));
            result.setFetchedCandidateCount(candidateCount);
            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            result.setOfflineCount(offlineCount);
            return result;
        } catch (Exception ex) {
            record.setStatus("failed");
            record.setFetchedCandidateCount(candidateCount);
            record.setSuccessCount(successCount);
            record.setFailureCount(failureCount);
            record.setOfflineCount(0);
            record.setFinishedAt(LocalDateTime.now());
            record.setErrorMessage(trimError(ex.getMessage()));
            syncRecordMapper.updateCompletion(record);
            throw BizException.of(ErrorCode.SYSTEM_ERROR, "岗位同步失败: " + trimError(ex.getMessage()));
        } finally {
            syncRunning.set(false);
        }
    }

    private List<String> fetchCandidateUrls(String keyword, String cityCode) {
        List<String> urls = new ArrayList<>();
        for (int page = 1; page <= Math.max(1, searchPageLimit); page++) {
            String url = "https://www.zhipin.com/web/geek/job?query="
                    + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&city=" + cityCode
                    + "&page=" + page;
            String html = fetchBody(url);
            Matcher matcher = JOB_URL_PATTERN.matcher(html);
            while (matcher.find()) {
                urls.add(matcher.group(1));
            }
        }
        return urls;
    }

    private JobDetail fetchJobDetail(String url) {
        String html = fetchBody(url);
        SeedJob seed = findSeedJob(url);
        String parsedTitle = parseTitle(html);
        boolean blocked = looksLikeBlockedPage(html, parsedTitle);
        JobDetail detail = new JobDetail();
        detail.setSourcePlatform(PLATFORM);
        detail.setSourceUrl(url);
        detail.setSourceJobKey(extractJobKey(url));
        detail.setTitle(firstNonBlank(parseTitle(html), fallbackTitleBySeed(url), "岗位待识别"));
        detail.setCompany(firstNonBlank(matchValue(COMPANY_PATTERN, html), fallbackCompanyBySeed(url), "来源岗位"));
        detail.setLocation(firstNonBlank(matchValue(LOCATION_PATTERN, html), fallbackLocationBySeed(url), "未知城市"));
        detail.setSalary(firstNonBlank(matchValue(SALARY_PATTERN, html), "BOSS详情页可见"));
        detail.setExperience(firstNonBlank(matchValue(EXPERIENCE_PATTERN, html), "经验待确认"));
        detail.setEducation(firstNonBlank(matchValue(EDUCATION_PATTERN, html), "学历待确认"));
        detail.setTags(parseTags(html, detail));
        if (blocked) {
            detail.setTitle(firstNonBlank(seed == null ? null : seed.title(), detail.getTitle(), "岗位待识别"));
            detail.setCompany(firstNonBlank(seed == null ? null : seed.company(), detail.getCompany(), "来源岗位"));
            detail.setLocation(firstNonBlank(seed == null ? null : seed.location(), detail.getLocation(), "未知城市"));
        }
        detail.setSkill(buildSkill(detail));
        detail.setDescription(buildDescription(html, detail));
        detail.setDimension(mapDimension(detail));
        if (blocked && seed != null) {
            detail.setDimension(firstNonBlank(seed.dimension(), detail.getDimension(), "技术能力"));
            detail.setSkill(firstNonBlank(seed.skill(), detail.getSkill(), "岗位技能待同步"));
        } else {
            detail.setDimension(firstNonBlank(detail.getDimension(), "技术能力"));
            detail.setSkill(firstNonBlank(detail.getSkill(), "岗位技能待同步"));
        }
        detail.setDescription(buildDescription(detail, blocked));
        detail.setSourceUpdatedAt(LocalDateTime.now());
        detail.setAvailabilityStatus("active");
        return detail;
    }

    private void upsertSnapshot(JobDetail detail, Long syncVersion, LocalDateTime seenAt) {
        QbCompetencyJobSnapshot existing = snapshotMapper.selectBySourceUrl(detail.getSourceUrl());
        if (existing == null) {
            QbCompetencyJobSnapshot snapshot = new QbCompetencyJobSnapshot();
            snapshot.setSourcePlatform(detail.getSourcePlatform());
            snapshot.setSourceUrl(detail.getSourceUrl());
            snapshot.setSourceJobKey(detail.getSourceJobKey());
            snapshot.setTitle(detail.getTitle());
            snapshot.setDimension(detail.getDimension());
            snapshot.setSkill(detail.getSkill());
            snapshot.setLocation(detail.getLocation());
            snapshot.setSalary(detail.getSalary());
            snapshot.setExperience(detail.getExperience());
            snapshot.setEducation(detail.getEducation());
            snapshot.setCompany(detail.getCompany());
            snapshot.setDescription(detail.getDescription());
            snapshot.setTagsJson(writeTags(detail.getTags()));
            snapshot.setSourceUpdatedAt(detail.getSourceUpdatedAt());
            snapshot.setLastSeenAt(seenAt);
            snapshot.setAvailabilityStatus(detail.getAvailabilityStatus());
            snapshot.setSyncVersion(syncVersion);
            snapshotMapper.insert(snapshot);
            return;
        }
        existing.setSourcePlatform(detail.getSourcePlatform());
        existing.setSourceJobKey(detail.getSourceJobKey());
        existing.setTitle(detail.getTitle());
        existing.setDimension(detail.getDimension());
        existing.setSkill(detail.getSkill());
        existing.setLocation(detail.getLocation());
        existing.setSalary(detail.getSalary());
        existing.setExperience(detail.getExperience());
        existing.setEducation(detail.getEducation());
        existing.setCompany(detail.getCompany());
        existing.setDescription(detail.getDescription());
        existing.setTagsJson(writeTags(detail.getTags()));
        existing.setSourceUpdatedAt(detail.getSourceUpdatedAt());
        existing.setLastSeenAt(seenAt);
        existing.setAvailabilityStatus(detail.getAvailabilityStatus());
        existing.setSyncVersion(syncVersion);
        snapshotMapper.update(existing);
    }

    private SyncMeta buildSyncMeta() {
        QbCompetencyJobSyncRecord latest = syncRecordMapper.selectLatest();
        QbCompetencyJobSyncRecord latestSuccess = syncRecordMapper.selectLatestSuccess();
        SyncMeta meta = new SyncMeta();
        meta.setPlatform(PLATFORM);
        meta.setLastAttemptAt(format(latest == null ? null : latest.getStartedAt()));
        meta.setLastSuccessAt(format(latestSuccess == null ? null : latestSuccess.getFinishedAt()));
        meta.setStatus(latest == null ? "seeded" : latest.getStatus());
        meta.setJobCount((int) snapshotMapper.countVisible());
        return meta;
    }

    private List<CompetencyDimensionDto> buildDimensions() {
        return DIMENSIONS.stream().map(seed -> {
            CompetencyDimensionDto dto = new CompetencyDimensionDto();
            dto.setName(seed.name());
            dto.setColor(seed.color());
            dto.setDesc(seed.desc());
            dto.setDetails(seed.details());
            dto.setCount(seed.details().size());
            return dto;
        }).collect(Collectors.toList());
    }

    private CompetencyJobDto toJobDto(QbCompetencyJobSnapshot row) {
        SeedJob seed = findSeedJob(row.getSourceUrl());
        boolean blocked = looksLikeBlockedPage(row.getDescription(), row.getTitle());
        CompetencyJobDto dto = new CompetencyJobDto();
        dto.setId(row.getId());
        dto.setTitle(row.getTitle());
        dto.setDimension(row.getDimension());
        dto.setSkill(row.getSkill());
        dto.setLocation(row.getLocation());
        dto.setSalary(row.getSalary());
        dto.setExperience(row.getExperience());
        dto.setEducation(row.getEducation());
        dto.setCompany(row.getCompany());
        dto.setDescription(row.getDescription());
        if (blocked && seed != null) {
            dto.setTitle(firstNonBlank(seed.title(), dto.getTitle(), "岗位待识别"));
            dto.setDimension(firstNonBlank(seed.dimension(), dto.getDimension(), "技术能力"));
            dto.setSkill(firstNonBlank(seed.skill(), dto.getSkill(), "岗位技能待同步"));
            dto.setLocation(firstNonBlank(seed.location(), dto.getLocation(), "未知城市"));
            dto.setCompany(firstNonBlank(seed.company(), dto.getCompany(), "来源岗位"));
            dto.setDescription(buildDescription(dtoToDetail(dto), true));
        }
        dto.setTags(readTags(row.getTagsJson()));
        dto.setExample("来源链接可查看最新岗位详情");
        dto.setSourcePlatform(row.getSourcePlatform());
        dto.setSourceUrl(row.getSourceUrl());
        dto.setSourceUpdatedAt(format(row.getSourceUpdatedAt()));
        dto.setAvailabilityStatus(row.getAvailabilityStatus());
        dto.setSourceNote("最近同步: " + format(row.getUpdatedAt()));
        return dto;
    }

    private SyncRecordItem toSyncRecordItem(QbCompetencyJobSyncRecord row) {
        SyncRecordItem item = new SyncRecordItem();
        item.setId(row.getId());
        item.setTriggerType(row.getTriggerType());
        item.setStatus(row.getStatus());
        item.setStartedAt(format(row.getStartedAt()));
        item.setFinishedAt(format(row.getFinishedAt()));
        item.setFetchedCandidateCount(safeInt(row.getFetchedCandidateCount()));
        item.setSuccessCount(safeInt(row.getSuccessCount()));
        item.setFailureCount(safeInt(row.getFailureCount()));
        item.setOfflineCount(safeInt(row.getOfflineCount()));
        item.setErrorMessage(row.getErrorMessage());
        return item;
    }

    private String fetchBody(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IOException("HTTP " + response.statusCode());
            }
            return response.body();
        } catch (Exception ex) {
            throw new RuntimeException("fetch failed: " + url, ex);
        }
    }

    private String parseTitle(String html) {
        String title = matchValue(TITLE_PATTERN, html);
        if (!StringUtils.hasText(title)) {
            return null;
        }
        return title.replaceAll("\\s+", " ").replace("_BOSS直聘.*", "").trim();
    }

    private String buildSkill(JobDetail detail) {
        List<String> tags = detail.getTags();
        if (!tags.isEmpty()) {
            return tags.stream().limit(3).collect(Collectors.joining("、"));
        }
        return firstNonBlank(detail.getDimension(), "岗位技能待同步");
    }

    private List<String> parseTags(String html, JobDetail detail) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        Matcher matcher = TAG_PATTERN.matcher(html);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        if (StringUtils.hasText(detail.getTitle())) {
            if (detail.getTitle().toLowerCase(Locale.ROOT).contains("linux")) tags.add("Linux");
            if (detail.getTitle().contains("嵌入式")) tags.add("嵌入式");
            if (detail.getTitle().contains("C++")) tags.add("C++");
            if (detail.getTitle().contains("C")) tags.add("C语言");
        }
        return new ArrayList<>(tags);
    }

    private String buildDescription(String html, JobDetail detail) {
        String base = "岗位来源于 BOSS直聘公开页面，系统按标题、标签和描述关键词完成能力维度归类。";
        String extension = "当前识别为“" + detail.getDimension() + "”，核心关键词包括：" + buildSkill(detail) + "。";
        return base + extension;
    }

    private String buildDescription(JobDetail detail, boolean blocked) {
        String base = blocked
                ? "岗位来源于 BOSS 直聘公开页面，本次抓取命中了访问限制页，当前先回退到系统内置岗位快照。"
                : "岗位来源于 BOSS 直聘公开页面，系统按标题、标签和描述关键词完成能力维度归类。";
        String extension = "当前识别为“" + firstNonBlank(detail.getDimension(), "技术能力") + "”，核心关键词包括：" + firstNonBlank(detail.getSkill(), "岗位技能待同步") + "。";
        return base + extension;
    }

    private String mapDimension(JobDetail detail) {
        String text = (firstNonBlank(detail.getTitle()) + " " + firstNonBlank(detail.getSkill()) + " " + firstNonBlank(detail.getDescription())).toLowerCase(Locale.ROOT);
        if (containsAny(text, "内核", "驱动", "系统调用", "linux c", "linux", "嵌入式")) return "系统编程";
        if (containsAny(text, "性能", "优化", "吞吐", "延迟", "dpdk")) return "性能优化";
        if (containsAny(text, "测试", "稳定", "质量", "鲁棒")) return "工程质量";
        if (containsAny(text, "需求", "方案", "独立负责", "设计")) return "问题求解";
        if (containsAny(text, "交付", "文档", "协作", "沟通", "答辩")) return "职业工程化";
        if (containsAny(text, "数据", "统计", "分析", "记录", "日志")) return "数据分析";
        if (containsAny(text, "迁移", "项目", "产品", "场景")) return "学习迁移";
        if (containsAny(text, "表达", "汇报", "沟通", "讲解")) return "协作表达";
        return "技术能力";
    }

    private boolean containsAny(String source, String... words) {
        for (String word : words) {
            if (source.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private JobDetail dtoToDetail(CompetencyJobDto dto) {
        JobDetail detail = new JobDetail();
        detail.setTitle(dto.getTitle());
        detail.setDimension(dto.getDimension());
        detail.setSkill(dto.getSkill());
        detail.setLocation(dto.getLocation());
        detail.setCompany(dto.getCompany());
        detail.setDescription(dto.getDescription());
        return detail;
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? Collections.emptyList() : tags);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> readTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private static String matchValue(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String extractJobKey(String url) {
        int idx = url.lastIndexOf('/');
        return idx >= 0 ? url.substring(idx + 1) : url;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : DATE_TIME.format(time);
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知错误";
        }
        return message.length() > 900 ? message.substring(0, 900) : message;
    }

    private static DetailItem detail(String title, String text) {
        DetailItem item = new DetailItem();
        item.setTitle(title);
        item.setText(text);
        return item;
    }

    private String fallbackTitleBySeed(String url) {
        return SEED_JOBS.stream().filter(item -> item.sourceUrl().equals(url)).map(SeedJob::title).findFirst().orElse(null);
    }

    private String fallbackLocationBySeed(String url) {
        return SEED_JOBS.stream().filter(item -> item.sourceUrl().equals(url)).map(SeedJob::location).findFirst().orElse(null);
    }

    private String fallbackCompanyBySeed(String url) {
        return SEED_JOBS.stream().filter(item -> item.sourceUrl().equals(url)).map(SeedJob::company).findFirst().orElse(null);
    }

    private SeedJob findSeedJob(String url) {
        return SEED_JOBS.stream().filter(item -> item.sourceUrl().equals(url)).findFirst().orElse(null);
    }

    private boolean looksLikeBlockedPage(String html, String title) {
        String safeTitle = StringUtils.hasText(title) ? title : "";
        String safeHtml = html == null ? "" : html;
        return safeTitle.contains("请稍候")
                || safeTitle.contains("验证")
                || safeTitle.contains("稍后再试")
                || (safeTitle.contains("BOSS") && !safeTitle.contains("工程师") && !safeTitle.contains("开发"))
                || safeHtml.contains("请稍候")
                || safeHtml.contains("验证")
                || safeHtml.contains("security-check")
                || safeHtml.contains("captcha");
    }

    @Data
    public static class CompetencyLayerResponse {
        private List<CompetencyDimensionDto> dimensions;
        private List<CompetencyJobDto> jobs;
        private SyncMeta syncMeta;
    }

    @Data
    public static class CompetencyDimensionDto {
        private String name;
        private Integer count;
        private String color;
        private String desc;
        private List<DetailItem> details;
    }

    @Data
    public static class DetailItem {
        private String title;
        private String text;
    }

    @Data
    public static class CompetencyJobDto {
        private Long id;
        private String title;
        private String dimension;
        private String skill;
        private String location;
        private String salary;
        private String experience;
        private String education;
        private String company;
        private String description;
        private List<String> tags;
        private String example;
        private String sourcePlatform;
        private String sourceUrl;
        private String sourceUpdatedAt;
        private String availabilityStatus;
        private String sourceNote;
    }

    @Data
    public static class SyncMeta {
        private String platform;
        private String lastSuccessAt;
        private String lastAttemptAt;
        private String status;
        private Integer jobCount;
    }

    @Data
    public static class SyncResult {
        private String status;
        private String message;
        private String startedAt;
        private String finishedAt;
        private Integer fetchedCandidateCount;
        private Integer successCount;
        private Integer failureCount;
        private Integer offlineCount;
    }

    @Data
    public static class SyncRecordItem {
        private Long id;
        private String triggerType;
        private String status;
        private String startedAt;
        private String finishedAt;
        private Integer fetchedCandidateCount;
        private Integer successCount;
        private Integer failureCount;
        private Integer offlineCount;
        private String errorMessage;
    }

    @Data
    private static class JobDetail {
        private String sourcePlatform;
        private String sourceUrl;
        private String sourceJobKey;
        private String title;
        private String dimension;
        private String skill;
        private String location;
        private String salary;
        private String experience;
        private String education;
        private String company;
        private String description;
        private List<String> tags = new ArrayList<>();
        private LocalDateTime sourceUpdatedAt;
        private String availabilityStatus;
    }

    private record DimensionSeed(String name, String color, String desc, List<DetailItem> details) {
    }

    private record SeedJob(String sourceUrl, String title, String dimension, String skill, String location, String company) {
    }
}
