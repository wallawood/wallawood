package org.server.gemini.internal.fixtures;

import jakarta.ws.rs.Path;
import org.server.gemini.GeminiController;
import org.server.gemini.GeminiResponse;

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
