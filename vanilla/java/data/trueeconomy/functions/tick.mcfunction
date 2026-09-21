scoreboard players add #t te.cycle 1
execute store result score #itv te.tmp run data get storage trueeconomy:main config.intervalTicks
execute unless score #itv te.tmp matches 1.. run scoreboard players set #itv te.tmp 200
execute if score #t te.cycle >= #itv te.tmp run function trueeconomy:engine/cycle
execute as @a run function trueeconomy:blackmarket/scan