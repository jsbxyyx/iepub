package io.github.jsbxyyx.iepub;

import org.junit.Test;

public class PropertiesUtilTest {

    @Test
    public void test_log() {
        PropertiesUtil.log("aaa");

        PropertiesUtil.log("bbb", new Exception("11111"));
    }

}
