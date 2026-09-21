$execute store result score #base te.tmp run data get storage trueeconomy:main commodities.$(k).base
$execute store result score #supp te.tmp run data get storage trueeconomy:main commodities.$(k).supply
$execute store result score #dem te.tmp run data get storage trueeconomy:main commodities.$(k).demand
$scoreboard players set #idx te.tmp $(i)
$execute store result score #cyc te.tmp run data get storage trueeconomy:tmp cyc
$execute store result score #seed te.tmp run data get storage trueeconomy:tmp seed
scoreboard players operation #delta te.tmp = #dem te.tmp
scoreboard players operation #delta te.tmp -= #supp te.tmp
scoreboard players set #m1 te.tmp 7
scoreboard players operation #m1 te.tmp *= #idx te.tmp
scoreboard players set #m2 te.tmp 3
scoreboard players operation #m2 te.tmp *= #cyc te.tmp
scoreboard players operation #m1 te.tmp += #m2 te.tmp
scoreboard players operation #m1 te.tmp += #seed te.tmp
scoreboard players set #mod te.tmp 9
scoreboard players operation #m1 te.tmp %= #mod te.tmp
scoreboard players set #of te.tmp 4
scoreboard players operation #m1 te.tmp -= #of te.tmp
scoreboard players operation #drift te.tmp = #m1 te.tmp
scoreboard players operation #cv te.tmp = #delta te.tmp
scoreboard players operation #cv te.tmp *= #base te.tmp
scoreboard players set #dv te.tmp 20
scoreboard players operation #cv te.tmp /= #dv te.tmp
scoreboard players operation #price te.tmp = #base te.tmp
scoreboard players operation #price te.tmp += #cv te.tmp
scoreboard players operation #price te.tmp += #drift te.tmp
scoreboard players set #lo te.tmp 2
scoreboard players operation #res te.tmp = #base te.tmp
scoreboard players operation #res te.tmp /= #lo te.tmp
execute if score #price te.tmp < #res te.tmp run scoreboard players operation #price te.tmp = #res te.tmp
scoreboard players set #hi te.tmp 3
scoreboard players operation #res te.tmp = #base te.tmp
scoreboard players operation #res te.tmp *= #hi te.tmp
execute if score #price te.tmp > #res te.tmp run scoreboard players operation #price te.tmp = #res te.tmp
$execute store result storage trueeconomy:main commodities.$(k).price int 1 run scoreboard players get #price te.tmp
scoreboard players operation #cv te.tmp = #delta te.tmp
scoreboard players operation #cv te.tmp /= #dv te.tmp
scoreboard players operation #supp te.tmp += #cv te.tmp
scoreboard players set #lim te.tmp 1
execute if score #supp te.tmp < #lim te.tmp run scoreboard players operation #supp te.tmp = #lim te.tmp
scoreboard players set #lim te.tmp 64
execute if score #supp te.tmp > #lim te.tmp run scoreboard players operation #supp te.tmp = #lim te.tmp
$execute store result storage trueeconomy:main commodities.$(k).supply int 1 run scoreboard players get #supp te.tmp
scoreboard players set #m2 te.tmp 3
scoreboard players operation #m2 te.tmp *= #cyc te.tmp
scoreboard players operation #m2 te.tmp += #idx te.tmp
scoreboard players set #mod te.tmp 5
scoreboard players operation #m2 te.tmp %= #mod te.tmp
scoreboard players set #of te.tmp 2
scoreboard players operation #m2 te.tmp -= #of te.tmp
scoreboard players operation #dem te.tmp += #m2 te.tmp
scoreboard players set #lim te.tmp 8
execute if score #dem te.tmp < #lim te.tmp run scoreboard players operation #dem te.tmp = #lim te.tmp
scoreboard players set #lim te.tmp 48
execute if score #dem te.tmp > #lim te.tmp run scoreboard players operation #dem te.tmp = #lim te.tmp
$execute store result storage trueeconomy:main commodities.$(k).demand int 1 run scoreboard players get #dem te.tmp