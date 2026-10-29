# Branding

Vector sources are the master copies. Rasterised PNGs are build output.

| File | Purpose |
| --- | --- |
| `icon.svg` | Marketplace / plugin-manager icon. Square, legible down to 32px. |
| `cover.svg` | Listing cover, 16:9. Wordmark, tagline, verified stats and a market-trend panel. |
| `feature.svg` | Feature sheet, 16:9. Eight systems, each with a one-line explanation. |
| `social.svg` | Open Graph / social card, 1200×630. Wordmark, stats and an illustrative supply/demand chart. |
| `listing.md` | Marketplace listing copy: title, description, commands, requirements, honest limits. |
| `launch-posts.md` | Discord, X, Reddit/SpigotMC and marketplace launch copy. |
| `render.sh` | Rasterises every source to every size in `png/`. Uses `rsvg-convert`, falls back to Inkscape. |
| `verify-claims.sh` | Checks every number printed in the art against the source and the 68-jar dist. |
| `png/` | Generated. Safe to delete; `render.sh` recreates it. |

## Palette

| Role | Value |
| --- | --- |
| Background, deep | `#0a110e` |
| Background, mid | `#101b16` |
| Panel | `#0d1914` |
| Rim / grid | `#1e2f26` |
| Primary (green) | `#3ddc7a` |
| Secondary (blue) | `#37c6e8` |
| Accent (gold) | `#ffd75e` |
| Accent (violet) | `#b07de8` |
| Body text | `#bcd8c7` |
| Muted text | `#6f9c82` |

Green is the through-line: it is the money colour in the shop GUI, so the art matches what a
player sees in game. Gold is used only for emphasis and never for a second meaning. Blue and
violet separate the market, banking and identity systems without introducing a competing primary.

## Changing the art

Edit the `.svg`, then:

```bash
./render.sh && ./verify-claims.sh
```

`verify-claims.sh` exists because the art states hard numbers: **734 shop entries**, **27 shop
categories**, **65 commodities**, **26 achievements**, **8 sectors**, **68 builds**, **237 tests**,
and the version range **1.8 → 26.3**. Those drift as the plugin grows, and an overstatement on a
marketplace listing is a refund request later. Run it before every upload.

The script also checks that the numbers are actually present in the SVG text, so a source change
cannot silently leave stale copy on the cover, feature sheet or social card.

## Design notes

- The icon is a trading column: three stacked bars under a rising price line. It reads as both
  "market chart" and "stack of goods", and stays legible at 32px.
- The cover is a strict two-column grid: wordmark and verified stats on the left, market-trend
  panel on the right, feature strip along the bottom.
- The feature sheet uses the same card geometry as the cover's system strip: four columns by two
  rows, one accent colour per system.
- The social card replaces the cover's price panel with an **illustrative** supply/demand chart.
  It is labelled as illustrative on the card, so it cannot be mistaken for live server data.
- The `BY SNIPERNODE` credit sits at the bottom-right of the cover, clear of the chart panel.

## Licensing note

No Minecraft assets are used. The chart and column motifs are original geometry, so there is
nothing here derived from Mojang or Microsoft IP. Typography is Inter, which is open source
(SIL OFL 1.1). The plugin itself is source-available at
<https://github.com/Snipernode/True-Economy>.
