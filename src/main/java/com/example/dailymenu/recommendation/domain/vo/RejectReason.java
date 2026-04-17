package com.example.dailymenu.recommendation.domain.vo;

public enum RejectReason {
    TOO_FAR,          // 너무 멀어요
    ATE_RECENTLY,     // 최근에 먹었어요
    NOT_THIS_TYPE,    // 이 종류 말고요
    OTHER             // 기타 (reject_detail에 사유 저장)
}
