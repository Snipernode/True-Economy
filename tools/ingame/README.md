# In-game tests

Real-server integration tests for TrueEconomy. The harness boots an actual
Paper server, installs the matching `dist/` jar, drives the plugin over RCON
and verifies documented behaviour: command output, console guards, the
append-only ledger and per-account persistence.

```
python3 tools/ingame/ingame_test.py prepare          # download Paper, install jar, first boot
python3 tools/ingame/ingame_test.py test             # run all scenarios (server stays up)
python3 tools/ingame/ingame_test.py stop             # shut the server down
python3 tools/ingame/ingame_test.py clean            # delete the server directory
```

Options: `--version 26.3` (any version with a `dist/true-economy-<v>.jar`),
`--server-dir /path`, `TE_JAVA=/path/to/java` env override.

Server lives in `/tmp/opencode/te-srv` by default (config `server.properties`
has `online-mode=false`, flat world, RCON on :25575, password `te-harness`).

## What is covered today

- Plugin enables on a real Paper 26.3 build
- `/market list` returns all 65 commodities, `/market view` shows price,
  supply, demand and elasticity
- Money lifecycle `give` → `set` → `take` reflected in `/economy top`
- Console guards (by-design refusals): market trade, bank accounts, shops,
  daily claim, per-player balance views
- `/auction list` and `/events status` endpoints respond
- `transactions.log` records every movement (ADMIN-GIVE / ADMIN-SET /
  ADMIN-TAKE lines observed)
- Account file persists `balance`, `bank` and `credit`

## Not covered (needs a joined player client)

Actions that require a live player — GUI screens, market trades, bank
deposits, auction create/bid, shop purchases, achievements, daily streaks —
are asserted as guards from console but not exercised end-to-end. To test
those, join with a client while the test server runs: it uses
`online-mode=false`, so any offline-capable client build can connect to
`localhost:25565` and the flows above become drivable.

Scenario descriptions live in `ingame_test.py`; every new shipped feature
should grow a scenario here before release.
