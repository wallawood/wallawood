package io.gemboot.internal.fixtures;

import jakarta.ws.rs.Path;
import io.gemboot.annotations.GeminiController;
import io.gemboot.GeminiResponse;

@GeminiController
public class RootController {

    @Path("/")
    public GeminiResponse home() {
        return GeminiResponse.success("# Welcome");
    }

    @Path("/about")
    public GeminiResponse about() {
        return GeminiResponse.success("# About");
    }
}
