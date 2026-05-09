package io.github.wallawood.internal.difixtures;

import io.github.wallawood.GeminiResponse;
import io.github.wallawood.annotations.GeminiController;
import io.github.wallawood.annotations.Path;

@GeminiController
public class GreetingController {

    private final GreetingService service;

    public GreetingController(GreetingService service) {
        this.service = service;
    }

    @Path("/greet")
    public GeminiResponse greet() {
        return GeminiResponse.success(service.greet());
    }
}
