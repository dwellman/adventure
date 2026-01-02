package com.demo.adventure.ai.runtime;

final class NarrationDebugPayload {
    private NarrationDebugPayload() {
    }

    static String debugJson(String mode, String rawEngineOutput, String sceneSnapshot, String colorEvent,
                            String playerUtterance, String canonicalCommand, String backstory) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"mode\":\"").append(escape(mode)).append("\",");
        sb.append("\"playerUtterance\":\"").append(escape(playerUtterance)).append("\",");
        sb.append("\"canonicalCommand\":\"").append(escape(canonicalCommand)).append("\",");
        sb.append("\"backstory\":\"").append(escape(backstory)).append("\",");
        sb.append("\"rawEngineOutput\":\"").append(escape(rawEngineOutput)).append("\",");
        sb.append("\"sceneSnapshot\":\"").append(escape(sceneSnapshot)).append("\",");
        sb.append("\"colorEvent\":\"").append(escape(colorEvent)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
