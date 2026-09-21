function trueeconomy:util/ensure
execute store result score #bal te.tmp run scoreboard players get @s te.bal
execute store result score #bq te.tmp run scoreboard players get @s te.bank
tellraw @s ["",{"text":"=== Balance ===","color":"gold","bold":true}]
tellraw @s ["  Crowns: ",{"score":{"name":"#bal","objective":"te.tmp"},"color":"green","bold":true}]
tellraw @s ["  Bank savings: ",{"score":{"name":"#bq","objective":"te.tmp"},"color":"blue","bold":true},{"text":"   "},{"text":"[Menu]","color":"gray","clickEvent":{"action":"run_command","value":"/function trueeconomy:menu"}}]