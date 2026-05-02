package io.gemboot;

import java.util.Optional;

public interface RequestInterceptor {
    Optional<GeminiResponse> intercept(RequestContext context);
}
