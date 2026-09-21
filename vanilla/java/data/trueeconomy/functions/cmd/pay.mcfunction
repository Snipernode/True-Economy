function trueeconomy:util/ensure
$scoreboard players set #amt te.tmp $(amt)
scoreboard players set #lo te.tmp 1
execute if score #amt te.tmp < #lo te.tmp run scoreboard players set #amt te.tmp 1
$execute if entity @s[name="$(to)"] run tellraw @s ["You cannot pay yourself"]
$execute unless entity @s[name="$(to)"] run execute if entity @a[name="$(to)"] run execute store result score #bal te.tmp run scoreboard players get @s te.bal
$execute unless entity @s[name="$(to)"] run execute if entity @a[name="$(to)"] run execute if score #bal te.tmp < #amt te.tmp run tellraw @s ["Insufficient funds: ",{"score":{"name":"#bal","objective":"te.tmp"}}," / ",{"score":{"name":"#amt","objective":"te.tmp"}}]
$execute unless entity @s[name="$(to)"] run execute if entity @a[name="$(to)"] run execute unless score #bal te.tmp < #amt te.tmp run function trueeconomy:util/pay_exec with storage trueeconomy:tmp
$execute unless entity @s[name="$(to)"] run execute unless entity @a[name="$(to)"] run tellraw @s ["Player '$(to)' not found or offline"]
execute if entity @a[name="$(to)"] run tellraw @a[name="$(to)"] ["You received ",{"score":{"name":"#amt","objective":"te.tmp"}}," crowns from ",{"selector":"@s"}]