/**
 * 将多条编号压缩为展示串（与后端 LocationNumberCompressor 规则一致）：
 * - 1,2,3,5,6,9 → 1~3，5~6，9
 * - 纯数字 1～20（含 9→10 变宽）→ 1~20；乱序如 1,2,3,5,6,7,4 → 1~7
 * - W1-R1-1,W1-R1-2,W1-R1-3 → W1-R1-1~3
 * - W1-R1-1~3，W1-R2-1~3（尾段完全一致）→ W1-R1~2
 * - H5前-1,H10-前1,H5-2,H5后-3 → H5（扁平基准折叠）
 * - 最终列表中 H5,H6,H7（扁平、无 -/~/_）→ H5~H7
 * - 1 与 01（纯数字）不因数值相等合并；同宽度时可 01~02
 * - 合并后按前缀分组排序：字母组 A→Z，其它组 A→Z，纯数字组最后
 */

function normalizeToken(s: string): string {
  if (s == null || s === '') return '';
  const n = s.normalize('NFKC').trim();
  if (!n) return '';
  const parts: string[] = [];
  for (let i = 0; i < n.length; ) {
    const cp = n.codePointAt(i)!;
    const ch = String.fromCodePoint(cp);
    if (!/\s/u.test(ch)) {
      parts.push(ch);
    }
    i += cp > 0xffff ? 2 : 1;
  }
  return parts.join('').toUpperCase();
}

function stripAllHan(s: string): string {
  if (!s) return '';
  return s.replace(/\p{Script=Han}/gu, '');
}

function trimCanonicalSeparators(s: string): string {
  if (!s) return '';
  let start = 0;
  let end = s.length;
  while (start < end) {
    const cp = s.codePointAt(start)!;
    if (cp === 0x2d || cp === 0x5f) {
      start += cp > 0xffff ? 2 : 1;
    } else {
      break;
    }
  }
  while (end > start) {
    const cp = codePointBefore(s, end);
    if (cp === 0x2d || cp === 0x5f) {
      end -= cp > 0xffff ? 2 : 1;
    } else {
      break;
    }
  }
  return s.slice(start, end);
}

function codePointBefore(s: string, pos: number): number {
  if (pos <= 0 || pos > s.length) return NaN;
  const c = s.charCodeAt(pos - 1);
  if (c >= 0xdc00 && c <= 0xdfff && pos >= 2) {
    const hi = s.charCodeAt(pos - 2);
    if (hi >= 0xd800 && hi <= 0xdbff) {
      return (hi - 0xd800) * 0x400 + (c - 0xdc00) + 0x10000;
    }
  }
  return s.codePointAt(pos - 1)!;
}

function isFlatCanonicalBase(canonical: string): boolean {
  if (!canonical) return false;
  for (let i = 0; i < canonical.length; ) {
    const cp = canonical.codePointAt(i)!;
    if (cp === 0x2d || cp === 0x5f) return false;
    i += cp > 0xffff ? 2 : 1;
  }
  return true;
}

function endsWithSepBeforeDigitSuffix(prefix: string): boolean {
  if (!prefix) return false;
  const cp = codePointBefore(prefix, prefix.length);
  return cp === 0x2d || cp === 0x5f;
}

function qualifiesForCanonicalCollapse(prefix: string): boolean {
  if (!prefix) return false;
  if (endsWithSepBeforeDigitSuffix(prefix)) return true;
  if (/\p{Script=Han}/u.test(prefix)) return true;
  const stripped = stripAllHan(prefix);
  for (let i = 0; i < stripped.length; ) {
    const cp = stripped.codePointAt(i)!;
    if (cp === 0x2d || cp === 0x5f) return true;
    i += cp > 0xffff ? 2 : 1;
  }
  return false;
}

function safeParseLong(tail: string): number {
  const n = parseInt(tail, 10);
  return Number.isNaN(n) ? Number.MIN_SAFE_INTEGER : n;
}

type OrdSeg = { text: string; order: number };

const PURE_DIGIT_SEGMENT = /^[0-9]+(~[0-9]+)?$/;
const LEADING_LETTERS = /^([A-Za-z]+)/;

/** 组间键：0 字母前缀、1 其它、2 纯数字 */
function sortGroupKey(text: string): string {
  if (text == null || text === '') return '1\u0000';
  if (PURE_DIGIT_SEGMENT.test(text)) return '2\u0000NUM';
  const m = text.match(LEADING_LETTERS);
  if (m) return `0\u0000${m[1].toUpperCase()}`;
  return `1\u0000${text.toUpperCase()}`;
}

function sortOutputSegments(segments: OrdSeg[]): OrdSeg[] {
  if (segments.length < 2) return segments.slice();
  return segments.slice().sort((a, b) => {
    const gk = sortGroupKey(a.text).localeCompare(sortGroupKey(b.text));
    return gk !== 0 ? gk : a.order - b.order;
  });
}

function formatRangeSegment(prefix: string, startTail: string, endTail: string): string {
  if (startTail === endTail) {
    return `${prefix}${startTail}`;
  }
  return `${prefix}${startTail}~${endTail}`;
}

/**
 * 二次压缩：当多个段满足 `base + mid + sep + tailRange` 且 tailRange 相同、mid 连续时，
 * 例如 `W1-R1-1~3，W1-R2-1~3` → `W1-R1~2`。
 */
function mergeBySameTailRangeOrd(segments: OrdSeg[]): OrdSeg[] {
  const pattern = /^(.*?)(\d+)([^\d])(\d+~\d+)$/;
  type GroupData = {
    indices: number[];
    mids: number[];
    orders: number[];
    basePrefix: string;
    tailRange: string;
  };
  const groups = new Map<string, GroupData>();

  segments.forEach((seg, idx) => {
    const m = seg.text.match(pattern);
    if (!m) return;
    const mid = parseInt(m[2], 10);
    if (Number.isNaN(mid)) return;
    const basePrefix = m[1];
    const tailRange = m[4];
    const key = `${basePrefix}@@${tailRange}`;
    const g = groups.get(key) ?? {
      indices: [],
      mids: [],
      orders: [],
      basePrefix,
      tailRange,
    };
    g.indices.push(idx);
    g.mids.push(mid);
    g.orders.push(seg.order);
    groups.set(key, g);
  });

  const replacementAtFirstIndex = new Map<number, OrdSeg>();
  const skipIndices = new Set<number>();

  for (const g of groups.values()) {
    const n = g.indices.length;
    if (n < 2) continue;

    type Pair = { segIdx: number; mid: number; order: number };
    const pairs: Pair[] = [];
    for (let i = 0; i < n; i++) {
      pairs.push({ segIdx: g.indices[i], mid: g.mids[i], order: g.orders[i] });
    }
    pairs.sort((a, b) => (a.mid !== b.mid ? a.mid - b.mid : a.segIdx - b.segIdx));

    let runStart = 0;
    while (runStart < pairs.length) {
      let runEnd = runStart;
      while (runEnd + 1 < pairs.length && pairs[runEnd + 1].mid === pairs[runEnd].mid + 1) {
        runEnd++;
      }
      const runLen = runEnd - runStart + 1;
      if (runLen >= 2) {
        const firstMid = pairs[runStart].mid;
        const lastMid = pairs[runEnd].mid;
        const midRange =
          firstMid === lastMid ? `${firstMid}` : `${firstMid}~${lastMid}`;
        let anchor = Number.MAX_SAFE_INTEGER;
        let minOrder = Number.MAX_SAFE_INTEGER;
        for (let k = runStart; k <= runEnd; k++) {
          anchor = Math.min(anchor, pairs[k].segIdx);
          minOrder = Math.min(minOrder, pairs[k].order);
        }
        replacementAtFirstIndex.set(anchor, { text: `${g.basePrefix}${midRange}`, order: minOrder });
        for (let k = runStart; k <= runEnd; k++) {
          const segIdx = pairs[k].segIdx;
          if (segIdx !== anchor) skipIndices.add(segIdx);
        }
      }
      runStart = runEnd + 1;
    }
  }

  const out: OrdSeg[] = [];
  segments.forEach((seg, idx) => {
    if (replacementAtFirstIndex.has(idx)) {
      out.push(replacementAtFirstIndex.get(idx)!);
      return;
    }
    if (skipIndices.has(idx)) return;
    out.push(seg);
  });
  return out;
}

function containsFlatMergeBreaker(t: string): boolean {
  if (t == null || t === '') return true;
  for (let i = 0; i < t.length; ) {
    const cp = t.codePointAt(i)!;
    if (cp === 0x7e || cp === 0x2d || cp === 0x5f) return true;
    i += cp > 0xffff ? 2 : 1;
  }
  return false;
}

type TailEntry = { num: number; tail: string; order: number; originalText: string };

/**
 * 末段合并：H5,H6,H7 → H5~H7；含 ~、-、_ 的段（如 W1-R1~2）保持原样。
 */
function mergeFlatPrefixedNumberRuns(segments: OrdSeg[]): OrdSeg[] {
  if (segments.length < 2) {
    return segments.slice();
  }
  const trailing = /^(.*?)(\d+)$/;
  const ineligible: OrdSeg[] = [];
  const byPrefix = new Map<string, TailEntry[]>();
  for (const seg of segments) {
    const t = seg.text;
    if (containsFlatMergeBreaker(t)) {
      ineligible.push(seg);
      continue;
    }
    const m = t.match(trailing);
    if (!m || m[1] === '') {
      ineligible.push(seg);
      continue;
    }
    const pfx = m[1];
    const tail = m[2];
    const num = safeParseLong(tail);
    if (num === Number.MIN_SAFE_INTEGER) {
      ineligible.push(seg);
      continue;
    }
    const arr = byPrefix.get(pfx) ?? [];
    arr.push({ num, tail, order: seg.order, originalText: t });
    byPrefix.set(pfx, arr);
  }
  const mergedOut: OrdSeg[] = [];
  for (const [pfx, group] of byPrefix) {
    group.sort((a, b) => (a.num !== b.num ? a.num - b.num : a.order - b.order));
    const uniq: TailEntry[] = [];
    for (const e of group) {
      const last = uniq[uniq.length - 1];
      if (last && last.num === e.num) {
        if (e.order < last.order) {
          uniq[uniq.length - 1] = e;
        }
        continue;
      }
      uniq.push(e);
    }
    let runStart = 0;
    while (runStart < uniq.length) {
      let runEnd = runStart;
      while (runEnd + 1 < uniq.length && uniq[runEnd + 1].num === uniq[runEnd].num + 1) {
        runEnd++;
      }
      const runLen = runEnd - runStart + 1;
      if (runLen >= 2) {
        const a = uniq[runStart];
        const b = uniq[runEnd];
        let minOrd = Number.MAX_SAFE_INTEGER;
        for (let k = runStart; k <= runEnd; k++) {
          minOrd = Math.min(minOrd, uniq[k].order);
        }
        const rangeText =
          a.originalText === b.originalText
            ? a.originalText
            : `${a.originalText}~${b.originalText}`;
        mergedOut.push({ text: rangeText, order: minOrd });
      } else {
        const one = uniq[runStart];
        mergedOut.push({ text: one.originalText, order: one.order });
      }
      runStart = runEnd + 1;
    }
  }
  const combined = [...ineligible, ...mergedOut];
  combined.sort((a, b) => a.order - b.order);
  return combined;
}

export function compressLocationNumbers(tokens: string[]): string {
  const cleaned: string[] = [];
  for (const t of tokens) {
    if (t == null) continue;
    const parts = String(t)
      .trim()
      .split(/[，,\s;]+/)
      .map((s) => normalizeToken(s.trim()))
      .filter((s) => s.length > 0);
    cleaned.push(...parts);
  }
  if (cleaned.length === 0) {
    return '';
  }

  const trailing = /^(.*?)(\d+)$/;
  type Parsed = { prefix: string; tailStr: string; firstIdx: number };

  const canonicalFirstIdx = new Map<string, number>();
  const canonicalKeyOrder: string[] = [];
  const prefixFirstIdx = new Map<string, number>();
  const rawFirstIdx = new Map<string, number>();
  const parsed: Parsed[] = [];
  const raw: string[] = [];

  cleaned.forEach((t, rowIdx) => {
    const m = t.match(trailing);
    if (m) {
      const prefix = m[1];
      const tailStr = m[2];
      const canonical = trimCanonicalSeparators(stripAllHan(prefix));
      if (
        canonical.length > 0 &&
        isFlatCanonicalBase(canonical) &&
        qualifiesForCanonicalCollapse(prefix)
      ) {
        if (!canonicalFirstIdx.has(canonical)) {
          canonicalFirstIdx.set(canonical, rowIdx);
          canonicalKeyOrder.push(canonical);
        }
        return;
      }
      if (!prefixFirstIdx.has(prefix)) {
        prefixFirstIdx.set(prefix, rowIdx);
      }
      parsed.push({ prefix, tailStr, firstIdx: rowIdx });
      return;
    }
    if (!rawFirstIdx.has(t)) {
      rawFirstIdx.set(t, rowIdx);
    }
    raw.push(t);
  });

  parsed.sort((a, b) => {
    const pc = a.prefix.localeCompare(b.prefix);
    if (pc !== 0) return pc;
    if (a.prefix !== '') {
      const na = safeParseLong(a.tailStr);
      const nb = safeParseLong(b.tailStr);
      if (na !== nb) return na - nb;
      return a.tailStr.localeCompare(b.tailStr);
    }
    const na = safeParseLong(a.tailStr);
    const nb = safeParseLong(b.tailStr);
    if (na !== nb) return na - nb;
    const len = a.tailStr.length - b.tailStr.length;
    if (len !== 0) return len;
    const t = a.tailStr.localeCompare(b.tailStr);
    if (t !== 0) return t;
    return a.firstIdx - b.firstIdx;
  });

  const deduped: Parsed[] = [];
  for (const p of parsed) {
    const last = deduped[deduped.length - 1];
    if (last && last.prefix === p.prefix && last.tailStr === p.tailStr) {
      continue;
    }
    deduped.push(p);
  }

  const chunksByPrefix = new Map<string, OrdSeg[]>();
  let i = 0;
  while (i < deduped.length) {
    const prefix = deduped[i].prefix;
    let j = i + 1;
    while (j < deduped.length && deduped[j].prefix === prefix) {
      j++;
    }
    let startTail = deduped[i].tailStr;
    let prevTail = startTail;
    let prevNum = safeParseLong(prevTail);
    let chunkFirstIdx = deduped[i].firstIdx;
    for (let k = i + 1; k < j; k++) {
      const currTail = deduped[k].tailStr;
      const currNum = safeParseLong(currTail);
      const consecutive = currNum === prevNum + 1;
      if (consecutive) {
        prevTail = currTail;
        prevNum = currNum;
        chunkFirstIdx = Math.min(chunkFirstIdx, deduped[k].firstIdx);
      } else {
        const arr = chunksByPrefix.get(prefix) ?? [];
        arr.push({ text: formatRangeSegment(prefix, startTail, prevTail), order: chunkFirstIdx });
        chunksByPrefix.set(prefix, arr);
        startTail = currTail;
        prevTail = currTail;
        prevNum = currNum;
        chunkFirstIdx = deduped[k].firstIdx;
      }
    }
    const arr = chunksByPrefix.get(prefix) ?? [];
    arr.push({ text: formatRangeSegment(prefix, startTail, prevTail), order: chunkFirstIdx });
    chunksByPrefix.set(prefix, arr);
    i = j;
  }

  const prefixesOrdered = [...chunksByPrefix.keys()].sort(
    (a, b) => (prefixFirstIdx.get(a) ?? 1e9) - (prefixFirstIdx.get(b) ?? 1e9),
  );

  const parsedOut: OrdSeg[] = [];
  for (const p of prefixesOrdered) {
    parsedOut.push(...(chunksByPrefix.get(p) ?? []));
  }
  const secondPass = mergeBySameTailRangeOrd(parsedOut);

  const rawDistinct = [...new Set(raw)];
  rawDistinct.sort((a, b) => (rawFirstIdx.get(a) ?? 1e9) - (rawFirstIdx.get(b) ?? 1e9));

  const all: OrdSeg[] = [];
  for (const key of canonicalKeyOrder) {
    all.push({ text: key, order: canonicalFirstIdx.get(key) ?? 1e9 });
  }
  all.push(...secondPass);
  for (const r of rawDistinct) {
    all.push({ text: r, order: rawFirstIdx.get(r) ?? 1e9 });
  }
  all.sort((a, b) => a.order - b.order);
  const flatMerged = sortOutputSegments(mergeFlatPrefixedNumberRuns(all));

  return flatMerged.map((o) => o.text).join('，');
}
