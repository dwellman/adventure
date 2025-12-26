package com.demo.adventure.domain.model;

import java.util.ArrayList;
import java.util.List;

public final class Description {
    private String text;
    private final List<DescriptionVersion> history = new ArrayList<>();

    public Description(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void record(String text, int worldClock) {
        history.add(new DescriptionVersion(text, worldClock));
        this.text = text;
    }

    public List<DescriptionVersion> getHistory() {
        return List.copyOf(history);
    }
}
