function trueeconomy:util/ensure
tellraw @s ["",{"text":"=========== Market ===========","color":"green","bold":true}]
scoreboard players set #i te.i 0
execute store result storage trueeconomy:tmp i int 1 run scoreboard players get #i te.i
function trueeconomy:market/list_loop
tellraw @s ["",{"text":"[Menu]","color":"gray","clickEvent":{"action":"run_command","value":"/function trueeconomy:menu"}}]