package io.github.wallawood.internal.difixtures;

import io.github.wallawood.annotations.Component;

@Component
public class GreetingService {

    public String greet() {
        return "hello from service";
    }
}
