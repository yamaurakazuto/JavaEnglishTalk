// 認証済みユーザーの最小情報を保持します。DB EntityをSecurityContextへ直接格納しないための値オブジェクトです。

package com.talkon.auth;

/** TalkOnPrincipalに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public record TalkOnPrincipal(Long id, String email, String displayName) {}
