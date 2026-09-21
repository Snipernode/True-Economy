function trueeconomy:util/ensure
tellraw @s ["",{"text":"=========== Pay ===========","color":"yellow","bold":true}]
tellraw @s [" ",{"text":"[Pay 100] ","color":"gold","clickEvent":{"action":"run_command","value":"/function trueeconomy:cmd/pay_p {\"amt\":100}"}},{"text":"[Pay 250] ","color":"gold","clickEvent":{"action":"run_command","value":"/function trueeconomy:cmd/pay_p {\"amt\":250}"}},{"text":"[Pay 500]","color":"gold","clickEvent":{"action":"run_command","value":"/function trueeconomy:cmd/pay_p {\"amt\":500}"}}]
tellraw @s ["  Pay a specific player (custom amount):"]
tellraw @s ["  ",{"text":"/function trueeconomy:cmd/pay {to:\"NAME\",amt:100}","color":"gray","clickEvent":{"action":"suggest_command","value":"/function trueeconomy:cmd/pay {to:\"\",amt:100}"}}]
tellraw @s [" ",{"text":"[Menu]","color":"gray","clickEvent":{"action":"run_command","value":"/function trueeconomy:menu"}}]