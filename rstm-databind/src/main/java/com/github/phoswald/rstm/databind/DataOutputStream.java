package com.github.phoswald.rstm.databind;

interface DataOutputStream extends AutoCloseable {

    void startObject(String name) throws Exception;

    void endObject(String name) throws Exception;

    void field(String name, Object value) throws Exception;
}
