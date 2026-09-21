$scoreboard players set #amt te.tmp $(amt)
scoreboard players operation @s te.bal -= #amt te.tmp
scoreboard players operation @s te.bank += #amt te.tmp
scoreboard players set #cap te.tmp 1000000000
execute if score @s te.bank > #cap te.tmp run scoreboard players operation @s te.bank = #cap te.tmp
tellraw @s ["Deposited ",{"score":{"name":"#amt","objective":"te.tmp"}}," crowns"]