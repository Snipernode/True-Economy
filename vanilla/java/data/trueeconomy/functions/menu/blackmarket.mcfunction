function trueeconomy:util/ensure
tellraw @s ["",{"text":"=========== Black Market ===========","color":"dark_purple","bold":true}]
tellraw @s ["  Stand within 1 block of rare blocks to discover them"]
execute if entity @s[tag=te.found.emerald_block] run tellraw @s [" ",{"text":"[Emerald Block x2] ","color":"dark_green","bold":true,"clickEvent":{"action":"run_command","value":"/function trueeconomy:blackmarket/trade {\"blk\":\"emerald_block\",\"amt\":1}"}}]
execute if entity @s[tag=te.found.diamond_block] run tellraw @s [" ",{"text":"[Diamond Block x2] ","color":"aqua","bold":true,"clickEvent":{"action":"run_command","value":"/function trueeconomy:blackmarket/trade {\"blk\":\"diamond_block\",\"amt\":1}"}}]
execute if entity @s[tag=te.found.gold_block] run tellraw @s [" ",{"text":"[Gold Block x2] ","color":"yellow","bold":true,"clickEvent":{"action":"run_command","value":"/function trueeconomy:blackmarket/trade {\"blk\":\"gold_block\",\"amt\":1}"}}]
execute if entity @s[tag=te.found.netherite_block] run tellraw @s [" ",{"text":"[Netherite Block x2] ","color":"dark_purple","bold":true,"clickEvent":{"action":"run_command","value":"/function trueeconomy:blackmarket/trade {\"blk\":\"netherite_block\",\"amt\":1}"}}]
tellraw @s [" ",{"text":"[Menu]","color":"gray","clickEvent":{"action":"run_command","value":"/function trueeconomy:menu"}}]