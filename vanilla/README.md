# True-Economy — Vanilla Version (no plugins/mods required)

True-Economy is available for **Minecraft Java (data pack, 1.20.2+)** and
**Minecraft Bedrock (ScriptAPI add-on, 1.21+)**, implementing the same living economy:
a supply-and-demand market, a discovery-based black market, a bank with interest, and
payments between players.

```
vanilla/
  java/     Java Edition data pack   (drop into your world's datapacks folder)
  bedrock/  Bedrock behavior pack    (ScriptAPI + action forms)
```

---

## Java Edition (data pack)

### Install

1. Copy the contents of `vanilla/java/` (the `pack.mcmeta` + `data/` folder)
   into `<world>/datapacks/true-economy/`.
2. Start the world (or run `/reload`). Loading creates the scoreboards and seeds
   the market state in `store trueeconomy:main`.
3. Open the menu:

```
/function trueeconomy:menu
```

Banks, the market and the black market are fully clickable from there.

### Gameplay

- **Crowns** — the currency. New players start with `config.startBalance`.
- **Market** — 8 commodities (coal, copper/iron/gold ingot, emerald, diamond,
  netherite ingot, blaze rod). Every `config.intervalTicks` (200 = ~10 s) each
  commodity's supply and demand drift and the price recalculates:

  `price = base + base·(demand − supply)/20 + drift`

  buying items raises demand, which pushes the price up for everyone.
- **Black market** — stand within one block of emerald/diamond/gold/netherite
  *blocks*; the scanner discovers them (`blackmarket/scan` runs every tick).
  Discovered blocks can then be bought at 2× the source commodity price.
- **Bank** — deposit/withdraw via the menu or `/function trueeconomy:bank/deposit {amt:100}`.
  Savings earn `config.interestPermille` (0.2%) per cycle, capped at `interestCap`.
- **Pay** — menu buttons pay the nearest player, or:

```
/function trueeconomy:cmd/pay {to:"Steve",amt:100}
```

  A `taxRate` (5‰) transaction tax is collected into `stats.taxes`.

### State & config

Everything persists automatically (storage `trueeconomy:main` + scoreboards). All
values are served from the storage so they can be tuned live:

```
data modify storage trueeconomy:main config.intervalTicks set value 100
data get storage trueeconomy:main commodities.iron_ingot
```

### Function reference

| Function | Purpose |
| --- | --- |
| `trueeconomy:menu` | Main clickable menu |
| `trueeconomy:cmd/balance` | Wallet + bank |
| `trueeconomy:cmd/pay {to, amt}` | Pay a named player |
| `trueeconomy:cmd/pay_p {amt}` | Pay nearest player |
| `trueeconomy:market/list` | Market table with buy buttons |
| `trueeconomy:market/trade {id, amt}` | Buy `amt` of `id` |
| `trueeconomy:menu/bank` | Bank menu |
| `trueeconomy:bank/deposit {amt}` / `depositAll` | Save crowns |
| `trueeconomy:bank/withdraw {amt}` / `withdrawAll` | Take crowns out |
| `trueeconomy:blackmarket/trade {blk, amt}` | Black-market purchase |
| `trueeconomy:engine/cycle` | One economy cycle (prices + interest) |
| `trueeconomy:blackmarket/scan` | Per-tick block-discovery scanner |

---

## Bedrock Edition (ScriptAPI add-on)

### Install

1. Copy `vanilla/bedrock/` into the world's `behavior_packs/` folder (or zip and
   import as a behavior pack with the Script API module enabled; `min_engine_version`
   is `1.21.0+`).
2. Enable **Experiments → Beta APIs / GameTest Framework** (ScriptAPI) for the world.
3. Join and open the menu:

```
!econ
```

### Gameplay

Same economy as Java, driven by `scripts/main.js`:

- Market menu (tap a commodity, buy 1/16/64), bank menu with deposit/withdraw,
  black market with block discovery by walking near rare blocks.
- Balances and market state persist in world dynamic properties
  (`trueeconomy:state`, JSON).
- Trading (`!econ pay <name> <amount>`), `!econ list`, `!econ market`, `!econ bank`.

| Command | Purpose |
| --- | --- |
| `!econ` / `!econ menu` | Main form menu |
| `!econ balance` | Show balance |
| `!econ market` | Market buy menu |
| `!econ bank` | Bank menu |
| `!econ pay <name> <amount>` | Pay a player (tax applies) |
| `!econ list` | Text market table |
| `!econ black` | Black market menu |
| `!econ help` | Help text |

---

## Design notes

- **Money model** — an integer balance per identity; prices are integers to keep
  the data pack's scoreboard math exact.
- **Deterministic randomness** — the market drift uses a hash of the cycle index,
  commodity index and seed, so prices are reproducible and need no true RNG.
- **Java edition limits** — no item *selling* yet (give-only trades); auctions,
  shops and seasonal events from the full plugin are not ported to vanilla yet.
- **Bedrock edition limits** — identity linking is not implemented; block-discovery
  state lives per device account in `state.players`.