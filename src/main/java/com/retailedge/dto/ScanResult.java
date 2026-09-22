package com.retailedge.dto;

public record ScanResult(int processed, int abandoned, String nextCursor, boolean hasNext) {
}