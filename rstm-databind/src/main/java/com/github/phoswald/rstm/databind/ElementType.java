package com.github.phoswald.rstm.databind;

sealed interface ElementType extends AnyType permits SimpleType, RecordType { }
