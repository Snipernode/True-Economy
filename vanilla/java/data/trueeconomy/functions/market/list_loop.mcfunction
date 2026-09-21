$scoreboard players set #i te.i $(i)
$execute if score #i te.i matches 0..31 run function trueeconomy:market/list_one with storage trueeconomy:tmp
$execute if score #i te.i matches 0..31 run scoreboard players add #i te.i 1
$execute if score #i te.i matches 0..31 run execute store result storage trueeconomy:tmp i int 1 run scoreboard players get #i te.i
$execute if score #i te.i matches 0..31 run function trueeconomy:market/list_loop with storage trueeconomy:tmp