package com.reportweb.dto;



import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.Data;



import java.time.LocalDateTime;

import java.util.Map;



public class MaterialLibraryDTOs {



    private MaterialLibraryDTOs() {

    }



    @Data

    public static class ListItem {

        private Long id;

        private String materialKey;

        private String primaryCategory;

        private String status;

        private String source;

        private String modificationType;

        private Map<String, String> properties;

        private Boolean pendingChange;

        private String submittedByUserName;

        private String reviewedByUserName;

        private String reviewComment;

        private LocalDateTime createdAt;

        private LocalDateTime reviewedAt;

        /** 我的提交列表展示用：新增驳回为 REJECTED；修改/删除驳回也为 REJECTED（库内 status 仍为 APPROVED） */
        private String submissionStatus;

    }



    @Data

    public static class ApprovalLogItem {

        private Long id;

        private String action;

        private String actorUserName;

        private String comment;

        private LocalDateTime createdAt;

    }



    @Data

    public static class CreateRequest {

        @NotBlank(message = "材质牌号不能为空")

        @Size(max = 100, message = "材质牌号长度不能超过100")

        private String materialKey;



        @NotBlank(message = "分类不能为空")

        private String primaryCategory;



        private Map<String, String> properties;

    }



    @Data

    public static class UpdateRequest {

        @NotBlank(message = "分类不能为空")

        private String primaryCategory;



        private Map<String, String> properties;

    }



    @Data

    public static class RejectRequest {

        @NotBlank(message = "驳回原因不能为空")

        @Size(max = 500, message = "驳回原因长度不能超过500")

        private String reviewComment;

    }



    @Data

    public static class KeysResponse {

        private java.util.List<String> keys;

    }



    @Data

    public static class CapabilitiesResponse {

        private boolean canReview;

        private boolean canSubmit;

        private long pendingReviewCount;

        private long rejectedCount;

    }

}

