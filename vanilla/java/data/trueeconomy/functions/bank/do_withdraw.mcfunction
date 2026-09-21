$scoreboard players set #amt te.tmp $(amt)
scoreboard players operation @s te.bank -= #amt te.tmp
scoreboard players operation @s te.bal += #amt te.tmp
tellraw @s ["Withdrew ",{"score":{"name":"#amt","objective":"te.tmp"}}," crowns"]