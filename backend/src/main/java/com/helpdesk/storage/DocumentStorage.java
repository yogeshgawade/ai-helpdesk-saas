package com.helpdesk.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface DocumentStorage {

    InputStream open(
            UUID organizationId,
            UUID documentId,
            String filename
    ) throws IOException;

    void store(
            UUID organizationId,
            UUID documentId,
            String filename,
            InputStream inputStream
    ) throws IOException;
}