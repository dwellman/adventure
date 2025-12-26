package com.demo.adventure.engine.mechanics.cells;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CellOpsTest {

    @Test
    void consumeClampsToZero() {
        Item item = new ItemBuilder()
                .withLabel("Ammo Box")
                .withDescription("")
                .withOwnerId(UUID.randomUUID())
                .build();
        item.setCell("ammo", new Cell(5, 2));

        CellMutationReceipt receipt = CellOps.consume(item, "ammo", 7);

        assertThat(receipt.beforeAmount()).isEqualTo(2L);
        assertThat(receipt.afterAmount()).isEqualTo(0L);
        assertThat(receipt.requestedDelta()).isEqualTo(7L);
        assertThat(receipt.appliedDelta()).isEqualTo(2L);
        assertThat(receipt.reason()).isEqualTo(CellMutationReason.CLAMPED_TO_ZERO);
    }

    @Test
    void replenishClampsToCapacity() {
        Item item = new ItemBuilder()
                .withLabel("Canteen")
                .withDescription("")
                .withOwnerId(UUID.randomUUID())
                .build();
        item.setCell("water", new Cell(5, 4));

        CellMutationReceipt receipt = CellOps.replenish(item, "water", 4);

        assertThat(receipt.beforeAmount()).isEqualTo(4L);
        assertThat(receipt.afterAmount()).isEqualTo(5L);
        assertThat(receipt.requestedDelta()).isEqualTo(4L);
        assertThat(receipt.appliedDelta()).isEqualTo(1L);
        assertThat(receipt.reason()).isEqualTo(CellMutationReason.CLAMPED_TO_CAPACITY);
    }

    @Test
    void transferClampsToTargetCapacity() {
        Item from = new ItemBuilder()
                .withLabel("Fuel Can")
                .withDescription("")
                .withOwnerId(UUID.randomUUID())
                .build();
        Item to = new ItemBuilder()
                .withLabel("Lantern")
                .withDescription("")
                .withOwnerId(UUID.randomUUID())
                .build();
        from.setCell("fuel", new Cell(10, 6));
        to.setCell("fuel", new Cell(5, 5));

        CellTransferResult result = CellOps.transfer(from, to, "fuel", 4);

        assertThat(result.transferReceipt().appliedDelta()).isEqualTo(0L);
        assertThat(from.getCell("fuel").getAmount()).isEqualTo(6L);
        assertThat(to.getCell("fuel").getAmount()).isEqualTo(5L);
        assertThat(result.toReceipt().reason()).isEqualTo(CellMutationReason.CLAMPED_TO_CAPACITY);
    }

    @Test
    void missingCellDoesNotCrashReads() {
        Item item = new ItemBuilder()
                .withLabel("Pack")
                .withDescription("")
                .withOwnerId(UUID.randomUUID())
                .build();

        CellReadResult read = CellOps.read(item, "mana");
        CellMutationReceipt receipt = CellOps.consume(item, "mana", 1);

        assertThat(read.missing()).isTrue();
        assertThat(read.name()).isEqualTo("MANA");
        assertThat(receipt.reason()).isEqualTo(CellMutationReason.MISSING_CELL);
    }
}
