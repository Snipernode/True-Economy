scoreboard objectives add te.bal dummy "Crowns"
scoreboard objectives add te.bank dummy "Bank savings"
scoreboard objectives add te.i dummy
scoreboard objectives add te.cycle dummy
scoreboard objectives add te.tmp dummy
scoreboard objectives add te.started dummy
function trueeconomy:engine/seed
data modify storage trueeconomy:tmp i set value 0
scoreboard players set #t te.cycle 0