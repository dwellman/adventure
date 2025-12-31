package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.engine.flow.trigger.TriggerOutcome;
import com.demo.adventure.engine.flow.trigger.TriggerType;
import com.demo.adventure.engine.command.CommandOutput;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CommandContext {
    private final CommandOutput output;
    private final GameRuntime runtime;

    public CommandContext(CommandOutput output, GameRuntime runtime) {
        this.output = Objects.requireNonNull(output, "output");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    public KernelRegistry registry() {
        return runtime.registry();
    }

    public UUID currentPlotId() {
        return runtime.currentPlotId();
    }

    public UUID playerId() {
        return runtime.playerId();
    }

    public List<Item> inventory() {
        return runtime.inventory();
    }

    public Plot currentPlot() {
        return runtime.currentPlot();
    }

    public void setCurrentPlot(UUID plotId) {
        runtime.setCurrentPlot(plotId);
    }

    public void print(String text) {
        output.emit(text);
    }

    public void narrate(String text) {
        runtime.narrate(text);
    }

    public void printHelp() {
        output.printHelp();
    }

    public void describe() {
        runtime.describe();
    }

    public void look(String target) {
        runtime.lookDirectionOrThing(target);
    }

    public void showInventory() {
        runtime.showInventory();
    }

    public void explore() {
        runtime.explore();
    }

    public void craft(String target) {
        runtime.craft(target);
    }

    public void how(String target) {
        runtime.how(target);
    }

    public Item take(String target) {
        return runtime.take(target);
    }

    public void drop(String target) {
        runtime.drop(target);
    }

    public void open(String target) {
        runtime.open(target);
    }

    public UseResult use(String target, String preposition, String object) {
        return runtime.use(target, preposition, object);
    }

    public void attack(String target) {
        runtime.attack(target);
    }

    public void flee() {
        runtime.flee();
    }

    public void put(String target, String object) {
        runtime.put(target, object);
    }

    public void rollDice(String argument) {
        runtime.rollDice(argument);
    }

    public void talk(String target) {
        runtime.talk(target);
    }

    public Direction parseDirection(String target) {
        return runtime.parseDirection(target);
    }

    public UUID move(Direction direction) {
        return runtime.move(direction);
    }

    public MoveResult tryMove(Direction direction) {
        return runtime.tryMove(direction);
    }

    public TriggerOutcome fireTrigger(TriggerType type, Thing target, Thing object) {
        return runtime.fireTrigger(type, target, object);
    }

    public CommandOutcome resolveTriggerOutcome(TriggerOutcome outcome) throws GameBuilderException {
        return runtime.resolveTriggerOutcome(outcome);
    }
}
