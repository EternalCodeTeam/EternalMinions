package com.eternalcode.minions.config;

import com.eternalcode.multification.notice.Notice;
import eu.okaeri.configs.OkaeriConfig;

public final class MessagesConfig extends OkaeriConfig {

    public Notice noPermission = Notice.chat(prefix() + "<white>Nie masz uprawnień do wykonania tej komendy! <gray>({PERMISSION})");
    public Notice playerNotFound = Notice.chat(prefix() + "<white>Nie znaleziono takiego gracza.");
    public Notice playerOnly = Notice.chat(prefix() + "<white>Ta komenda jest dostępna tylko dla graczy.");
    public Notice correctUsage = Notice.chat(prefix() + "<white>Poprawne użycie: <green>{USAGE}");
    public Notice correctUsageHead = Notice.chat(prefix() + "<white>Poprawne użycie:");
    public Notice correctUsageEntry = Notice.chat("<dark_gray>➤</dark_gray> <green>{USAGE}");
    public Notice reloadCompleted = Notice.chat(prefix() + "<white>Konfiguracja została przeładowana.");

    public Notice minionToolUpdated = Notice.chat(prefix() + "<white>Narzędzie miniona zostało zaktualizowane.");
    public Notice minionStorageCollected = Notice.chat(prefix() + "<white>Odebrano przedmioty z magazynu miniona.");
    public Notice minionPickedUp = Notice.chat(prefix() + "<white>Minion został podniesiony.");
    public Notice minionOwnerRequired = Notice.chat(prefix() + "<red>Tylko właściciel może zarządzać tym minionem.");
    public Notice minionNotFound = Notice.chat(prefix() + "<red>Ten minion już nie istnieje.");
    public Notice minionPlaced = Notice.chat(prefix() + "<white>Postawiono miniona.");
    public Notice minionPlacementBlocked = Notice.chat(prefix() + "<red>W tym miejscu nie można postawić miniona.");
    public Notice minionItemReceived = Notice.chat(prefix() + "<white>Otrzymano przedmiot miniona.");

    private static String prefix() {
        return "<b><gradient:#00ffbb:#84ff7c>EternalMinions</gradient></b> <dark_gray>➤</dark_gray> ";
    }
}
