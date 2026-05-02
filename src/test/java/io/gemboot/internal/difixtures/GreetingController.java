package io.gemboot.internal.difixtures;

import io.gemboot.GeminiResponse;
import io.gemboot.annotations.GeminiController;
import io.gemboot.annotations.Path;

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
