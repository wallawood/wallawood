package io.github.wallawood.internal.fixtures;

import io.github.wallawood.annotations.Path;
import io.github.wallawood.annotations.GeminiController;
import io.github.wallawood.GeminiResponse;

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
