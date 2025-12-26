package com.demo.adventure.test;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class ConsoleCaptureExtension implements BeforeEachCallback, AfterEachCallback {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream buffer;
    private ByteArrayOutputStream errorBuffer;

    @Override
    public void beforeEach(ExtensionContext context) {
        originalOut = System.out;
        buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        originalErr = System.err;
        errorBuffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorBuffer, true, StandardCharsets.UTF_8));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }
        originalOut = null;
        originalErr = null;
        buffer = null;
        errorBuffer = null;
    }

    public void reset() {
        if (buffer != null) {
            buffer.reset();
        }
        if (errorBuffer != null) {
            errorBuffer.reset();
        }
    }

    public String output() {
        if (buffer == null) {
            return "";
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public String error() {
        if (errorBuffer == null) {
            return "";
        }
        return errorBuffer.toString(StandardCharsets.UTF_8);
    }
}
