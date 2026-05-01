package io.gemboot.internal.fixtures;

import io.gemboot.annotations.Path;
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
