function trueeconomy:util/ensure
$scoreboard players set #amt te.tmp $(amt)
scoreboard players set #lo te.tmp 1
execute if score #amt te.tmp < #lo te.tmp run scoreboard players set #amt te.tmp 1
$execute store result score #pr te.tmp run data get storage trueeconomy:main commodities.$(id).price
scoreboard players operation #cost te.tmp = #pr te.tmp
scoreboard players operation #cost te.tmp *= #amt te.tmp
execute store result score #bal te.tmp run scoreboard players get @s te.bal
execute if score #bal te.tmp < #cost te.tmp run tellraw @s ["Not enough crowns: need ",{"score":{"name":"#cost","objective":"te.tmp"}}]
execute unless score #bal te.tmp < #cost te.tmp run function trueeconomy:market/buy with storage trueeconomy:tmp