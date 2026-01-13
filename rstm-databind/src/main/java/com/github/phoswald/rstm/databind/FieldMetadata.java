package com.github.phoswald.rstm.databind;

public interface FieldMetadata {

    /**
     * The name of the field, the name of the record component
     */
    String name();

    /**
     * Allows determining the concrete type, without giving public access to these classes
     */
    Kind kind();

    /**
     * The type of the primitive, simple or record field, or the element type of the list or map field.
     */
    Class<?> clazz();
}
