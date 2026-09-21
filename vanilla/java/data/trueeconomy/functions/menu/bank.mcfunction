function trueeconomy:util/ensure
execute store result score #bal te.tmp run scoreboard players get @s te.bal
execute store result score #bq te.tmp run scoreboard players get @s te.bank
tellraw @s ["",{"text":"=========== Bank ===========","color":"blue","bold":true}]
tellraw @s ["  Crownal: ",{"score":{"name":"#bal","objective":"te.tmp"},"color":"green"}]
tellraw @s ["  Savings: ",{"score":{"name":"#bq","objective":"te.tmp"},"color":"blue"},{"text":"  (interest 0.2%/cycle)"}]
tellraw @s [" ",{"text":"[Deposit 100] ","color":"green","clickEvent":{"action":"run_command","value":"/function trueeconomy:bank/deposit {\"amt\":100}"}},{"text":"[Deposit 500] ","color":"green","clickEvent":{"action":"run_command","value":"/function trueeconomy:bank/deposit {\"amt\":500}"}},{"text":"[Deposit ALL]","color":"green","clickEvent":{"action":"run_command","value":"/function trueeconomy:bank/depositAll"}}]
tellraw @s [" ",{"text":"[Withdraw 100] ","color":"yellow","clickEvent":{"action":"run_command","value":"/function trueeconomy:bank/withdraw {\"amt\":100}"}},{"text":"[Withdraw 500] ","color":"yellow","clickEvent":{"action":"run_command","value":"/function trueeconomy:bank/withdraw {\"amt\":500}"}},{"text":"[Withdraw ALL]","color":"yellow","clickEvent":{"action":"run_command","value":"/function trueeconomy:bank/withdrawAll"}}]
tellraw @s [" ",{"text":"[Menu]","color":"gray","clickEvent":{"action":"run_command","value":"/function trueeconomy:menu"}}]