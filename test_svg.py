# Let's inspect dark_mode.svg vs light_mode.svg structure like Andrew6rant
import html

left_ascii = [
    r"           ,g@M%%%%N%Nw,,            ",
    r"        ,M*|`||*%gNM=]mM%g||%N,      ",
    r"       p!` `' |`'' '''|||jhlj%w      ",
    r"     ,@L `  ,,       ''`|j%M]mM      ",
    r"    ]j'` .,wp@pw,    `.   ''''|%Wg   ",
    r"  /{[|]@@@@@@@@@pp.         ,||||||  ",
    r" '` ']@@@@@@@@@@@@@p       ,```'`    ",
    r"   :]%%@@@@%%%%%%k%h '*||mkr    *    ",
    r" '  j%M`    |jkk'   ~nrn=|i   ;`     ",
    r"   ! jrr*^`           `"! L''!       ",
    r"   j lp;,.  ,/ @@   ,;\nmy \"  ,~      ",
    r"  i r @@@@mmHM @@@@ `^****M*,p ;.    ",
    r"  | ]@@@HHH]g@M%%%%H,jmgpmb% j       ",
    r"  ;;%%%%%k%@[,.n|;.;j%%k|%k%%',[     ",
    r"   H|%%k%%%j%k||,;;j;!!'|%ij}]@      ",
    r"  \"djjmkL,\"]] [,,,,wwxw;|#kjk`      ",
    r"    %;%km%%%M%M|%%jkkii||| [         ",
    r"     kjj%%kkkl!|||||||j|||\"         ",
    r"     |jm%H@@@b%%kkmk%i!,[            ",
    r"     @p|j%%%%jkk|||j*``;j[           ",
    r"     ]@@@g|'''`''' `  ,;j%k          ",
    r"     @@@@@mgmp;,,,::;jj%%k%          ",
    r"    @@@@@@@@%%kgki!|jjjj%k%@ .       ",
    r" . ^['' %@@@HH%b%k{illljkjj%%%% ;  `,",
    r" =[' `. %HH%%%%H@gkilljjj%kk%\".   `;i"
]
print("Lines:", len(left_ascii))
