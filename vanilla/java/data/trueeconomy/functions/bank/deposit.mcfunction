function trueeconomy:util/ensure
$scoreboard players set #amt te.tmp $(amt)
scoreboard players set #lo te.tmp 1
execute if score #amt te.tmp < #lo te.tmp run scoreboard players set #amt te.tmp 1
execute store result score #bal te.tmp run scoreboard players get @s te.bal
execute if score #bal te.tmp < #amt te.tmp run tellraw @s ["You only have ",{"score":{"name":"#bal","objective":"te.tmp"}}," crowns"]
execute unless score #bal te.tmp < #amt te.tmp run function trueeconomy:bank/do_deposit with storage trueeconomy:tmp