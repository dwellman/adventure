package com.demo.adventure.ai.client;

import java.net.URI;
import java.time.Duration;
import java.util.List;

public record AiChatRequest(
        URI endpoint,
        String model,
        List<AiChatMessage> messages,
        double temperature,
        double topP,
        boolean logprobs,
        Integer topLogprobs,
        Duration timeout,
        ResponseFormat responseFormat
) {
    public AiChatRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static Builder builder() {
        return new Builder();
    }

    public record ResponseFormat(String type) {
        public ResponseFormat {
            type = type == null ? "" : type.trim();
        }

        public static ResponseFormat jsonObject() {
            return new ResponseFormat("json_object");
        }
    }

    public static final class Builder {
        private URI endpoint;
        private String model;
        private List<AiChatMessage> messages = List.of();
        private double temperature = 0.0;
        private double topP = 1.0;
        private boolean logprobs;
        private Integer topLogprobs;
        private Duration timeout = Duration.ofSeconds(30);
        private ResponseFormat responseFormat;

        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<AiChatMessage> messages) {
            this.messages = messages == null ? List.of() : List.copyOf(messages);
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(double topP) {
            this.topP = topP;
            return this;
        }

        public Builder logprobs(boolean logprobs) {
            this.logprobs = logprobs;
            return this;
        }

        public Builder topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public AiChatRequest build() {
            return new AiChatRequest(
                    endpoint,
                    model,
                    messages,
                    temperature,
                    topP,
                    logprobs,
                    topLogprobs,
                    timeout,
                    responseFormat
            );
        }
    }
}
