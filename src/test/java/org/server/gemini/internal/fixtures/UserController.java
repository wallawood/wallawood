package org.server.gemini.internal.fixtures;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.server.gemini.GeminiController;
import org.server.gemini.GeminiResponse;

@GeminiController
@Path("/users")
public class UserController {

    @Path("/")
    public GeminiResponse listUsers() {
        return GeminiResponse.success("# Users");
    }

    @Path("/{id}")
    public GeminiResponse getUser(@PathParam("id") String id) {
        return GeminiResponse.success("# User " + id);
    }

    @Path("/search")
    public GeminiResponse search(@QueryParam("q") String query) {
        return GeminiResponse.success("# Search: " + query);
    }
}
