$execute store result score #pr te.tmp run data get storage trueeconomy:main commodities.$(src).price
$execute store result score #mult te.tmp run data get storage trueeconomy:main blackmarket.$(blk).mult
scoreboard players operation #cost te.tmp = #pr te.tmp
scoreboard players operation #cost te.tmp *= #mult te.tmp
scoreboard players operation #cost te.tmp *= #amt te.tmp
execute store result score #bal te.tmp run scoreboard players get @s te.bal
execute if score #bal te.tmp < #cost te.tmp run tellraw @s ["Black market: need ",{"score":{"name":"#cost","objective":"te.tmp"}}," crowns"]
execute unless score #bal te.tmp < #cost te.tmp run function trueeconomy:blackmarket/buy with storage trueeconomy:tmp