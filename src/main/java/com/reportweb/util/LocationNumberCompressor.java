package com.reportweb.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将检测数据中提取的多条编号压缩展示，例如 {@code 1,2,3,5,6,9} → {@code 1~3，5~6，9}；
 * 纯数字 {@code 1…20}（含 9 与 10 位数不同）收成 {@code 1~20}；乱序如 {@code 1,2,3,5,6,7,4} → {@code 1~7}（先按数值排序再收区间）。
 * {@code 1-焊-1,1-焊-2,1-母-1,1-母-2}（行序可乱）→ {@code 1-焊-1~2，1-母-1~2}（按各行中前缀首次出现顺序输出）。
 * <p>
 * 「基准位号」折叠：凡匹配 {@code 前缀-末段数字} 且去掉汉字后的前缀为<strong>扁平</strong>（不含 {@code -}/{@code _}）时，
 * 只输出该基准（如 {@code H5前-1}、{@code H5-2} → {@code H5}），不再展示尾段区间；{@code W1-R1-1} 等复合前缀仍走原有压缩。
 * <p>
 * 末段再合并：最终片段列表中扁平「前缀+数字」（段内无 {@code ~}/{@code -}/{@code _}）按相同前缀、数值连续收成区间，
 * 例如 {@code H5，H6，H7} → {@code H5~7}。
 * <p>
 * 二次压缩（字母无关）：凡能匹配 {@code base + 数字(mid) + 单非数字(sep) + 尾段(数字~数字)} 的段（如 {@code W1-R1-1~3}、{@code 19R1-W5-1~3}、{@code PROJ-Q4-1~3}），
 * 在相同 {@code base} 与相同尾段下将连续 {@code mid} 压成短式（如 {@code W1-R1~2}、{@code 19R1-W5~15}）；{@code mid} 在断点处拆成多段极大连续子序列后分别合并。
 * <p>
 * 比较与合并前对每条编号做 {@link #normalizeToken(String)}：NFKC、去空白、拉丁字母 {@link Locale#ROOT} 大写，
 * 使全角半角、大小写、空格差异视为同一编号。
 */
public final class LocationNumberCompressor {

    private static final Pattern TRAILING_INT = Pattern.compile("^(.*?)(\\d+)$");
    private static final Pattern SECOND_PASS_PATTERN = Pattern.compile("^(.*?)(\\d+)([^\\d])(\\d+~\\d+)$");
    private static final Pattern IS_HAN = Pattern.compile("\\p{IsHan}");
    /** 一格内可能用中英文逗号/分号/空白写了多个编号，须先拆成独立 token 再压缩 */
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[，,\\s;]+");

    private LocationNumberCompressor() {
    }

    /**
     * @param tokens 各行编号，已 trim；空串忽略；遍历顺序视为检测数据行序
     * @return 中文逗号分隔的压缩串；无有效输入返回空串
     */
    public static String compressJoined(Iterable<String> tokens) {
        if (tokens == null) {
            return "";
        }
        List<String> cleaned = new ArrayList<>();
        for (String t : tokens) {
            if (t == null) {
                continue;
            }
            String s = t.trim();
            if (s.isEmpty()) {
                continue;
            }
            for (String part : TOKEN_SPLIT.split(s)) {
                String p = normalizeToken(part);
                if (!p.isEmpty()) {
                    cleaned.add(p);
                }
            }
        }
        if (cleaned.isEmpty()) {
            return "";
        }

        LinkedHashMap<String, Integer> canonicalFirstIdx = new LinkedHashMap<>();
        Map<String, Integer> prefixFirstIdx = new HashMap<>();
        Map<String, Integer> rawFirstIdx = new HashMap<>();
        List<Parsed> parsed = new ArrayList<>();
        List<String> raw = new ArrayList<>();

        int rowIdx = 0;
        for (String t : cleaned) {
            Matcher m = TRAILING_INT.matcher(t);
            if (!m.matches()) {
                rawFirstIdx.putIfAbsent(t, rowIdx);
                raw.add(t);
                rowIdx++;
                continue;
            }
            String prefix = m.group(1);
            String tailStr = m.group(2);
            String canonical = trimCanonicalSeparators(stripAllHan(prefix));
            if (!canonical.isEmpty()
                    && isFlatCanonicalBase(canonical)
                    && endsWithSepBeforeDigitSuffix(prefix)) {
                canonicalFirstIdx.putIfAbsent(canonical, rowIdx);
                rowIdx++;
                continue;
            }
            prefixFirstIdx.putIfAbsent(prefix, rowIdx);
            parsed.add(new Parsed(prefix, tailStr, rowIdx));
            rowIdx++;
        }

        parsed.sort(buildParsedComparator());

        List<Parsed> deduped = new ArrayList<>();
        for (Parsed p : parsed) {
            if (deduped.isEmpty()) {
                deduped.add(p);
                continue;
            }
            Parsed last = deduped.get(deduped.size() - 1);
            if (last.prefix.equals(p.prefix) && last.tailStr.equals(p.tailStr)) {
                continue;
            }
            deduped.add(p);
        }
        parsed = deduped;

        Map<String, List<OrdSeg>> chunksByPrefix = new HashMap<>();
        int i = 0;
        while (i < parsed.size()) {
            String prefix = parsed.get(i).prefix;
            int j = i + 1;
            while (j < parsed.size() && prefix.equals(parsed.get(j).prefix)) {
                j++;
            }
            String startTail = parsed.get(i).tailStr;
            String prevTail = startTail;
            long prevNum = safeParseLong(prevTail);
            int chunkFirstIdx = parsed.get(i).firstIdx;
            for (int k = i + 1; k < j; k++) {
                String currTail = parsed.get(k).tailStr;
                long currNum = safeParseLong(currTail);
                boolean consecutive = currNum == prevNum + 1;
                if (consecutive) {
                    prevTail = currTail;
                    prevNum = currNum;
                    chunkFirstIdx = Math.min(chunkFirstIdx, parsed.get(k).firstIdx);
                } else {
                    chunksByPrefix.computeIfAbsent(prefix, x -> new ArrayList<>())
                            .add(new OrdSeg(formatRangeSegment(prefix, startTail, prevTail), chunkFirstIdx));
                    startTail = currTail;
                    prevTail = currTail;
                    prevNum = currNum;
                    chunkFirstIdx = parsed.get(k).firstIdx;
                }
            }
            chunksByPrefix.computeIfAbsent(prefix, x -> new ArrayList<>())
                    .add(new OrdSeg(formatRangeSegment(prefix, startTail, prevTail), chunkFirstIdx));
            i = j;
        }

        List<String> prefixesOrdered = new ArrayList<>(chunksByPrefix.keySet());
        prefixesOrdered.sort(Comparator.comparingInt(p -> prefixFirstIdx.getOrDefault(p, Integer.MAX_VALUE)));

        List<OrdSeg> out = new ArrayList<>();
        for (String p : prefixesOrdered) {
            out.addAll(chunksByPrefix.get(p));
        }
        out = mergeBySameTailRangeOrd(out);

        LinkedHashSet<String> rawDistinct = new LinkedHashSet<>(raw);
        List<String> rawSorted = new ArrayList<>(rawDistinct);
        rawSorted.sort(Comparator.comparingInt(s -> rawFirstIdx.getOrDefault(s, Integer.MAX_VALUE)));

        List<OrdSeg> all = new ArrayList<>();
        for (Map.Entry<String, Integer> e : canonicalFirstIdx.entrySet()) {
            all.add(new OrdSeg(e.getKey(), e.getValue()));
        }
        all.addAll(out);
        for (String r : rawSorted) {
            all.add(new OrdSeg(r, rawFirstIdx.getOrDefault(r, Integer.MAX_VALUE)));
        }
        all.sort(Comparator.comparingInt((OrdSeg o) -> o.order));
        all = mergeFlatPrefixedNumberRuns(all);

        List<String> joined = new ArrayList<>(all.size());
        for (OrdSeg o : all) {
            joined.add(o.text);
        }
        return String.join("，", joined);
    }

    /**
     * 末段合并：输出列表中扁平「前缀+数字」段（无 {@code ~}/{@code -}/{@code _}）按相同前缀、数值连续收成区间，
     * 例如 {@code H5，H6，H7} → {@code H5~7}；{@code W1-R1~2} 等不参与。
     */
    private static List<OrdSeg> mergeFlatPrefixedNumberRuns(List<OrdSeg> segments) {
        if (segments == null || segments.isEmpty()) {
            return segments == null ? new ArrayList<>() : new ArrayList<>(segments);
        }
        if (segments.size() < 2) {
            return new ArrayList<>(segments);
        }
        List<OrdSeg> ineligible = new ArrayList<>();
        Map<String, List<TailEntry>> byPrefix = new HashMap<>();
        for (OrdSeg seg : segments) {
            String t = seg.text;
            if (containsFlatMergeBreaker(t)) {
                ineligible.add(seg);
                continue;
            }
            Matcher m = TRAILING_INT.matcher(t);
            if (!m.matches()) {
                ineligible.add(seg);
                continue;
            }
            String pfx = m.group(1);
            if (pfx.isEmpty()) {
                ineligible.add(seg);
                continue;
            }
            String tail = m.group(2);
            long num = safeParseLong(tail);
            if (num == Long.MIN_VALUE) {
                ineligible.add(seg);
                continue;
            }
            byPrefix.computeIfAbsent(pfx, k -> new ArrayList<>())
                    .add(new TailEntry(num, tail, seg.order, t));
        }
        List<OrdSeg> mergedOut = new ArrayList<>();
        for (Map.Entry<String, List<TailEntry>> en : byPrefix.entrySet()) {
            String pfx = en.getKey();
            List<TailEntry> group = en.getValue();
            group.sort(Comparator.comparingLong((TailEntry e) -> e.num).thenComparingInt(e -> e.order));
            List<TailEntry> uniq = new ArrayList<>();
            for (TailEntry e : group) {
                if (!uniq.isEmpty() && uniq.get(uniq.size() - 1).num == e.num) {
                    TailEntry last = uniq.get(uniq.size() - 1);
                    if (e.order < last.order) {
                        uniq.set(uniq.size() - 1, e);
                    }
                    continue;
                }
                uniq.add(e);
            }
            int runStart = 0;
            while (runStart < uniq.size()) {
                int runEnd = runStart;
                while (runEnd + 1 < uniq.size()
                        && uniq.get(runEnd + 1).num == uniq.get(runEnd).num + 1) {
                    runEnd++;
                }
                int runLen = runEnd - runStart + 1;
                if (runLen >= 2) {
                    TailEntry a = uniq.get(runStart);
                    TailEntry b = uniq.get(runEnd);
                    int minOrd = Integer.MAX_VALUE;
                    for (int k = runStart; k <= runEnd; k++) {
                        minOrd = Math.min(minOrd, uniq.get(k).order);
                    }
                    mergedOut.add(new OrdSeg(formatRangeSegment(pfx, a.tail, b.tail), minOrd));
                } else {
                    TailEntry one = uniq.get(runStart);
                    mergedOut.add(new OrdSeg(one.originalText, one.order));
                }
                runStart = runEnd + 1;
            }
        }
        List<OrdSeg> combined = new ArrayList<>(ineligible.size() + mergedOut.size());
        combined.addAll(ineligible);
        combined.addAll(mergedOut);
        combined.sort(Comparator.comparingInt(o -> o.order));
        return combined;
    }

    private static boolean containsFlatMergeBreaker(String t) {
        if (t == null || t.isEmpty()) {
            return true;
        }
        for (int i = 0; i < t.length(); ) {
            int cp = t.codePointAt(i);
            if (cp == '~' || cp == '-' || cp == '_') {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    private static Comparator<Parsed> buildParsedComparator() {
        return Comparator.comparing((Parsed a) -> a.prefix)
                .thenComparing((a, b) -> {
                    if (!a.prefix.isEmpty()) {
                        long na = safeParseLong(a.tailStr);
                        long nb = safeParseLong(b.tailStr);
                        int c = Long.compare(na, nb);
                        if (c != 0) {
                            return c;
                        }
                        return a.tailStr.compareTo(b.tailStr);
                    }
                    // 纯数字：按数值排序后再收区间，避免录入顺序如 1,2,3,5,6,7,4 无法收成 1~7
                    long na = safeParseLong(a.tailStr);
                    long nb = safeParseLong(b.tailStr);
                    int c = Long.compare(na, nb);
                    if (c != 0) {
                        return c;
                    }
                    int len = Integer.compare(a.tailStr.length(), b.tailStr.length());
                    if (len != 0) {
                        return len;
                    }
                    int t = a.tailStr.compareTo(b.tailStr);
                    if (t != 0) {
                        return t;
                    }
                    return Integer.compare(a.firstIdx, b.firstIdx);
                });
    }

    private static String formatRangeSegment(String prefix, String startTail, String endTail) {
        if (startTail.equals(endTail)) {
            return prefix + startTail;
        }
        return prefix + startTail + "~" + endTail;
    }

    /**
     * 二次压缩：当多个段可解析为 {@code basePrefix + mid + sep + tailRange}（见 {@link #SECOND_PASS_PATTERN}），且 {@code tailRange} 相同时，
     * 将 {@code mid} 按数值排序后拆成<strong>极大连续子序列</strong>，每个子序列长度 ≥2 时单独合并为 {@code basePrefix + firstMid~lastMid}（短式，不重复拼尾段）。
     * <p>
     * 示例（与具体字母无关）：{@code W1-R1-1~3，W1-R2-1~3} → {@code W1-R1~2}；
     * {@code 19R1-W1-1~3…19R1-W2-1~3} 与 {@code 19R1-W4-1~3…} 之间缺 W3 → {@code 19R1-W1~2} 与 {@code 19R1-W4~15} 两段；
     * 同理 {@code PROJ-Q1-1~3，PROJ-Q2-1~3} 与 {@code PROJ-Q4-1~3…PROJ-Q6-1~3} 之间缺 Q3 → {@code PROJ-Q1~2} 与 {@code PROJ-Q4~6}。
     * <p>
     * 规则：仅对 {@code tailRange} 含 {@code ~} 的段生效；单点 {@code mid} 不做二次合并，保持一次压缩后的原段。
     */
    private static List<OrdSeg> mergeBySameTailRangeOrd(List<OrdSeg> segments) {
        if (segments == null || segments.isEmpty()) {
            return segments == null ? new ArrayList<>() : new ArrayList<>(segments);
        }
        if (segments.size() < 2) {
            return new ArrayList<>(segments);
        }
        Map<String, GroupData> groups = new HashMap<>();

        for (int idx = 0; idx < segments.size(); idx++) {
            OrdSeg ordSeg = segments.get(idx);
            Matcher m = SECOND_PASS_PATTERN.matcher(ordSeg.text);
            if (!m.matches()) {
                continue;
            }
            long mid = Long.parseLong(m.group(2));
            SecondPassParsed parsed = new SecondPassParsed(m.group(1), m.group(4));
            String key = parsed.basePrefix + "@@" + parsed.tailRange;
            GroupData g = groups.computeIfAbsent(key, k -> new GroupData(parsed.basePrefix));
            g.indices.add(idx);
            g.mids.add(mid);
            g.orders.add(ordSeg.order);
        }

        Map<Integer, OrdSeg> replacementAtFirstIndex = new HashMap<>();
        LinkedHashSet<Integer> skipIndices = new LinkedHashSet<>();
        for (GroupData g : groups.values()) {
            int n = g.indices.size();
            if (n < 2) {
                continue;
            }
            List<SegMidOrd> pairs = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                pairs.add(new SegMidOrd(g.indices.get(i), g.mids.get(i), g.orders.get(i)));
            }
            pairs.sort(Comparator.comparingLong((SegMidOrd p) -> p.mid).thenComparingInt(p -> p.segIdx));

            int runStart = 0;
            while (runStart < pairs.size()) {
                int runEnd = runStart;
                while (runEnd + 1 < pairs.size()
                        && pairs.get(runEnd + 1).mid == pairs.get(runEnd).mid + 1) {
                    runEnd++;
                }
                int runLen = runEnd - runStart + 1;
                if (runLen >= 2) {
                    long firstMid = pairs.get(runStart).mid;
                    long lastMid = pairs.get(runEnd).mid;
                    String midRange = firstMid == lastMid ? String.valueOf(firstMid) : firstMid + "~" + lastMid;
                    int anchor = Integer.MAX_VALUE;
                    int minOrder = Integer.MAX_VALUE;
                    for (int k = runStart; k <= runEnd; k++) {
                        SegMidOrd p = pairs.get(k);
                        anchor = Math.min(anchor, p.segIdx);
                        minOrder = Math.min(minOrder, p.order);
                    }
                    replacementAtFirstIndex.put(anchor, new OrdSeg(g.basePrefix + midRange, minOrder));
                    for (int k = runStart; k <= runEnd; k++) {
                        int segIdx = pairs.get(k).segIdx;
                        if (segIdx != anchor) {
                            skipIndices.add(segIdx);
                        }
                    }
                }
                runStart = runEnd + 1;
            }
        }

        List<OrdSeg> out = new ArrayList<>();
        for (int idx = 0; idx < segments.size(); idx++) {
            if (replacementAtFirstIndex.containsKey(idx)) {
                out.add(replacementAtFirstIndex.get(idx));
                continue;
            }
            if (skipIndices.contains(idx)) {
                continue;
            }
            out.add(segments.get(idx));
        }
        return out;
    }

    static String stripAllHan(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return IS_HAN.matcher(s).replaceAll("");
    }

    static String trimCanonicalSeparators(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        int start = 0;
        int end = s.length();
        while (start < end) {
            int cp = s.codePointAt(start);
            if (cp == '-' || cp == '_') {
                start += Character.charCount(cp);
            } else {
                break;
            }
        }
        while (end > start) {
            int cp = s.codePointBefore(end);
            if (cp == '-' || cp == '_') {
                end -= Character.charCount(cp);
            } else {
                break;
            }
        }
        return s.substring(start, end);
    }

    /** 折叠后的基准不得仍含层级分隔符，否则保留 {@code W1-R1-1~3} 类压缩。 */
    private static boolean isFlatCanonicalBase(String canonical) {
        if (canonical == null || canonical.isEmpty()) {
            return false;
        }
        for (int i = 0; i < canonical.length(); ) {
            int cp = canonical.codePointAt(i);
            if (cp == '-' || cp == '_') {
                return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }

    /** {@code Q3} 无前导分隔符，走区间压缩；{@code H5-1} 前缀以 {@code -} 结尾，走基准折叠。 */
    private static boolean endsWithSepBeforeDigitSuffix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return false;
        }
        int cp = prefix.codePointBefore(prefix.length());
        return cp == '-' || cp == '_';
    }

    private static long safeParseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }

    /**
     * NFKC 兼容分解、剔除 Unicode 空白、拉丁字母统一为大写，供合并键与输出一致。
     */
    static String normalizeToken(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFKC);
        if (n.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); ) {
            int cp = n.codePointAt(i);
            if (!Character.isWhitespace(cp)) {
                sb.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    private static final class Parsed {
        final String prefix;
        final String tailStr;
        final int firstIdx;

        Parsed(String prefix, String tailStr, int firstIdx) {
            this.prefix = prefix != null ? prefix : "";
            this.tailStr = tailStr != null ? tailStr : "";
            this.firstIdx = firstIdx;
        }
    }

    private static final class OrdSeg {
        final String text;
        final int order;

        OrdSeg(String text, int order) {
            this.text = text != null ? text : "";
            this.order = order;
        }
    }

    private static final class SecondPassParsed {
        final String basePrefix;
        final String tailRange;

        SecondPassParsed(String basePrefix, String tailRange) {
            this.basePrefix = basePrefix != null ? basePrefix : "";
            this.tailRange = tailRange != null ? tailRange : "";
        }
    }

    private static final class GroupData {
        final String basePrefix;
        final List<Integer> indices = new ArrayList<>();
        final List<Long> mids = new ArrayList<>();
        final List<Integer> orders = new ArrayList<>();

        GroupData(String basePrefix) {
            this.basePrefix = basePrefix;
        }
    }

    private static final class SegMidOrd {
        final int segIdx;
        final long mid;
        final int order;

        SegMidOrd(int segIdx, long mid, int order) {
            this.segIdx = segIdx;
            this.mid = mid;
            this.order = order;
        }
    }

    private static final class TailEntry {
        final long num;
        final String tail;
        final int order;
        final String originalText;

        TailEntry(long num, String tail, int order, String originalText) {
            this.num = num;
            this.tail = tail != null ? tail : "";
            this.order = order;
            this.originalText = originalText != null ? originalText : "";
        }
    }
}
