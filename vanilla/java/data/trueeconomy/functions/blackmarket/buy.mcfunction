function trueeconomy:util/ensure
$scoreboard players set #amt te.tmp $(amt)
$execute store result score #pr te.tmp run data get storage trueeconomy:main commodities.$(src).price
$execute store result score #mult te.tmp run data get storage trueeconomy:main blackmarket.$(blk).mult
scoreboard players operation #cost te.tmp = #pr te.tmp
scoreboard players operation #cost te.tmp *= #mult te.tmp
scoreboard players operation #cost te.tmp *= #amt te.tmp
scoreboard players operation @s te.bal -= #cost te.tmp
$give @s minecraft:$(blk) $(amt)
execute store result score #sale te.tmp run data get storage trueeconomy:main stats.sales
scoreboard players operation #sale te.tmp += #cost te.tmp
execute store result storage trueeconomy:main stats.sales int 1 run scoreboard players get #sale te.tmp
tellraw @s ["Bought from black market: ",{"score":{"name":"#amt","objective":"te.tmp"}}," x $(blk) for ",{"score":{"name":"#cost","objective":"te.tmp"}}," crowns"]