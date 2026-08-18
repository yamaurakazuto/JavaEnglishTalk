// 共通エラーレスポンスの形を定義します。全APIで一貫した失敗情報を返すためのDTOです。

package com.talkon.common;

import java.util.Map;

public record ApiError(
    String code, String message, Map<String, String> fieldErrors, String traceId) {}
