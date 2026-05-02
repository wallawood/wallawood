package io.gemboot.internal.difixtures;

import io.gemboot.annotations.Component;

@Component
public class GreetingService {

    public String greet() {
        return "hello from service";
    }
}
