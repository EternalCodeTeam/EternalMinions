package com.eternalcode.minions.examples;

import com.eternalcode.minions.EternalMinionsApi;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

public final class MinionApiCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
        "behaviors",
        "owned",
        "snapshot",
        "at",
        "give",
        "inspect",
        "create",
        "remove",
        "rotate",
        "chest",
        "tool",
        "storage",
        "upgrade",
        "progress",
        "status",
        "access",
        "shop"
    );

    private final BehaviorExamples behaviors;
    private final MinionQueryExamples queries;
    private final MinionItemExamples items;
    private final MinionManagementExamples management;
    private final MinionStatusExamples statuses;
    private final MinionAccessExamples access;
    private final MinionShopExamples shop;

    public MinionApiCommand(EternalMinionsApi api, MinionShopExamples shop) {
        this.behaviors = new BehaviorExamples(api.minionBehaviorService());
        this.queries = new MinionQueryExamples(api.minionService());
        this.items = new MinionItemExamples(api.minionItemService());
        this.management = new MinionManagementExamples(
            api.minionManagementService(),
            api.minionService()
        );
        this.statuses = new MinionStatusExamples(api.minionStatusService());
        this.access = new MinionAccessExamples(api.minionAccessService(), api.minionService());
        this.shop = shop;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] arguments
    ) {
        if (arguments.length == 0) {
            ExampleMessages.usage(sender);
            return true;
        }

        return switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "behaviors" -> this.behaviors.show(sender);
            case "owned" -> this.queries.showOwned(sender);
            case "snapshot" -> this.queries.showSnapshot(sender, arguments);
            case "at" -> this.queries.showAt(sender);
            case "give" -> this.items.give(sender, arguments);
            case "inspect" -> this.items.inspect(sender);
            case "create" -> this.management.create(sender, arguments);
            case "remove" -> this.management.remove(sender, arguments);
            case "rotate" -> this.management.rotate(sender, arguments);
            case "chest" -> this.management.setChest(sender, arguments);
            case "tool" -> this.management.setTool(sender, arguments);
            case "storage" -> this.management.setStorage(sender, arguments);
            case "upgrade" -> this.management.setUpgrade(sender, arguments);
            case "progress" -> this.management.setProgress(sender, arguments);
            case "status" -> this.statuses.update(sender, arguments);
            case "access" -> this.access.check(sender, arguments);
            case "shop" -> this.shop.update(sender, arguments);
            default -> {
                ExampleMessages.usage(sender);
                yield true;
            }
        };
    }

    @Override
    public @NotNull List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] arguments
    ) {
        if (arguments.length != 1) {
            return List.of();
        }

        String prefix = arguments[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream()
            .filter(subcommand -> subcommand.startsWith(prefix))
            .toList();
    }
}
