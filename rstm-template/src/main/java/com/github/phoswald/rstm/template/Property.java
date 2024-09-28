package com.github.phoswald.rstm.template;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

record Property<T> (String name, Type type, Function<TemplateArgument<?>, T> accessor) {

    T getValue(TemplateArgument<?> argument) {
        return accessor.apply(argument);
    }

    static Property<?> create(TemplateCompilation<?> compilation, String name) {
        if (name.startsWith("#")) {
            String key = name.substring(1);
            getResourceText(compilation.resources(), key); // for verification, throws MissingResourceException
            return new Property<>(name, String.class, //
                    argument -> getResourceText(argument.resources(), key));
        } else {
            Class<?> argumentClass = compilation.argumentClass();
            Method method = getPropertyAccessor(argumentClass, name);
            return new Property<>(name, method.getGenericReturnType(), //
                    argument -> getPropertyValue(argumentClass, name, method, argument.instance()));
        }
    }

    private static String getResourceText(ResourceBundle resources, String name) {
        return resources.getString(name);
    }

    private static Method getPropertyAccessor(Class<?> clazz, String name) {
        for (String methodName : List.of(name, "get" + name.substring(0, 1).toUpperCase() + name.substring(1))) {
            try {
                return clazz.getMethod(methodName);
            } catch (NoSuchMethodException | SecurityException e) {
                continue;
            }
        }
        throw new IllegalArgumentException("Failed to lookup method: " + clazz.getName() + "::" + name);
    }

    private static Object getPropertyValue(Class<?> clazz, String name, Method accessor, Object instance) {
        try {
            return accessor.invoke(instance);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke method: " + clazz.getName() + "::" + name
                    + ", instance of " + instance.getClass().getName(), e);
        }
    }

    Property<String> toStringProperty() {
        return new Property<>(name, type, accessor.andThen( //
                result -> result == null ? null : result.toString()));
    }

    Property<Boolean> toBooleanProperty() {
        return new Property<>(name, type, accessor.andThen( //
                result -> result != null && result != Boolean.FALSE && !result.toString().isEmpty()));
    }

    Property<Collection<?>> toArrayProperty() {
        return new Property<>(name, type, accessor.andThen( //
                result -> result == null ? List.of() : List.of((Object[]) result)));
    }

    Property<Collection<?>> toCollectionProperty() {
        return new Property<>(name, type, accessor.andThen( //
                result -> result == null ? List.of() : (Collection<?>) result));
    }

    Property<Collection<?>> toMapProperty() {
        return new Property<>(name, type, accessor.andThen( //
                result -> (result == null ? Map.of() : (Map<?, ?>) result).entrySet()));
    }
}
