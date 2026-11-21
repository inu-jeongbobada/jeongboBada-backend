package com.inu.jeongbobada.domain.courseReview.enums;

public enum AcademicYear {
    YEAR_2023(2023),
    YEAR_2024(2024),
    YEAR_2025(2025),
    YEAR_2026(2026);

    private final int value;

    AcademicYear(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
