package com.demo.adventure.ai.client;

public final class AiJson {
    private AiJson() {
    }

    public static String escape(String text) {
        String escaped = text == null ? "" : text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    public static String extractJsonString(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex);
        if (colon < 0) {
            return null;
        }
        int quote = json.indexOf('"', colon);
        if (quote < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = quote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String extractLogprobSnippet(String response) {
        if (response == null) {
            return "";
        }
        int logIdx = response.indexOf("\"top_logprobs\"");
        if (logIdx < 0) {
            return "";
        }
        int brace = response.indexOf('{', logIdx);
        int close = response.indexOf('}', brace);
        if (brace > 0 && close > brace) {
            return response.substring(brace, Math.min(response.length(), brace + 200));
        }
        return "";
    }
}
