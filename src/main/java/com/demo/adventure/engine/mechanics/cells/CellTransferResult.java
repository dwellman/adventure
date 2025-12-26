package com.demo.adventure.engine.mechanics.cells;

public record CellTransferResult(
        CellMutationReceipt fromReceipt,
        CellMutationReceipt toReceipt,
        CellTransferReceipt transferReceipt
) {
}
