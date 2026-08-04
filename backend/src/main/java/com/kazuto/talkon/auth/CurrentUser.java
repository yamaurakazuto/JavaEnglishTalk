package com.kazuto.talkon.auth;
import com.kazuto.talkon.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
public final class CurrentUser {
    private CurrentUser() {}
    public static TalkOnPrincipal require(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof TalkOnPrincipal p))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "ログインが必要です。");
        return p;
    }
}

