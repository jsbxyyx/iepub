package io.github.jsbxyyx.iepub;

import nl.siegmann.epublib.domain.TOCReference;

public class IepubTocNode {

    private TOCReference tocReference;

    public IepubTocNode(TOCReference tocReference) {
        this.tocReference = tocReference;
    }

    public TOCReference getTocReference() {
        return tocReference;
    }

    @Override
    public String toString() {
        return tocReference.getTitle();
    }

}
