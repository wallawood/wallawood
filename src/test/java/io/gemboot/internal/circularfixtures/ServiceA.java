package io.gemboot.internal.circularfixtures;

import io.gemboot.annotations.Component;

@Component
public class ServiceA {
    public ServiceA(ServiceB b) {}
}
