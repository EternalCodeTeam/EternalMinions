package com.eternalcode.minions.gui;

import com.eternalcode.minions.config.MinionPanelAction;
import com.eternalcode.minions.config.MinionPanelElementConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MinionPanelLayout {

    private final int rows;
    private final Map<MinionPanelAction, List<Integer>> actionSlots;

    private MinionPanelLayout(int rows, Map<MinionPanelAction, List<Integer>> actionSlots) {
        this.rows = rows;
        this.actionSlots = actionSlots;
    }

    public static MinionPanelLayout from(MinionPanelConfig config) {
        if (config.title == null) {
            throw new IllegalArgumentException("Minion panel title is required");
        }
        if (config.pattern == null) {
            throw new IllegalArgumentException("Minion panel pattern is required");
        }
        if (config.elements == null) {
            throw new IllegalArgumentException("Minion panel elements are required");
        }

        int rows = config.pattern.size();
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Minion panel pattern must contain between 1 and 6 rows, found " + rows);
        }

        validateElements(config.elements);
        Map<MinionPanelAction, List<Integer>> slots = new EnumMap<>(MinionPanelAction.class);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            String row = config.pattern.get(rowIndex);
            if (row == null) {
                throw new IllegalArgumentException("Minion panel pattern row " + (rowIndex + 1) + " is required");
            }
            if (row.length() != 9) {
                throw new IllegalArgumentException(
                    "Minion panel pattern row " + (rowIndex + 1) + " must contain exactly 9 symbols, found " + row.length()
                );
            }

            for (int column = 0; column < row.length(); column++) {
                char symbol = row.charAt(column);
                MinionPanelElementConfig element = config.elements.get(symbol);
                if (element == null) {
                    throw new IllegalArgumentException("Minion panel symbol '" + symbol + "' has no element definition");
                }
                slots.computeIfAbsent(element.action, ignored -> new ArrayList<>()).add(rowIndex * 9 + column);
            }
        }

        slots.replaceAll((action, positions) -> List.copyOf(positions));
        return new MinionPanelLayout(rows, Collections.unmodifiableMap(slots));
    }

    private static void validateElements(Map<Character, MinionPanelElementConfig> elements) {
        for (Map.Entry<Character, MinionPanelElementConfig> entry : elements.entrySet()) {
            Character symbol = entry.getKey();
            MinionPanelElementConfig element = entry.getValue();
            if (symbol == null) {
                throw new IllegalArgumentException("Minion panel element symbol is required");
            }
            if (element == null) {
                throw new IllegalArgumentException("Minion panel symbol '" + symbol + "' has no element definition");
            }
            if (element.action == null) {
                throw new IllegalArgumentException("Minion panel symbol '" + symbol + "' action is required");
            }
            if (element.material == null) {
                throw new IllegalArgumentException("Minion panel symbol '" + symbol + "' material is required");
            }
            if (element.displayName == null) {
                throw new IllegalArgumentException("Minion panel symbol '" + symbol + "' displayName is required");
            }
            if (element.lore == null) {
                throw new IllegalArgumentException("Minion panel symbol '" + symbol + "' lore is required");
            }
            for (int line = 0; line < element.lore.size(); line++) {
                if (element.lore.get(line) == null) {
                    throw new IllegalArgumentException(
                        "Minion panel symbol '" + symbol + "' lore line " + (line + 1) + " is required"
                    );
                }
            }
            if (element.amount < 1 || element.amount > 64) {
                throw new IllegalArgumentException(
                    "Minion panel symbol '" + symbol + "' amount must be between 1 and 64"
                );
            }
            if (element.customModelData < 0) {
                throw new IllegalArgumentException(
                    "Minion panel symbol '" + symbol + "' customModelData cannot be negative"
                );
            }
        }
    }

    public int rows() {
        return this.rows;
    }

    public List<Integer> slots(MinionPanelAction action) {
        return this.actionSlots.getOrDefault(action, List.of());
    }
}
