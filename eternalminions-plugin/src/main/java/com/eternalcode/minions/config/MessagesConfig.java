package com.eternalcode.minions.config;

import com.eternalcode.multification.notice.Notice;
import eu.okaeri.configs.OkaeriConfig;

public final class MessagesConfig extends OkaeriConfig {

    public Notice noPermission = Notice.chat(prefix() + "<white>Nie masz uprawnień do wykonania tej komendy!");
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
    public Notice minionTypeUnknown = Notice.chat(prefix() + "<red>Nieznany typ miniona.");
    public Notice minionPaused = Notice.chat(prefix() + "<white>Minion został wstrzymany.");
    public Notice minionResumed = Notice.chat(prefix() + "<white>Minion wznowił pracę.");

    public Notice upgradePurchased = Notice.chat(prefix() + "<white>Ulepszenie zakupione!");
    public Notice upgradeMaxed = Notice.chat(prefix() + "<red>To ulepszenie ma już maksymalny poziom.");
    public Notice upgradeRequiresLevel = Notice.chat(prefix() + "<red>Minion ma za niski poziom na to ulepszenie.");
    public Notice upgradeCannotAfford = Notice.chat(prefix() + "<red>Nie masz wystarczających przedmiotów na to ulepszenie.");

    public Notice chestLinkStart = Notice.chat(prefix() + "<white>Kliknij PPM skrzynię, aby połączyć ją z minionem.");
    public Notice chestLinked = Notice.chat(prefix() + "<white>Skrzynia została połączona z minionem.");
    public Notice chestUnlinked = Notice.chat(prefix() + "<white>Połączenie ze skrzynią zostało usunięte.");
    public Notice chestLinkTooFar = Notice.chat(prefix() + "<red>Ta skrzynia jest za daleko od miniona.");
    public Notice chestLinkExpired = Notice.chat(prefix() + "<red>Czas na wybór skrzyni minął.");

    public Notice minionRotated = Notice.chat(prefix() + "<white>Minion został obrócony.");
    public Notice minionModeChanged = Notice.chat(prefix() + "<white>Tryb pracy miniona został zmieniony.");

    private static String prefix() {
        return "<b><gradient:#FACC15:#FFE15F:#FACC15>ᴍɪɴɪᴏɴꜱ</gradient></b> <dark_gray>➤</dark_gray> ";
    }
}
