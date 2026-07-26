# TODO — stan implementacji vs. specyfikacja funkcjonalna

Legenda: ✅ zrobione • 🟡 częściowo / szkielet bez treści • ❌ brak

Dokument porównuje obecny stan kodu (moduły `eternalminions-api`, `eternalminions-plugin`) ze
specyfikacją funkcjonalną systemu minionków. Odnośniki do plików są względne wobec katalogu
repozytorium.

---

## Zmiany wprowadzone w tej sesji (do przetestowania)

Wszystkie cztery bugi z pierwotnej sekcji 0 zostały naprawione:

1. ✅ **Narzędzie w łapce pokazuje realny ekwipunek.** `createBody()` w `ArmorStandMinionRenderer`
   i `NpcMinionRenderer` używa teraz `minion.equipment().tool()` zamiast sztywnego
   `DIAMOND_PICKAXE`. Brak narzędzia = pusta ręka.
2. ✅ **Odświeżanie ekwipunku na żywo.** Dodano `MinionRenderer#refreshEquipment(MinionId, ItemStack)`
   (domyślnie no-op, zaimplementowane w `AbstractEntityLibMinionRenderer` z zabezpieczeniem
   `instanceof WrapperLivingEntity` — dla `RotatingHeadMinionRenderer` nadal nic nie robi, bo tam
   nie ma trzymanego narzędzia). `MinionRenderService.refreshEquipment(Minion)` i
   `MinionLifecycleService.update()` wołają to po każdej zmianie danych miniona, więc zmiana
   narzędzia w panelu natychmiast aktualizuje model.
3. ✅ **LPM podnosi miniona, PPM otwiera panel.** `MinionInteractionListener` rozróżnia teraz pakiet
   `ATTACK` (lewy klik → podniesienie) od `INTERACT_ENTITY` (prawy klik → panel), zamiast robić to
   samo dla obu.
4. ✅ **Podniesienie miniona zachowuje stan.** `MinionItemFactory.create(Minion)` zapisuje w PDC
   przedmiotu: typ (`behaviorId`), poziom, zserializowane narzędzie i całą zawartość magazynu.
   `MinionPlacementListener` odczytuje to przez `readState(item)` i odtwarza miniona z tymi samymi
   danymi zamiast stawiać świeżego. `MinionLifecycleService.pickup()` oddaje graczowi przedmiot z
   pełnym stanem oraz **każdy przedmiot z wewnętrznego magazynu** (do ekwipunku, a przy braku
   miejsca — upuszczone pod nogami, więc nic nie ginie).

Build (`./gradlew build`) przechodzi w całości, wszystkie dotychczasowe testy jednostkowe zielone.
Nie dodano nowych testów do tej zmiany (patrz sekcja "Do zrobienia dalej" niżej) — do zweryfikowania
ręcznie na serwerze testowym, patrz instrukcja testowania na końcu tego dokumentu.

**Świadomie pominięte w tej turze** (zostawione jako uproszczenie, opisane w oryginalnej specyfikacji
jako bardziej rygorystyczne): spec chce "odmówić podniesienia, jeśli nie da się bezpiecznie zachować
przedmiotów" — zaimplementowany wariant zamiast tego zawsze pozwala podnieść i upuszcza nadmiar na
ziemię pod graczem. Bezpieczne (nic nie ginie), ale nie blokuje podniesienia przy pełnym ekwipunku.

### Iteracja 2: warstwa "typ minionka jako config" (`minions/*.yml`)

Zrealizowany punkt 2 z sugerowanej kolejności prac (fundament pod przyszłe profesje):

1. ✅ **`MinionTypeConfig`** (`config/MinionTypeConfig.java`) — plik YAML per typ miniona z polami:
   `displayName` (MiniMessage), `workIntervalTicks`, `idleIntervalTicks`, `storageCapacity`,
   `npcScale`, `headTexture` (base64 z minecraft-heads.com), `helmet/chestplate/leggings/boots`
   (XMaterial) i `armorColor` (#RRGGBB dla skórzanej zbroi).
2. ✅ **`MinionTypeService`** (`minion/MinionTypeService.java`) — przy starcie skanuje
   `plugins/EternalMinions/minions/*.yml` (nazwa pliku = id typu). Gdy katalog pusty, tworzy
   domyślny `miner.yml` (Górnik: głowa + czerwona skórzana zbroja `#D63A3A`). Configi mapowane są
   **raz** do niemutowalnych obiektów `MinionType` z prefabrykowanymi ItemStackami (pofarbowana
   zbroja, oteksturowana głowa przez Paper `PlayerProfile`) — zero parsowania w gorącej ścieżce.
   `/minions reload` przebudowuje mapowanie (nowe pliki .yml wymagają restartu).
3. ✅ **Renderery czytają wygląd z typu**: ArmorStand dostaje hełm+zbroję z configu; NPC dostaje
   skina z `headTexture` (przez `MannequinMeta.setProfile`), skalę z `npcScale` i zbroję (hełm
   PLAYER_HEAD jest u NPC pomijany, żeby nie zakrywać skina blokiem). Narzędzie w ręce nadal
   wyłącznie z ekwipunku miniona.
4. ✅ **Hologram wieloliniowy i konfigurowalny** — linie w `config.yml` (`hologramLines`,
   MiniMessage) z placeholderami `{TYPE}` (nazwa typu), `{OWNER}` (nick właściciela), `{LEVEL}`
   (poziom). Domyślnie trzy linie: typ / właściciel / poziom. Tekst renderowany raz przy spawnie
   holograma (poziom nie zmienia się jeszcze w trakcie życia miniona — gdy dojdzie system leveli,
   trzeba dodać odświeżanie hologramu przy awansie).
5. ✅ **Przedmiot miniona** = oteksturowana głowa typu z nazwą z configu; PDC zawsze niesie id typu.
6. ✅ **`/minions give [typ]`** — opcjonalny argument typu z walidacją (`minionTypeUnknown` w
   messages.yml); bez argumentu daje typ domyślny (`miner`).
7. ✅ **Pojemność magazynu** nowego miniona z configu typu (przy odtwarzaniu z podniesionego
   przedmiotu zachowywana jest pojemność zapisana w przedmiocie).
8. ✅ **Interwały pracy per typ** w `MinionActionEngine` (fallback 40/100 ticków dla nieznanego
   typu). Postawienie przedmiotu z nieistniejącym już typem jest blokowane z komunikatem.

**Ograniczenie (świadome):** nowe pliki typów tworzą osobne "skórki/parametry", ale **każdy typ
nadal zachowuje się jak górnik** (kopanie 3×3) — rejestr zachowań per profesja to punkt 3 kolejności
prac. Poziomy/ulepszenia w configu typu też czekają na punkt 4 (system jeszcze nie istnieje).

### Iteracja 3: rejestr zachowań (`MinionBehavior`)

Zrealizowany punkt 3 z sugerowanej kolejności prac:

1. ✅ **`MinionBehavior`** (`scheduler/MinionBehavior.java`) — interfejs profesji:
   `boolean execute(Minion, ScheduledMinion, World)`; zwraca, czy wykonano pracę (steruje wyborem
   interwału work/idle).
2. ✅ **`MiningBehavior`** (`scheduler/MiningBehavior.java`) — cała logika kopania (skan 3×3,
   `canMine`, drop do magazynu, animacja) wyniesiona z `MinionActionEngine` 1:1, z wstrzykniętymi
   zależnościami (registry/persistence/renderer).
3. ✅ **`MinionActionEngine` odchudzony** — zostało tylko: harmonogram (kopiec), budżet czasu/akcji,
   rozwiązanie typ→behavior i świat; nie zna już pojęcia "kopania".
4. ✅ **`behavior` w configu typu** — `minions/*.yml` ma nowe pole `behavior` (na razie tylko
   `MINER`, enum `MinionBehaviorType`), mapowane do `MinionType` i używane przez silnik do wyboru
   implementacji z mapy (`Map<MinionBehaviorType, MinionBehavior>` w wiringu pluginu).

Dodanie nowej profesji (np. Drwal) = nowa klasa `MinionBehavior` + nowa wartość enum + wpis w mapie
w `EternalMinionsPlugin` + plik typu z `behavior: LUMBERJACK`. Silnik i scheduler bez zmian.

### Iteracja 4: poziomy + postęp numeryczny + start/pauza

Zrealizowany punkt 4 z kolejności prac (bez sklepu ulepszeń — same poziomy) oraz start/pauza z
punktu 5:

1. ✅ **`MinionProgress` = (level, progress)** — licznik wykonanych czynności (wykopanych bloków);
   `advanced(MinionType)` nalicza +1 i awansuje przez progi. Progi w `minions/*.yml`:
   `levelThresholds` (łączny postęp wymagany na kolejne poziomy; domyślnie
   `[1000, 5000, 15000, 40000]` → max poziom 5; pusta lista = zawsze poziom 1). Walidacja: dodatnie,
   ściśle rosnące.
2. ✅ **Naliczanie w `MiningBehavior`** — każdy wykopany blok = +1 postępu; przy awansie hologram
   odświeża się natychmiast (nowa metoda `MinionRenderer#refreshHologram`, aktualizacja
   `TextDisplay` bez respawnu encji; celowo brak odświeżania co blok — zero kosztu w hot pathie).
3. ✅ **Persystencja postępu** — nowa kolumna `progress BIGINT` w `eternal_minion_progress` z
   automatyczną migracją starych baz (próba `SELECT progress` → przy braku kolumny
   `ALTER TABLE ADD COLUMN` — działa na H2/MySQL/MariaDB/Postgres). Postęp zapisywany też w PDC
   przedmiotu przy podnoszeniu i odtwarzany przy stawianiu.
4. ✅ **Start/pauza w panelu** — nowa akcja `TOGGLE_ACTIVE` (symbol `A` w domyślnym patternie,
   ikona LEVER), przełącza `minion.active()`; wstrzymany minion nie pracuje (silnik odpytuje go w
   rytmie idle). Komunikaty `minionPaused`/`minionResumed` w messages.yml.
5. ✅ **Nowe placeholdery panelu** — `{MINION_MAX_LEVEL}`, `{MINION_PROGRESS}`,
   `{MINION_PROGRESS_REQUIRED}` (`MAX` na maks. poziomie), `{MINION_STATUS}` (teksty `statusWorking`
   / `statusPaused` w panel.yml); `{MINION_BEHAVIOR}` pokazuje teraz displayName typu. Domyślny
   element informacji rozszerzony o postęp i status.

**Uwaga migracyjna:** istniejący `panel.yml` nie dostanie automatycznie nowego elementu `A` ani
nowych linii lore — usuń `panel.yml` (i ewentualnie `minions/*.yml` dla `levelThresholds`), żeby
wygenerowały się nowe domyślne.

### Iteracja 5: system ulepszeń + link do skrzyni

Zrealizowana reszta punktu 4 (ulepszenia) i duży kawałek punktu 5 (link do skrzyni):

**Ulepszenia:**
1. ✅ Trzy rodzaje (`MinionUpgradeKind`): **SPEED** (cykl pracy w tickach), **RANGE** (promień
   kopania 1–3, czyli 3×3 → 5×5 → 7×7) i **CAPACITY** (sloty magazynu, max 54). Tiery per typ w
   `minions/*.yml` (sekcja `upgrades`): `requiredLevel` (wymagany poziom miniona), `value`,
   `costMaterial` + `costAmount` (płatność przedmiotami z ekwipunku — celowo bez Vault; ekonomia
   dojdzie razem ze sklepem).
2. ✅ Panel ulepszeń (`MinionUpgradePanel`) otwierany z głównego panelu (symbol `U`, butelka XP) —
   trzy ikony (konfigurowalne w `panel.yml` → `upgradeElements`) z placeholderami tieru, wartości,
   kosztu i wymaganego poziomu; klik = zakup z pełną walidacją (max tier / za niski poziom / brak
   przedmiotów — osobne komunikaty). Zakup zapisuje się **natychmiast** (`lifecycle.updateNow`,
   bez czekania na dirty-flush), więc restart tuż po zakupie niczego nie cofa.
3. ✅ Efekty działają realnie: SPEED zmienia interwał planowania, RANGE rozszerza pole kopania
   (`MinionMiningTargets` prekomputuje tablice ofsetów+yaw dla promieni 1–3, środek zawsze
   sprawdzany ostatni), CAPACITY powiększa magazyn od ręki (`MinionStorage.resized`).
4. ✅ Persystencja: tabela `eternal_minion_upgrades` + zapis w PDC przedmiotu (podniesienie/
   postawienie zachowuje tiery); przy wczytywaniu z bazy magazyn jest przywracany do pojemności
   wynikającej z ulepszeń.

**Link do skrzyni:**
1. ✅ Przycisk w panelu (symbol `L`, hopper, placeholder `{MINION_CHEST}`): bez połączenia — klik
   uruchamia tryb wyboru (`chestLinkTimeoutSeconds`, domyślnie 10 s) i zamyka panel; gracz klika
   PPM dowolny kontener (`Container`: skrzynia/beczka/…) w zasięgu `chestLinkDistanceBlocks`
   (domyślnie 10) w tym samym świecie. Z połączeniem — klik rozłącza.
2. ✅ Dostawa łupu: wykopane przedmioty najpierw trafiają do połączonej skrzyni; gdy pełna,
   nieistniejąca albo w niezaładowanym chunku — do wewnętrznego magazynu; nadmiar jak dotąd na
   ziemię. Nic nie jest usuwane bez śladu.
3. ✅ Persystencja: tabela `eternal_minion_chests`; link **nie** jedzie w przedmiocie przy
   podniesieniu (celowo — minion zmienia miejsce, dystans by się nie zgadzał).

**Świadomie pominięte:** sprawdzanie uprawnień właściciela do kontenera (integracje z ochroną
terenu dojdą osobno), stan `STORAGE_FULL` w hologramie, ulepszenie dystansu linku.

### Iteracja 7: pozostałe profesje (model Hypixel Skyblock)

Zrealizowany punkt 3 dla wszystkich profesji + duży kawałek sekcji 2. Model jak na Hypixel:
minion to **timerowy generator zasobów** — produkuje swój drop-table do magazynu w cyklu, a nie
wymaga realnych drzew/mobów w świecie (zgodnie z tym jak działają miniony na Hypixel Skyblock).

1. ✅ **Wspólna baza `AbstractMinionBehavior`** — deponowanie do skrzyni→magazyn→ziemia, naliczanie
   postępu, animacja, guard „pełny magazyn = idle" (nowe `MinionStorage.hasFreeSlot`). Górnik
   przepisany na tę bazę bez zmiany zachowania.
2. ✅ **`GeneratorBehavior`** (jedna klasa dla Drwala/Rolnika/Rybaka/Zabójcy/Craftera) — losuje
   drop-table typu (`MinionDrop`: materiał, min-max, szansa) i deponuje. Domyślne tabele:
   - Drwal: dąb (1-2), sadzonka (10%), jabłko (3%).
   - Rolnik: pszenica (1-2), nasiona (50%).
   - Rybak: dorsz (70%), łosoś (20%), rozdymka (5%), pryzmaryn (5%) — **wymaga wody** obok
     (`requiresWater`, jak fishing minion na Hypixel).
   - Zabójca: zgnilizna (1-2), sztabka żelaza (2%), szmaragd (1%).
   - Crafter: patyk (1-2) — placeholder, bo Hypixel nie ma tej profesji.
3. ✅ **`CollectorBehavior`** (Zbieracz) — zbiera przedmioty z ziemi w promieniu (`collectorRadiusBlocks`,
   domyślnie 4) do magazynu/skrzyni, max 8 encji na akcję (bounded scan na głównym wątku).
4. ✅ **`SellerBehavior`** (Sprzedawca) — sprzedaje skonfigurowane itemy (`sellPrices`) z połączonej
   skrzyni **i** magazynu, wypłaca kasę właścicielowi przez **Vault**. Vault to miękka zależność
   (brak artefaktu w buildzie) — rozwiązywany przez odizolowany `VaultEconomyHook` (refleksja tylko
   tu, poza hot-pathem, degraduje się do no-op gdy brak Vault/ekonomii). `sellBatch` limituje sztuki
   na akcję.
5. ✅ **8 domyślnych typów** ma teraz przypisaną profesję + drop-table/ceny (miner→MINER,
   lumberjack→LUMBERJACK, farmer→FARMER, fisherman→FISHERMAN, killer→KILLER, collector→COLLECTOR,
   seller→SELLER, crafter→CRAFTER). `MinionWork` grupuje parametry per-profesja, żeby konstruktor
   `MinionType` nie eksplodował.

**Świadomie uproszczone vs. Hypixel:** Zabójca nie spawnuje realnych mobów (produkuje ich dropy na
timerze — funkcjonalnie jak Hypixel), Rolnik/Drwal nie sadzą/ścinają realnych roślin (generują
plony). To celowy, wydajny model generatora — bez modyfikacji świata i bez wymogu dostarczania
surowców. Można w przyszłości dodać warianty „realnych" profesji, ale generator jest wierny temu,
co gracz widzi na Hypixel (zasoby lecą do magazynu w cyklu).

### Iteracja 6: rotacja + tryb pracy (LINEAR) + filtr materiałów

Domknięcie punktu 5 (poza sklepem):

**Rotacja:**
1. ✅ `MinionDirection` (S/W/N/E z offsetami i yaw), `MinionSettings(direction, miningMode)` na
   każdym minionie. Nowy minion przejmuje **kierunek patrzenia gracza** przy stawianiu
   (`MinionDirection.fromYaw`).
2. ✅ Przycisk „Rotacja" w panelu (symbol `R`, kompas) obraca o 90°; model (armor stand/NPC)
   obraca się natychmiast (`MinionRenderer#refreshRotation` → pakiet rotacji, bez respawnu).
   Encja spawnuje się od razu w dobrym kierunku (`RenderLocation` używa yaw kierunku).

**Tryb pracy:**
3. ✅ `MiningMode` SQUARE/LINEAR + przycisk „Tryb pracy" (symbol `M`, kilof). SQUARE = dotychczasowe
   pole wokół siebie (skalowane zasięgiem). LINEAR = kopanie w linii w kierunku patrzenia; długość
   linii = `2*promień+1` (rośnie z ulepszeniem zasięgu).

**Filtr materiałów:**
4. ✅ W `minions/*.yml`: `blockedMaterials` (blacklista — domyślnie spawner + kontenery, BEDROCK
   zawsze) i `allowedMaterials` (whitelista — pusta = wszystko poza blacklistą). Sprawdzane w
   `MinionType.canMine(Material)` (EnumSet, brak alokacji w hot pathie). Minion nigdy nie zniszczy
   też własnej połączonej skrzyni.

**Persystencja:** tabela `eternal_minion_settings` (kierunek + tryb) + tryb w PDC przedmiotu
(kierunek celowo nie — nowy minion bierze kierunek gracza). Wszystko z bezpiecznym fallbackiem na
domyślne przy nieznanych wartościach.

### Iteracja 5b: domyślne wyglądy 8 typów minionków

Na życzenie użytkownika przy pustym katalogu `minions/` generuje się teraz **8 plików typów**
z gotowym wyglądem (tekstura głowy z minecraft-heads + kolor skórzanej zbroi):

| Plik | Nazwa | Kolor zbroi |
| --- | --- | --- |
| `miner.yml` | Górnik | `#C8C8C8` (szary) |
| `lumberjack.yml` | Drwal | `#00C8FF` (błękitny) |
| `farmer.yml` | Rolnik | `#00FF00` (zielony) |
| `fisherman.yml` | Rybak | `#00FFFF` (cyjan) |
| `collector.yml` | Zbieracz | `#FFFF00` (żółty) |
| `crafter.yml` | Crafter | `#7D4B1E` (brązowy) |
| `seller.yml` | Sprzedawca | `#C800FF` (fioletowy) |
| `killer.yml` | Zabójca | `#FF0000` (czerwony) |

Techniczne: `ConfigService.create` dostał wariant z seedem wartości domyślnych (nadpisuje pola
przed pierwszym zapisem pliku; istniejące pliki bez zmian), a `MinionTypeService` trzyma listę
`DEFAULT_TYPES`. **Uwaga:** wszystkie typy mają na razie `behavior: MINER` — dopóki nie powstaną
kolejne profesje, każdy z nich kopie jak górnik (to tylko skórki + osobne parametry/ulepszenia).

### Poprawka po zgłoszeniu użytkownika: duplikacja przedmiotów z magazynu

Po pierwszym wdrożeniu punktu 4 wyżej zgłoszony został bug: po podniesieniu miniona zawartość
magazynu trafiała do gracza fizycznie (`giveOrDrop` per slot) **i jednocześnie** była zakodowana w
PDC samego przedmiotu miniona (`items.create(current)` brał `current` razem z jego pełnym
magazynem). Przy ponownym postawieniu `MinionPlacementListener` odtwarzał magazyn z PDC — więc
gracz miał te same przedmioty dwa razy (raz w ekwipunku z podniesienia, raz odtworzone w nowym
minionie).

✅ Naprawione: `MinionLifecycleService.pickup()` koduje w przedmiocie miniona stan z **wyzerowanym
magazynem** (`current.withStorage(new MinionStorage(storage.capacity()))`), bo zawartość magazynu
jest już przekazana graczowi osobno jako luźne przedmioty. Typ, poziom i narzędzie nadal są
zachowywane w przedmiocie tak jak wcześniej.
(`eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionLifecycleService.java`)

---

## 0. Pilne poprawki / bugi (rzeczy, które już "działają", ale źle) — ✅ naprawione, patrz wyżej

Historyczny opis problemów (przed poprawką) zachowany dla kontekstu:

- ~~Narzędzie w łapce ignorowało ekwipunek miniona~~ — naprawione, patrz punkt 1 wyżej.
- ~~Brak odświeżania wyglądu po zmianie ekwipunku~~ — naprawione, patrz punkt 2 wyżej.
- ~~Lewy przycisk myszy nie podnosił miniona~~ — naprawione, patrz punkt 3 wyżej.
- ~~Podniesienie miniona kasowało jego stan~~ — naprawione, patrz punkt 4 wyżej.

---

## 1. Rdzeń danych (właściciel, typ, poziom, statystyki, ulepszenia)

- ✅ Właściciel, typ (`behaviorId` jako `String`), pozycja, aktywność, narzędzie, magazyn —
  `Minion`, `MinionEquipment`, `MinionStorage`, `MinionPosition`.
- ✅ Poziom + numeryczny postęp (`MinionProgress(level, progress)`) z progami per typ w
  `minions/*.yml` (`levelThresholds`) — awans automatyczny podczas pracy (iteracja 4).
- ❌ Brak systemu ulepszeń w ogóle: brak modelu ulepszenia, brak tabeli w bazie, brak kosztów,
  brak panelu ulepszeń, brak wpływu na zasięg/szybkość/pojemność/wydajność.
- ❌ Brak `behaviorId` jako realnego rejestru zachowań — cała logika kopania jest zaszyta wprost w
  `MinionActionEngine`, nie ma pluggable "Miner/Killer/Fisherman/Seller/Lumberjack/Collector".
  `behaviorId` to dziś tylko etykieta tekstowa ("miner"), niewykorzystywana do wyboru zachowania.
- ❌ Brak limitu minionków na gracza, brak sprawdzania uprawnień do terenu / integracji z
  ochroną (WorldGuard itp.) przy stawianiu.
- ❌ Brak opcji wymuszania ładowania chunków — i tak jest to zgodne z domyślnym wymaganiem
  specyfikacji (`world.isChunkLoaded()` check w `MinionActionEngine.execute()` pomija niezaladowane
  chunki), ale nie ma configurowalnej flagi dla administratora, gdyby chciał to świadomie włączyć.

## 2. Rodzaje minionków

| Typ | Status |
| --- | --- |
| Górnik | ✅ kopie realne bloki wokół siebie (SQUARE/LINEAR, zasięg z ulepszeń, filtr materiałów) |
| Zabójca | ✅ generator dropów mobów (model Hypixel; nie spawnuje realnych mobów) |
| Rybak | ✅ generator ryb, wymaga wody obok |
| Sprzedawca | ✅ sprzedaje skrzynię/magazyn przez Vault (brak jeszcze ShopGUIPlus/EconomyShopGUI) |
| Drwal | ✅ generator kłód/sadzonek/jabłek |
| Zbieracz | ✅ zbiera przedmioty z ziemi w promieniu |
| Rolnik | ✅ generator plonów |
| Crafter | ✅ generator (placeholder — brak odpowiednika na Hypixel) |

Logika działania jest wydzielona do strategii `MinionBehavior` (patrz iteracja 3) — kopanie żyje w
`MiningBehavior`, silnik jest generyczny. Kolejne profesje to nowe implementacje interfejsu.

Tryby kopania górnika:
- ✅ pole wokół miniona (`SQUARE`) na `Y-1`, skalowane ulepszeniem zasięgu (3×3/5×5/7×7).
- ✅ tryb `LINEAR` (kopanie w linii w kierunku patrzenia) + przełącznik trybu w panelu.
- ✅ whitelista/blacklista bloków per typ (`blockedMaterials`/`allowedMaterials`), BEDROCK i
  połączona skrzynia zawsze chronione.
- ❌ brak modyfikatorów Fortune/Silk Touch, własnych tabel dropu.

## 3. Wyświetlanie miniona (NPC / ArmorStand)

- ✅ Wybór renderera globalnie przez `MinionsConfig.minionRenderer`
  (`ARMOR_STAND` / `ROTATING_HEAD` / `NPC`).
- ✅ Kierunek patrzenia + animacja pracy dla ArmorStand i NPC (`faceTarget`, machnięcie/łuk ręki).
- ❌ Brak wyboru renderera **per typ minionka** (spec: "ustawienie może być inne dla każdego typu
  minionka") — dziś jest to jedna globalna wartość dla całego pluginu.
- ❌ Brak atrybutu `minecraft:generic.scale` konfigurowalnego per typ dla NPC — `NpcMinionRenderer`
  ma sztywne `0.55D` wpisane w kodzie, żadnego configu, żadnych bezpiecznych min/max granic.
- ❌ Brak konfiguracji wyglądu ArmorStand per typ (mały/duży, widoczność ramion/podstawy, poza
  głowy/tułowia/nóg, hełm/zbroja) — wszystko zaszyte na stałe w `createBody()`.
- ❌ Brak wsparcia dla **stroju zależnego od profesji** (np. górnik: głowa górnika + czerwona
  skórzana zbroja) — patrz sekcja 8 niżej, to punkt zgłoszony przez użytkownika.
- ✅ NPC/ArmorStand nie chodzą, nie dostają obrażeń, nie są celem (Mannequin `immovable`, ArmorStand
  bez AI — potwierdzone przez typ encji, brak dodatkowej pracy potrzebnej).

## 4. Narzędzia profesji

- 🟡 Slot narzędzia w panelu istnieje (`TOOL_SLOT`), zapisuje się w `MinionEquipment` i w bazie
  (`MinionEquipmentTable`), ale:
  - ❌ nie ma **żadnej walidacji** — dowolny przedmiot (nawet ziemia) można włożyć jako "narzędzie",
    plugin nie sprawdza, czy pasuje do profesji.
  - ❌ nie ma configu whitelisty materiałów per profesja/poziom, nie ma sprawdzania enchantów,
    `CustomModelData`, nazwy przedmiotu ani wytrzymałości.
  - ❌ minion nie przestaje pracować, gdy brakuje narzędzia (`MinionActionEngine.mine()` samo bierze
    `minion.equipment().tool()` i jeśli `null`, po prostu woła `block.getDrops()` bez narzędzia —
    czyli **kopie gołą ręką bez żadnej kary/blokady**).
  - ❌ brak zużywania wytrzymałości narzędzia.
  - ❌ brak statusów `gotowe / brak / nieprawidłowe / zniszczone` w hologramie/panelu.
  - ✅ narzędzie widoczne w łapce modelu odzwierciedla teraz to, co jest w slocie (naprawione w tej
    sesji).
  - ✅ przy podniesieniu miniona narzędzie jest zachowywane razem z resztą danych (naprawione w tej
    sesji).

## 5. Poziom, postęp i ulepszenia

- ✅ Licznik postępu + configurowalne progi poziomów per typ + persystencja (kolumna `progress`
  z migracją starych baz) — iteracja 4.
- ❌ System ulepszeń (zasięg/szybkość/pojemność/wydajność/link do skrzyni, koszty, panel zakupów,
  zapis zakupionych ulepszeń w bazie) — nadal brak; poziomy na razie niczego nie odblokowują poza
  liczbą w panelu/hologramie.

## 6. Sterowanie minionkiem

- ✅ Postawienie: sprawdzenie wolnego miejsca (`target.isEmpty()` + blok nad), zapis w bazie,
  przypisanie właściciela (`MinionPlacementListener`).
  - ❌ brak sprawdzania limitu minionków gracza i uprawnień do terenu/budowy.
  - ❌ kierunek/rotacja przy stawianiu nie jest przejmowana z ustawienia gracza (nie ma w ogóle pola
    rotacji na `Minion`/`MinionPosition`).
- 🟡 PPM: otwiera panel, ale weryfikacja dostępu to tylko `ownerId == player` — brak "zaufany
  gracz"/administrator z uprawnieniami, o czym mówi spec.
- ✅ LPM: podnosi miniona (naprawione w tej sesji), PPM otwiera panel.
- 🟡 Bezpieczny flow podniesienia z pkt. 1-5 specyfikacji: zatrzymanie pracy ✅, zapis stanu w
  przedmiocie ✅, przeniesienie magazynu ✅ (do ekwipunku / upuszczone pod graczem), usunięcie
  dopiero po przekazaniu ✅. Brakuje jeszcze: ❌ odmowa podniesienia, gdy nie da się bezpiecznie
  zachować przedmiotów (obecnie zawsze się udaje, nadmiar ląduje na ziemi zamiast zablokować akcję).

## 7. Panel zarządzania

- ✅ W pełni konfigurowalny układ/sloty/materiały/lore/CustomModelData (`MinionPanelConfig`,
  `MinionPanelElementConfig`, `PanelItemFactory`).
- ✅ Sloty: informacje, magazyn, narzędzie, odbiór przedmiotów, podniesienie.
- ❌ Brak akcji: **Rotacja**, **Ulepszenia**, **Link do skrzyni**, **Tryb pracy**, **Filtr**,
  **Start/pauza**, **Podgląd zasięgu** — żadna z nich nie istnieje w `MinionPanelAction` (dziś:
  `NONE, TOOL_SLOT, STORAGE_SLOT, MINION_INFORMATION, COLLECT_ITEMS, PICKUP_MINION`).
- 🟡 Panel informacji pokazuje tylko typ/poziom/magazyn — brakuje czasu pracy, liczby wykonanych
  czynności, zdobytych przedmiotów, zasięgu, cyklu pracy, statusu narzędzia zgodnie z przykładem w
  spec.

## 8. Łączenie ze skrzynią

- ✅ Wybór skrzyni klikiem (tryb linkowania z timeoutem), zapis lokalizacji w bazie, sprawdzanie
  odległości/świata/istnienia kontenera przy każdej dostawie, fallback do wewnętrznego magazynu —
  iteracja 5.
- ❌ Brakuje: sprawdzanie uprawnień właściciela do kontenera (ochrona terenu), stan `STORAGE_FULL`
  w hologramie/panelu, konfigurowalne zachowanie przy pełnym magazynie.

## 9. Hologram

- 🟡 Hologram jest wieloliniowy i konfigurowalny (`hologramLines` w config.yml, MiniMessage) z
  placeholderami `{TYPE}`, `{OWNER}`, `{LEVEL}`. Brakuje jeszcze placeholderów ze specyfikacji:
  `{progress}`, `{range}`, `{speed}`, `{storage}`, `{tool_status}`, `{status}`, `{actions}`,
  `{mode}` (większość czeka na systemy, które ich dotyczą — progres, ulepszenia, statusy narzędzia).
  Tekst renderuje się raz przy spawnie — po dodaniu progresu/statusów trzeba dodać odświeżanie po
  zmianie danych (nie co tick).

## 10. CustomModelData

- ✅ Wspierane dla ikon panelu (`MinionPanelElementConfig.customModelData` +
  `PanelItemFactory.create()`).
- ❌ Nie wspierane dla samego przedmiotu-miniona (`MinionItemFactory.create()` nie ma pola CMD) ani
  dla ekwipunku/narzędzi profesji.

## 11. Sklep z minionkami

- ❌ Brak w całości — nie ma GUI sklepu, integracji Vault, płatności przedmiotami, limitów zakupu.

## 12. Komendy i uprawnienia

- 🟡 Istnieją tylko `/minions give` i `/minions reload`
  (`MinionGiveCommand`, `ReloadCommand`) — oba dają zawsze ten sam, jedyny typ miniona ("górnik").
- ❌ Brak `/minion` (menu), `/minion sklep`, `/minion lista`, `/minion admin`; brak `give <gracz>
  <typ> [liczba]` z wyborem typu (bo nie ma wielu typów); brak uprawnień typu
  `minions.limit.<liczba>`, `minions.admin`.

## 13. Dane i statystyki (baza danych)

Obecne tabele: `MinionTable` (pozycja/właściciel/typ/aktywność), `MinionEquipmentTable` (narzędzie),
`MinionProgressTable` (poziom), `MinionStorageTable` (magazyn).

- ❌ Brak tabeli/kolumn na: ulepszenia, tryb pracy, filtr materiałów/mobów, lokalizację połączonej
  skrzyni, sposób wyświetlania/skalę per minion (jeśli miałoby być nadpisywalne per instancja),
  statystyki (łączny czas pracy, liczba cykli, liczba obsłużonych celów, zarobiona kwota, data
  postawienia).
- ✅ Persistencja jest asynchroniczna z "dirty tracking" (`DirtyMinionTracker`,
  `MinionPersistenceService`) — sam mechanizm zapisu jest solidny, brakuje tylko kolumn na nowe
  dane, gdy te funkcje powstaną.

---

## Zgłoszone dodatkowo przez użytkownika

### A. Osobne configi per typ minionka w `/minions/`

Chciane: pliki typu `miner.yml` w folderze `plugins/EternalMinions/minions/`, każdy opisujący dla
danego typu: parametry pracy, ulepszenia, progi poziomów, **oraz wygląd** — osobno dla NPC i dla
ArmorStand (np. dla górnika: tekstura głowy w stylu górnika + czerwona skórzana zbroja).

**Stan dziś:** nie istnieje żaden mechanizm per-typ. Nie ma nawet folderu `resources/` w module
(`eternalminions-plugin/src/main/resources` nie istnieje), więc dziś w ogóle nie ma domyślnych
plików konfiguracyjnych dostarczanych z jarem poza tym, co generuje `OkaeriConfig` programowo
(`MinionsConfig`, `MinionPanelConfig`, `MessagesConfig`, `DatabaseConfig`). Żeby to zrobić, trzeba:

1. Zdefiniować `MinionBehaviorConfig` (OkaeriConfig) z polami: identyfikator typu, parametry pracy
   (zasięg, cykl, tryb kopania itd.), listę poziomów/progów, listę ulepszeń, oraz blok wyglądu:
   - `npc`: skórka/tekstura głowy, zbroja (helm/chestplate/leggings/boots), skala,
   - `armorStand`: helm, zbroja, pozy (o ile potrzebne różne od domyślnych).
2. Wczytywać wszystkie pliki z katalogu `minions/*.yml` przy starcie (`ConfigService` już umie
   tworzyć/wczytywać pojedyncze configi — trzeba dodać metodę skanującą katalog i tworzącą po jednym
   configu na plik, z nazwą pliku jako identyfikatorem typu).
3. Podłączyć wynik do rejestru zachowań (patrz punkt 1 wyżej — obecnie nie istnieje) oraz do
   rendererów, żeby `createBody()` czytał helm/zbroję/skórkę z configu danego typu zamiast stałego
   `XMaterial.PLAYER_HEAD`. Narzędzie w łapce ma zostać jak jest dziś — czytane z ekwipunku miniona,
   nie z configu typu (patrz punkt B).

To razem jest największy pojedynczy kawałek pracy w całym dokumencie — w praktyce wymaga
wprowadzenia całej warstwy "typ minionka jako dane konfiguracyjne", której dziś nie ma (dziś typ to
tylko etykieta tekstowa).

### B. Narzędzie w łapce ma pokazywać to, co jest w GUI, nie to, co w configu

✅ Naprawione w tej sesji — `minion.equipment().tool()` jest teraz jedynym źródłem prawdy dla
narzędzia w łapce, zarówno przy tworzeniu encji, jak i przy zmianie w panelu (`refreshEquipment`).
Punkt A (per-typ domyślne/wymagane narzędzie z configu) wciąż nie istnieje — gdy powstanie, config
typu powinien być tylko sugestią/wymogiem startowym, a wyświetlane ma pozostać zawsze to faktycznie
włożone przez gracza, zgodnie z tym, co tu naprawiono.

---

## Sugerowana kolejność prac

1. ✅ **Bugi z sekcji 0** — zrobione w tej sesji.
2. ✅ **Warstwa "typ minionka jako config"** — zrobione (iteracja 2 wyżej): `MinionTypeConfig` +
   `MinionTypeService` + `minions/*.yml` + wygląd per typ w rendererach. Bez sekcji
   leveli/ulepszeń w configu — dojdzie razem z punktem 4.
3. ✅ **Rejestr zachowań** — zrobione (iteracja 3 wyżej): interfejs `MinionBehavior`,
   `MiningBehavior`, pole `behavior` w configu typu. Kolejne profesje
   (Zabójca/Rybak/Drwal/Zbieracz/Sprzedawca) to teraz osobne implementacje bez ruszania silnika.
4. ✅ **Poziomy i ulepszenia** — poziomy + postęp (iteracja 4), ulepszenia SPEED/RANGE/CAPACITY z
   kosztami w przedmiotach i panelem zakupów (iteracja 5). Brak jeszcze: ulepszenie „wydajność",
   koszty w walucie (Vault — razem ze sklepem).
5. ✅ **Panel: rotacja, tryb pracy, filtr, link do skrzyni, start/pauza** — komplet: start/pauza
   (it. 4), link do skrzyni (it. 5), rotacja + tryb LINEAR + filtr materiałów (it. 6).
6. **Sklep i komendy** (`/minion lista`, `/minion admin`, `/minion sklep`) — zależne od punktu 2/4.
7. **Integracje sprzedawcy** (ShopGUIPlus/EconomyShopGUI) — dopiero po tym, jak profesja Sprzedawca
   w ogóle powstanie.

---

## Jak przetestować iterację 7 (profesje)

1. **Usuń folder `plugins/EternalMinions/minions/`** → restart wygeneruje 8 plików z profesjami
   i drop-table. Ustaw testowo `workIntervalTicks: 10` w kilku, żeby szybciej widzieć efekty.
2. `/minions give lumberjack` → postaw → co cykl do magazynu wpadają kłody dębu (czasem sadzonka/
   jabłko). Podobnie `farmer` (pszenica), `killer` (zgnilizna + rzadkie żelazo/szmaragd).
3. `/minions give fisherman` → postaw **z dala od wody** → nie pracuje; postaw obok kałuży wody →
   zaczyna produkować ryby.
4. `/minions give collector` → rzuć przedmioty na ziemię obok (promień 4) → minion je zbiera do
   magazynu; encje itemów znikają.
5. `/minions give seller` → połącz go ze skrzynią (przycisk hopper), do której inny minion (np.
   miner) wrzuca urobek → jeśli masz plugin ekonomii z Vault, sprzedawca sprzedaje itemy z
   `sellPrices` i dostajesz kasę. Bez Vaulta sprzedawca po prostu nie pracuje (brak błędów).
6. Wszystkie profesje respektują link do skrzyni, ulepszenia (szybkość/pojemność), poziomy/postęp,
   pauzę i hologram — to ten sam silnik, różni się tylko zachowanie.

## Jak przetestować iterację 6 (rotacja, tryb pracy, filtr)

1. Usuń `panel.yml` i `minions/*.yml` (nowe domyślne: przyciski `R`/`M` w panelu, sekcje
   `blockedMaterials`/`allowedMaterials` w typach).
2. Postaw miniona patrząc w różne strony → armor stand/NPC od razu jest obrócony w Twoją stronę.
3. Panel → kompas („Rotacja") → minion obraca się o 90° na żywo; lore pokazuje aktualny kierunek.
4. Panel → kilof („Tryb pracy") → przełącz na „Linia": minion kopie teraz w linii w kierunku,
   w którym patrzy (obróć go kompasem, żeby zmienić linię); z powrotem „Kwadrat" = pole wokół.
5. Filtr: w `minions/miner.yml` ustaw `allowedMaterials: [STONE]` → minion kopie tylko kamień,
   ignoruje ziemię/rudy. Wyczyść i ustaw `blockedMaterials: [DIAMOND_ORE]` → kopie wszystko opróc
   diamentów. Postaw skrzynię w polu kopania → minion jej nie rusza.
6. Restart serwera → kierunek i tryb zachowane. Podnieś/postaw → tryb zachowany, kierunek = nowy
   kierunek patrzenia (celowo).

## Jak przetestować iterację 5 (ulepszenia + link do skrzyni)

1. Usuń `panel.yml` i `minions/miner.yml` (nowe domyślne z przyciskami `U` i `L` oraz sekcją
   `upgrades`). Do testów obniż w `miner.yml` progi: `levelThresholds: [10, 25, 50]`.
2. Postaw miniona, daj sobie diamenty (`/give @s diamond 64`). PPM → butelka XP („Ulepszenia"):
   - zakup **Szybkości** przy poziomie 1 → komunikat o za niskim poziomie; po awansie na 2 →
     zakup zdejmuje 8 diamentów, minion wyraźnie przyspiesza (cykl 30 ticków).
   - **Zasięg** tier 1 (poziom 2, 16 diamentów) → minion kopie 5×5 pod sobą zamiast 3×3.
   - **Pojemność** (poziom 3) → magazyn w panelu ma więcej slotów… uwaga: sloty `S` w patternie
     to tylko podgląd — liczba w `{STORAGE_CAPACITY}` rośnie do 18.
   - bez diamentów → komunikat o braku przedmiotów; po wykupieniu wszystkich tierów → „MAX".
3. Restart serwera → tiery zostają (tabela w bazie); podnieś/postaw miniona → tiery i pojemność
   zostają (PDC przedmiotu).
4. **Link do skrzyni**: postaw skrzynię obok miniona, PPM → hopper („Link do skrzyni") → panel się
   zamyka, klik PPM w skrzynię → „Skrzynia została połączona". Od teraz urobek wpada do skrzyni
   (magazyn miniona zostaje pusty). Zapełnij skrzynię → nadmiar wraca do magazynu miniona.
5. Klik w hopper ponownie → rozłączenie. Skrzynia dalej niż 10 kratek → „za daleko". Odczekanie
   >10 s bez kliknięcia → „czas minął". Zniszcz połączoną skrzynię → minion po prostu wraca do
   swojego magazynu (bez błędów).

## Jak przetestować iterację 4 (poziomy, postęp, pauza)

1. Usuń `panel.yml` i `minions/miner.yml` (żeby wygenerowały się nowe domyślne z progami i
   przyciskiem pauzy). Stara baza może zostać — kolumna `progress` doda się sama przy starcie.
2. W `minions/miner.yml` ustaw niskie progi do testów, np. `levelThresholds: [10, 25, 50]`
   (max poziom 4) i `workIntervalTicks: 5`, restart.
3. Postaw miniona nad kamieniem → w panelu (PPM) element "Informacje" pokazuje
   `Poziom: 1/4`, `Postęp: X/10`, `Status: Pracuje`. Otwieraj panel ponownie — postęp rośnie.
4. Po 10 wykopanych blokach → hologram sam zmienia `Poziom: 1` na `Poziom: 2` (bez restartu).
5. Kliknij dźwignię (Start/pauza) → komunikat "Minion został wstrzymany", minion przestaje kopać,
   status w panelu = `Pauza`; drugi klik wznawia.
6. Podnieś miniona (LPM) i postaw ponownie → poziom i postęp zachowane. Restart serwera → też
   zachowane (kolumna w bazie).

## Jak przetestować iterację 2 (typy z `minions/*.yml`)

1. Usuń stary folder configów albo zostaw — przy starcie serwera plugin utworzy
   `plugins/EternalMinions/minions/miner.yml`.
2. Postaw miniona → powinien mieć **czerwoną skórzaną zbroję** (armor stand), a nad nim
   **trzyliniowy hologram**: `Górnik` / `Właściciel: <twój nick>` / `Poziom: 1`. Linie edytujesz
   w `config.yml` → `hologramLines` (placeholdery `{TYPE}`, `{OWNER}`, `{LEVEL}`).
3. W `miner.yml` wklej w `headTexture` wartość "Value" dowolnej głowy z minecraft-heads.com,
   zrób restart → armor stand ma oteksturowaną głowę; w trybie NPC (config.yml →
   `minionRenderer: NPC`) tekstura staje się skinem NPC, a `npcScale` zmienia rozmiar.
4. Skopiuj `miner.yml` jako np. `digger.yml`, zmień `displayName` i `armorColor`, restart →
   `/minions give digger` daje przedmiot z nową nazwą; postawiony minion ma inny kolor zbroi.
   `/minions give costam` → komunikat o nieznanym typie.
5. `/minions reload` po zmianie np. `armorColor` w istniejącym pliku → nowo stawiane miniony
   dostają nowy kolor (już zespawnowane przebiorą się po ponownym wejściu w zasięg/restarcie).
6. Zmień `workIntervalTicks` na np. 10 → minion kopie wyraźnie szybciej.

---

## Jak przetestować zmiany z tej sesji

1. `./gradlew build` — powinno przejść bez błędów (już zweryfikowane).
2. Wejdź na serwer testowy, `/minions give` — dostajesz przedmiot miniona.
3. Postaw miniona, otwórz panel (**PPM** na minionie) i włóż dowolny kilof/narzędzie w slot
   narzędzia (`T`) → **model od razu powinien pokazać to narzędzie w ręce**, bez potrzeby
   przebudowywania/reloadu.
4. Poczekaj aż minion wykopie kilka bloków (widać po magazynie w panelu) — upewnij się, że w ręce
   cały czas jest to samo narzędzie, które włożyłeś, a nie domyślny kilof.
5. **LPM** (lewy klik) na minionie → powinien zniknąć i trafić do ekwipunku jako przedmiot; **PPM**
   dalej ma tylko otwierać panel, nie podnosić.
6. Rozbij/podnieś miniona z jakimś narzędziem w slocie i czymś w magazynie wewnętrznym (nie klikaj
   "Odbierz przedmioty" wcześniej) → sprawdź, że po podniesieniu dostałeś zarówno przedmiot
   narzędzia w ekwipunku (albo na ziemi, jeśli ekwipunek pełny), jak i zawartość magazynu — nic nie
   powinno zniknąć.
7. Postaw ponownie ten sam (podniesiony) przedmiot minionka → poziom i narzędzie powinny wrócić
   takie, jakie były przed podniesieniem, ale **magazyn ma być pusty** — te przedmioty dostałeś już
   fizycznie w kroku 6, więc nie powinny się pojawić drugi raz w nowo postawionym minionie
   (to był zgłoszony bug na duplikację — sprawdź, że po tej poprawce już się nie powtarza).
