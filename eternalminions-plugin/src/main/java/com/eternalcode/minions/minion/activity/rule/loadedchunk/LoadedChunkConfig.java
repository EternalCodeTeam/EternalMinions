package com.eternalcode.minions.minion.activity.rule.loadedchunk;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

public class LoadedChunkConfig extends OkaeriConfig {

    @Comment({
            "Whether minions freeze while their own chunk is not loaded.",
            "Always freezes when triggered - there is nothing meaningful to slow down,",
            "the chunk simply is not in memory."
    })
    public boolean enabled = true;
}
