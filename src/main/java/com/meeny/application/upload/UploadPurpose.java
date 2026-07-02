package com.meeny.application.upload;

public enum UploadPurpose {
    PROFILE("profiles"),
    CREW("crews"),
    PLAY("plays"),
    PIN("pins");

    private final String prefix;

    UploadPurpose(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
