package com.reportweb.service;

/**
 * 检测内容首行/回退用的结构化字段（与单项 Word、概述共用）。
 */
public final class DetectionContentDetail {
    public final String methodName;
    public final String componentName;
    public final String type;
    public final String total;
    public final String locationDesc;
    public final String locationNumber;

    public DetectionContentDetail(String methodName, String componentName, String type, String total,
                                  String locationDesc, String locationNumber) {
        this.methodName = methodName != null ? methodName : "";
        this.componentName = componentName != null ? componentName : "";
        this.type = type != null ? type : "";
        this.total = total != null ? total : "";
        this.locationDesc = locationDesc != null ? locationDesc : "";
        this.locationNumber = locationNumber != null ? locationNumber : "";
    }
}
