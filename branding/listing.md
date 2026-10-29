# TrueEconomy 2.0 — marketplace listing

## Listing title

**TrueEconomy 2.0 — A Living Server Economy | Market, Auctions, Banking & Shops**

## Short description

Turn money into a simulation: 65 commodities with supply and demand, 734 shop entries, escrowed
auctions, credit-scored banking, city taxes and player shops. 68 per-version builds, 1.8 → 26.3.

## Long description

### Your economy should react

Most economy plugins store a number and subtract it. TrueEconomy models the movement. Every
commodity has a base price, elasticity, supply and demand. Buying pushes the price up, selling
pushes it down, and time pulls it back toward base. The market screen shows the cause — supply,
demand, recent movement and a sparkline — instead of asking players to trust a random number.

Money also has a shape. There is a wallet you spend from and a bank that pays interest. Moving
between them is a decision. Loans need a credit score, defaulting damages it, and repaying repairs
it. An auction bid is held in escrow, not in the bidder's pocket.

### What is inside

- **Dynamic commodity market** — 65 commodities with base price, elasticity, supply, demand,
  price history, trends, breadth, eight sectors and market alerts.
- **Shop catalog** — 27 categories and 734 entries with finite global stock, restocking, a
  buy/sell spread, 5 custom items, 8 timed sales and an unobtainable-item blacklist.
- **Discovery-gated shops** — categories can require blocks a player has stepped on before they
  appear. Discoveries persist per identity and merge when identities are linked.
- **Auction house** — buy-it-now listings, escrowed bidding with a minimum increment, listing
  fees, sales tax, expiration resolution and one-click claiming.
- **Banking and credit** — wallet/bank split, compounding interest, credit-scored loans, credit
  repair on repayment and penalties on default.
- **Player shops** — create shops, hire up to five employees, set wages and track production,
  revenue and profit.
- **Government** — transaction tax collected into city treasuries, plus grants.
- **Seasonal events** — crisis and boom events that multiply commodity prices.
- **Identity linking** — tie a Java UUID and a Bedrock XUID with a 6-character code (5-minute
  TTL). Currency, bank balance and discoveries merge.
- **Progression** — 26 achievements, an escalating daily streak, a player dashboard with net
  worth, lifetime totals and a week-long chart.
- **Interface** — 16 GUI screens and 7 commands. The player never has to read a config file to
  use the economy.
- **Observability** — 27 transaction types written to an async append-only log, plus account and
  discovery persistence.
- **Storage and integrations** — YAML by default, SQLite or MySQL for larger servers. Vault
  economy provider and PlaceholderAPI placeholders. No hard dependencies.

### Version reach

The release ships **68 per-version jars**, one for each supported Minecraft version from **1.8 to
26.3**. Each jar is built against its own API line, with reflective shims for materials, item
meta, glints, unbreakable flags, custom model data and chat components. A vanilla variant (data
pack + Bedrock add-on) is included in the repository for servers that cannot run plugins.

### Commands

| Command | What it does |
| --- | --- |
| `/economy` | Opens the main menu. Subcommands: `balance`, `pay`, `top`, `set`, `give`, `take`, `reset`, `tax`, `market`, `auction`, `bank`, `loan`, `shop`, `events`, `link` |
| `/market` | `list`, `view <id>`, `trade <id> <amount> [sell]`, `gui` |
| `/auction` | `create`, `bid`, `buy`, `list`, `mylist`, `cancel`, `claim`, `gui` |
| `/bank` | `deposit`, `withdraw`, `interest`, `loan`, `credit` |
| `/shop` | Opens the shop menu. Subcommands: `create`, `list`, `manage`, `hire`, `fire`, `wage` |
| `/claim` | Claims the daily economy reward |
| `/events` | `status`, `history` |

Aliases: `/eco` and `/money` for `/economy`; `/ah` for `/auction`.

### Permissions

Ten nodes, all shallow: `economy.use`, `economy.admin`, `economy.market`, `economy.auction`,
`economy.bank`, `economy.link`, `economy.shop.create`, `economy.shop.trade`,
`economy.shop.admin`, `economy.government.admin`. Player-facing nodes default to true; staff
nodes default to op.

### Requirements

- Spigot or Paper **1.8 → 26.3**. Download the jar that matches the server version.
- Java 17+ to build from source; the shipped jars target Java 8 bytecode.
- Optional: Vault (economy provider) and PlaceholderAPI (`%trueeconomy_*%`).
- No hard dependencies. The plugin boots without Vault, PlaceholderAPI or a region plugin.

### Honest notes

- **The auction vendor-side bidding loop is not scheduled.** The config carries the knobs, but
  the shipped loop is player-driven. Manual listings, escrowed bids and buy-it-now are live.
- **There is no Vault bank support.** `hasBankSupport()` is false by design; TrueEconomy has its
  own wallet/bank split and credit model.
- **Government is single-region.** The shipped model uses one `default` region, not a multi-city
  simulation.
- **No NPC vendor entities spawn.** The vendor pricing model exists; nothing currently spawns it.
- **237 automated tests are included.** They cover the ledger, market, shops, auctions, banking,
  persistence and migration paths. Run them against your own config before a production rollout.
- **Loader ports are separate.** The Fabric/Forge/NeoForge scaffolding is not the Spigot/Paper
  product. Three NeoForge lines and Forge 1.16.5 are intentionally blocked; the server-plugin
  jars are the shipped path.

### What you get

- The correct shaded jar for your server version, chosen from 68 builds.
- A commented `config.yml` (5,300+ lines) and a separate `market/items.yml` with all 65
  commodities.
- Fifteen documentation files (14 numbered design documents plus an index) covering
  architecture, the ledger, market, shops, auctions, banking, government, identity, progression,
  commands, configuration and the data model.
- The source repository, so you can audit or extend it.

### Links

- Source and releases: <https://github.com/Snipernode/True-Economy>
- Documentation: <https://github.com/Snipernode/True-Economy/tree/main/docs>

---

## Suggested tags

`economy`, `market`, `auction`, `bank`, `shops`, `vault`, `placeholderapi`, `mysql`, `sqlite`,
`rpg`, `1.8`, `1.21`, `26.3`

## Suggested price band

Leave this to the seller. The asset set makes no pricing claim.
