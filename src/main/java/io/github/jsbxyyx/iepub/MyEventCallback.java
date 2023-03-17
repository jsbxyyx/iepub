package io.github.jsbxyyx.iepub;

import nl.siegmann.epublib.browsersupport.Navigator;

public interface MyEventCallback {

    void call(Navigator navigator, int x, int y);

}
