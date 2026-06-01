package com.reportweb.service;

import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.repository.ProjectComponentRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 多选部件：解析 ID 列表、拼接规格/材质（与 Word 中单部件格式一致）。
 */
@Component
public class ReportComponentMergeHelper {

    public static final String SPEC_SEP = "/";

    public List<Integer> resolveComponentIds(Report report) {
        if (report.getProjectComponentIds() != null && !report.getProjectComponentIds().isEmpty()) {
            return new ArrayList<>(report.getProjectComponentIds());
        }
        if (report.getProjectComponentId() != null) {
            return new ArrayList<>(Collections.singletonList(report.getProjectComponentId()));
        }
        return new ArrayList<>();
    }

    public List<ProjectComponent> loadOrdered(ProjectComponentRepository repo, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProjectComponent> out = new ArrayList<>();
        for (Integer id : ids) {
            if (id == null) {
                continue;
            }
            repo.findById(id).ifPresent(out::add);
        }
        return out;
    }

    /** 与 Word 中 Φ 管径+壁厚 常见块一致 */
    public String formatSpecPhi(ProjectComponent c) {
        if (c == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        if (c.getPipeDiameter() != null && !c.getPipeDiameter().isEmpty()) {
            b.append("Φ").append(c.getPipeDiameter());
        }
        if (c.getWallThickness() != null && !c.getWallThickness().isEmpty()) {
            b.append("mm × ").append(c.getWallThickness()).append("mm");
        }
        return b.length() > 0 ? b.toString() : "";
    }

    /** 螺栓等：M + 直径 + 壁厚 */
    public String formatSpecM(ProjectComponent c) {
        if (c == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        if (c.getPipeDiameter() != null && !c.getPipeDiameter().isEmpty()) {
            b.append("M").append(c.getPipeDiameter());
        }
        if (c.getWallThickness() != null && !c.getWallThickness().isEmpty()) {
            b.append("mm × ").append(c.getWallThickness()).append("mm");
        }
        return b.length() > 0 ? b.toString() : "";
    }

    public String formatSpecWithPrefix(ProjectComponent c, String diameterPrefix) {
        if (c == null) {
            return "";
        }
        String prefix = (diameterPrefix != null && !diameterPrefix.isEmpty()) ? diameterPrefix : "M";
        StringBuilder sb = new StringBuilder();
        if (c.getPipeDiameter() != null && !c.getPipeDiameter().isEmpty()) {
            sb.append(prefix).append(c.getPipeDiameter());
        }
        if (c.getWallThickness() != null && !c.getWallThickness().isEmpty()) {
            sb.append("mm × ").append(c.getWallThickness()).append("mm");
        }
        return sb.length() > 0 ? sb.toString() : "";
    }

    public String mergeSpecsPhi(List<ProjectComponent> comps) {
        return joinSpecParts(comps, this::formatSpecPhi);
    }

    public String mergeSpecsM(List<ProjectComponent> comps) {
        return joinSpecParts(comps, this::formatSpecM);
    }

    public String mergeSpecsWithPrefix(List<ProjectComponent> comps, String diameterPrefix) {
        return joinSpecParts(comps, c -> formatSpecWithPrefix(c, diameterPrefix));
    }

    /**
     * 按部件保存的规格前缀（PHI|M|NONE，null=按名称自动）与牙距拼接规格，多部件用 {@link #SPEC_SEP} 连接。
     * 与 Word/检测内容行中「部件规格」展示规则一致。
     */
    public String mergeSpecsUnified(List<ProjectComponent> comps) {
        return joinSpecParts(comps, this::formatSpecUnified);
    }

    /** 保存前校验并规范化规格前缀：null/空=自动；否则 PHI|M|NONE（不区分大小写）。 */
    public static String normalizeSpecPrefixForSave(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        String u = t.toUpperCase(Locale.ROOT);
        if ("PHI".equals(u) || "Φ".equals(t)) {
            return "PHI";
        }
        if ("M".equals(u)) {
            return "M";
        }
        if ("NONE".equals(u)) {
            return "NONE";
        }
        throw new IllegalArgumentException("规格前缀仅支持 PHI、M、NONE，或留空表示按部件名称自动");
    }

    /** 保存前规范化牙距：空为 null；超长则抛错。 */
    public static String normalizeThreadPitchForSave(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > 50) {
            throw new IllegalArgumentException("牙距长度不能超过50个字符");
        }
        return t;
    }

    /**
     * 单部件规格：前缀由 {@link ProjectComponent#getSpecPrefix()} 或名称（螺栓/螺帽→M，否则Φ）决定；
     * 管径+壁厚格式与 {@link #formatSpecPhi}/{@link #formatSpecM} 一致；有牙距时追加 {@code × 牙距}。
     */
    public String formatSpecUnified(ProjectComponent c) {
        if (c == null) {
            return "";
        }
        String mode = resolveSpecPrefixMode(c);
        String pd = c.getPipeDiameter() != null ? c.getPipeDiameter().trim() : "";
        String wt = c.getWallThickness() != null ? c.getWallThickness().trim() : "";
        String pitch = c.getThreadPitch() != null ? c.getThreadPitch().trim() : "";

        StringBuilder sb = new StringBuilder();
        if (!pd.isEmpty()) {
            switch (mode) {
                case "PHI":
                    sb.append("Φ").append(pd);
                    break;
                case "M":
                    sb.append("M").append(pd);
                    break;
                case "NONE":
                    sb.append(pd);
                    break;
                default:
                    sb.append("Φ").append(pd);
            }
        }
        if (!wt.isEmpty()) {
            sb.append("mm × ").append(wt).append("mm");
        }
        if (!pitch.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" × ");
            }
            sb.append(pitch);
        }
        return sb.length() > 0 ? sb.toString() : "";
    }

    /** PHI|M|NONE；null/空表示按名称自动。 */
    private String resolveSpecPrefixMode(ProjectComponent c) {
        String raw = c.getSpecPrefix();
        if (raw == null || raw.isBlank()) {
            String nm = c.getComponentName() != null ? c.getComponentName() : "";
            if (nm.contains("螺栓") || nm.contains("螺帽")) {
                return "M";
            }
            return "PHI";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("PHI".equals(u) || "Φ".equals(raw.trim())) {
            return "PHI";
        }
        if ("M".equals(u)) {
            return "M";
        }
        if ("NONE".equals(u)) {
            return "NONE";
        }
        return "PHI";
    }

    private String joinSpecParts(List<ProjectComponent> comps, java.util.function.Function<ProjectComponent, String> formatter) {
        if (comps == null || comps.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (ProjectComponent c : comps) {
            String s = formatter.apply(c);
            if (s != null && !s.isEmpty()) {
                parts.add(s);
            }
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join(SPEC_SEP, parts);
    }

    /** 按顺序拼接材质，去掉连续重复 */
    public String mergeMaterials(List<ProjectComponent> comps) {
        if (comps == null || comps.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (ProjectComponent c : comps) {
            String m = c.getMaterial() != null ? c.getMaterial().trim() : "";
            if (m.isEmpty()) {
                continue;
            }
            if (!parts.isEmpty() && parts.get(parts.size() - 1).equals(m)) {
                continue;
            }
            parts.add(m);
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join(SPEC_SEP, parts);
    }

    public ProjectComponent firstOrNull(List<ProjectComponent> comps) {
        if (comps == null || comps.isEmpty()) {
            return null;
        }
        return comps.get(0);
    }

    /** DTO：优先使用 projectComponentIds，否则单 ID */
    public List<Integer> resolveIdsFromDto(List<Integer> projectComponentIds, Integer projectComponentId) {
        if (projectComponentIds != null && !projectComponentIds.isEmpty()) {
            List<Integer> out = new ArrayList<>();
            for (Integer id : projectComponentIds) {
                if (id != null && id > 0 && !out.contains(id)) {
                    out.add(id);
                }
            }
            return out;
        }
        if (projectComponentId != null && projectComponentId > 0) {
            return new ArrayList<>(Collections.singletonList(projectComponentId));
        }
        return new ArrayList<>();
    }

    /**
     * 按顺序加载并校验：同属 projectId、部件名称一致。
     */
    public List<ProjectComponent> validateAndLoadOrdered(Integer projectId, List<Integer> ids,
                                                         ProjectComponentRepository repo) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProjectComponent> out = new ArrayList<>();
        String firstName = null;
        for (Integer id : ids) {
            ProjectComponent c = repo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("部件不存在"));
            if (!Objects.equals(c.getProjectId(), projectId)) {
                throw new IllegalArgumentException("所选部件不属于当前项目");
            }
            String nm = c.getComponentName() != null ? c.getComponentName().trim() : "";
            if (firstName == null) {
                firstName = nm;
            } else if (!firstName.equals(nm)) {
                throw new IllegalArgumentException("所选部件名称必须一致");
            }
            out.add(c);
        }
        return out;
    }
}
