// 共通エラーレスポンスの形を定義します。全APIで一貫した失敗情報を返すためのDTOです。

package com.talkon.common;

import java.util.Map;

/** ApiErrorに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public record ApiError(
    String code, String message, Map<String, String> fieldErrors, String traceId) {}
