package com.demo.adventure.buui;

public class Cell {
    private final Object value;
    private final CellFunction function;

    public Cell(Object value) {
        this(value, CellFunction.NONE);
    }

    public Cell(Object value, CellFunction function) {
        this.value = value;
        this.function = function == null ? CellFunction.NONE : function;
    }

    public static Cell sum(Number... values) {
        return new Cell(values, CellFunction.SUM);
    }

    public Object value() {
        return value;
    }

    public CellFunction function() {
        return function;
    }

    public String render(FormatterRegistry registry, Formatter formatterOverride) {
        Object toRender = applyFunction();
        if (toRender instanceof String s) {
            return s;
        }
        if (formatterOverride != null) {
            return formatterOverride.format(toRender);
        }
        return registry.format(toRender);
    }

    private Object applyFunction() {
        if (function == CellFunction.SUM) {
            double sum = 0.0;
            if (value instanceof Number[] nums) {
                for (Number n : nums) {
                    if (n != null) sum += n.doubleValue();
                }
            } else if (value instanceof double[] doubles) {
                for (double d : doubles) sum += d;
            } else if (value instanceof Iterable<?> iterable) {
                for (Object o : iterable) {
                    if (o instanceof Number n) {
                        sum += n.doubleValue();
                    }
                }
            } else if (value instanceof Number n) {
                sum += n.doubleValue();
            }
            return sum;
        }
        return value;
    }
}
