scoreboard players set #t te.cycle 0
scoreboard players add #g te.cycle 1
execute store result storage trueeconomy:tmp cyc int 1 run scoreboard players get #g te.cycle
execute store result storage trueeconomy:main meta.cycle int 1 run scoreboard players get #g te.cycle
execute store result storage trueeconomy:tmp seed int 1 run data get storage trueeconomy:main meta.seed
scoreboard players set #i te.i 0
execute store result storage trueeconomy:tmp i int 1 run scoreboard players get #i te.i
function trueeconomy:engine/loop
execute as @a run function trueeconomy:bank/interest