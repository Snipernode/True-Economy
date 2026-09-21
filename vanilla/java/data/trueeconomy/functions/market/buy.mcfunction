function trueeconomy:util/ensure
$scoreboard players set #amt te.tmp $(amt)
$execute store result score #pr te.tmp run data get storage trueeconomy:main commodities.$(id).price
scoreboard players operation #cost te.tmp = #pr te.tmp
scoreboard players operation #cost te.tmp *= #amt te.tmp
scoreboard players operation @s te.bal -= #cost te.tmp
$execute store result score #de te.tmp run data get storage trueeconomy:main commodities.$(id).demand
scoreboard players operation #de te.tmp += #amt te.tmp
scoreboard players set #lim te.tmp 64
execute if score #de te.tmp > #lim te.tmp run scoreboard players operation #de te.tmp = #lim te.tmp
$execute store result storage trueeconomy:main commodities.$(id).demand int 1 run scoreboard players get #de te.tmp
$give @s minecraft:$(id) $(amt)
execute store result score #sale te.tmp run data get storage trueeconomy:main stats.sales
scoreboard players operation #sale te.tmp += #cost te.tmp
execute store result storage trueeconomy:main stats.sales int 1 run scoreboard players get #sale te.tmp
execute store result score #tx te.tmp run data get storage trueeconomy:main stats.taxes
scoreboard players operation #tx te.tmp += #cost te.tmp
execute store result storage trueeconomy:main stats.taxes int 1 run scoreboard players get #tx te.tmp
tellraw @s ["Bought ",{"score":{"name":"#amt","objective":"te.tmp"}}," x $(id) for ",{"score":{"name":"#cost","objective":"te.tmp"}}," crowns"]