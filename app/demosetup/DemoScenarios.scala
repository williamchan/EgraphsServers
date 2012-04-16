package demosetup

import services.blobs.Blobs.Conversions._

import javax.imageio.ImageIO
import services.{AppConfig, ImageUtil}
import services.blobs.Blobs
import services.db.Schema
import play.Play
import models.{CelebrityStore, EnrollmentStatus, Account, Celebrity}

class DemoScenarios extends DeclaresDemoScenarios {
  val demoCategory = "Demo Preparation"

  val boxSignatureStr = """{"t":[[2127581962,2212121800,2243778276,2271000326,2300709481,2309719008,2331820110,2353264906,2376092321,2392479228,2410878180,2426398701,2443776238,2460898562,2478983650,2495379348,2511992988,2529727839,2548164140,2566088281,2587209252,2605022226,2623256151,2641108349,2660269632,2677351356,2696082457,2712413319,2730555671,2749031017,2764462152,2782944171,2801049106,3044364650,3065352601,3086852969,3110228607,3115093306,3132815977,3153369959,3184669880,3203320180,3222112763,3242670732,3247376019,3262919629,3279617797,3299607907,3315144997,3333265862,3352231297,3368901269,3387106396,3404866839,3422757937,3439366822,3456166837,3472116380,3488261028,3506299130,3522441260,3539746410,3557221757,3572910507,3589712850,3607393518,3626377868,3645372230,3941120218,3960078777,3981484238,4004458167,4009588701,4028290592,4049824467,4066240196,4085468107,4103215767,4120735240,4137826786,4157518932,4174348429,4193174538,4211951156,4230641332,4249587471,4268160536,4288630071,11303382,30669516,49678342,66782676,82777421,100073781,116675445,132917751,151513861,167695265,186519310,202302573,220481883,238831943,257465532,275456462,294732520,312255391,332615352,350511836,367414290,386032406,405164773,423632603,455449003,475051551,494735726,499294196,514987082,533050632,552310256,571653181,588779495,606047551,621934686,637937274,654000174,670479263,688279330,707182123,724140064,756011126,776572736,781596460,799384461,847440551,868596030,889254580,911315533,915937000,934231532,1014469484,1037297292,1059719456,1082366882,1104119932,1127499870,1214898096,1232136688,1481402233,1505178223,1525294315,1544580602,1549415135,1568736805,1587068040,1605789984,1622853624,1656018191,1676541014,1699486630,1703986613,1719995051,1737726532,1754430334,1770792025,1786982155,1805289712,1821733355,1842410525,1861947953,1881678352,1899368215,1918243749,1936387025,1952787824,1970872954,1986716824,2002975877,2021790394,2038469381,2058156173,2075048865,2094101872,2112414333,2128459483,2144378792,2164707429,2183577071,2201953649,2219420401,2238938693,2515182279,2536662112,2558321851,2579503963,2583948734,2601616771,2618209919,2635848551,2655085874,2673543605,2693119342,2710419159,2726802793,2745045879,2765938912,2784030325,2802194125,2818699989,2834754059,2852974710,2869108845,2885972752,2906555584,2924472099,2941389223,2958593205,2974708544,2992337805,3010245553,3026824612,3042803895,3060647123,3077701594,3094712360,3110709149,3128913684,3149437930,3171067614,3189660085,3211119555,3229395152,3247360283,3264776499,3284171089,3300051539,3317760123,3333968393,3350278862,3367146444,3386276273,3406566763,3423826741,3441445314,3461627050,3478701963,3496833201,3515847065,3535133930,3552453854,3568484764,3585885933,3603449770,3623809170,3639715633,3657573921,3673907693,3733335569,3753661481,3783117035,3806660103,3827249594,3849812783,3867297694,3889255201,3910159828,3931651930,3936085504,3951698961,3970294641,3988112080,4006481125,4026357773,4083140562,4105638064,4124372629,4146758383,4151295135,4182894135,4187523782,4207051399,4222844234,4243303912,4259242639,4276167775,4292491772,14161029,32368939,49045584,67108454,83320793,99051359,115155168,133093693,149972303,167885798,188259229,206132798,519416061,538307885,556086178,573346999,593722949,616205381,620819319,652448333,656898445,673049647,692908324,708999847,725577879,741834744,757476695,777432265,795900025,812618557,829557516,847561053,865474905,883320149,901467735,920882324,938887470,956688716,975011226,994580149,1012407809,1029902037,1048649153,1066402486,1085022193,1101519395,1403999356,1424098325,1444359818,1465467138,1470865531,1488114367,1505343257,1522097507,1539645765,1555816908,1571599766,1590663568,1608521068,1627872307,1647141373,1664159284,1682707825,1701005353,1719078847,1735728965,1753599766,1772386458,1791247348,1809299945,1830030094,1848021273,1864697613,1882690138,1898763785,1916621878,1935006484,1952494282,1969754365,1988402067,2008954566,2029771811,2046508864,2062669522,2337644785,2355774265,2378848446,2398199773,2420744347,2425249044,2444409026,2460099528,2476972284,2493240508,2511224704,2528649745,2545276805,2563555544,2582818614,2603064105,2621915682,2638173876,2656018496,2674713738,2691765513,2710584073,2729028807,2748266322,2764486265,2781976207,2801993416,2819531368,2837606233,2855539775,2875897805,2895427923,2913054703,2929885873,2948020883,2965810325,2983482232,3001229933,3019471716,3035569622,3051380625,3067843822,3084471535,3100940467,3117855337,3135556038,3153987902,3172050825,3188539458,3206272365,3224193092,3241742638,3258711125,3276686676,3297796122,3316522405,3332500262,3349053878,3367274258,3383211223,3401006587,3419022996,3437577562,3655820723,3675865668,3698051992,3718147540,3739726966,3744470177,3760620443,3778829967,3795582918,3812822964,3829342712,3845885474,3864146492,3879968726,3898063222,3914144352,3931209768,3949487662,3965845977,3984247848,4000716836,4019343553,4036875883,4056550147,4076372428,4094509964,4114611758,4135817848,4154631703,4174879753,4192601955,4210235534,4226993898,4243558007,4262048234,4279290397,4052572,20945537,39452528,311206050,332179549,352349069,371946721,391876992,396155467,411960281,429447397,445590073,479603812,484024649,503305636,519063317,538624209,554950346,571095612,588078698,604559588,621998786,639709756,659668738,677483590,695497267,711507886,731868512,748220940,766125202,781838166,800908952,820835879,837301477,855252217,872098130,891243199,911221577,929135036,945923910,965624247,983353790,1001819739,1020382759,1038486750,1054581996,1074022568,1090543289,1111175038,1133269309,1153146846,1172090928,1188556496,1205022647,1223437358,1239414696,1255971158,1287466297,1310069277,1329779936,1409183640,1431275259,1456074697,1460895279,1455596691,1460413478]],"x":[[3,4.037036,5.307269,7.202152,9.98582,14.6995,21.57762,33.02299,49.7105,63.5068,73.74274,80.22006,85.99112,92.18254,95.23926,97.33014,99.87559,102.5557,106.1646,110.308,116.0172,124.5606,133.3142,145.1672,159.0125,168.374,173.6664,178.9752,185.4,190.8963,196.5989,203.5478,211.7549,220.8903,228.3749,232.037,236.8628,245.2926,256.6052,270.3274,282.5785,289.2455,294.5912,299.7677,306.1904,312.686,318.2773,322.4525,325.6155,328.4786,332.0677,335.5015,338.6671,344.1976,354.0956,365.8801,373.9644,381.2857,391.0846,401.8398,410.6192,418.6279,426.5564,437.1278,452.7786,468.8603,482.2178,492.5089,500.8545,505.9568,512.1724,527.014,564.5967,615.5841,652.3952,674.5985,686.955,694.357,700.439,705.2411,708.701,711.3187,712.9092,715.0841,720.0619,728.0923,735.2866,739.8997,744.822,751.1324,758.6688,769.1981,779.6141,784.9226,790.4214,797.31,806.314,815.3522,822.7339,828.3655,833.9971,838.2953,843.1985,848.9476,856.1696,863.1613,868.8255,874.6519,881.7856,888.8994,892.6368,895.6331,898.1505,900.7852,903.4178,907.42,912.9762,916.9188,920.3462,922.584,924.5433,927.7164,931.8788,935.5936,938.361,940.4031,943.0453,945.4207,946.8653,948.2192,949.8427,951.5089,953.8173,955.6495,956.8961,958.0062,959.298,961.0882,962.989,964.7004,966.0222,967.8213,970.6136,972.5891,973.8782,974.9638,975.9151,976.2711,974.9691,972.3611,970.6254,969.6667,968.4938,968.1462,968.0062,967.7054,966.8756,965.7408,964.5157,964.1527,964.0452,964.0133,964.0409,964.2713,964.821,965.2802,966.3052,967.5348,968.1584,969.0098,969.7436,970.2202,971.0281,971.712,971.9886,972.5151,973.5599,973.8695,973.9612,974.0255,974.2667,974.7827,974.9355,975.0549,975.5717,976.836,977.655,977.8236,977.4291,976.4233,976.1254,976.0741,976.3922,977.8198,980.9465,984.7989,986.607,987.5872,987.8406,987.6564,986.9351,986.2399,985.8118,985.2404,985.0341,984.7507,984.2224,984.0658,984.0194,983.9686,983.6943,982.9094,981.936,980.7217,979.2137,978.1003,977.3259,977.0224,976.488,975.4408,975.0935,974.7313,973.9573,973.2465,972.8137,972.204,971.8011,971.2372,971.0702,971.0207,971.006,971.0017,971.0004,971.0001,970.9999,970.9999,971.0369,971.2701,971.7836,971.9358,971.9809,971.9943,971.9982,971.9994,971.9998,971.9998,971.9999,972.0369,972.3071,973.0909,974.0269,975.0079,976.0763,977.541,979.5676,980.9088,982.1951,983.4651,983.8044,983.6827,983.1281,982.4823,981.1428,980.0422,979.0125,977.9666,976.7308,975.1794,973.7197,971.5465,968.4581,965.3209,964.3542,963.8086,962.9432,961.9831,960.9949,959.9243,958.3109,955.1661,950.4565,944.6907,937.9824,930.7725,926.5992,923.733,921.8097,918.9435,913.9462,907.8358,903.0253,898.0445,890.865,878.1451,867.3392,856.9153,845.9008,835.0076,829.4466,825.7249,819.7703,808.8948,797.5984,785.6587,774.8988,769.1922,765.6124,762.811,759.1661,753.3084,745.0173,737.7458,733.2579,729.3727,726.0733,722.7253,718.8445,714.3983,709.0809,703.8017,698.4968,693.8508,688.8817,683.1501,674.1926,657.5385,633.0114,614.5589,601.1285,590.5936,578.8795,566.2235,555.9921,547.2939,537.2722,521.3029,502.2749,490.674,482.2367,474.8849,463.9659,446.6565,425.3427,408.8793,400.7049,395.2829,390.4171,384.9013,375.3781,364.0379,354.0482,342.1254,326.2964,316.0508,307.8298,300.2458,289.9247,276.348,266.6216,257.6286,248.2233,239.3995,231.4517,224.2079,215.9875,202.9593,185.025,167.637,155.6332,144.7061,138.8388,135.2485,129.5921,120.2495,106.5925,93.02739,82.26737,74.96811,68.43499,64.12888,60.03819,56.01131,52.04039,48.27122,44.74703,40.73986,36.84885,34.54781,33.0512,31.20036,28.28159,25.60195,22.73391,20.92116,19.01368,17.22627,15.51149,13.11452,10.81171,8.499766,6.925857,5.570624,5.169074,5.050096,5.088917,5.618937,7.14635,8.821139,10.46552,12.54534,13.82825,14.68985,15.2044,16.0976,17.28817,18.78909,19.9375,21.01852,22.26474,23.74511,24.62818,24.88983,24.93032,24.72009,24.21336,24.06322,24.01873,24.00555,24.00164,23.96345,23.72991,23.21627,23.06408,22.87084,21.88766,19.55931,18.09165,16.4716,14.21381,12.58187,11.65389,10.453,9.874964,9.259248,9.076814,8.985722,8.736509,8.218225,8.064658,7.945083,7.465209,6.434136,6.128633,6.038113,6.011292,6.003345,6.00099,6.000293,6.000086,6.000025,6.000007,6.037038,6.30727,7.016968,7.412434,6.862943,6.255687,6.112795,6.329716,7.060656,7.721675,7.917532,7.938528,7.611415,6.403383,4.712114,4.210997,4.099554,4.251719,4.48199,3.883553,3.261793,3.077568,3.022983,3.006809,3.002017,3.000597,3.000177,3.000052,3.000015,2.92593,2.459535,1.432455,1.128135,1.037966,1.011249,1.003333,1.000987,1.000292,1.000087,1.000026,1.000008,1.037039,1.270233,1.820809,2.206165,2.764789,2.930308,2.97935,2.993881,3.035224,3.269695,3.783613,3.972922,4.251235,4.778143,4.934264,4.980522,4.994228,5.035326,5.306763,6.053854,6.71966,6.916935,6.975388,6.992707,6.997838,6.999359,6.962772,6.72971,6.179173,5.793829,5,5]],"y":[[739,738.9999,738.9999,738.9999,738.9999,738.9629,738.5074,736.5577,733.0911,731.6195,731.1835,731.2025,732.097,734.1398,734.7451,734.9244,734.9775,735.0303,735.2682,735.7831,735.9357,735.9809,735.9572,735.728,735.2156,735.1009,735.2891,735.7893,735.9745,736.2516,736.7042,736.2827,734.4171,731.4198,728.3836,727.9655,728.9527,729.6526,729.6378,729.1889,729.0559,729.0165,729.0048,729.0013,729.0004,729.0001,728.9999,728.9999,729.074,729.6144,731.1079,732.4393,732.8338,732.9877,733.2556,733.7794,733.9346,733.9805,733.9942,733.9982,734.0364,734.27,734.7836,734.9728,735.3253,736.3926,738.0792,739.6901,740.6118,740.8109,740.055,736.2014,727.3559,717.5869,712.6923,710.7977,710.2363,710.0699,710.0207,710.0061,710.0388,710.2707,710.8579,711.5134,712.8928,714.2274,715.7711,716.9691,718.2501,719.7407,720.7009,721.4298,722.6088,723.4396,724.8339,725.9877,727.2556,728.7794,729.9346,730.9435,731.7239,732.2144,733.0635,733.9817,734.6982,734.9476,735.3177,736.3163,737.501,737.8892,738.2264,738.8077,739.2022,739.7635,739.9669,740.2494,740.7775,740.934,740.9804,740.9941,740.9982,740.9624,740.6925,739.9459,739.2802,739.0829,739.0245,739.0072,739.0021,739.0005,739.0001,738.963,738.7297,738.2161,738.064,737.9819,737.6983,736.9476,736.2807,736.0831,736.0246,736.0072,736.0021,736.0005,736.0001,735.963,735.6927,734.8718,733.1101,727.44,714.8711,702.4803,691.883,684.1875,676.537,668.3813,660.7426,653.7755,650.0816,648.0612,646.2773,644.7858,643.0106,641.5216,640.3767,637.4819,632.6983,628.9846,625.4398,621.9821,616.9946,612.0354,607.1956,602.2802,596.6755,592.2001,588.1333,584.558,581.6097,577.9584,573.0617,564.1294,551.742,538.2198,527.2874,518.8629,512.6631,508.3075,500.4244,486.8665,477.7011,473.3188,470.6129,466.2556,458.2609,447.3365,435.6923,424.2792,412.0086,404.6692,398.7538,393.9641,389.2486,384.7403,379.5526,373.4971,367.6658,364.7898,363.197,361.6139,358.8115,354.5367,351.0849,347.4326,342.9059,338.5647,335.1673,332.0125,328.6333,324.3728,319.4438,315.4648,312.397,309.8583,307.2173,304.731,301.624,297.7404,292.7379,285.6631,278.2705,271.6357,265.818,259.2794,250.8976,239.5252,225.2667,214.079,205.7642,196.1153,184.5527,175.8304,168.8757,163.5928,159.3608,155.2921,150.4569,146.6539,143.3789,138.2234,130.9921,125.6273,121.4822,118.1799,115.2755,112.5261,109.1559,106.1203,103.6653,102.4564,101.8389,100.9893,100.2931,100.0128,99.48527,98.44008,98.13039,98.03864,97.97441,97.69612,96.90996,95.97332,95.02913,94.26789,93.82011,93.20596,92.80177,92.20052,91.76311,90.96685,90.28648,90.08488,89.98811,89.73722,89.1814,88.79449,88.2354,88.03271,87.75043,87.22235,87.06588,86.94544,86.42828,85.16393,84.34487,84.10218,84.03027,84.15712,85.12063,87.40611,88.4907,88.51576,87.6343,86.4472,85.87325,85.25874,85.03963,84.75248,84.18592,83.72176,82.69534,81.46529,80.84157,79.9901,79.29336,79.08692,79.02576,79.00763,79.00226,79.00067,79.07427,79.54053,80.56757,80.90891,81.30634,82.38706,84.04061,85.41944,85.82798,85.83792,85.1742,83.64421,83.19087,83.09359,83.36106,84.3292,85.54198,86.19762,87.28078,88.52763,89.1193,89.73905,89.92268,89.97709,89.99321,89.99799,89.9994,89.99982,89.99995,89.99998,89.96296,89.72977,89.21622,89.02703,88.71172,87.84051,86.47126,84.73222,84.17992,83.79405,83.27231,83.33994,83.80443,83.94205,83.98283,83.99491,83.99849,83.96252,83.6926,82.94595,82.31732,82.35328,82.80838,82.94322,82.98318,82.99502,82.99852,82.99957,82.99987,82.99996,82.99998,83.03703,83.27023,83.78377,83.93593,83.98102,83.99438,84.03537,84.41788,85.97567,88.99279,91.88675,94.11459,95.07098,96.24326,97.59059,98.6935,100.6499,102.1926,104.1311,106.5203,109.3023,111.0896,112.9895,114.7376,116.3297,119.3569,126.5502,140.0889,154.9893,164.4783,171.2899,177.0118,182.4109,186.8625,191.4407,197.538,206.0483,215.3106,225.7957,233.7172,239.4718,245.0287,252.1936,259.0574,264.017,268.1902,274.3156,280.7602,286.5585,291.2025,294.1711,295.9025,298.4896,304.7747,314.1554,320.5645,326.7598,334.114,341.0708,346.1691,350.1241,354.5182,359.3387,363.4337,368.3507,373.4372,377.2776,380.0822,383.1725,387.1622,392.7147,397.508,401.0394,403.4561,407.4314,414.0537,421.2011,425.1707,427.8283,430.5417,434.2345,439.4028,449.823,467.429,479.9419,488.4272,493.8302,496.7645,497.7079,498.469,499.9537,502.2455,508.332,519.6539,530.6011,537.9188,545.3092,553.4249,562.348,571.5105,579.8179,587.7238,595.5477,604.2363,612.2181,616.8794,619.705,622.357,626.6983,635.392,647.8939,656.1537,661.1936,665.0203,668.6726,671.6066,673.9204,676.2726,679.0437,681.7166,683.916,685.975,687.9926,690.0348,692.3436,695.324,698.4663,700.5826,702.3207,705.169,709.4204,712.5319,714.8613,716.9218,718.7175,720.1755,721.7556,723.0016,724.593,727.1386,729.7447,731.8873,733.6702,734.9022,735.9709,736.9913,737.9974,738.9621,739.6183,739.3313,738.1351,737,737]]}"""
  val blobs = AppConfig.instance[Blobs]
  val schema = AppConfig.instance[Schema]
  val celebrityStore = AppConfig.instance[CelebrityStore]

  toDemoScenarios add DemoScenario(
  "Enroll All Celebrities",
  demoCategory,
  """
  Changes All Celebrities to be Enrolled status
  """, {
    () =>
      import org.squeryl.Query
      import org.squeryl.PrimitiveTypeMode._
      val celebrities: Query[(Celebrity)] = from(schema.celebrities)(
        (c) => select(c)
      )
      for (celebrity <- celebrities) {
        celebrity.copy(enrollmentStatusValue = EnrollmentStatus.Enrolled.value).save()
      }
  }
  )

  toDemoScenarios add DemoScenario(
  "Create Gabe Kapler",
  demoCategory,
  """
  Creates the following logins: gabe@egraphs.com, gabe1@egraphs.com, gabe2@egraphs.com, gabe3@egraphs.com. 
  Their URLs are, respectively: /Gabe-Kapler, /Gabe-Kapler-1, /Gabe-Kapler-2, /Gabe-Kapler-3
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Gabe", "Kapler1", "gabe1@egraphs.com", "gabekapler", publicName = "Gabe Kapler 1")
      createCelebrity("Gabe", "Kapler2", "gabe2@egraphs.com", "gabekapler", publicName = "Gabe Kapler 2")
      createCelebrity("Gabe", "Kapler3", "gabe3@egraphs.com", "gabekapler", publicName = "Gabe Kapler 3")
  }
  )

  toDemoScenarios add DemoScenario(
  "Create all celebrities",
  demoCategory,
  "", {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Clayton", "Kershaw", "ckershaw@egraphs.com", "claytonkershaw")
      createCelebrity("Don", "Mattingly", "dmattingly@egraphs.com", "donmattingly")
      createCelebrity("David", "Ortiz", "dortiz@egraphs.com", "davidortiz")
      createCelebrity("Dustin", "Pedroia", "dpedroia@egraphs.com", "dustinpedroia")
      createCelebrity("Hanley", "Ramirez", "hramirez@egraphs.com", "hanleyramirez")
      createCelebrity("Evan", "Longoria", "elongoria@egraphs.com", "evanlongoria")
      createCelebrity("Prince", "Fielder", "pfielder@egraphs.com", "princefielder")
      createCelebrity("Curtis", "Granderson", "cgranderson@egraphs.com", "curtisgranderson")
      createCelebrity("Nick", "Swisher", "nswisher@egraphs.com", "nickswisher")
      createCelebrity("Barry", "Bonds", "bbonds@egraphs.com", "barrybonds")
      createCelebrity("Ryan", "Braun", "rbraun@egraphs.com", "ryanbraun")
      createCelebrity("Zach", "Apter", "zach@egraphs.com", "zachapter")
      createCelebrity("Bono", "Braun", "bono@egraphs.com", "paulhewson", publicName = "Bono")

      createCelebrity("Gabe", "Kapler1", "gabe1@egraphs.com", "gabekapler", publicName = "Gabe Kapler 1")
      createCelebrity("Gabe", "Kapler2", "gabe2@egraphs.com", "gabekapler", publicName = "Gabe Kapler 2")
      createCelebrity("Gabe", "Kapler3", "gabe3@egraphs.com", "gabekapler", publicName = "Gabe Kapler 3")
  }
  )

  toDemoScenarios add DemoScenario(
  "Generate all signatures",
  demoCategory,
  "Finds all products currently in database and generates sample Egraphs to tmp/files. Run this after generating celebrities.", {
    () =>
      import org.squeryl.PrimitiveTypeMode._
      val imageUtil = AppConfig.instance[ImageUtil]
      val sig = imageUtil.createSignatureImage(boxSignatureStr, None)

      val q = from(schema.products)(prod => select(prod))

      q.foreach {
        product =>
          val fileName = product.celebrity.urlSlug.get + "_" + product.urlSlug + ".png"
          println("Writing " + fileName)
          val image = imageUtil.createEgraphImage(sig, product.photoImage, 0, 0)
          ImageIO.write(image, "PNG", Play.getFile("tmp/files/" + fileName))
      }

      println("Wrote all sample egraphs")
  }
  )

  toDemoScenarios add DemoScenario(
  "Zach Apter",
  demoCategory,
  "Prepares data for Zach's ad-hoc meetings. Creates: Zach Apter (zach@egraphs.com/derp)", {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Zach", "Apter", "zach@egraphs.com", "zachapter")
  }
  )

  toDemoScenarios add DemoScenario(
  "Bono",
  demoCategory,
  "Prepares data for ad-hoc meetings that necessitate non-sports stars. Creates: Bono (bono@egraphs.com/derp)", {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Bono", "Braun", "bono@egraphs.com", "paulhewson", publicName = "Bono")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 16 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Hendricks Sports Management meeting. Creates:
  Clayton Kershaw (ckershaw@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Clayton", "Kershaw", "ckershaw@egraphs.com", "claytonkershaw")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 17 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Ray Schulte meeting. Creates:
  Don Mattingly (dmattingly@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Don", "Mattingly", "dmattingly@egraphs.com", "donmattingly")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 18 2012 agent meetings",
  demoCategory,
  """
  Prepares data for SFX meeting. Creates:
  David Ortiz (dortiz@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("David", "Ortiz", "dortiz@egraphs.com", "davidortiz")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 19 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Aces, Sam and Seth Levinson meeting. Creates:
  Dustin Pedroia (dpedroia@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Dustin", "Pedroia", "dpedroia@egraphs.com", "dustinpedroia")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 20 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Wasserman Media Group meeting. Creates:
  Hanley Ramirez (hramirez@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Hanley", "Ramirez", "hramirez@egraphs.com", "hanleyramirez")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 24 2012 agent meetings",
  demoCategory,
  """
  Prepares data for TWC Sports Management and Boras Corporation meetings. Creates:
  Evan Longoria (elongoria@egraphs.com/derp),
  Prince Fielder (pfielder@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Evan", "Longoria", "elongoria@egraphs.com", "evanlongoria")
      createCelebrity("Prince", "Fielder", "pfielder@egraphs.com", "princefielder")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 25 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Matt Brown meeting. Creates:
  Curtis Granderson (cgranderson@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Curtis", "Granderson", "cgranderson@egraphs.com", "curtisgranderson")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 26 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Dan Lozano meeting. Creates:
  Nick Swisher (nswisher@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Nick", "Swisher", "nswisher@egraphs.com", "nickswisher")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 27 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Beverly Hills Sports Council and CAA meetings. Creates:
  Barry Bonds (bbonds@egraphs.com/derp),
  Ryan Braun (rbraun@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Barry", "Bonds", "bbonds@egraphs.com", "barrybonds")
      createCelebrity("Ryan", "Braun", "rbraun@egraphs.com", "ryanbraun")
  }
  )

  toDemoScenarios add DemoScenario(
  "Feb 10 2012 agent meetings",
  demoCategory,
  """
  Creates: Ne-Yo (neyo@egraphs.com/derp)
  """, {
    () =>
      DemoScenario.clearAll()
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
      createCelebrity("Ne-Yo", "", "neyo@egraphs.com", "neyo", "Ne-Yo")
  }
  )

  private def createCelebrity(firstName: String, lastName: String, email: String, s3ResourceId: String, publicName: String = null) {
    println("Creating Celebrity " + email + " ...")

    val profile = "demo/" + s3ResourceId + "/" + s3ResourceId + "-profile.jpg"
    val productA = "demo/" + s3ResourceId + "/" + s3ResourceId + "-product-a.jpg"
    val productB = "demo/" + s3ResourceId + "/" + s3ResourceId + "-product-b.jpg"
    val realPublicName = if (publicName == null) firstName + " " + lastName else publicName

    val celebrity = Celebrity(
      firstName = Some(firstName),
      lastName = Some(lastName),
      publicName = Some(realPublicName),
      description = Some(
        "Love all my fans out there from Seattle to Swaziland." +
          " Your support makes the game worth playing."
      ),
      enrollmentStatusValue = EnrollmentStatus.Enrolled.value
    ).save()

    blobs.getStaticResource(profile) foreach {
      profilePhotoBlob =>
        celebrity.saveWithProfilePhoto(profilePhotoBlob.asByteArray)
    }

    Account(email = email,
      celebrityId = Some(celebrity.id)
    ).withPassword("derp").right.get.save()

    blobs.getStaticResource(productA) foreach {
      productAPhotoBlob =>
        celebrity.newProduct.copy(
          priceInCurrency = 50,
          name = firstName + "'s Product A",
          description = "Buy my Egraph A!",
          storyTitle = "The Story",
          storyText = "{signer_link}{signer_name}{end_link} was born on top. He proved it to the world every single day throughout the 2011 season. A few days afterwards he got a note from {recipient_name} on his iPad. This was his response."
        ).save().withPhoto(productAPhotoBlob.asByteArray).save()
    }

    blobs.getStaticResource(productB) foreach {
      productBPhotoBlob =>
        celebrity.newProduct.copy(
          priceInCurrency = 100,
          name = firstName + "'s Product B",
          description = "Buy my Egraph B!",
          storyTitle = "The Story",
          storyText = "{signer_link}{signer_name}{end_link} was born on top. He proved it to the world every single day throughout the 2011 season. A few days afterwards he got a note from {recipient_name} on his iPad. This was his response."
        ).save().withPhoto(productBPhotoBlob.asByteArray).save()
    }
  }

  //  toDemoScenarios add DemoScenario(
  //  "Testing",
  //  demoCategory,
  //  """
  //  Testing
  //  """, {
  //    () =>
  //      testBlobs("davidortiz")
  //      testBlobs("dustinpedroia")
  //      testBlobs("hanleyramirez")
  //      testBlobs("evanlongoria")
  //      testBlobs("princefielder")
  //      testBlobs("curtisgranderson")
  //      testBlobs("nickswisher")
  //      testBlobs("barrybonds")
  //      testBlobs("ryanbraun")
  //      testBlobs("gabekapler")
  //      testBlobs("davidprice")
  //
  //  }
  //  )
  //
  //  private def testBlobs(s3ResourceId: String) {
  //    println(s3ResourceId + "...")
  //    val profile = "demo/" + s3ResourceId + "/" + s3ResourceId + "-profile.jpg"
  //    val productA = "demo/" + s3ResourceId + "/" + s3ResourceId + "-product-a.jpg"
  //    val productB = "demo/" + s3ResourceId + "/" + s3ResourceId + "-product-b.jpg"
  //    println("profile: " + blobs.getStaticResource(profile))
  //    println("product-a: " + blobs.getStaticResource(productA))
  //    println("product-b: " + blobs.getStaticResource(productB))
  //    println()
  //  }

}