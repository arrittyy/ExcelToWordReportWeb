package com.reportweb.service;

import com.reportweb.dto.MaterialLibraryDTOs;
import com.reportweb.entity.MaterialApprovalLog;
import com.reportweb.entity.MaterialLibraryEntry;
import com.reportweb.entity.User;
import com.reportweb.repository.MaterialApprovalLogRepository;
import com.reportweb.repository.MaterialLibraryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialLibraryService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_DELETED = "DELETED";
    public static final String SOURCE_USER = "USER";
    public static final String SOURCE_SEEDED = "SEEDED";

    public static final String MOD_CREATE = "CREATE";
    public static final String MOD_UPDATE = "UPDATE";
    public static final String MOD_DELETE = "DELETE";

    public static final String ACTION_SUBMIT = "submit";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE_REQUEST = "delete_request";
    public static final String ACTION_APPROVE = "approve";
    public static final String ACTION_REJECT = "reject";
    public static final String ACTION_RESUBMIT = "resubmit";
    public static final String ACTION_DISMISS_REJECTION = "dismiss_rejection";

    private final MaterialLibraryEntryRepository entryRepository;
    private final MaterialApprovalLogRepository logRepository;
    private final MaterialPropertyService materialPropertyService;

    public List<MaterialLibraryDTOs.ListItem> listEntries(String category, String keyword) {
        List<MaterialLibraryDTOs.ListItem> items = new ArrayList<>();
        for (MaterialLibraryEntry entry : entryRepository.findAllForLibraryList()) {
            Map<String, String> displayProps = resolveDisplayProperties(entry);
            if (category != null && !category.isBlank()
                    && !MaterialCategoryUtils.matchesCategory(category, displayProps)
                    && !category.equals(entry.getPrimaryCategory())) {
                continue;
            }
            if (!MaterialCategoryUtils.matchesKeyword(keyword, entry.getMaterialKey(), displayProps)) {
                continue;
            }
            items.add(toListItem(entry, displayProps));
        }
        items.sort(Comparator.comparing(MaterialLibraryDTOs.ListItem::getMaterialKey, String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    public List<String> listAllKeys() {
        return entryRepository.findAllEffectiveForCache().stream()
                .map(MaterialLibraryEntry::getMaterialKey)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public List<MaterialLibraryDTOs.ListItem> listPending() {
        return entryRepository.findByStatusOrderByCreatedAtDesc(STATUS_PENDING).stream()
                .map(e -> toListItem(e, e.getProperties()))
                .collect(Collectors.toList());
    }

    public long countPending() {
        return entryRepository.countByStatus(STATUS_PENDING);
    }

    public long countRejectedByUser(String userId) {
        return entryRepository.countRejectedSubmissionsByUser(userId);
    }

    public List<MaterialLibraryDTOs.ListItem> listMySubmissions(String userId) {
        return entryRepository.findBySubmittedByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSubmissionListItem)
                .collect(Collectors.toList());
    }

    public MaterialLibraryDTOs.CapabilitiesResponse capabilities(User user) {
        MaterialLibraryDTOs.CapabilitiesResponse resp = new MaterialLibraryDTOs.CapabilitiesResponse();
        resp.setCanReview(MaterialLibraryReviewerUtils.canReview(user));
        resp.setCanSubmit(user != null);
        resp.setPendingReviewCount(MaterialLibraryReviewerUtils.canReview(user) ? countPending() : 0);
        resp.setRejectedCount(user == null ? 0 : countRejectedByUser(user.getId()));
        return resp;
    }

    public List<MaterialLibraryDTOs.ApprovalLogItem> listLogs(Long entryId) {
        return logRepository.findByEntryIdOrderByCreatedAtDesc(entryId).stream()
                .map(this::toLogItem)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaterialLibraryDTOs.ListItem submit(User user, MaterialLibraryDTOs.CreateRequest request) {
        validateCreateRequest(request);
        String canonicalKey = materialPropertyService.canonicalizeMaterialKey(request.getMaterialKey());

        if (entryRepository.findByMaterialKeyIgnoreCase(canonicalKey).isPresent()) {
            throw new IllegalArgumentException("该牌号已存在，请使用编辑功能");
        }
        if (entryRepository.existsPendingByMaterialKeyIgnoreCase(canonicalKey)) {
            throw new IllegalArgumentException("该牌号已有待审核记录");
        }

        MaterialLibraryEntry entry = new MaterialLibraryEntry();
        entry.setMaterialKey(canonicalKey);
        entry.setPrimaryCategory(request.getPrimaryCategory());
        entry.setStatus(STATUS_PENDING);
        entry.setSource(SOURCE_USER);
        entry.setModificationType(MOD_CREATE);
        entry.setProperties(buildProperties(request, canonicalKey));
        entry.setSubmittedByUserId(user.getId());
        entry.setSubmittedByUserName(resolveDisplayName(user));
        entry = entryRepository.save(entry);
        appendLog(entry.getId(), ACTION_SUBMIT, user, null);
        return toListItem(entry, entry.getProperties());
    }

    @Transactional
    public MaterialLibraryDTOs.ListItem updateEntry(Long id, User user, MaterialLibraryDTOs.UpdateRequest request) {
        MaterialLibraryEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("材质记录不存在"));

        if (STATUS_DELETED.equals(entry.getStatus())) {
            throw new IllegalArgumentException("已删除的材质不可编辑");
        }
        if (STATUS_PENDING.equals(entry.getStatus())) {
            throw new IllegalArgumentException("待审核中的记录不可再次编辑，请等待审核结果");
        }

        validateUpdateRequest(request, entry.getPrimaryCategory());

        if (STATUS_REJECTED.equals(entry.getStatus())) {
            if (!user.getId().equals(entry.getSubmittedByUserId())) {
                throw new IllegalArgumentException("仅提交人可修改被驳回的记录");
            }
            entry.setProperties(buildPropertiesFromUpdate(request, entry));
            entry.setPrimaryCategory(request.getPrimaryCategory());
            entry.setStatus(STATUS_PENDING);
            entry.setModificationType(MOD_CREATE.equals(entry.getModificationType()) ? MOD_CREATE : MOD_UPDATE);
            entry.setReviewComment(null);
            entry.setReviewedAt(null);
            entry.setReviewedByUserId(null);
            entry.setReviewedByUserName(null);
            entry = entryRepository.save(entry);
            appendLog(entry.getId(), ACTION_RESUBMIT, user, null);
            return toListItem(entry, entry.getProperties());
        }

        if (STATUS_APPROVED.equals(entry.getStatus())) {
            entry.setApprovedSnapshot(MaterialCategoryUtils.copyProperties(entry.getProperties()));
            entry.setProperties(buildPropertiesFromUpdate(request, entry));
            entry.setPrimaryCategory(request.getPrimaryCategory());
            entry.setStatus(STATUS_PENDING);
            entry.setModificationType(MOD_UPDATE);
            entry.setSubmittedByUserId(user.getId());
            entry.setSubmittedByUserName(resolveDisplayName(user));
            entry.setReviewComment(null);
            entry.setReviewedAt(null);
            entry.setReviewedByUserId(null);
            entry.setReviewedByUserName(null);
            entry = entryRepository.save(entry);
            appendLog(entry.getId(), ACTION_UPDATE, user, null);
            materialPropertyService.refreshMaterialCache();
            return toListItem(entry, resolveDisplayProperties(entry));
        }

        throw new IllegalArgumentException("当前状态不允许编辑");
    }

    @Transactional
    public MaterialLibraryDTOs.ListItem requestDelete(Long id, User user) {
        MaterialLibraryEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("材质记录不存在"));

        if (STATUS_DELETED.equals(entry.getStatus())) {
            throw new IllegalArgumentException("材质已删除");
        }
        if (STATUS_PENDING.equals(entry.getStatus())) {
            throw new IllegalArgumentException("该记录已在待审核中");
        }
        if (!STATUS_APPROVED.equals(entry.getStatus())) {
            throw new IllegalArgumentException("仅已通过的材质可申请删除");
        }

        entry.setApprovedSnapshot(MaterialCategoryUtils.copyProperties(entry.getProperties()));
        entry.setStatus(STATUS_PENDING);
        entry.setModificationType(MOD_DELETE);
        entry.setSubmittedByUserId(user.getId());
        entry.setSubmittedByUserName(resolveDisplayName(user));
        entry.setReviewComment(null);
        entry.setReviewedAt(null);
        entry.setReviewedByUserId(null);
        entry.setReviewedByUserName(null);
        entry = entryRepository.save(entry);
        appendLog(entry.getId(), ACTION_DELETE_REQUEST, user, null);
        materialPropertyService.refreshMaterialCache();
        return toListItem(entry, resolveDisplayProperties(entry));
    }

    @Transactional
    public MaterialLibraryDTOs.ListItem approve(Long entryId, User reviewer) {
        MaterialLibraryEntry entry = requirePendingEntry(entryId);

        if (MOD_DELETE.equals(entry.getModificationType())) {
            entry.setStatus(STATUS_DELETED);
        } else {
            entry.setStatus(STATUS_APPROVED);
        }
        entry.setApprovedSnapshot(null);
        entry.setReviewedByUserId(reviewer.getId());
        entry.setReviewedByUserName(resolveDisplayName(reviewer));
        entry.setReviewComment(null);
        entry.setReviewedAt(LocalDateTime.now());
        entry = entryRepository.save(entry);
        appendLog(entry.getId(), ACTION_APPROVE, reviewer, null);
        materialPropertyService.refreshMaterialCache();
        return toListItem(entry, resolveDisplayProperties(entry));
    }

    @Transactional
    public MaterialLibraryDTOs.ListItem reject(Long entryId, User reviewer, String reviewComment) {
        MaterialLibraryEntry entry = requirePendingEntry(entryId);
        String comment = reviewComment == null ? null : reviewComment.trim();

        if (MOD_CREATE.equals(entry.getModificationType())) {
            entry.setStatus(STATUS_REJECTED);
            entry.setApprovedSnapshot(null);
        } else {
            if (entry.getApprovedSnapshot() != null) {
                entry.setProperties(MaterialCategoryUtils.copyProperties(entry.getApprovedSnapshot()));
            }
            entry.setApprovedSnapshot(null);
            entry.setStatus(STATUS_APPROVED);
            entry.setModificationType(MOD_CREATE.equals(entry.getSource()) ? MOD_CREATE : MOD_UPDATE);
        }

        entry.setReviewedByUserId(reviewer.getId());
        entry.setReviewedByUserName(resolveDisplayName(reviewer));
        entry.setReviewComment(comment);
        entry.setReviewedAt(LocalDateTime.now());
        entry = entryRepository.save(entry);
        appendLog(entry.getId(), ACTION_REJECT, reviewer, comment);
        materialPropertyService.refreshMaterialCache();
        return toListItem(entry, resolveDisplayProperties(entry));
    }

    @Transactional
    public void deleteDraft(Long id, User user) {
        MaterialLibraryEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("材质记录不存在"));
        boolean isOwner = user.getId().equals(entry.getSubmittedByUserId());
        if (!isOwner && !MaterialLibraryReviewerUtils.canReview(user)) {
            throw new IllegalArgumentException("无权删除该记录");
        }

        if (STATUS_APPROVED.equals(entry.getStatus()) && hasReviewComment(entry)) {
            if (!isOwner) {
                throw new IllegalArgumentException("仅提交人可消除驳回提示");
            }
            entry.setReviewComment(null);
            entry.setReviewedAt(null);
            entry.setReviewedByUserId(null);
            entry.setReviewedByUserName(null);
            entryRepository.save(entry);
            appendLog(entry.getId(), ACTION_DISMISS_REJECTION, user, null);
            return;
        }

        if (!STATUS_REJECTED.equals(entry.getStatus()) && !STATUS_PENDING.equals(entry.getStatus())) {
            throw new IllegalArgumentException("仅待审核或已驳回的草稿可删除");
        }
        if (STATUS_PENDING.equals(entry.getStatus())
                && (MOD_UPDATE.equals(entry.getModificationType()) || MOD_DELETE.equals(entry.getModificationType()))) {
            if (entry.getApprovedSnapshot() != null) {
                entry.setProperties(MaterialCategoryUtils.copyProperties(entry.getApprovedSnapshot()));
            }
            entry.setApprovedSnapshot(null);
            entry.setStatus(STATUS_APPROVED);
            entry.setModificationType(SOURCE_SEEDED.equals(entry.getSource()) ? MOD_UPDATE : MOD_CREATE);
            entryRepository.save(entry);
            materialPropertyService.refreshMaterialCache();
            return;
        }
        entryRepository.delete(entry);
        materialPropertyService.refreshMaterialCache();
    }

    private MaterialLibraryEntry requirePendingEntry(Long entryId) {
        MaterialLibraryEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("材质记录不存在"));
        if (!STATUS_PENDING.equals(entry.getStatus())) {
            throw new IllegalArgumentException("仅待审核记录可执行该操作");
        }
        return entry;
    }

    private void validateCreateRequest(MaterialLibraryDTOs.CreateRequest request) {
        if (!MaterialCategoryUtils.isValidCategory(request.getPrimaryCategory())) {
            throw new IllegalArgumentException("无效的分类");
        }
        Map<String, String> properties = MaterialCategoryUtils.sanitizeProperties(request.getProperties());
        if (!MaterialCategoryUtils.matchesCategory(request.getPrimaryCategory(), properties)) {
            throw new IllegalArgumentException("请至少填写一个该分类对应的标准字段");
        }
    }

    private void validateUpdateRequest(MaterialLibraryDTOs.UpdateRequest request, String fallbackCategory) {
        String category = request.getPrimaryCategory() != null && !request.getPrimaryCategory().isBlank()
                ? request.getPrimaryCategory() : fallbackCategory;
        if (!MaterialCategoryUtils.isValidCategory(category)) {
            throw new IllegalArgumentException("无效的分类");
        }
        Map<String, String> properties = MaterialCategoryUtils.sanitizeProperties(request.getProperties());
        if (!MaterialCategoryUtils.matchesCategory(category, properties)) {
            throw new IllegalArgumentException("请至少填写一个该分类对应的标准字段");
        }
    }

    private Map<String, String> buildProperties(MaterialLibraryDTOs.CreateRequest request, String canonicalKey) {
        Map<String, String> properties = new HashMap<>(MaterialCategoryUtils.sanitizeProperties(request.getProperties()));
        properties.putIfAbsent("GB5310牌号", canonicalKey);
        return properties;
    }

    private Map<String, String> buildPropertiesFromUpdate(
            MaterialLibraryDTOs.UpdateRequest request, MaterialLibraryEntry existing) {
        Map<String, String> properties = new HashMap<>(MaterialCategoryUtils.sanitizeProperties(request.getProperties()));
        if (existing != null && existing.getProperties() != null) {
            for (String key : MaterialCategoryUtils.PRESERVE_ON_USER_UPDATE_KEYS) {
                if (!properties.containsKey(key)) {
                    String preserved = existing.getProperties().get(key);
                    if (preserved != null && !preserved.trim().isEmpty()) {
                        properties.put(key, preserved.trim());
                    }
                }
            }
        }
        return properties;
    }

    private Map<String, String> resolveDisplayProperties(MaterialLibraryEntry entry) {
        if (STATUS_PENDING.equals(entry.getStatus())
                && entry.getApprovedSnapshot() != null
                && !entry.getApprovedSnapshot().isEmpty()
                && (MOD_UPDATE.equals(entry.getModificationType()) || MOD_DELETE.equals(entry.getModificationType()))) {
            return MaterialCategoryUtils.copyProperties(entry.getApprovedSnapshot());
        }
        return entry.getProperties() == null ? Map.of() : MaterialCategoryUtils.copyProperties(entry.getProperties());
    }

    private void appendLog(Long entryId, String action, User actor, String comment) {
        MaterialApprovalLog log = new MaterialApprovalLog();
        log.setEntryId(entryId);
        log.setAction(action);
        log.setActorUserId(actor.getId());
        log.setActorUserName(resolveDisplayName(actor));
        log.setComment(comment);
        logRepository.save(log);
    }

    private String resolveDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getUserName();
    }

    private MaterialLibraryDTOs.ListItem toSubmissionListItem(MaterialLibraryEntry entry) {
        MaterialLibraryDTOs.ListItem item = toListItem(entry, resolveDisplayProperties(entry));
        item.setSubmissionStatus(resolveSubmissionStatus(entry));
        return item;
    }

    private String resolveSubmissionStatus(MaterialLibraryEntry entry) {
        if (STATUS_PENDING.equals(entry.getStatus())) {
            return STATUS_PENDING;
        }
        if (STATUS_REJECTED.equals(entry.getStatus())) {
            return STATUS_REJECTED;
        }
        if (STATUS_DELETED.equals(entry.getStatus())) {
            return STATUS_DELETED;
        }
        if (STATUS_APPROVED.equals(entry.getStatus()) && hasReviewComment(entry)) {
            return STATUS_REJECTED;
        }
        return STATUS_APPROVED;
    }

    private static boolean hasReviewComment(MaterialLibraryEntry entry) {
        String comment = entry.getReviewComment();
        return comment != null && !comment.trim().isEmpty();
    }

    private MaterialLibraryDTOs.ListItem toListItem(MaterialLibraryEntry entry, Map<String, String> displayProps) {
        MaterialLibraryDTOs.ListItem item = new MaterialLibraryDTOs.ListItem();
        item.setId(entry.getId());
        item.setMaterialKey(entry.getMaterialKey());
        item.setPrimaryCategory(entry.getPrimaryCategory());
        item.setStatus(entry.getStatus());
        item.setSource(entry.getSource());
        item.setModificationType(entry.getModificationType());
        item.setProperties(displayProps == null ? Map.of() : new HashMap<>(displayProps));
        item.setPendingChange(STATUS_PENDING.equals(entry.getStatus()));
        item.setSubmittedByUserName(entry.getSubmittedByUserName());
        item.setReviewedByUserName(entry.getReviewedByUserName());
        item.setReviewComment(entry.getReviewComment());
        item.setCreatedAt(entry.getCreatedAt());
        item.setReviewedAt(entry.getReviewedAt());
        return item;
    }

    private MaterialLibraryDTOs.ApprovalLogItem toLogItem(MaterialApprovalLog log) {
        MaterialLibraryDTOs.ApprovalLogItem item = new MaterialLibraryDTOs.ApprovalLogItem();
        item.setId(log.getId());
        item.setAction(log.getAction());
        item.setActorUserName(log.getActorUserName());
        item.setComment(log.getComment());
        item.setCreatedAt(log.getCreatedAt());
        return item;
    }
}
