function trueeconomy:util/ensure
$scoreboard players set #amt te.tmp $(amt)
scoreboard players set #lo te.tmp 1
execute if score #amt te.tmp < #lo te.tmp run scoreboard players set #amt te.tmp 1
execute if entity @p[limit=1] run execute store result score #bal te.tmp run scoreboard players get @s te.bal
execute if entity @p[limit=1] run execute if score #bal te.tmp < #amt te.tmp run tellraw @s ["Insufficient funds: ",{"score":{"name":"#bal","objective":"te.tmp"}}," / ",{"score":{"name":"#amt","objective":"te.tmp"}}]
execute if entity @p[limit=1] run execute unless score #bal te.tmp < #amt te.tmp run function trueeconomy:util/pay_exec_p
execute if entity @p[limit=1] run tellraw @p ["You received ",{"score":{"name":"#amt","objective":"te.tmp"}}," crowns from ",{"selector":"@s"}]