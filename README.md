# Guild Plugin - Kompletny system gildii dla Minecrafta

Guild Plugin to kompleksowy plugin serwerowy dla Minecrafta, który zapewnia kompletny system gildii/klanów dla twojego serwera. Dzięki temu pluginowi gracze mogą tworzyć i zarządzać własnymi gildiami, zapraszać członków, nawiązywać relacje między gildiami i korzystać z różnych funkcji gildyjnych.

## Główne Funkcje

### Zarządzanie Gildią
- Twórz i dostosowuj gildie (nazwa, tag, opis)
- Zarządzaj członkami gildii (zapraszaj, wyrzucaj, awansuj, degraduj)
- System uprawnień oparty na rolach (Lider, Oficer, Członek)
- Ustawiaj i teleportuj się do domu gildii
- System aplikacji do gildii

### System Ekonomii
- Zarządzanie funduszami gildii (wpłaty, wypłaty, przelewy)
- Konfiguracja opłaty za utworzenie gildii
- Integracja z systemem ekonomii (obsługa wielu pluginów ekonomicznych przez Vault)

### System Relacji
- Zarządzanie relacjami między gildiami (sojusz, wrogość, neutralność, wojna, rozejm)
- Powiadomienia o statusie relacji
- Alerty o stanie wojny

### System Poziomów
- Rozwój poziomu gildii
- Zwiększanie limitu członków
- Odblokowywanie dodatkowych funkcji gildii

### Interfejs Użytkownika
- Kompletny graficzny interfejs użytkownika (GUI)
- Intuicyjny system menu
- Konfigurowalny wygląd interfejsu

## Funkcje Techniczne

- **Przetwarzanie Asynchroniczne**: Wszystkie operacje na bazie danych są asynchroniczne, co zapewnia brak wpływu na wydajność serwera
- **Wsparcie dla wielu baz danych**: Obsługa zarówno SQLite, jak i MySQL
- **Wsparcie dla Placeholderów**: Integracja z PlaceholderAPI
- **Integracja Uprawnień**: Pełna zgodność z systemem uprawnień Bukkit
- **Wysoka Wydajność**: Zoptymalizowany kod zapewnia płynne działanie serwera

## Polecenia

- `/guild` - Główne polecenie gildii
- `/guildadmin` - Polecenie administracyjne gildii

## Uprawnienia

- Korzysta z wbudowanego systemu uprawnień

## Zmienne Podstawowych Informacji o Gildii

### Podstawowe Info Gildii
- `%guild_name%` - Nazwa gildii
- `%guild_tag%` - Tag gildii
- `%guild_membercount%` - Obecna liczba członków
- `%guild_maxmembers%` - Maksymalna liczba członków
- `%guild_level%` - Poziom gildii
- `%guild_balance%` - Saldo gildii (2 miejsca po przecinku)
- `%guild_frozen%` - Status gildii (Normalna/Zamrożona/Brak Gildii)

### Info o Gildii Gracza
- `%guild_role%` - Rola gracza w gildii (Lider/Oficer/Członek)
- `%guild_joined%` - Kiedy gracz dołączył do gildii
- `%guild_contribution%` - Wkład gracza w gildię

## Zmienne Sprawdzania Statusu Gildii

### Status Gracza
- `%guild_hasguild%` - Czy gracz ma gildię (Tak/Nie)
- `%guild_isleader%` - Czy gracz jest liderem (Tak/Nie)
- `%guild_isofficer%` - Czy gracz jest oficerem (Tak/Nie)
- `%guild_ismember%` - Czy gracz jest członkiem (Tak/Nie)

## Zmienne Sprawdzania Uprawnień Gildii

### Status Uprawnień
- `%guild_caninvite%` - Czy może zapraszać graczy (Tak/Nie)
- `%guild_cankick%` - Czy może wyrzucać członków (Tak/Nie)
- `%guild_canpromote%` - Czy może awansować członków (Tak/Nie)
- `%guild_candemote%` - Czy może degradować członków (Tak/Nie)
- `%guild_cansethome%` - Czy może ustawiać dom gildii (Tak/Nie)
- `%guild_canmanageeconomy%` - Czy może zarządzać ekonomią gildii (Tak/Nie)

## Wymagania

- Wersja serwera Minecraft: 1.21+
- Wersja Java: JDK 17+
- Opcjonalne zależności: Vault (dla wsparcia ekonomii), PlaceholderAPI (dla wsparcia placeholderów)

## Kroki Instalacji

1. Umieść plik jar pluginu w folderze `plugins` swojego serwera
2. Uruchom serwer - plugin automatycznie wygeneruje pliki konfiguracyjne
3. Edytuj pliki konfiguracyjne według potrzeb
4. Zrestartuj serwer, aby zastosować zmiany

---

# GuildPlugin - Kompletny system gildii dla Minecrafta

[English Above | Oficjalna Strona/Dokumentacja](http://chenasyd.codewaves.cn/)

GuildPlugin to wysokowydajny plugin systemu gildii dla serwerów Minecraft, wspierający wiele języków, obejmujący zarządzanie gildiami, ekonomię, relacje, poziomy, GUI i inne bogate funkcje, dostosowany do wielu popularnych pluginów ekonomicznych i uprawnień, całkowicie darmowy i open source!

## Główne Cechy

### Zarządzanie Gildią
- Wsparcie dla tworzenia, rozwiązywania, edycji gildii (nazwa, tag, opis)
- Dodawanie i usuwanie członków, awanse i degradacje, system uprawnień oparty na rolach "Lider/Oficer/Członek"
- Ustawianie domu gildii i teleportacja
- Mechanizm aplikacji/zaproszeń do gildii

### System Ekonomii
- Zarządzanie funduszami gildii: wpłaty, wypłaty, przelewy
- Konfigurowalny koszt utworzenia gildii
- Wsparcie dla integracji z wieloma pluginami ekonomicznymi przez Vault

### System Relacji
- Wsparcie dla relacji między gildiami: sojusz, wrogość, neutralność, wojna, rozejm itp.
- Zmiana statusu i mechanizm powiadomień, alarmy wojenne i informacje zwrotne o sojuszach

### System Poziomów
- Wzrost poziomu gildii, zwiększanie limitu członków
- Odblokowywanie większej liczby funkcji gildii

### Interfejs Użytkownika
- Kompletne menu GUI i interfejs operacyjny
- Możliwość niestandardowej konfiguracji, łatwa obsługa

### Cechy Techniczne
- Wszystkie operacje na bazie danych są asynchroniczne, bez lagów
- Elastyczne przełączanie między SQLite i MySQL
- Głęboka integracja z PlaceholderAPI, bogate zmienne
- Pełna zgodność z systemem uprawnień Bukkit
- Wysoka optymalizacja wydajności, stabilność i niezawodność

## Szybki Start

### Wymagania Środowiskowe
- Wersja serwera Minecraft: 1.21+
- Wersja Java: JDK 17+
- Opcjonalne zależności: Vault (wsparcie ekonomii), PlaceholderAPI (wsparcie zmiennych)

### Kroki Instalacji
1. Pobierz najnowszą wersję pliku jar pluginu i umieść go w katalogu `/plugins` serwera
2. Uruchom serwer, plugin automatycznie wygeneruje pliki konfiguracyjne
3. Zmodyfikuj pliki konfiguracyjne według potrzeb (config.yml/messages.yml/gui.yml/database.yml)
4. Zrestartuj serwer, aby zmiany weszły w życie

### Budowanie Maven (Deweloperzy)
```bash
mvn clean package
```

## Przegląd Głównych Poleceń

#### Polecenia Gracza
| Polecenie | Węzeł Uprawnień | Opis |
|------|----------|------|
| /guild                 | guild.use         | Otwórz menu główne |
| /guild create ...      | guild.create      | Utwórz gildię |
| /guild info            | guild.info        | Zobacz informacje o gildii |
| /guild members         | guild.members     | Zobacz listę członków |
| /guild invite ...      | guild.invite      | Zaproś do dołączenia |
| /guild kick ...        | guild.kick        | Usuń członka |
| /guild leave           | guild.leave       | Opuść gildię |
| /guild delete          | guild.delete      | Rozwiąż gildię |
| /guild promote ...     | guild.promote     | Awansuj członka |
| /guild demote ...      | guild.demote      | Zdegraduj członka |
| /guild accept ...      | guild.accept      | Zaakceptuj zaproszenie |
| /guild decline ...     | guild.decline     | Odrzuć zaproszenie |
| /guild sethome         | guild.sethome     | Ustaw dom |
| /guild home            | guild.home        | Teleportuj do domu |
| /guild apply ...       | guild.apply       | Aplikuj o dołączenie |

#### Polecenia Administratora
| Polecenie | Węzeł Uprawnień | Opis |
|------|----------|------|
| /guildadmin              | guild.admin           | Główne polecenie zarządzania |
| /guildadmin reload       | guild.admin.reload    | Przeładuj pliki konfiguracyjne |
| /guildadmin list         | guild.admin.list      | Zobacz wszystkie gildie |
| /guildadmin info ...     | guild.admin.info      | Zobacz szczegóły gildii |
| /guildadmin delete ...   | guild.admin.delete    | Wymuś usunięcie gildii |
| /guildadmin kick ... ... | guild.admin.kick      | Usuń gracza |
| /guildadmin relation     | guild.admin.relation  | Zarządzaj relacjami |
| /guildadmin test         | guild.admin.test      | Funkcje testowe |

## Wsparcie Zmiennych (PlaceholderAPI)

#### Zmienne Gildii
- %guild_name%：Nazwa gildii
- %guild_tag%：Tag gildii
- %guild_membercount%：Obecna liczba członków
- %guild_maxmembers%：Maksymalna liczba członków
- %guild_level%：Poziom gildii
- %guild_balance%：Fundusze (2 miejsca po przecinku)
- %guild_frozen%：Status (Normalny/Zamrożony/Brak Gildii)

#### Zmienne Gracza
- %guild_role%：Rola (Lider/Oficer/Członek)
- %guild_joined%：Czas dołączenia
- %guild_contribution%：Wartość wkładu

#### Zmienne Statusu
- %guild_hasguild%：Czy posiada gildię
- %guild_isleader%、%guild_isofficer%、%guild_ismember%：Określenie roli

#### Zmienne Uprawnień
- %guild_caninvite%、%guild_cankick%、%guild_canpromote%、%guild_candemote%、%guild_cansethome%、%guild_canmanageeconomy%

## Przykład Konfiguracji

### config.yml
```yaml
database:
  type: sqlite # Wsparcie dla sqlite lub mysql
  mysql:
    host: localhost
    port: 3306
    database: guild
    username: root
    password: ""
    pool-size: 10

guild:
  min-name-length: 3
  max-name-length: 20
  max-tag-length: 6
  max-description-length: 100
  max-members: 50
  creation-cost: 1000.0
permissions:
  default:
    can-create: true
    can-invite: true
    can-kick: true
    can-promote: true
    can-demote: false
    can-delete: false
```

## Struktura Bazy Danych (Główne Tabele)

- guilds (tabela gildii)
- guild_members (tabela członków)
- guild_applications (tabela aplikacji)
- guild_relations (tabela relacji)
- guild_economy (tabela ekonomii)

Przykłady SQL zobacz w plugins/database.sql.

## FAQ Częste Pytania

- Plugin nie uruchamia się? Sprawdź wersję serwera, JDK, czy zależności są kompletne, oraz czy format pliku konfiguracyjnego jest poprawny.
- System ekonomii nie działa? Potwierdź, że Vault i plugin ekonomiczny są zainstalowane, a config.yml jest poprawnie skonfigurowany.
- Błąd połączenia z bazą danych? Sprawdź konfigurację bazy danych, status działania MySQL, uprawnienia konta itp.
- Błąd interfejsu GUI? Sprawdź format pliku konfiguracyjnego i zastępowanie zmiennych.
- Niepowodzenie tworzenia gildii? Sprawdź fundusze gracza, czy nazwa nie jest zduplikowana lub za długa, czy gracz nie dołączył już do innej gildii itp.

## Dziennik Aktualizacji

### v1.0.0
- Wydanie wersji początkowej
- Kompletny system zarządzania gildiami
- Integracja systemu ekonomii
- Zarządzanie relacjami gildii
- System poziomów
- Kompletny interfejs GUI
- Wsparcie dla wielu baz danych
- System uprawnień
- Integracja z PlaceholderAPI
### v1.2.3
- Wydanie podstawowych funkcji
- Całkowicie poprawna obsługa logiki
- Pełne wsparcie dla rozszerzeń pluginu
- Pełna implementacja GUI
- Pełne wsparcie dla folia
- Wsparcie dla wielu baz danych
- Pełne wykorzystanie wbudowanego systemu uprawnień

### Planowane Funkcje
- [ ] System wojen gildii (częściowo zaimplementowany)
- [ ] Sklep gildii
- [ ] System zadań gildii
- [ ] Ranking gildii
- [ ] System wydarzeń gildii
- [ ] Magazyn gildii
- [ ] System ogłoszeń gildii
- [ ] System logów gildii
### Więcej Funkcji
- [ ] Rynek rozszerzeń pluginu (Warsztat)
- [ ] Szybkie pobieranie aktualizacji zasobów
- [ ] Szybkie zgłaszanie błędów
- [ ] Wskazywanie lokalizacji kodu błędu lub konkretnego problemu
- [ ] Oddzielne szczegółowe logi dla pluginu
- [ ] Więcej zabezpieczeń ułatwiających wykrywanie luk
- [ ] Bardziej kompletna logika systemu pluginu

## Strona Projektu i Wsparcie

- GitHub: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- Oficjalna Strona/Dokumentacja: [http://chenasyd.codewaves.cn/](http://chenasyd.codewaves.cn/)
- Strona Autora na Bilibili: [https://space.bilibili.com/1930829400](https://space.bilibili.com/1930829400)

## Licencja

GuildPlugin jest objęty licencją [GNU GPL v3.0](https://github.com/chenasyd/-GuildPlugin/blob/main/LICENSE), zachęcamy do wtórnego rozwoju i wkładu!
