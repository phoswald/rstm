package com.github.phoswald.rstm.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HttpRequestTest {
    
    @Test
    void relativizePath() {
        // href="" refers to the current page
        // href="." refers to the current directory 
        // href="" and href="." are the same of the current page is a directory ("foo/") 
        // href="../" refers to the parent directory
        // href=".." refers to the parent directory, and would triggers a redirect (illogical, should be avoided)
        
        assertEquals("." /* "" */, HttpRequest.builder().path("/").build().relativizePath("/"));
        assertEquals("baz", HttpRequest.builder().path("/").build().relativizePath("/baz"));
        assertEquals("baz/", HttpRequest.builder().path("/").build().relativizePath("/baz/"));
        assertEquals("foo/baz", HttpRequest.builder().path("/").build().relativizePath("/foo/baz"));
        
        assertEquals("." /* "" */, HttpRequest.builder().path("/foo").build().relativizePath("/"));
        assertEquals("baz", HttpRequest.builder().path("/foo").build().relativizePath("/baz"));
        assertEquals("baz/", HttpRequest.builder().path("/foo").build().relativizePath("/baz/"));
        assertEquals("foo" /* "" */, HttpRequest.builder().path("/foo").build().relativizePath("/foo"));
        assertEquals("foo/", HttpRequest.builder().path("/foo").build().relativizePath("/foo/"));
        assertEquals("foo/baz", HttpRequest.builder().path("/foo").build().relativizePath("/foo/baz"));
        assertEquals("foo/baz/", HttpRequest.builder().path("/foo").build().relativizePath("/foo/baz/"));
        
        assertEquals("../", HttpRequest.builder().path("/foo/").build().relativizePath("/"));
        assertEquals("../baz", HttpRequest.builder().path("/foo/").build().relativizePath("/baz"));
        assertEquals("../baz/", HttpRequest.builder().path("/foo/").build().relativizePath("/baz/"));
        assertEquals("../foo", HttpRequest.builder().path("/foo/").build().relativizePath("/foo"));
        assertEquals("." /* "" */, HttpRequest.builder().path("/foo/").build().relativizePath("/foo/"));
        assertEquals("baz", HttpRequest.builder().path("/foo/").build().relativizePath("/foo/baz"));
        assertEquals("baz/", HttpRequest.builder().path("/foo/").build().relativizePath("/foo/baz/"));
        
        assertEquals("../", HttpRequest.builder().path("/foo/bar").build().relativizePath("/"));
        assertEquals("../baz", HttpRequest.builder().path("/foo/bar").build().relativizePath("/baz"));
        assertEquals("../baz/", HttpRequest.builder().path("/foo/bar").build().relativizePath("/baz/"));
        assertEquals("." /* "" */, HttpRequest.builder().path("/foo/bar").build().relativizePath("/foo/"));
        assertEquals("bar" /* "" */, HttpRequest.builder().path("/foo/bar").build().relativizePath("/foo/bar"));
        assertEquals("bar/", HttpRequest.builder().path("/foo/bar").build().relativizePath("/foo/bar/"));
        assertEquals("baz", HttpRequest.builder().path("/foo/bar").build().relativizePath("/foo/baz"));
        assertEquals("baz/", HttpRequest.builder().path("/foo/bar").build().relativizePath("/foo/baz/"));
        
        assertEquals("../../", HttpRequest.builder().path("/foo/bar/").build().relativizePath("/"));
        assertEquals("../../baz", HttpRequest.builder().path("/foo/bar/").build().relativizePath("/baz"));
        assertEquals("../../baz/", HttpRequest.builder().path("/foo/bar/").build().relativizePath("/baz/"));
        assertEquals("../", HttpRequest.builder().path("/foo/bar/").build().relativizePath("/foo/"));
        assertEquals("../bar", HttpRequest.builder().path("/foo/bar/").build().relativizePath("/foo/bar"));
        assertEquals("." /* "" */, HttpRequest.builder().path("/foo/bar/").build().relativizePath("/foo/bar/"));
        assertEquals("../baz", HttpRequest.builder().path("/foo/bar/").build().relativizePath("/foo/baz"));
        assertEquals("../baz/", HttpRequest.builder().path("/foo/bar/").build().relativizePath("/foo/baz/"));
    }
}
