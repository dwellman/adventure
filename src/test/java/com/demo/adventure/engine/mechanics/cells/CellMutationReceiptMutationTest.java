package com.demo.adventure.engine.mechanics.cells;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.model.Thing;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CellMutationReceiptMutationTest {

    @Test
    void detectsTamperedReceiptFields() {
        Plot plot = new PlotBuilder()
                .withLabel("Lab")
                .withDescription("A quiet lab.")
                .build();
        Item canteen = new ItemBuilder()
                .withLabel("Canteen")
                .withDescription("A sturdy canteen.")
                .withOwnerId(plot)
                .build();
        long capacity = 10L;
        canteen.setCells(Map.of(
                Thing.normalizeCellKey("WATER"),
                new CellSpec(capacity, 5L).toCell()
        ));

        CellMutationReceipt receipt = CellOps.consume(canteen, "WATER", 3L);
        ReceiptAudit audit = new ReceiptAudit(capacity);

        assertThat(audit.validate(receipt)).isEmpty();

        CellMutationReceipt mutated = new CellMutationReceipt(
                receipt.thingId(),
                receipt.cellName(),
                receipt.field(),
                receipt.beforeAmount(),
                receipt.afterAmount() + 1L,
                receipt.beforeVolume(),
                receipt.afterVolume(),
                receipt.requestedDelta(),
                receipt.appliedDelta(),
                receipt.reason()
        );

        assertThat(audit.validate(mutated)).isNotEmpty();
    }

    private static final class ReceiptAudit {
        private static final double EPSILON = 0.0001;
        private final long capacity;

        private ReceiptAudit(long capacity) {
            this.capacity = capacity;
        }

        private List<String> validate(CellMutationReceipt receipt) {
            List<String> errors = new ArrayList<>();
            if (receipt.afterAmount() < 0 || receipt.afterAmount() > capacity) {
                errors.add("after amount out of bounds");
            }
            if (receipt.beforeAmount() < 0 || receipt.beforeAmount() > capacity) {
                errors.add("before amount out of bounds");
            }
            if (!close(receipt.beforeVolume(), Cell.volumeFor(receipt.beforeAmount(), capacity))) {
                errors.add("before volume mismatch");
            }
            if (!close(receipt.afterVolume(), Cell.volumeFor(receipt.afterAmount(), capacity))) {
                errors.add("after volume mismatch");
            }
            if (receipt.appliedDelta() != null && receipt.reason() != CellMutationReason.MISSING_CELL) {
                long delta = Math.abs(receipt.afterAmount() - receipt.beforeAmount());
                if (delta != receipt.appliedDelta()) {
                    errors.add("applied delta mismatch");
                }
            }
            return errors;
        }

        private boolean close(double left, double right) {
            return Math.abs(left - right) <= EPSILON;
        }
    }
}
