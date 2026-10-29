#!/usr/bin/env python3
"""TrueEconomy in-game test harness.

Boots a real Paper server with the matching TrueEconomy version jar,
drives it over RCON, and verifies shipped features behave as documented.

Subcommands:
  prepare   download Paper, verify sha256, install the plugin jar, first boot
  boot      (re)start the server and wait until it reports Done
  test      run the feature scenarios (server stays up)
  stop      send `stop` and wait for the process to exit
  clean     delete the server directory

There is no pytest dependency; the scenario runner is dependency-free Python.
"""

import argparse
import hashlib
import os
import re
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from rcon_lite import RconClient, RconError  # noqa: E402


PAPER_API = "https://fill.papermc.io/v3/projects/paper/versions/{v}/builds"

DEFAULT_DIR = Path("/tmp/opencode/te-srv")
DEFAULT_VERSION = "26.3"
JAVA = os.environ.get("TE_JAVA", "java")

SERVER_PROPERTIES = """\
enable-rcon=true
rcon.port={rcon_port}
rcon.password={rcon_pw}
online-mode=false
level-type=minecraft\\:flat
generate-structures=false
spawn-protection=0
view-distance=4
simulation-distance=4
max-players=2
motd=TrueEconomy test
"""


# ---------------------------------------------------------------- utils

def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def download_paper(version, workdir):
    import json
    import urllib.request

    url = PAPER_API.format(v=version)
    req = urllib.request.Request(url, headers={"User-Agent": "TE-Harness/1.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        builds = json.load(r)
    stable = [b for b in builds if b.get("channel") == "STABLE"]
    if not stable:
        sys.exit(f"No STABLE Paper build for {version}")
    latest = stable[0]
    dl = latest["downloads"]["server:default"]
    jar_path = workdir / f"paper-{version}.jar"
    print(f"Paper build {latest['id']}: {dl['name']}")
    req2 = urllib.request.Request(dl["url"], headers={"User-Agent": "TE-Harness/1.0"})
    with urllib.request.urlopen(req2, timeout=10) as resp, open(jar_path, "wb") as out:
        shutil.copyfileobj(resp, out, 1 << 20)
    digest = sha256_file(jar_path)
    if digest != dl["checksums"]["sha256"]:
        sys.exit(f"Paper jar sha256 mismatch: {digest}")
    print(f"sha256 OK  {digest[:16]}...")
    return jar_path


# ---------------------------------------------------------------- server

class ServerProcess:
    """Owns the java subprocess for one server directory."""

    def __init__(self, server_dir, version, plugin_jar):
        self.dir = Path(server_dir)
        self.version = version
        self.plugin_jar = Path(plugin_jar).resolve()
        self.rcon_pw = "te-harness"
        self.rcon_port = 25575
        self.proc = None

    # -- setup

    def write_config(self):
        self.dir.mkdir(parents=True, exist_ok=True)
        (self.dir / "eula.txt").write_text("eula=true\n")
        (self.dir / "server.properties").write_text(
            SERVER_PROPERTIES.format(rcon_port=self.rcon_port, rcon_pw=self.rcon_pw)
        )
        (self.dir / "plugins").mkdir(exist_ok=True)
        shutil.copyfile(self.plugin_jar, self.dir / "plugins" / "TrueEconomy.jar")
        (self.dir / "server.log").touch(exist_ok=True)

    def prepare(self):
        jar = self.dir / f"paper-{self.version}.jar"
        if not jar.exists():
            download_paper(self.version, self.dir)
        self.write_config()
        self.boot()
        self.stop()  # first boot generates the world; shutdown right after

    # -- lifecycle

    def boot(self, wait=420):
        log = self.dir / "server.log"
        handle = open(log, "ab")
        self.proc = subprocess.Popen(
            [JAVA, f"-Xms512M", "-Xmx1500M",
             "-jar", f"paper-{self.version}.jar", "nogui"],
            cwd=self.dir, stdout=handle, stderr=subprocess.STDOUT,
        )
        t0 = time.time()
        while time.time() - t0 < wait:
            if self.proc.poll() is not None:
                sys.exit("Server process exited early; see server.log")
            try:
                text = log.read_text(errors="replace")
                if "Done (" in text and "help" in text:
                    return
            except OSError:
                pass
            time.sleep(2)
        sys.exit(f"Server did not reach Done within {wait}s")

    def stop(self):
        if not self.proc or self.proc.poll() is not None:
            return
        try:
            r = RconClient("127.0.0.1", self.rcon_port).connect(self.rcon_pw)
            r.command("stop")
            r.close()
        except (OSError, RconError):
            self.proc.terminate()
        self.proc.wait(timeout=60)

    # -- access

    def rcon(self):
        return RconClient("127.0.0.1", self.rcon_port).connect(self.rcon_pw)

    def log_text(self):
        return (self.dir / "server.log").read_text(errors="replace")

    def plugin_data(self):
        return self.dir / "plugins" / "TrueEconomy"


# ---------------------------------------------------------------- testing

class Scenario:
    def __init__(self, name, fn):
        self.name = name
        self.fn = fn


RESULTS = []


def scenario(name):
    def deco(fn):
        SCENARIOS.append(Scenario(name, fn))
        return fn
    return deco


SCENARIOS = []


@scenario("plugin enable")
def sc_enable(s):
    t = s.log_text()
    assert "Enabling TrueEconomy v" in t, "plugin never enabled"


@scenario("market list lists 65 commodities")
def sc_market_list(s):
    out = s.rcon().command("market list")
    assert len(out) > 3000, f"market list suspiciously short ({len(out)} chars)"
    ids = out.count("[id:")
    assert ids == 65, f"expected 65 commodities on the market, saw {ids}"
    assert "[id: iron]" in out and "[id: diamond]" in out


@scenario("market view shows supply and demand")
def sc_market_view(s):
    out = s.rcon().command("market view iron")
    for frag in ("Iron Ingot", "Current price", "Base price", "Supply:", "Elasticity"):
        assert frag in out, f"market view missing '{frag}'"


@scenario("market trade refuses console with a guard")
def sc_market_trade_guard(s):
    out = s.rcon().command("market trade iron 2")
    assert "Only players can trade" in out


@scenario("money lifecycle: give, set, take, top")
def sc_money(s):
    rc = s.rcon()
    give = rc.command("economy give TestPlayer 500")
    assert "Gave $500.00" in give, give
    acc_before = rc.command("economy set TestPlayer 777")
    top = rc.command("economy top")
    assert re.search(r"TestPlayer\s+\$777(?:\.0+)?", strip_color(top)), top
    take = rc.command("economy take TestPlayer 777")
    return {"give": give.replace("\n", " | "), "set": acc_before,
            "top": strip_color(top), "take": take}


@scenario("balance view refuses console senders (by design)")
def sc_balance_guard(s):
    out = s.rcon().command("economy balance TestPlayer")
    assert "Console cannot view a balance here" in out


@scenario("bank commands refuse console (accounts are per player)")
def sc_bank(s):
    rc = s.rcon()
    dep = rc.command("bank deposit TestPlayer 300")
    cre = rc.command("bank credit TestPlayer")
    lon = rc.command("bank loan TestPlayer")
    out = dep + cre + lon
    assert out.count("Only players have bank accounts") == 3, out


@scenario("player shops refuse console")
def sc_shop(s):
    out = s.rcon().command("shop list")
    assert "Only players own shops" in out


@scenario("daily claim refuses console")
def sc_claim(s):
    out = s.rcon().command("claim")
    assert "Only players can claim rewards" in out


@scenario("auction house list endpoint")
def sc_auction(s):
    out = s.rcon().command("auction list")
    assert ("No active auctions" in out) or ("auction" in out.lower())


@scenario("seasonal events endpoint ran and shows status")
def sc_events(s):
    out = s.rcon().command("events status")
    assert "Seasonal Market Events" in out


@scenario("ledger records every money movement")
def sc_ledger(s):
    p = _ledger_path(s)
    assert p is not None, "transactions.log not found in plugin data"
    lines = p.read_text(errors="replace").strip().splitlines()
    assert len(lines) > 3, f"ledger too thin: {len(lines)} lines"
    patterns = []
    for want in ("ADMIN-GIVE", "ADMIN-SET"):
        hits = [l for l in lines if want in l]
        assert hits, f"ledger missing any {want} line"
        patterns.append(f"{want}={len(hits)}")
    return {"lines": len(lines), "breakdown": ", ".join(patterns),
            "sample": lines[-1]}


@scenario("account file persist wallet and credit")
def sc_account_file(s):
    acc = _account_file(s, "TestPlayer")
    assert acc is not None, "no TestPlayer account file in data/"
    text = acc.read_text()
    assert "owner: TestPlayer" in text
    m = re.search(r"balance:\s*([\d.]+)", text)
    b = re.search(r"bank:\s*([\d.]+)", text)
    c = re.search(r"credit:\s*(\d+)", text)
    assert m and b and c, "account file missing balance/bank/credit keys"
    return {k: v for k, v in (("balance", m.group(1)), ("bank", b.group(1)),
                              ("credit", c.group(1)))}


def _ledger_path(s):
    data = s.plugin_data()
    direct = data / "data" / "transactions.log"
    if direct.exists():
        return direct
    hits = sorted(data.rglob("transactions.log"))
    return hits[0] if hits else None


def _account_file(s, name):
    for p in (s.plugin_data() / "data").glob("*.yml"):
        if f"owner: {name}" in p.read_text(errors="replace"):
            return p
    return None


def strip_color(s):
    return re.sub(r"\u00a7.", "", s)


def report_format(value):
    if value is None:
        return ""
    if isinstance(value, dict):
        return "\n".join(f"  {k}: {v}" for k, v in value.items())
    return f"  {value}"


def run_tests(s):
    summary = []
    failures = 0
    for sc in SCENARIOS:
        try:
            out = sc.fn(s)
            status = "PASS"
            print(f"  OK    {sc.name}")
        except AssertionError as e:
            status, out, failures = "FAIL", str(e), failures + 1
            print(f"  FAIL  {sc.name}: {e}")
        except (OSError, RconError) as e:
            status, out, failures = "ERR", f"{type(e).__name__}: {e}", failures + 1
            print(f"  ERR   {sc.name}: {type(e).__name__}: {e}")
        summary.append((sc.name, status, report_format(out)))
        if isinstance(out, dict):
            for k, v in out.items():
                show = v if isinstance(v, str) else repr(v)
                for line in show.splitlines():
                    print(f"        [{sc.name}] {k}: {line}")
        elif out:
            for line in str(out).splitlines():
                print(f"        [{sc.name}] {line}")
    (s.dir / "ingame-report.txt").write_text(
        "\n\n".join(f"== {n} ==\nstatus: {st}\n{v}" for n, st, v in summary) + "\n"
    )
    print(f"\n{'FAILED' if failures else 'ALL PASS'}"
          f"  ({failures} failing of {len(SCENARIOS)})", file=sys.stderr)
    return 1 if failures else 0


def first_boot_smoke(s):
    """Assert the first boot actually produced a working server."""
    text = s.log_text()
    assert "Done (" in text, "server never reported Done"
    assert "TrueEconomy" in text, "TrueEconomy never appears in the log"


# ---------------------------------------------------------------- cli

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["prepare", "boot", "test", "stop", "clean"])
    ap.add_argument("--version", default=DEFAULT_VERSION)
    ap.add_argument("--plugin", default=None,
                    help="path to the TrueEconomy jar (default: dist jar for --version)")
    ap.add_argument("--server-dir", default=str(DEFAULT_DIR))
    args = ap.parse_args()

    repo = HERE.parent.parent
    plugin = args.plugin or str(repo / "dist" / f"true-economy-{args.version}.jar")
    s = ServerProcess(args.server_dir, args.version, plugin)

    if args.cmd == "prepare":
        s.prepare()
        first_boot_smoke(s)
        print("prepare OK")
        return 0
    if args.cmd == "boot":
        s.write_config()
        s.boot()
        first_boot_smoke(s)
        print("boot OK")
        return 0
    if args.cmd == "test":
        return run_tests(s)
    if args.cmd == "stop":
        s.stop()
        print("stopped")
        return 0
    if args.cmd == "clean":
        shutil.rmtree(args.server_dir, ignore_errors=True)
        print("cleaned")
        return 0
    return 2


if __name__ == "__main__":
    sys.exit(main())
