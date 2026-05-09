package io.github.wallawood.internal.fixtures;

import io.github.wallawood.annotations.Path;
import io.github.wallawood.annotations.PathParam;
import io.github.wallawood.annotations.QueryParam;
import io.github.wallawood.annotations.GeminiController;
import io.github.wallawood.GeminiResponse;

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
