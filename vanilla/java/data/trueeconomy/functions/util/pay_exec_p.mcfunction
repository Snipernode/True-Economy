scoreboard players operation @s te.bal -= #amt te.tmp
scoreboard players operation #tax te.tmp = #amt te.tmp
execute store result score #rate te.tmp run data get storage trueeconomy:main config.taxRate
scoreboard players operation #tax te.tmp *= #rate te.tmp
scoreboard players set #rate te.tmp 1000
scoreboard players operation #tax te.tmp /= #rate te.tmp
scoreboard players operation #recv te.tmp = #amt te.tmp
scoreboard players operation #recv te.tmp -= #tax te.tmp
execute if entity @p run scoreboard players operation @p te.bal += #recv te.tmp
execute store result score #tx te.tmp run data get storage trueeconomy:main stats.taxes
scoreboard players operation #tx te.tmp += #tax te.tmp
execute store result storage trueeconomy:main stats.taxes int 1 run scoreboard players get #tx te.tmp
tellraw @s ["Paid ",{"score":{"name":"#recv","objective":"te.tmp"}}," crowns (+",{"score":{"name":"#tax","objective":"te.tmp"}}," tax)"]