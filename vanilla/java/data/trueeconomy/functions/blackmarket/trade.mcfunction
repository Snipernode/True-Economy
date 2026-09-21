function trueeconomy:util/ensure
$scoreboard players set #amt te.tmp $(amt)
scoreboard players set #lo te.tmp 1
execute if score #amt te.tmp < #lo te.tmp run scoreboard players set #amt te.tmp 1
$data modify storage trueeconomy:tmp src set from storage trueeconomy:main blackmarket.$(blk).src
function trueeconomy:blackmarket/calc with storage trueeconomy:tmp