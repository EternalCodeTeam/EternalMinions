package com.eternalcode.minions.minion.tool;

import org.bukkit.Material;

public enum ToolCategory {

    PICKAXE,
    AXE,
    SHOVEL,
    HOE,
    FISHING_ROD,
    WEAPON,
    ANY,
    NONE;

    public static ToolCategory of(Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Tool material must not be null");
        }

        String materialName = material.name();
        if (materialName.equals("FISHING_ROD")) {
            return FISHING_ROD;
        }
        if (materialName.equals("TRIDENT") || materialName.endsWith("_SWORD")) {
            return WEAPON;
        }
        if (materialName.endsWith("_PICKAXE")) {
            return PICKAXE;
        }
        if (materialName.endsWith("_AXE")) {
            return AXE;
        }
        if (materialName.endsWith("_SHOVEL")) {
            return SHOVEL;
        }
        if (materialName.endsWith("_HOE")) {
            return HOE;
        }

        return NONE;
    }
}
