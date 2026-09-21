import { world, system } from "@minecraft/server";
import { ActionFormData, ModalFormData } from "@minecraft/server-ui";

const KEY = "trueeconomy:state";
const ORDER = ["coal", "copper_ingot", "iron_ingot", "gold_ingot", "emerald", "diamond", "netherite_ingot", "blaze_rod"];
const COMMODITY_NAMES = {
  coal: "Coal", copper_ingot: "Copper Ingot", iron_ingot: "Iron Ingot",
  gold_ingot: "Gold Ingot", emerald: "Emerald", diamond: "Diamond",
  netherite_ingot: "Netherite Ingot", blaze_rod: "Blaze Rod"
};
const BLOCKS = {
  emerald_block: { src: "emerald", mult: 2 },
  diamond_block: { src: "diamond", mult: 2 },
  gold_block: { src: "gold_ingot", mult: 2 },
  netherite_block: { src: "netherite_ingot", mult: 2 }
};

const DEFAULT_STATE = {
  config: { startBalance: 500, intervalSeconds: 10, taxPermille: 5, interestPermille: 2, interestCap: 1000000 },
  meta: { seed: 12345, cycle: 0 },
  commodities: {
    coal: { base: 20, price: 20, supply: 24, demand: 24 },
    copper_ingot: { base: 32, price: 32, supply: 20, demand: 20 },
    iron_ingot: { base: 48, price: 48, supply: 20, demand: 20 },
    gold_ingot: { base: 120, price: 120, supply: 12, demand: 20 },
    emerald: { base: 360, price: 360, supply: 10, demand: 20 },
    diamond: { base: 420, price: 420, supply: 8, demand: 24 },
    netherite_ingot: { base: 900, price: 900, supply: 4, demand: 24 },
    blaze_rod: { base: 180, price: 180, supply: 10, demand: 14 }
  },
  players: {},
  discovered: {},
  stats: { sales: 0, taxes: 0, trades: 0 }
};

let state = null;

function load() {
  try {
    const raw = world.getDynamicProperty(KEY);
    if (typeof raw === "string") state = JSON.parse(raw);
  } catch (e) { state = null; }
  if (!state) {
    state = JSON.parse(JSON.stringify(DEFAULT_STATE));
    save();
  }
}

function save() {
  world.setDynamicProperty(KEY, JSON.stringify(state));
}

function getPlayer(id) {
  if (!state.players[id]) {
    state.players[id] = { name: "", balance: state.config.startBalance, bank: 0 };
    save();
  }
  return state.players[id];
}

function fmt(n) { return `${n} crowns`; }

function hash(idx, cyc) {
  const r = ((idx * 7 + cyc * 3 + state.meta.seed) % 9) - 4;
  return r;
}

function churn() {
  state.meta.cycle++;
  ORDER.forEach((id, idx) => {
    const c = state.commodities[id];
    const delta = c.demand - c.supply;
    let price = c.base + (c.base * delta) / 20 + hash(idx, state.meta.cycle);
    price = Math.max(Math.floor(c.base / 2), Math.min(Math.floor(price), c.base * 3));
    c.price = price;
    c.supply = Math.max(1, Math.min(64, c.supply + Math.floor(delta / 8)));
    c.demand = Math.max(8, Math.min(48, c.demand + (((idx * 3 * state.meta.cycle) % 5) - 2)));
  });
  // interest
  for (const id of Object.keys(state.players)) {
    const p = state.players[id];
    const int = Math.floor((p.bank * state.config.interestPermille) / 1000);
    p.bank = Math.min(state.config.interestCap, p.bank + int);
    save();
  }
  save();
}

function listMarket(player) {
  const lines = ORDER.map(id => {
    const c = state.commodities[id];
    return `${COMMODITY_NAMES[id]}: ${c.price} (supply ${c.supply}/demand ${c.demand})`;
  }).join("\n");
  player.sendMessage(`\u00A7gTrue-Economy Market\u00A7r\n${lines}`);
}

async function marketMenu(player) {
  const form = new ActionFormData();
  form.title("True-Economy Market");
  form.body("Select a commodity to buy.");
  ORDER.forEach(id => {
    const c = state.commodities[id];
    form.button(`${COMMODITY_NAMES[id]} - ${c.price}`, "textures/items/" + (id === "coal" ? "coal" : id) + "");
  });
  form.button("Back");
  const { selection } = await form.show(player);
  if (selection === undefined) return;
  if (selection === ORDER.length) return;
  const id = ORDER[selection];
  await buyMenu(player, id);
}

async function buyMenu(player, id) {
  const c = state.commodities[id];
  const form = new ActionFormData();
  form.title(COMMODITY_NAMES[id]);
  form.body(`Current price: ${c.price} crowns`);
  form.button("Buy 1", "textures/ui/icon_plus");
  form.button("Buy 16");
  form.button("Buy 64");
  form.button("Back");
  const { selection } = await form.show(player);
  if (selection === undefined || selection === 3) return;
  const amt = [1, 16, 64][selection];
  trade(player, id, amt);
}

function trade(player, id, amt) {
  const c = state.commodities[id];
  const p = getPlayer(player.id);
  const cost = c.price * amt;
  if (p.balance < cost) {
    player.sendMessage(`Not enough crowns: need ${cost}, you have ${p.balance}.`);
    return;
  }
  p.balance -= cost;
  c.demand = Math.min(64, c.demand + amt);
  state.stats.sales += cost;
  player.sendMessage(`${amt} x ${COMMODITY_NAMES[id]} bought for ${cost} crowns.`);
  save();
  // give items (best-effort; count as raw resource via dynamic item ids below)
  tryGift(player, itemId(id), amt);
}

function itemId(id) {
  if (id === "coal") return "minecraft:coal";
  if (id === "copper_ingot") return "minecraft:copper_ingot";
  if (id === "iron_ingot") return "minecraft:iron_ingot";
  if (id === "gold_ingot") return "minecraft:gold_ingot";
  if (id === "emerald") return "minecraft:emerald";
  if (id === "diamond") return "minecraft:diamond";
  if (id === "netherite_ingot") return "minecraft:netherite_ingot";
  return "minecraft:blaze_rod";
}

function tryGift(player, type, count) {
  try {
    const item = {
      typeId: type,
      amount: count
    };
    player.runCommand(`give @s ${type} ${count}`);
    void item;
  } catch (e) {
    player.sendMessage("(Could not grant items directly; you were charged the market price.)");
  }
}

async function bankMenu(player) {
  const p = getPlayer(player.id);
  const form = new ActionFormData();
  form.title("True-Economy Bank");
  form.body(`Balance: ${p.balance}\nSavings: ${p.bank}\nInterest: ${state.config.interestPermille / 10}%/cycle`);
  form.button("Deposit 100");
  form.button("Deposit ALL");
  form.button("Withdraw 100");
  form.button("Withdraw ALL");
  form.button("Back");
  const { selection } = await form.show(player);
  if (selection === undefined || selection === 4) return;
  if (selection === 0) deposit(player, 100);
  else if (selection === 1) deposit(player, p.balance);
  else if (selection === 2) withdraw(player, 100);
  else withdraw(player, p.bank);
}

function deposit(player, amt) {
  const p = getPlayer(player.id);
  if (p.balance < amt) { player.sendMessage(`Only have ${p.balance} crowns.`); return; }
  p.balance -= amt;
  p.bank += amt;
  player.sendMessage(`Deposited ${amt} crowns.`);
  save();
}

function withdraw(player, amt) {
  const p = getPlayer(player.id);
  if (p.bank < amt) { player.sendMessage(`Bank has only ${p.bank} crowns.`); return; }
  p.bank -= amt;
  p.balance += amt;
  player.sendMessage(`Withdrew ${amt} crowns.`);
  save();
}

async function mainMenu(player) {
  const p = getPlayer(player.id);
  const form = new ActionFormData();
  form.title("True-Economy");
  form.body(`Balance: ${p.balance} crowns | Bank: ${p.bank}`);
  form.button("Balance");
  form.button("Buy from Market");
  form.button("Bank");
  form.button("Black Market");
  form.button("Help");
  const { selection } = await form.show(player);
  if (selection === undefined) return;
  if (selection === 0) player.sendMessage(fmt(p.balance));
  else if (selection === 1) await marketMenu(player);
  else if (selection === 2) await bankMenu(player);
  else if (selection === 3) await blackMarketMenu(player);
  else help(player);
}

async function blackMarketMenu(player) {
  const state0 = state;
  const disc = state0.discovered[player.id] || [];
  const form = new ActionFormData();
  form.title("Black Market");
  form.body(`Discovered blocks: ${disc.length ? disc.join(", ") : "none yet"}
Stand next to emerald/diamond/gold/netherite blocks to discover them.`);
  const discovered = disc;
  for (const blk of discovered) {
    const b = BLOCKS[blk];
    form.button(`${blk} (x${b.mult})`);
  }
  form.button("Rescan");
  form.button("Back");
  const { selection } = await form.show(player);
  if (selection === undefined) return;
  if (selection === discovered.length + 1) return;
  if (selection === discovered.length) { player.sendMessage("Black Market scanner active - walk near rare blocks."); return; }
  const blk = discovered[selection];
  const b = BLOCKS[blk];
  const src = state0.commodities[b.src];
  const cost = src.price * b.mult;
  if (getPlayer(player.id).balance < cost) { player.sendMessage(`Need ${cost} crowns.`); return; }
  getPlayer(player.id).balance -= cost;
  tryGift(player, `minecraft:${blk}`, 1);
  state0.stats.sales += cost;
  save();
  player.sendMessage(`Bought 1 x ${blk} for ${cost} crowns.`);
}

function help(player) {
  player.sendMessage(`True-Economy
- Market prices move with supply/demand each cycle.
- Banks pay interest on savings.
- Black market: discover rare blocks to unlock buys.
Commands: balance, market, bank, pay <name> <amt>`);
}

function handleChat(event) {
  const { message, sender } = event;
  const m = message.trim();
  const prefix = m.startsWith("!econ");
  const prefix2 = m.startsWith("/econ");
  if (!prefix && !prefix2) return;
  event.cancel = true;
  const args = m.replace(/^[!\/]econ\s*/i, "").trim().split(/\s+/);
  const cmd = args[0] ? args[0].toLowerCase() : "";
  const p = getPlayer(sender.id);
  switch (cmd) {
    case "menu": case "": void mainMenu(sender); break;
    case "balance": case "bal": sender.sendMessage(fmt(p.balance)); break;
    case "market": void marketMenu(sender); break;
    case "bank": void bankMenu(sender); break;
    case "black": case "bm": void blackMarketMenu(sender); break;
    case "pay": {
      if (args.length < 3) { sender.sendMessage("Usage: !econ pay <name> <amt>"); return; }
      doPay(sender, args[1], parseInt(args[2], 10));
      break;
    }
    case "list": case "marketlist": listMarket(sender); break;
    case "help": help(sender); break;
    default: sender.sendMessage("Unknown command. Try: !econ menu"); break;
  }
}

function doPay(from, toName, amt) {
  if (!Number.isFinite(amt) || amt < 1) { from.sendMessage("Amount must be >= 1."); return; }
  const p = getPlayer(from.id);
  if (p.balance < amt) { from.sendMessage(`Insufficient funds: ${p.balance}.`); return; }
  const to = world.getAllPlayers().find(q => q.name === toName);
  if (!to) { from.sendMessage(`Player ${toName} not found or offline.`); return; }
  const tax = Math.floor((amt * state.config.taxPermille) / 1000);
  const recv = amt - tax;
  p.balance -= amt;
  getPlayer(to.id).balance += recv;
  state.stats.taxes += tax;
  save();
  from.sendMessage(`Paid ${recv} crowns (+${tax} tax) to ${toName}.`);
  to.sendMessage(`You received ${recv} crowns from ${from.name}.`);
}

world.beforeEvents.chatSend.subscribe(handleChat);
world.afterEvents.playerSpawn.subscribe(({ player }) => {
  getPlayer(player.id);
});

load();
system.runInterval(churn, state.config.intervalSeconds * 20);
system.runInterval(() => {
  world.getAllPlayers().forEach(player => {
    // passive discovery: scan blocks around each player's feet
    discoverBlocks(player);
  });
}, 40);

function discoverBlocks(player) {
  const disc = state.discovered[player.id] || [];
  const loc = player.location;
  for (const blk of Object.keys(BLOCKS)) {
    if (disc.includes(blk)) continue;
    let found = false;
    for (let dx = -2; dx <= 2 && !found; dx++) {
      for (let dz = -2; dz <= 2 && !found; dz++) {
        for (let dy = -1; dy <= 2 && !found; dy++) {
          try {
            const b = player.dimension.getBlock({ x: Math.floor(loc.x + dx), y: Math.floor(loc.y + dy), z: Math.floor(loc.z + dz) });
            if (b && b.typeId === `minecraft:${blk}`) found = true;
          } catch (e) { /* unloaded */ }
        }
      }
    }
    if (found) {
      disc.push(blk);
      state.discovered[player.id] = disc;
      save();
      player.sendMessage(`\u00A7fYou discovered the Black Market supply of ${blk}!`);
    }
  }
}