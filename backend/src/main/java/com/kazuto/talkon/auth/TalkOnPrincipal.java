// 認証済みユーザーの最小情報を保持します。DB EntityをSecurityContextへ直接格納しないための値オブジェクトです。

package com.kazuto.talkon.auth;

public record TalkOnPrincipal(Long id, String email, String displayName) {}
