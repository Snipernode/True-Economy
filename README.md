# True-Economy

A server-side economy plugin for Spigot/Paper that simulates a living economy: dynamic
supply-and-demand commodity markets, a discovery-based black market, player shops, an
auction house with escrow, banking and loans, government taxes, seasonal market events,
NPC merchants, and cross-account identity linking.

Rebuilt from decompiled sources and upgraded with modern storage, integrations, GUIs and
logging.

## Features

- **Dynamic market** — every commodity has base price, elasticity, supply and demand.
  Prices drift back toward their base over time; buying pushes them up, selling pushes
  them down. NPC merchants reprice against the same supply/demand state.
- **Black market (discoveries)** — shop sections unlock only when a player has *stepped on*
  the required blocks (e.g. bedrock, netherrack). Discoveries persist per identity and are
  merged when identities are linked.
- **Player shops** — hire up to 5 employees per shop, pay wages, earn revenue and produce
  output through a production line.
- **Auction house** — create listings with a buy-it-now, escrowed bidding with a minimum
  increment, listing fee and sales tax, expiration resolution and easy claiming.
- **Banking & loans** — wallet/bank split, interest per cycle, loans gated by credit
  score, credit repair on repayment and penalties on default.
- **Government** — sales/transaction tax collected into regional city wallets, plus grants.
- **Seasonal events** — random crisis/boom market events that multiply commodity prices.
- **Identity linking** — tie Java (UUID) and Bedrock (XUID) identities together with a
  6-character code; currency, bank and discoveries are merged.

## Integrations (upgrade)

- **Vault** — the plugin registers an economy provider (`net.milkbowl.vault:VaultAPI`).
- **PlaceholderAPI** — `%trueeconomy_*%` placeholders.
- **Storage** — YAML (default), SQLite or MySQL.
- **Async transaction logging** — every pay/tax/auction/shop/bank/admin transaction is
  appended to a log file.

## Commands

| Command | Description |
| --- | --- |
| `/economy balance` | Show wallet, bank and credit score |
| `/economy pay <player> <amount>` | Pay another player (transaction tax applies) |
| `/economy link start` | Generate a 6-char identity-link code (5 min TTL) |
| `/economy link <code>` | Complete a link from your other account |
| `/market list` | List commodities and prices |
| `/market view <id>` | Supply/demand and trend for a commodity |
| `/market trade <id> <amount> [sell]` | Buy or sell on the market |
| `/auction create <starting> [buynow] [duration]` | List an item |
| `/auction bid <id> <amount>` / `/auction buy <id>` | Escrowed bidding / buy-it-now |
| `/auction list` / `/auction mylist` / `/auction claim` | Browse, review, claim |
| `/bank deposit|withdraw <amount>` | Move money wallet ↔ bank |
| `/bank loan <amount>` | Take a loan (credit-score gated) |
| `/bank interest` / `/bank credit` | Rates, limits, credit report |
| `/shop <name>` | Open a shop / black-market sections |
| `/events` / `/events history` | Active & past seasonal market events |

## Configuration

- `config.yml` — market, auction, banking, npc, shops (sections + required blocks),
  government, seasonal and storage settings. See the file for the per-key reference.
- `market/items.yml` — commodity definitions (`base-price`, `elasticity`).
- Data files: `data/<ownerhash>.yml` accounts, `data/discoveries.yml` black-market
  unlocks, `data/links.yml` identity links.

## Build

Requires JDK 17+ (bytecode targets Java 8). Profiles per Minecraft version:

```bash
mvn -P v1.16 clean package   # also: v1.17, v1.18, v1.19, v1.20, v1.21, v26.1, v26.2
```

Artifacts land in `target/true-economy-<version>.jar` (shaded, includes SQLite/MySQL
drivers). Drop the JAR into your server's `plugins/` folder.

## Releases

Prebuilt JARs for all supported versions are attached to the
[Releases](https://github.com/Snipernode/True-Economy/releases) page.

## Notes

This codebase was reconstructed from the plugin's compiled JARs, then rebuilt and
extended. Some features referenced by the plugin's marketing (e.g. an active AI-bidder
loop) are config-only stubs in the current source.

## Loader builds: known blocked versions

The multi-loader builds (`true-economy-fabric-*`, `true-economy-forge-*`,
`true-economy-neoforge-*`) cover MC 1.16.5 → 26.3. Three NeoForge lines cannot be
built and are intentionally excluded:

- **NeoForge 1.20.2 / 1.20.3 / 1.20.5** — these predate the NeoForm publication era:
  their Maven POMs declare no `neoform` dependency, so modern toolchain (moddev)
  cannot process them. The era-correct NeoGradle 6.x (FG6-style userdev) path also
  fails because the upstream userdev `config.json` uses an `ats` string (NG6 expects
  an array) and NeoForge never published the `*_mapped_official_*` artifact these
  builds require.
- **NeoForge 26.2 / 26.3** — NeoForm cannot recompile the decompiled sources: they use
  Java 25 "Flexible Constructor Bodies" (a preview feature) and the recompiler runs
  without `--enable-preview` (javac `contents()` override error / ECJ `Pb(1103)`).
- **Forge 1.16.5** — the FG5-era ForgeGradle toolchain is incompatible with modern
  JDKs and produces an SRG-named compile classpath under official mappings; parked.
  Scaffolding remains in the build config for future work.