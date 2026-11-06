# AmonPack — Dokumentacja Wersji Dungeony, Broń Palna & Battle Royale Zombie
**Branch:** `feature/dungeons-and-guns` (Opublikowany w repozytorium GitHub)  
**Wersja:** 1.1 (Build produkcyjny na serwer)  
**Plik wynikowy JAR:** `target/AmonPack-1.1.jar`

---

## 1. Podsumowanie Wprowadzonych Zmian

W tej gałęzi projekt został przystosowany do w pełni samodzielnego działania na serwerze (bez wymagania zewnętrznych pluginów magii), zachowując kluczowe moduły rozgrywki:

1. **Całkowite usunięcie ruchów magicznych PK (ProjectKorra abilities):**
   - Usunięto ponad 65 autorskich zdolności bendingu PK z pakietu `Abilities.PK_Abilities` (w tym gałęzie Chi, Wody, Ognia, Ziemi i Powietrza).
   - Usunięto zbędną rejestrację sub-elementów Dymu i Dźwięku oraz listenerów combo/triggerów z PK (`AbilitiesListener`, `SpecialTriggerManager`).
   - Usunięto twarde zależności do klas ProjectKorra w głównym kodzie. Wprowadzono bezpieczny adapter `Plugin.ProjectKorraHook`, autorską bazę `Plugin.SQLite` oraz `Plugin.TempBlock`.
   - Z `plugin.yml` usunięto `depend: ['ProjectKorra']` — plugin działa w 100% samodzielnie bez ProjectKorra na serwerze (`softdepend: [WorldEdit, FastAsyncWorldEdit, ProjectKorra, ProtocolLib]`).

2. **Usunięcie ulepszeń magii PK (Upgrades):**
   - Z konfiguracji drzewka umiejętności (`RPG/skilltree.yml`) usunięto 51 węzłów ulepszeń zdolności PK (`IsSkillUpgrade`, `IsAbilityUpgrade`, `IsPassiveUpgrade`).
   - Usunięto powiązane menu GUI ulepszeń PK (`SkillUpgradeMenuHolder`).

3. **W PEŁNI PRZYWRÓCONY Tryb Zombie (Battle Royale / PvPvE):**
   - Cały pakiet `RPG.BattleRoyale` został przywrócony i zaktualizowany pod Bukkit API 1.21.
   - Dostępne są wszystkie zaawansowane mechaniki: unikalne typy zombie, infekcja wirusem i antidotum, mechanika krwi, barykady, wykrywanie hałasu, plecaki taktyczne, boty AI wypełniające luki w graczach, strefy burzy/skażenia oraz zrzuty zaopatrzenia (Air Drop).
   - Przywrócono konfigurację `src/main/resources/battleroyale.yml` oraz komendę `/hungergames` (aliasy: `/hg`, `/br`, `/battleroyale`).

4. **Poprawka Paczki Zasobów (Texture Pack Fix):**
   - W konfiguracji `pack/pack_config.yml` zmieniono `AutoSendOnJoin: false` oraz `ForcePack: false`.
   - Plugin nie zmusza graczy do pobierania packa przy wejściu i nie wyrzuca ich z serwera, co zapobiega crashom i usuwaniu własnych texturepacków graczy.

5. **Nienaruszone i w 100% zachowane:**
   - **System Dungeonów (Lochów Rogue-lite):** Pełna logika generowania, walk, fal potworów, bossów, skrzyń z łupami, kluczy, błogosławieństw oraz ulepszeń startowych (`/dungbuild`).
   - **System Broni Palnej (Guns):** Wszystkie archetypy broni, rusznikarstwo, modyfikacje broni, amunicja, efekty dźwiękowe/cząsteczkowe i pancerze strzeleckie.
   - **Systemy Wspierające RPG:** Progresja etapowa (`slow_progression`), customowe bloki, rudy i przedmioty (ItemsAdder / PackManager), bossowie MythicMobs, crafting na formach, bounties oraz system drużyn (party).

---

## 2. System Dungeonów (Lochów Rogue-lite)

Modułowy system lochów instancjonowanych działający na dedykowanych światach tymczasowych.

### Główne cechy
- **Instancje tymczasowe:** Każdy rajd lochu tworzy odizolowany świat (lub instancję), w którym gracze przechodzą przez sekwencję sal i wyzwań.
- **Kondycje i zadania w pokojach (`DungeonCondition`):**
  - `KILL_MOBS`: Eliminacja wyznaczonych potworów lub bossów MythicMobs.
  - `INTERACTION`: Aktywacja dźwigni, przycisków, składanie ofiar lub interakcja z blokami.
  - `KEY_INSERTION`: Otwieranie wrót przy użyciu znalezionych w lochu kluczy.
  - `PRESSURE_PLATE`: Mechanizmy naciskowe wymagające koordynacji drużyny.
  - `PARKOUR` / `REBUILD`: Wyzwania zręcznościowe oraz odbudowywanie struktur.
- **Błogosławieństwa (Dungeon Blessings) i Wyposażenie Unikalne:**
  - *Sierp Wiatru (Wind Sickle):* Ładowanie pędu w biegu, fala uderzeniowa i obrażenia dźwiękowe.
  - *Bliźniacze Ostrza Błękitnego Ducha (Blue Spirit Swords):* Płynna walka dwoma mieczami, bonusy za parowanie.
  - *Sztylety Mai (Mai Daggers):* Rzut wachlarzem sztyletów, oznaczanie i spowalnianie celów.
  - *Młot Ziemi (Earth Hammer):* Zwiększona obrona, fala uderzeniowa przy 100% naładowania.
  - *Laska Wody (Water Staff):* Aura lecząca sojuszników, odpychanie i spowalnianie wrogów wokół.
- **Skrzynie ze Skarbami (`DungeonLootChest`):**
  - Wybór nagród po ukończeniu etapów (ulepszenia statystyk HP, DEF, ATK, krytyków lub perki broni).
- **Startowe Ulepszenia Lochów (`/dungbuild`, `dung_build.yml`):**
  - Odblokowywanie trwałych bonusów przed wejściem do lochu na podstawie poziomu statystyki `DUNGEON` (dodatkowe życia, startowe mikstury, redukcja cooldownów).

### Komendy Dungeonów
| Komenda | Opis | Uprawnienia |
|---|---|---|
| `/dungeons start <nazwa>` | Rozpoczyna rajd do wybranego lochu (np. `makapu_bandyci`, `przykladowy_dungeon`) | Gracze / Drużyna |
| `/dungeons leave` | Opuszczenie aktywnego lochu i powrót na spawn | Uczestnik lochu |
| `/dungeons skills` | Otwiera menu umiejętności dungeonowych | Uczestnik lochu |
| `/dungeons reload` | Przeładowuje pliki konfiguracji lochów | Administrator (`amonpack.admin`) |
| `/dungbuild` | Otwiera GUI startowych ulepszeń eksploracji lochów | Wszyscy gracze |

---

## 3. System Broni Palnej i Rusznikarni (Guns & Gunsmithing)

Autorski silnik broni palnej z fizyką ray-castingu, odrzutem, modyfikacjami i amunicją.

### Archetypy Broni Palnej (`GunType`)
1. **Flintlock Pistol (Pistolet Skałkowy):**
   - Lekka, poręczna broń boczna o szybkim czasie przeładowania (2.4s).
2. **Flintlock Musket (Muszkiet Piechoty):**
   - Długodystansowa broń piechoty o wysokiej celności, dużych obrażeniach pojedynczego strzału i bagnecie.
3. **Blunderbuss (Garłacz Rozpylający):**
   - Potężny rozrzut śrutu, zadający olbrzymie obrażenia w zwarciu.
4. **Pepperbox (Pieprzniczka Obrotowa):**
   - 4-strzałowy pistolet z magazynkiem obrotowym umożliwiający oddanie szybkiej serii strzałów.

### Mechanika Strzelania i Modyfikacje
- **Ray-Casting:** Precyzyjna detekcja trafień z uwzględnieniem trafień w głowę (Headshot multiplier 1.5x).
- **Poziomy Ulepszeń (Tier 1-5):** Bronie w świecie gry lub z zestawów ulepszeń (`KEY_UPGRADE_TIER`) zyskują wyższe obrażenia, szybszy przeładunek i mniejszy rozrzut.
- **Unikalne Modyfikacje (`GunUniqueMod`):**
  - `SCOPED`: Luneta przybliżająca obraz.
  - `EXTENDED_MAG`: Powiększony magazynek.
  - `DRAGON_BREATH`: Amunicja podpalająca.
  - `HEAVY_FRAME`: Zwiększone obrażenia kosztem odrzutu.
  - `LIGHTWEIGHT`: Zwiększona prędkość ruchu strzelca.
  - `DUAL_SHOT`: Podwójny strzał.

### Typy Amunicji (`AmmoType`)
- `LEAD_BULLET` (Ołowiana Kula): Standardowy pocisk do pistoletów i muszkietów.
- `SCATTER_SHOT` (Śrut): Wiązka śrucin do garłaczy i strzelb.
- `SLUG_CARTRIDGE` (Breneka): Ciężki pocisk pojedynczy o ogromnej sile obalającej.
- `DRAGON_CARTRIDGE` & `DRAGON_SCATTER_SHOT`: Płonące pociski ze smoczym prochem.

---

## 4. Tryb Battle Royale / Apokalipsa Zombie (`RPG.BattleRoyale`)

Kompletny tryb PvPvE łączący mechaniki Battle Royale z survivalem zombie.

### Główne Elementy Rozgrywki
- **Arena i Zmniejszająca się Strefa:**
  - Dynamicznie kurcząca się strefa (Border) zadająca rosnące obrażenia graczom poza kręgiem.
  - Wizualizacja krawędzi strefy cząsteczkami i kompas nawigacyjny.
- **Specjalne Zombie (`CustomZombieManager`):**
  - *Skoczek Zombie (Leaper):* Szybki mutant skaczący na odległość w stronę gracza.
  - *Strzelec Zombie (Gunner):* Zombie wyposażony w broń palną i strzelający ołowiem.
  - *Prześladowca (Stalker):* Niewidoczny zabójca atakujący z ukrycia.
  - *Spuchlak (Bloater):* Potężny mutant wybuchający po śmierci chmurą toksycznego kwasu.
  - *Ocaleniec (Survivor):* Zombie z plecakiem pełnym zapasów medycznych i amunicji.
- **System Infekcji Wirusowej (`InfectionManager`):**
  - Ugryzienie przez zombie lub kontakt z krwią może zarazić gracza wirusem.
  - Zegar infekcji (domyślnie 5 minut do przemiany). Uleczenie wymaga Antidotum (`KEY_BR_CURE`).
- **Plecaki Taktyczne (`BackpackManager`):**
  - Start gry bez plecaka (dostępny tylko dolny pasek hotbar 0-8; reszta slotów zablokowana).
  - Zdobycie plecaka Tier I, II lub III odblokowuje kolejne rzędy ekwipunku.
- **Wydarzenia Dynamiczne (`DynamicEventManager`):**
  - *Air Drop (Zrzut Zaopatrzenia):* Lądujące na spadochronie skrzynie z elitarnym ekwipunkiem i amunicją.
  - *Blackout (Awaria zasilania):* Ciemność i ślepota na arenie.
  - *Blood Moon (Krwawy Księżyc):* Wściekłe, podwójnie szybkie zombie.
  - *Alarmy Samochodowe:* Przypadkowe potrącenie wraku wyzwala głośny alarm wabiący hordę.
- **Barykady i Zamknięte Drzwi:**
  - Budowanie barykad z desek opóźniających zombie.
  - Otwieranie żelaznych drzwi za pomocą kluczy (`KeyManager`).

### Komendy Battle Royale
| Komenda | Opis | Uprawnienia |
|---|---|---|
| `/hungergames join` | Dołączenie do lobby meczu | Wszyscy gracze |
| `/hungergames leave` | Opuszczenie gry | Gracze |
| `/hungergames start` | Wymuszenie startu meczu przez admina | `hungergames.admin` |
| `/hungergames stop` | Zatrzymanie aktywnego meczu | `hungergames.admin` |
| `/hungergames event <airdrop\|blackout\|bloodmoon>` | Wywołanie zdarzenia dynamicznego | `hungergames.admin` |
| `/hungergames wand` | Różdżka do konfiguracji lootu i drzwi na arenie | `hungergames.admin` |

---

## 5. Instrukcja Wdrożenia na Serwerze

1. **Plik wtyczki:**
   - Gotowy, skompilowany plik JAR znajduje się pod ścieżką:  
     `target/AmonPack-1.1.jar`
2. **Wymagania serwera:**
   - Silnik: Paper / Purpur / Spigot 1.20.x – 1.21.x.
   - Java: 17 lub 21.
   - ProjectKorra: **NIEWYMAGANY**. Jeśli PK jest na serwerze, AmonPack bezpiecznie z nim współpracuje; jeśli go nie ma, wtyczka startuje bezbłędnie.
   - Opcjonalne: WorldEdit / FastAsyncWorldEdit, ProtocolLib, ItemsAdder.
3. **Instalacja:**
   - Skopiuj plik `AmonPack-1.1.jar` do folderu `plugins/` na serwerze.
   - Uruchom serwer. W konsoli zobaczysz bezbłędny start:
     `[AmonPack] AmonPack włączony [Bending: false, SkillTree: false, Dungeons: true, BattleRoyale: true, RPG: true]`
