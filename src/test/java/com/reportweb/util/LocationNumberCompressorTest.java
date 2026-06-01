package com.reportweb.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationNumberCompressorTest {

    @Test
    void compressesSecondPassWhenTailRangeSame() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "W1-R1-1", "W1-R1-2", "W1-R1-3",
                "W1-R2-1", "W1-R2-2", "W1-R2-3"
        ));
        assertEquals("W1-R1~2", out);
    }

    @Test
    void doesNotMergeSecondPassWhenTailRangeDifferent() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "W1-R1-1", "W1-R1-2", "W1-R1-3",
                "W1-R2-1", "W1-R2-2"
        ));
        assertEquals("W1-R1-1~3，W1-R2-1~2", out);
    }

    @Test
    void doesNotMergeSecondPassWhenMiddleNotContinuous() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "W1-R1-1", "W1-R1-2", "W1-R1-3",
                "W1-R3-1", "W1-R3-2", "W1-R3-3"
        ));
        // R1 与 R3 之间缺 R2：两段 mid 各为单点连续子序列，长度 <2，不做二次合并
        assertEquals("W1-R1-1~3，W1-R3-1~3", out);
    }

    /** 断点拆段：同一 base+尾段下 mid 缺号时拆成多段短式（19R1-W1~2 与 19R1-W4~15） */
    @Test
    void secondPassSplitsAtGapIntoMultipleRanges() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "19R1-W1-1", "19R1-W1-2", "19R1-W1-3",
                "19R1-W2-1", "19R1-W2-2", "19R1-W2-3",
                "19R1-W4-1", "19R1-W4-2", "19R1-W4-3",
                "19R1-W5-1", "19R1-W5-2", "19R1-W5-3",
                "19R1-W6-1", "19R1-W6-2", "19R1-W6-3",
                "19R1-W7-1", "19R1-W7-2", "19R1-W7-3",
                "19R1-W8-1", "19R1-W8-2", "19R1-W8-3",
                "19R1-W9-1", "19R1-W9-2", "19R1-W9-3",
                "19R1-W10-1", "19R1-W10-2", "19R1-W10-3",
                "19R1-W11-1", "19R1-W11-2", "19R1-W11-3",
                "19R1-W12-1", "19R1-W12-2", "19R1-W12-3",
                "19R1-W13-1", "19R1-W13-2", "19R1-W13-3",
                "19R1-W14-1", "19R1-W14-2", "19R1-W14-3",
                "19R1-W15-1", "19R1-W15-2", "19R1-W15-3"
        ));
        assertEquals("19R1-W1~2，19R1-W4~15", out);
    }

    @Test
    void keepsMixedRawAndCompressedCompatible() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "1", "2", "3",
                "W1-R1-1", "W1-R1-2", "W1-R1-3",
                "W1-R2-1", "W1-R2-2", "W1-R2-3",
                "X-自定义"
        ));
        assertEquals("1~3，W1-R1~2，X-自定义", out);
    }

    /** 一格内多条「已带尾段」的编号：先按逗号拆 token，再与多行录入一样走二次压缩。 */
    @Test
    void splitsMultipleCodesInOneCellThenSecondPass() {
        String oneCell = String.join("，",
                "19R1-W5-1~3", "19R1-W6-1~3", "19R1-W7-1~3", "19R1-W8-1~3", "19R1-W9-1~3",
                "19R1-W10-1~3", "19R1-W11-1~3", "19R1-W12-1~3", "19R1-W13-1~3", "19R1-W14-1~3", "19R1-W15-1~3");
        String out = LocationNumberCompressor.compressJoined(List.of(oneCell));
        assertEquals("19R1-W5~15", out);
    }

    @Test
    void secondPassMergesConsecutiveMidSameTail() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "19R1-W5-1", "19R1-W5-2", "19R1-W5-3",
                "19R1-W6-1", "19R1-W6-2", "19R1-W6-3",
                "19R1-W7-1", "19R1-W7-2", "19R1-W7-3"
        ));
        assertEquals("19R1-W5~7", out);
    }

    @Test
    void normalizesCaseAndSpacesForMerge() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "Q3", " q5 ", "q4"
        ));
        assertEquals("Q3~5", out);
    }

    @Test
    void normalizesFullWidthDigits() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "N３", "n4"
        ));
        assertEquals("N3~4", out);
    }

    @Test
    void secondPassMergesMixedCaseSamePrefix() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "19r1-w5-1", "19R1-W5-2", "19R1-w5-3",
                "19r1-w6-1", "19R1-W6-2", "19r1-w6-3"
        ));
        assertEquals("19R1-W5~6", out);
    }

    /** 非 W 字母：PROJ-Q 缺 Q3 时拆成 PROJ-Q1~2 与 PROJ-Q4~6 */
    @Test
    void secondPassSplitsAtGapForLetterOtherThanW() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "PROJ-Q1-1", "PROJ-Q1-2", "PROJ-Q1-3",
                "PROJ-Q2-1", "PROJ-Q2-2", "PROJ-Q2-3",
                "PROJ-Q4-1", "PROJ-Q4-2", "PROJ-Q4-3",
                "PROJ-Q5-1", "PROJ-Q5-2", "PROJ-Q5-3",
                "PROJ-Q6-1", "PROJ-Q6-2", "PROJ-Q6-3"
        ));
        assertEquals("PROJ-Q1~2，PROJ-Q4~6", out);
    }

    /** 汉字修饰 + 末段数字：多条收成扁平基准位号 */
    @Test
    void mergesHanDecoratedSameFlatBaseToSingleCanonical() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "H5前-1", "H5前-2", "H5前-3", "H5前-4",
                "H5-1", "H5-2", "H5-3", "H5-4",
                "H5后-1", "H5后-2", "H5后-3", "H5后-4"
        ));
        assertEquals("H5", out);
    }

    /** 基准折叠后再按前缀连续尾数合并 */
    @Test
    void mergesFlatLetterNumberRunsAfterCanonicalCollapse() {
        assertEquals("H5~7", LocationNumberCompressor.compressJoined(List.of("H5-1", "H6-1", "H7-1")));
        assertEquals("H5，H8~9", LocationNumberCompressor.compressJoined(List.of("H5-1", "H8-1", "H9-1")));
    }

    @Test
    void singleHanDecoratedEntryStillCollapsesToCanonical() {
        assertEquals("H5", LocationNumberCompressor.compressJoined(List.of("H5-1")));
        assertEquals("Q6", LocationNumberCompressor.compressJoined(List.of("Q6前-1", "Q6-2")));
        assertEquals("Z9", LocationNumberCompressor.compressJoined(List.of("Z9后-1")));
    }

    @Test
    void mergesOneHaoStylePrefixesToDigitCanonical() {
        String out = LocationNumberCompressor.compressJoined(List.of(
                "1号前-1", "1号-2", "1号后-3"
        ));
        assertEquals("1", out);
    }

    /** 纯数字 1 与 01 不因数值相等合并或去重 */
    @Test
    void pureDigitTokensPreserveDistinctLeadingZeros() {
        String out = LocationNumberCompressor.compressJoined(List.of("1", "01"));
        assertEquals("1，01", out);
    }

    /** 同宽度前导零数字可压区间 */
    @Test
    void paddedDigitsMergeRangeWhenSameWidth() {
        assertEquals("01~02", LocationNumberCompressor.compressJoined(List.of("01", "02")));
    }

    /** 纯数字 1～20：9 与 10 位数不同仍收成一段 */
    @Test
    void pureDigitsMergeOneThroughTwenty() {
        List<String> nums = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            nums.add(String.valueOf(i));
        }
        assertEquals("1~20", LocationNumberCompressor.compressJoined(nums));
    }

    /** 纯数字乱序含补号：收成 1~7 */
    @Test
    void pureDigitsMergeWhenOutOfOrderIncludesGapFill() {
        assertEquals("1~7", LocationNumberCompressor.compressJoined(List.of(
                "1", "2", "3", "5", "6", "7", "4")));
    }
}

