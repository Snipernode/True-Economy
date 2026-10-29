# Launch posts — TrueEconomy 2.0

All posts below use the verified numbers from `verify-claims.sh`. Re-run it before publishing if
the build changes.

## Discord announcement

**TrueEconomy 2.0 is live — your server's money is now a simulation.**

Most economy plugins store a number and subtract it. TrueEconomy models the movement:

- **65 commodities** with base price, elasticity, supply and demand. Player trades move the price.
- **734 shop entries** across 27 categories, with finite stock, restocking, custom items and timed sales.
- **Escrowed auctions** — buy-it-now, minimum increments, fees, tax, expiry and claiming.
- **Banking with credit** — wallet/bank split, compounding interest, credit-scored loans and default penalties.
- **Player shops** — hire up to five employees, set wages, track production and profit.
- **Government and events** — transaction tax into city treasuries, grants, and seasonal crisis/boom events.
- **Identity linking** — tie Java and Bedrock accounts with a 6-character code and merge currency, bank and discoveries.
- **Progression** — 26 achievements, a daily streak and a dashboard with net worth and a week-long chart.

**68 per-version builds, 1.8 → 26.3.** Vault and PlaceholderAPI integrations. YAML, SQLite or MySQL. No hard dependencies.

Source and docs: https://github.com/Snipernode/True-Economy

---

## X / Twitter thread

**1/3**
Your server economy shouldn't be a number in a config file.

TrueEconomy 2.0 makes it a simulation: 65 commodities with supply, demand and elasticity. Player trades move the price.

68 builds, 1.8 → 26.3.

🧵

**2/3**
Inside:

• 734 shop entries across 27 categories
• Escrowed auctions
• Credit-scored loans
• City taxes and grants
• Player shops with employees
• 26 achievements and a daily streak

All GUI-first.

**3/3**
Storage: YAML, SQLite or MySQL.
Integrations: Vault + PlaceholderAPI.
237 automated tests. No hard dependencies.

Source, docs and 68 builds:
github.com/Snipernode/True-Economy

---

## Reddit / SpigotMC forum post

**Title:** TrueEconomy 2.0 — a living server economy with supply and demand, escrowed auctions and credit-scored banking

**Body:**

I rebuilt TrueEconomy around one idea: prices should be *caused*, not random.

Every commodity has a base price, elasticity, supply and demand. Buying pushes the price up,
selling pushes it down, and time pulls it back toward base. The market screen shows the cause —
supply, demand, recent movement and a sparkline.

**What ships in 2.0**

- 65 commodities across 8 sectors, with price history and trends.
- 27 shop categories and 734 entries, with finite stock, restocking, a buy/sell spread, 5 custom
  items and 8 timed sales.
- Discovery-gated shop sections that unlock by stepping on blocks.
- An auction house with escrowed bidding, buy-it-now, fees, sales tax, expiry and claiming.
- Banking with wallet/bank separation, compounding interest, credit-scored loans and defaults.
- Player shops with up to five employees, wages, production and profit tracking.
- Government transaction tax into city treasuries, plus grants.
- Seasonal crisis and boom events that reprice the market.
- Java ↔ Bedrock identity linking with a 6-character code.
- 26 achievements, a daily streak and a player dashboard.
- 16 GUI screens and 7 commands.

**Technical**

- 68 per-version jars, Minecraft 1.8 → 26.3.
- YAML by default; SQLite and MySQL supported.
- Vault economy provider and PlaceholderAPI placeholders.
- No hard dependencies.
- 237 automated tests included.

**Honest notes**

The auction vendor-side bidding loop is not scheduled — the config knobs are reserved, but the
shipped loop is player-driven. There is no Vault bank support by design. Government is
single-region. No NPC vendor entities spawn. Test on a staging server before a production rollout.

Source, docs and releases: https://github.com/Snipernode/True-Economy

---

## BuiltByBit / marketplace update blurb

**TrueEconomy 2.0 — a living economy, not a balance file.**

65 commodities with real supply and demand. 734 shop entries. Escrowed auctions. Credit-scored
banking. Player shops with employees. City taxes and seasonal market events. Java ↔ Bedrock
identity linking. 26 achievements and a daily streak.

68 per-version builds for Minecraft 1.8 → 26.3. Vault and PlaceholderAPI ready. YAML, SQLite or
MySQL. No hard dependencies.

---

## One-liner

TrueEconomy 2.0 turns server money into a simulation — 65 commodities, 734 shop entries,
escrowed auctions, credit-scored banking and city taxes, on 68 builds from 1.8 to 26.3.
