package com.dev_infinity.commands;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author MrMicky
 */
public class Command {

    private String name;
    private String[] alias;
    private Method method;
    private Object object;

    public Command(String name, String[] alias, Method method, Object object) {
        this.name = name;
        this.alias = alias;
        this.method = method;
        this.object = object;
    }

    public String getName() {
        return name;
    }

    public String[] getAlias() {
        return alias;
    }

    public Method getMethod() {
        return method;
    }

    public Object getObject() {
        return object;
    }

    @Override
    public String toString() {
        return "Command{name='" + name + "'" + ", alias=" + Arrays.toString(alias) + '}';
    }
}
