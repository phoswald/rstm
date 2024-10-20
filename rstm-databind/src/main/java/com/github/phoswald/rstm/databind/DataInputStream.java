package com.github.phoswald.rstm.databind;

import java.util.Map;

import com.github.phoswald.rstm.databind.Databinder.ClassInfo;

interface DataInputStream extends AutoCloseable {

    <T> void readObject(ClassInfo<T> classInfo, Map<String, Object> map) throws Exception;
}
