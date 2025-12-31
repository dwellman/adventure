package com.demo.adventure.buui;

import java.util.List;

public record MarkdownDocument(String source, List<MarkdownToken> tokens, List<String> lines) {

    public String render() {
        return String.join("\n", lines == null ? List.of() : lines);
    }
}
