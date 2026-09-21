function trueeconomy:util/ensure
execute store result score #amt te.tmp run scoreboard players get @s te.bank
scoreboard players operation @s te.bank -= #amt te.tmp
scoreboard players operation @s te.bal += #amt te.tmp
tellraw @s ["Withdrew ALL ",{"score":{"name":"#amt","objective":"te.tmp"}}," crowns"]