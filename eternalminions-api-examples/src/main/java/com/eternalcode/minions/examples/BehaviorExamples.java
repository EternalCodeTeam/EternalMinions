package com.eternalcode.minions.examples;

import com.eternalcode.minions.behavior.MinionBehaviorDetails;
import com.eternalcode.minions.behavior.MinionBehaviorService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

final class BehaviorExamples {

    private final MinionBehaviorService behaviors;

    BehaviorExamples(MinionBehaviorService behaviors) {
        this.behaviors = behaviors;
    }

    boolean show(CommandSender sender) {
        List<String> behaviorIds = new ArrayList<>();
        for (MinionBehaviorDetails behavior : this.behaviors.findAll()) {
            behaviorIds.add(behavior.id());
        }

        ExampleMessages.info(sender, "Default behavior: " + this.behaviors.defaultBehavior().id());
        ExampleMessages.info(sender, "Enabled behaviors: " + String.join(", ", behaviorIds));
        return true;
    }
}
