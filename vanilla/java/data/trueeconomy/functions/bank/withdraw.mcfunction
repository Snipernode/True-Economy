function trueeconomy:util/ensure
$scoreboard players set #amt te.tmp $(amt)
scoreboard players set #lo te.tmp 1
execute if score #amt te.tmp < #lo te.tmp run scoreboard players set #amt te.tmp 1
execute if entity @s[scores={te.bank=0..}] run execute store result score #bq te.tmp run scoreboard players get @s te.bank
execute if entity @s[scores={te.bank=0..}] run execute if score #bq te.tmp < #amt te.tmp run tellraw @s ["Bank has only ",{"score":{"name":"#bq","objective":"te.tmp"}}," crowns"]
execute if entity @s[scores={te.bank=0..}] run execute unless score #bq te.tmp < #amt te.tmp run function trueeconomy:bank/do_withdraw with storage trueeconomy:tmp