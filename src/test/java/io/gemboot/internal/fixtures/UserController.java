package io.gemboot.internal.fixtures;

import io.gemboot.annotations.Path;
import io.gemboot.annotations.PathParam;
import io.gemboot.annotations.QueryParam;
import io.gemboot.annotations.GeminiController;
import io.gemboot.GeminiResponse;

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
