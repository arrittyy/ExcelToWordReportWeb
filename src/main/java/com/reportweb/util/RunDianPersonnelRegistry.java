package com.reportweb.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * 润电内部检测人员姓名与记录编号用人员代码。
 * 与前端 {@code frontend/src/constants/rundianPersonnel.ts} 保持同步。
 */
public final class RunDianPersonnelRegistry {

  private static final Map<String, Integer> NAME_TO_CODE;
  private static final List<String> CANONICAL_NAMES_SORTED;

  static {
    LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
    List<String> canonical = new ArrayList<>();
    add(m, canonical, 10, "蔡红生");
    add(m, canonical, 12, "李世涛");
    add(m, canonical, 14, "杨旭");
    add(m, canonical, 20, "魏泉泉");
    add(m, canonical, 22, "靳峰");
    add(m, canonical, 27, "陈岩");
    add(m, canonical, 33, "胡锋涛");
    add(m, canonical, 34, "马东方");
    add(m, canonical, 37, "牛保献");
    add(m, canonical, 40, "周书康");
    add(m, canonical, 43, "李世铭");
    add(m, canonical, 44, "李艳军");
    add(m, canonical, 45, "侯家绪");
    add(m, canonical, 46, "高建忠");
    add(m, canonical, 47, "徐亮");
    add(m, canonical, 48, "郭文");
    add(m, canonical, 52, "符勇");
    add(m, canonical, 53, "闫宁");
    add(m, canonical, 54, "高秀娜");
    add(m, canonical, 56, "魏烁");
    add(m, canonical, 57, "张书浩");
    add(m, canonical, 59, "王志永");
    add(m, canonical, 60, "王红宝");
    add(m, canonical, 61, "张晓霓");
    add(m, canonical, 62, "蒋豹");
    add(m, canonical, 64, "王凌颉");
    add(m, canonical, 65, "王鹏飞");
    add(m, canonical, 67, "贾新杰");
    add(m, canonical, 69, "宋可可");
    add(m, canonical, 70, "武莹莹");
    add(m, canonical, 71, "王强");
    add(m, canonical, 72, "杨希锐");
    add(m, canonical, 73, "肖乐园");
    add(m, canonical, 74, "马泽军");
    add(m, canonical, 75, "张庆巍");
    add(m, canonical, 76, "张博炜");
    m.put("张博玮", 76);
    add(m, canonical, 77, "陈莉君");
    add(m, canonical, 78, "白鹏辉");
    add(m, canonical, 90, "卢申");
    add(m, canonical, 91, "王佳朋");
    add(m, canonical, 92, "孙赞");
    add(m, canonical, 93, "王志明");
    add(m, canonical, 94, "朱培营");
    add(m, canonical, 95, "句慧文");
    NAME_TO_CODE = Collections.unmodifiableMap(m);
    Collator collator = Collator.getInstance(Locale.CHINA);
    canonical.sort(collator);
    CANONICAL_NAMES_SORTED = Collections.unmodifiableList(canonical);
  }

  private RunDianPersonnelRegistry() {
  }

  private static void add(Map<String, Integer> map, List<String> canonical, int code, String name) {
    map.put(name, code);
    canonical.add(name);
  }

  public static boolean contains(String name) {
    if (name == null) {
      return false;
    }
    return NAME_TO_CODE.containsKey(name.trim());
  }

  public static OptionalInt codeOf(String name) {
    if (name == null) {
      return OptionalInt.empty();
    }
    Integer code = NAME_TO_CODE.get(name.trim());
    return code != null ? OptionalInt.of(code) : OptionalInt.empty();
  }

  public static Set<String> allRecognizedNames() {
    return NAME_TO_CODE.keySet();
  }

  public static List<String> sortedDisplayNames() {
    return CANONICAL_NAMES_SORTED;
  }
}
