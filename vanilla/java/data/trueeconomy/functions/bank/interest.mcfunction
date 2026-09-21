execute if entity @s[scores={te.bank=1..}] run execute store result score #int te.tmp run scoreboard players get @s te.bank
execute if entity @s[scores={te.bank=1..}] run execute store result score #rate te.tmp run data get storage trueeconomy:main config.interestPermille
execute if entity @s[scores={te.bank=1..}] run scoreboard players operation #int te.tmp *= #rate te.tmp
execute if entity @s[scores={te.bank=1..}] run scoreboard players set #rate te.tmp 1000
execute if entity @s[scores={te.bank=1..}] run scoreboard players operation #int te.tmp /= #rate te.tmp
execute if entity @s[scores={te.bank=1..}] run scoreboard players operation @s te.bank += #int te.tmp
execute if entity @s[scores={te.bank=1..}] run execute store result score #cap te.tmp run data get storage trueeconomy:main config.interestCap
execute if entity @s[scores={te.bank=1..}] run execute if score @s te.bank > #cap te.tmp run scoreboard players operation @s te.bank = #cap te.tmp
execute if entity @s[scores={te.bank=1..}] run execute if score #int te.tmp matches 1.. run tellraw @s ["Bank interest +",{"score":{"name":"#int","objective":"te.tmp"}}," crowns"]