package com.meeny.application.upload;

public enum UploadPurpose {
    PROFILE("profiles"),
    CREW("crews"),
    PIN("pins");

    private final String prefix;

    UploadPurpose(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
