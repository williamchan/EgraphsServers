package utils

import play.libs.Codec
import java.net.URLEncoder
import services.blobs.Blobs
import play.Play
import scala.Array

object TestConstants {

  lazy val ApiRoot = "/api/1.0"

  lazy val shortWritingStr = "{\"x\":[[67]],\"y\":[[198]],\"t\":[[324217524]]}"

  lazy val emptyWritingStr = "{\"x\":[],\"y\":[],\"t\":[]}"

  lazy val messageStr = "{\"x\":[[8,14,18,24,32,36,42,49,56,64,77,84,95,101,115,122,131,139,147,157,167,175,184,193,202,211,220,228,238,246,257,268,278,301,313,324,337,349,362,377,388,404,418,433,449,462,479,492,508,522,538,551,565,581,593,609,623,640,668,681,693,707,720,734,748,762,776,790,802,815,828,843,856,870,882,895,907,919,943,954,967,980,990,1003,1011,1018,1019]],\"y\":[[697,692,692,692,695,695,695,695,695,695,693,692,689,688,683,679,672,664,658,651,643,636,630,624,615,609,602,595,587,580,570,559,549,526,516,505,497,485,475,461,450,436,424,411,397,387,372,362,348,338,326,316,305,293,283,269,260,246,225,215,205,194,185,175,163,154,144,135,127,117,109,100,90,81,74,65,56,51,35,29,22,14,6,0,-7,-15,-17]],\"t\":[[0,93434000,109153250,125487625,148833291,157992291,174485958,190074625,206450000,222536250,255101041,270267416,303910208,321347625,354199041,370975875,386687791,403483000,420249750,437584125,453220416,470098166,486947416,503879583,520889666,536913458,554204833,570143083,587078500,604558541,621185833,637997375,653709208,670138208,687612791,703240000,719862083,737348541,754186958,770897833,787607083,803206166,819988625,836710333,853481583,870259666,887080125,903491250,920293916,937437125,953180125,970041375,986805541,1004626083,1021148458,1037634291,1053541333,1070423125,1087363291,1104282916,1119776791,1136519000,1153551958,1170357958,1187141625,1204059083,1220965333,1236801708,1253889083,1270833291,1287581041,1303722916,1320531500,1337918000,1353803625,1370907208,1388525333,1403778333,1421610791,1438146708,1454810333,1470365291,1487184583,1504355666,1520002958,1536630583,1553404000]]}"

  lazy val signatureStr = "{\"x\":[[5,15,20,27,33,40,47,53,60,66,72,78,85,90,97,105,110,116,124,129,137,144,152,160,168,176,186,193,202,211,219,226,236,244,251,260,280,289,299,309,319,330,342,353,365,377,387,399,410,421,432,443,455,465,477,488,499,511,522,533,545,569,582,594,609,620,632,644,655,667,678,690,701,713,723,736,746,757,768,778,789,798,810,820,831,843,856,878,891,902,916,926,937,949,959,969,979,988,997,1004,1011,1016,1018,1021,1023]],\"y\":[[1011,1008,1008,1008,1007,1005,1005,1005,1005,1004,1003,1002,1000,998,997,994,991,989,987,985,983,980,978,975,973,970,967,964,961,959,956,955,953,951,949,948,941,938,932,929,925,921,917,911,907,902,899,896,894,891,886,882,877,874,871,868,865,861,857,852,848,841,837,832,827,824,820,818,815,811,808,803,799,795,794,791,788,784,781,778,774,771,769,765,761,757,751,741,738,734,728,723,719,715,712,708,704,700,697,693,690,687,684,681,679]],\"t\":[[2848553403,3076591153,3093772737,3108801362,3131821695,3141920153,3156472695,3173032778,3189782320,3205009653,3220990820,3237434320,3253336112,3269764070,3286381653,3302106320,3319188362,3335313070,3352257403,3369449362,3385200445,3402347153,3418667862,3435431195,3452733237,3469412945,3486006237,3503229028,3518767028,3536268987,3552472487,3568479612,3585425653,3601637612,3618402612,3636403070,3653442612,3669932570,3686360862,3703540778,3719290362,3734908278,3751638528,3768472778,3785267028,3802162987,3819108403,3835979695,3851679820,3868393695,3885333070,3902144820,3919317153,3934961737,3951912778,3968690945,3985503445,4002360862,4019376112,4035189237,4051621737,4069840903,4086343028,4103346570,4119327778,4136053528,4153047403,4169209612,4185014403,4201821195,4218656570,4235673903,4252559070,4268271112,4284911945,6624524,23350107,39990232,56803399,73652399,90289191,107185316,124353982,139938899,157288899,173976107,190431524,207339774,224287524,241105566,256736149,273393024,290190399,307040107,324217524,341077816,356683482,373413149,390143649,406790399,423629482,440263149,457387232,474161982,490982774]]}"

  lazy val signingAreaMessageStr = "{\"x\":[[57,49,46,44,41,39,36,33,31,29,26,22,19,17,14,12,11,10,8,7,7,7,6,6,6,6,6,6,6,6,6,6,6,7,7,7,7,8,8,9,9,9,9,9,9,10,10,11,11,11,11,11,11,12,12,12,12,12,12,12,12,12,12,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,12,12,12,12,12,12,12,12,11,11,11,11,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,9,9,8,8,8,8,8,7,7,7,7,7,7,7,7,7,7,7,7,7,6,6,5,2],[52,61,63,66,69,72,75,79,82,85,89,92,95,99,101,105,109,113,116,120,124,129,133,138,142,146,151,156,161,166,170,175,180,186,190,195,200,205,210,216,221,226,232,239,245,250,257,265,271,278,285,293,300,308,317,323,330,338,345,352,358,365,372,377,383,391,397,403,409,416,422,435,442,450,457,465,474,480,488,495,503,510,518,525,532,540,547,553,561,567,575,582,589,594,601,614,620,627,634,639,644,651,658,663,668,674,680,686,693,698,704,711,716,722,728,741,747,754,761,768,774,781,788,795,800,805,812,817,823,828,834,839,843,848,853,858,862,867,871,876,883,887,892,896,900,903,907,911,915,919,923,926,930,934,937,941,945,948,952,955,959,962,965,969,975,979,982,985,989,992,995,999,1003,1006,1009,1011,1012,1014,1015,1016,1017,1018,1018,1019,1020,1021,1021,1021,1021,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1021,1021,1021,1020,1020,1020,1020,1019,1019,1019,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1017,1017,1017,1017,1017,1017,1017,1017,1017,1017,1017,1017,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1015]],\"y\":[[19,13,13,12,12,9,8,7,6,5,4,3,3,3,3,3,3,3,3,5,7,9,12,15,19,22,27,31,34,38,42,45,47,50,51,54,56,58,61,64,66,70,73,75,80,83,85,90,94,97,101,105,107,111,116,120,124,129,133,138,143,148,152,157,161,167,172,181,186,191,196,201,206,211,216,221,227,232,236,243,249,254,260,264,271,277,284,289,295,304,309,322,330,335,342,347,355,360,366,371,378,383,389,393,400,406,411,416,422,429,435,440,445,453,460,464,478,485,490,496,503,510,516,523,528,537,541,547,552,559,566,571,576,581,587,592,598,603,610,620,625,630,636,641,647,651,657,663,669,673,677,679,681,683,685,687,692,693,695,696,698],[10,4,4,4,3,3,3,3,3,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,3,3,3,3,3,4,4,5,5,6,6,6,6,7,7,7,7,7,7,7,7,7,7,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,7,6,6,5,5,5,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,4,4,4,4,3,3,3,4,5,6,8,10,12,13,16,18,21,22,24,27,28,32,35,38,42,45,47,51,53,56,59,62,66,70,73,76,77,80,82,85,88,91,95,98,100,103,105,107,110,114,117,120,124,127,129,132,134,138,142,145,152,155,158,161,164,168,173,177,179,183,185,188,192,197,201,204,207,211,214,218,222,226,231,237,241,246,251,254,258,261,265,269,274,279,283,287,290,296,301,307,311,315,321,327,332,337,341,352,357,362,366,371,376,382,386,390,395,401,406,411,415,419,424,430,435,440,444,449,460,464,469,473,478,484,489,494,498,503,508,513,518,522,526,531,537,542,547,552,557,564,569,574,579,592,596,601,606,612,617,622,626,630,636,641,646,649,653,656,659,664,668,671,674,677,678,680,681,683,685,687,688,690,693,695,696,698]],\"t\":[[0,124472417,140800417,156364709,172587334,190634417,205002917,220453750,236781084,252297292,268332167,284712709,300226709,316552709,333076875,348638375,364391709,380829542,396362542,412490625,428894459,446053125,462102625,478566042,494634834,511690417,527887417,545010959,560860167,578236709,594399084,611502792,627357792,644793959,661327209,677149417,694204042,711498459,727495750,744967417,761095584,778203459,793945125,811030917,828791792,845381167,862246042,878016209,894571792,911690750,927790417,943889417,961258459,978343667,994232542,1011445500,1027276042,1044449375,1061979000,1077915500,1093935667,1111253959,1127245542,1144600584,1161546792,1177289750,1194430042,1212190125,1229014667,1245344084,1261941209,1278089500,1293940125,1310883375,1328259209,1344122417,1361266042,1378289667,1394594792,1410483084,1427453834,1444856334,1460677042,1477903917,1493950834,1511041334,1528093709,1543995084,1561164584,1578310959,1594196959,1612072709,1628769417,1645360167,1661626084,1677708834,1694823834,1711001209,1728307917,1744169750,1761520959,1777398375,1794581125,1810494292,1827485625,1844839542,1860669584,1878273334,1894238709,1911597792,1927802459,1944947167,1960707875,1977902125,1995463084,2012016709,2028893917,2045416875,2062037459,2079028792,2094878542,2110919000,2128375292,2144207917,2161434000,2177382334,2194846250,2210597959,2227876417,2244997875,2260788042,2278199000,2294152417,2311439667,2327349167,2344292834,2361967000,2378876875,2396099000,2413083792,2428317667,2444590375,2460920959,2477337417,2494666417,2510716167,2528159500,2543912750,2560982959,2578151584,2594035125,2611273459,2627122459,2644207417,2661472000,2677241709,2694435375,2710474667,2727663209,2745004417,2760542459],[3965991875,4107179750,4123454125,4139131084,4155287792,4171703209,4187249250,4203049667,4219453750,4235082042,4250995917,4267304584,4282756875,3889288,20222246,35879454,51777704,68202746,84241079,100613204,117261038,133925204,150664788,164756454,179970496,196122996,212585996,227786996,243829038,260168329,275735871,291815538,308139329,323718579,339722371,356157996,371703454,387841079,404281663,419821621,435806704,452332829,467871538,483928204,500149746,516903871,533590788,550011246,565464871,582295788,599097913,617197579,632977538,648711454,665600704,682350788,699432413,716350163,732204454,749127496,765991079,782930829,798850121,815915204,832867913,849798371,865464746,882087288,899396871,916622246,932752079,949411829,966446079,983169704,998880871,1015478204,1032066538,1048766538,1065447996,1082108371,1098779746,1115519954,1132208746,1149130913,1165987163,1182916663,1199867579,1215353996,1232603454,1249765121,1265582829,1282300704,1300262163,1316668704,1332399871,1349164204,1366103954,1383068746,1399899288,1415459788,1432219704,1448985038,1465653413,1482505496,1499394913,1516317829,1533107746,1550216329,1565912079,1583130413,1598952496,1615732954,1632456329,1649219496,1666361871,1683630329,1700154913,1716838288,1733568746,1750238454,1766026079,1782980954,1799730204,1816481413,1833171746,1848662204,1865470954,1882172163,1898795246,1915504663,1932123288,1948883538,1965823496,1982957496,1999829788,2016515371,2032005996,2048738371,2065492454,2082315163,2100241788,2116934121,2133556788,2149015246,2165746704,2182707579,2198667288,2216493371,2233286121,2248878663,2265705871,2282613288,2299693288,2316466079,2332011788,2348770413,2365554871,2382276329,2399786579,2416489329,2433176621,2448678121,2466532704,2482703621,2500129663,2516936913,2533556246,2549244913,2565959413,2582990996,2599936038,2615525121,2632343038,2649183121,2665918746,2682681829,2699868079,2715450871,2732287288,2748941413,2765652621,2782352079,2799130579,2816451996,2832182496,2867346704,2883919579,2900819246,2917266163,2931031996,2947607913,2963259413,2979162579,2995290621,3010826996,3026823871,3043262246,3058828746,3076134246,3108573246,3110203329,3123417663,3139063704,3154962829,3171262288,3187671538,3203061288,3219086996,3235452413,3251084413,3267213746,3283533163,3300259288,3316828246,3332617288,3349561871,3366358038,3382372454,3398997079,3415632246,3432432913,3449080871,3466306454,3483676913,3499204288,3516204079,3533137163,3548667454,3565431663,3582282496,3599008246,3615990246,3633066621,3649856246,3665313329,3682749329,3700080496,3715813121,3732434371,3749353663,3766264871,3783078829,3799771788,3816502829,3832105621,3848851704,3865454954,3882309871,3899185621,3915982288,3933126079,3948722621,3965421038,3982146871,3999181246,4016331079,4033180996,4048744163,4065355621,4083288246,4099411163,4116917538,4132466246,4149401163,4166320663,4183131329,4198784746,4215626038,4232406621,4249162829,4265948746,4282873913,4705408,20442950,37091450,53714867,70374867,87187117,104255825,121260075,138033575,155125325,171604658,187726283,204209242,221057950,237959408,254727950,271528283,288333950,305449117,321106992,338275325,353762950,370554575,387168492,403796533,420514658,437627908,454446992,471481742,488289492,503871950,520715700,537478700,554241117,571953325,588624950,605284325,621844992,637270950,653998575,670876492,688072950,704084117,720796617,737484992,754383200,771174825,788046200,804737742,821512117,837296700,854062283,870667158,887680783,904448825,921227117,938215158,954001825,971556533,988636158,1005211992,1020772450,1037615408,1054338867,1071309408,1087066533,1103839200,1120672742,1137367450,1154021617,1170609783,1187346283,1204133117,1220855075,1237775075,1254686658,1271500075,1288748783,1304000075,1320980158,1338169200,1354319992,1371083283,1388605450,1405461075,1421008408,1436993408,1454925533,1470586033,1487231742,1504053367,1520815992]]}"

  lazy val signingAreaSignatureStr = "{\"x\":[[1017,1016,1016,1016,1016,1016,1016,1016,1017,1017,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1017,1015,1015,1015,1015,1015,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1014,1014,1014,1013,1012,1009,1007,1004,999,994,989,985,981,977,973,968,964,960,955,950,946,941,936,932,927,923,918,914,909,904,898,894,888,883,877,872,867,860,853,848,841,834,827,819,811,804,797,790,782,775,768,753,746,738,729,723,715,707,700,693,687,679,672,665,659,652,644,637,630,623,616,610,603,596,591,584,577,571,559,553,547,541,534,527,522,514,508,501,494,489,481,474,467,460,453,445,440,434,427,420,416,411,401,395,390,384,377,372,366,360,354,349,343,338,333,328,324,319,314,310,306,301,297,292,287,283,275,270,265,260,256,251,247,243,238,234,229,225,221,216,211,207,203,199,195,191,186,177,173,169,164,158,153,148,144,139,134,131,127,123,119,116,113,110,106,102,99,95,90,86,78,74,70,66,62,58,54,50,46,43,39,36,32,29,24,21,17,15,13,12,10,9,8,8,8,8,8,8,7,7,7,7,7,7,7,8,8,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,8,8,8,8,8,8,8,9,9,10,11,11,11,12,12,12,12,12,12,12,12,12,12,12,12,12,12,12,12,11,10,8]],\"y\":[[707,715,719,724,728,731,735,738,741,746,750,754,758,762,765,770,775,779,783,786,789,794,799,803,807,811,815,818,822,827,832,835,840,844,847,852,856,860,863,867,873,877,882,887,891,894,898,901,906,911,915,919,922,926,930,935,940,943,946,950,952,956,961,964,969,971,974,979,982,985,989,992,994,997,998,999,1000,1001,1002,1003,1005,1006,1007,1008,1009,1010,1010,1010,1011,1011,1011,1011,1013,1014,1014,1015,1015,1015,1015,1015,1015,1015,1015,1015,1014,1013,1013,1013,1013,1012,1011,1011,1010,1010,1009,1009,1008,1008,1008,1008,1008,1007,1007,1007,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1007,1007,1007,1007,1007,1007,1007,1007,1007,1008,1008,1008,1008,1008,1008,1008,1009,1009,1009,1009,1010,1010,1011,1011,1011,1011,1012,1012,1012,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1013,1013,1013,1013,1013,1012,1012,1012,1012,1012,1013,1013,1012,1011,1010,1008,1006,1004,1003,1001,999,996,991,987,983,979,976,974,971,968,966,963,959,956,954,952,950,947,945,941,938,934,930,926,923,920,916,911,906,902,899,896,891,885,880,875,871,865,858,853,848,836,831,825,819,812,803,798,791,783,775,770,765,759,752,747,742,738,733,727,721,717,713,709,704,701,697,691,687,684,680,678]],\"t\":[[3845565904,3986642821,4003072279,4018540821,4038566196,4051097404,4066374904,4082536654,4098971654,4114950904,4130763529,4147409904,4162577487,4178554529,4195012321,4210674029,4227009529,4243699862,4260114487,4275756946,4292659862,14516566,31342566,48303525,64006191,80627816,97435233,114166275,130977150,147933233,163605108,181901566,198798775,215129233,231842441,247589525,264259566,281111108,298252191,314002816,332058441,348690566,365116400,381948358,397622816,414438275,431413733,447263150,463998816,480686191,497346108,514137858,530721650,547714191,564525025,581350025,598206400,613832191,630519983,648112400,664891191,680342608,697198441,713798733,731505691,748697775,764395483,780575483,797464066,814124108,830822983,847681941,864499733,881355275,898200733,913711150,931538816,947116775,963963608,986960650,1018907483,1034940108,1051387775,1066937775,1099466733,1115780483,1132329275,1149359150,1165772983,1179254733,1195496108,1211215691,1227251400,1243379316,1258883983,1274887108,1291366816,1306894650,1322849108,1339289275,1354836441,1370861650,1387285566,1403229775,1419243358,1435841483,1451024816,1467052691,1483430983,1499086858,1515349525,1531950941,1548607191,1564192775,1581032150,1597994066,1614890566,1630588066,1647322233,1663950108,1680566816,1697211108,1713963358,1730558816,1747627858,1764711358,1780415900,1797168941,1813821400,1830590941,1847530775,1864787066,1880568483,1897337775,1915349066,1932031316,1948686483,1965317983,1980947233,1997898441,2014896941,2030454900,2047352858,2064029733,2080799983,2097694233,2114593691,2131415775,2148150566,2163746691,2180499775,2197140025,2213838441,2230914566,2247632816,2264707400,2280551566,2297298441,2315135983,2331537483,2347721150,2363958691,2380729358,2397551608,2414472691,2431368150,2448109483,2464861816,2480615275,2497227483,2513953108,2530718191,2547338900,2563944191,2580804983,2597608525,2614830358,2630746191,2647989525,2664901900,2680723316,2698297233,2714329483,2731137108,2747298900,2764152608,2781209900,2798108858,2813897608,2830609400,2847260566,2864088316,2881222025,2898090775,2914837525,2930451608,2947112858,2963776941,2980544650,2997163316,3013972316,3031169108,3047987150,3064891941,3080635275,3098388316,3114461775,3130920358,3147138233,3164123275,3181012566,3197980650,3214880275,3230394733,3247181025,3263888358,3280616566,3297539858,3314333191,3331137816,3348032483,3363763525,3380613733,3397336316,3414091733,3431121191,3447916316,3464783900,3480477025,3497258691,3515369608,3531876233,3548437858,3563979733,3580735941,3597479358,3614995483,3630700566,3647427983,3664158525,3681511400,3697056858,3714879900,3730395941,3747062691,3763732900,3780490733,3797552900,3814839566,3831563066,3847159191,3863942483,3880945691,3898863650,3915546566,3932263941,3948247566,3964016316,3980881150,3998306191,4014355025,4031747816,4047533025,4064700816,4080632775,4097994358,4114033400,4131083316,4148298316,4164251108,4181338233,4197408691,4214313608,4231628900,4247363775,4266630525,4229437,20897312,37534229,51576187,67953270,83508979,99282062,115535520,131136020,147093354,163586229,179058437,195166312,211508645,227252520,243035854,259457479,275149562,291401020,307780687,323252354,339222770,355703729,371223437,387400062,403904020,420624645,436969062,455226895,470100104,486204687,503451770,519541687,536874395,553180687,569122604,586392979,602307270,619286770,636775812,652618062,669901437,685941062,703381729,719357687,736584270,752603437,770063645,785910687,803747729,820583729,837306729,852256687,869412520,886822770,904080770,929140687,936915729,954159854,970220937,986561729,1002525229,1019767604,1036827520,1052629062,1069810395,1086824812,1102670312,1120142229,1135881395,1153017562,1168945104,1186878270,1203626604,1220449145,1236544187,1252351895,1283414895,1299003604,1314906520]]}"

  lazy val fakeAudio: Array[Byte] = "my audio".getBytes("UTF-8")

  def fakeAudioStr(): String = {
    Codec.encodeBASE64(fakeAudio)
  }

  def voiceStr(): String = {
    val wavBinary = Blobs.Conversions.fileToByteArray(Play.getFile("test/files/44khz.wav"))
    Codec.encodeBASE64(wavBinary)
  }

  def voiceStr_8khz(): String = {
    val wavBinary = Blobs.Conversions.fileToByteArray(Play.getFile("test/files/8khz.wav"))
    Codec.encodeBASE64(wavBinary)
  }

  /**
   * Making an HTTP request, even to a localhost API, requires that special characters be escaped.
   */
  def voiceStrPercentEncoded(): String = {
    URLEncoder.encode(voiceStr(), "US-ASCII")
  }

  def fakeAudioStrPercentEncoded(): String = {
    URLEncoder.encode(fakeAudioStr(), "US-ASCII")
  }

}