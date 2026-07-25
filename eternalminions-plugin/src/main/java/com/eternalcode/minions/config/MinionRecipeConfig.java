package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.OkaeriConfig;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MinionRecipeConfig extends OkaeriConfig {

    public String id = "sticks";
    public String displayName = "<gray>Patyki";
    public Map<XMaterial, Integer> ingredients = defaultIngredients();
    public XMaterial resultMaterial = XMaterial.STICK;
    public int resultAmount = 4;

    public MinionRecipeConfig() {
    }

    private static Map<XMaterial, Integer> defaultIngredients() {
        Map<XMaterial, Integer> ingredients = new LinkedHashMap<>();
        ingredients.put(XMaterial.OAK_PLANKS, 2);
        return ingredients;
    }
}
