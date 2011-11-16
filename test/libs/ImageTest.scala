package libs

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import java.io._
import xml.Elem
import util.parsing.json.JSON
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.Graphics
import libs.ImageUtil._

class ImageTest extends UnitFlatSpec
with ShouldMatchers {

  val rawCapture = "{\n   \"originalX\": [[67.000000,95.148125,121.414230,151.011597,184.484879,220.291779,255.567886,286.501556,309.481903,323.661255,330.677368,329.015503,314.300873,286.385437,250.558655,217.980347,193.512695,176.263016,165.300140,159.014847,157.115494,160.330505,169.060867,178.425415,184.570480,187.983826,189.809998,191.832565,195.283707,199.417374,204.345871,209.509872,213.854752,217.919907,221.642914,224.634918,227.225143,230.400024,234.414795,239.085846,243.766159,248.264023,253.522644,260.599274,271.696045,288.132111,308.927979,333.386017,359.410614,382.158661,396.454376,402.097565,400.621460,388.776703,364.600494,333.177917,305.941589,287.241943,278.034637,279.602814,290.141541,301.486359,309.847778,315.732635,319.328156,321.800903,323.903931,325.749268,327.481232,329.846252,331.954407,333.949432,335.725739,337.252045,339.370941,342.000000,345.000000],[504.000000,499.518463,491.116547,477.701172,463.022522,451.228851,443.919617,440.198364,438.910583,440.825317,450.096344,469.917389,498.345825,528.880188,553.557007,569.461304,578.432922,581.054138,574.423401,557.310608,534.647583,513.747375,500.665863,495.308350,499.128357,515.371277,534.443298,545.205383,545.208984,532.876709,509.148621,479.599579,451.696136,431.650665,419.896454,415.000000,414.000000],[715.000000,720.444397,727.279724,735.749451,741.888672,740.707703,730.320740,711.687561,690.425903,671.866882,655.812378,644.944336,643.353821,653.697388,675.243591,698.442444,714.353271,718.178711,709.349182,688.547852,662.088196,635.026062,612.007690,598.002258,594.370972,601.850586,622.474182,646.177490,666.533997,679.787781,687.196289,691.724731,695.622070,698.924988,702.311035,706.277283,710.193176,712.723816,714.695862,716.465393,718.841553,720.990051,723.330322,726.468201,730.879395,737.445679,746.613464,757.737244,772.255371,789.520020,808.820679,826.020874,836.932007,842.127930,843.741516,842.441833,836.908630,824.898804,807.192200,786.686523,766.758911,750.298889,737.014404,727.893066,722.523804,720.266235,721.375122,727.851807,740.919006,761.049988,784.681396,809.646240,834.487671,854.181458,864.720337,869.324463,870.096069,865.102478,850.734009,829.439636,803.019104,779.301880,764.941223,759.908447,763.083923,780.432190,813.905762,857.675659,906.000000,950.000000]],\n   \"originalY\": [[198.000000,208.518494,226.561005,252.721741,287.361938,326.070160,365.131836,398.816772,423.538239,439.381653,447.187103,443.536896,420.714630,382.285797,336.158752,294.639618,263.300629,242.755737,231.335022,226.617767,228.664505,239.937607,256.833344,271.209839,278.395477,281.191223,282.389954,282.152557,280.267395,277.708832,276.506287,276.149994,275.970337,275.398590,273.784729,271.676941,269.200562,267.133484,265.632111,265.261353,265.670013,267.272583,269.784454,274.343506,282.435089,296.202942,317.578613,348.134338,384.076782,414.948608,433.466217,441.323273,439.651306,423.155914,390.194336,349.909424,316.084259,293.839752,281.174713,281.236938,290.070160,296.798523,301.273621,304.155121,305.342224,304.916199,302.641815,299.708649,296.765503,294.967529,293.286652,292.010834,290.447632,288.095581,285.769043,283.000000,281.000000],[393.000000,385.222198,367.991730,344.664215,322.307892,305.535645,295.640167,290.745209,289.961517,296.877441,319.519196,354.264893,393.930267,426.608917,446.328522,455.393585,457.005463,449.964539,430.211700,404.729370,380.327209,361.578400,349.986176,344.144012,342.190796,341.760193,339.447449,332.132568,320.928162,307.571289,297.465546,290.471252,286.658112,286.231995,289.513153,299.000000,301.000000],[356.000000,369.074036,384.244110,405.961151,432.099548,457.399811,479.007294,495.446564,504.317444,505.242157,497.034668,478.454681,456.171722,434.347137,413.028748,391.489990,369.256287,346.964813,325.026581,310.081940,303.135376,302.262299,305.818420,312.057281,317.424347,319.421997,317.754639,312.408752,306.454407,301.542023,298.086487,296.729309,297.141998,299.634644,303.188019,306.981598,310.364899,312.478455,313.549133,313.866394,313.960388,313.988220,314.033508,314.528412,317.008392,323.335785,332.877228,347.185791,368.314270,396.278259,428.934235,459.128601,481.297302,496.273224,505.858673,510.735870,508.921692,494.384186,464.150848,426.526154,390.118835,360.701874,339.615356,327.626740,322.778259,323.675018,329.385162,337.595581,347.880127,362.668152,383.753479,412.260223,444.558533,471.239502,486.811646,494.721924,497.510162,492.743713,474.849976,444.733307,406.809845,371.239929,345.034027,328.454498,318.653168,313.971283,312.361847,310.144226,306.000000,295.000000]],\n   \"time\": [[13331445844472,13331448640856,13331448883353,13331449284887,13331449651436,13331450040379,13331450424036,13331450807652,13331451196301,13331451571027,13331452335120,13331452715422,13331453111580,13331453485879,13331453868402,13331454261722,13331454635498,13331455017782,13331455412529,13331455785207,13331456166898,13331456560894,13331456934011,13331457322500,13331457715549,13331458088385,13331458473840,13331458866709,13331459240098,13331459624481,13331460018091,13331460393174,13331460799934,13331461222643,13331461589850,13331461980530,13331462398425,13331462784410,13331463194394,13331463581897,13331463992719,13331464379978,13331464795079,13331465178617,13331465586223,13331465970468,13331466378919,13331466787303,13331467197008,13331467589763,13331467976468,13331468388426,13331468775448,13331469185619,13331469577402,13331469986701,13331470400979,13331470789692,13331471175473,13331471589816,13331471976246,13331472385216,13331472793954,13331473176490,13331473595481,13331473985074,13331474395116,13331474780376,13331475193705,13331475583284,13331475997763,13331476384461,13331476797005,13331477181443,13331477595114,13331477162456,13331477576030],[13331482295981,13331484203587,13331484579732,13331484963259,13331485355108,13331485732594,13331486113970,13331486505963,13331486879408,13331487268582,13331487656101,13331488030463,13331488418728,13331488808915,13331489181762,13331489569535,13331489962249,13331490333965,13331490718466,13331491111336,13331491487520,13331491869581,13331492262880,13331492636444,13331493022759,13331493414481,13331493788265,13331494173448,13331494565704,13331494939233,13331495325081,13331495717795,13331496091326,13331496476315,13331496860931,13331496457829,13331496842532],[13331501508864,13331503005778,13331503391273,13331503781078,13331504154058,13331504537167,13331504930789,13331505307305,13331505689585,13331506082593,13331506457650,13331506842396,13331507233159,13331507608029,13331507993670,13331508385253,13331508761544,13331509151287,13331509544649,13331509910928,13331510297876,13331510688413,13331511062217,13331511445285,13331511838884,13331512212854,13331512599228,13331512991101,13331513364939,13331513748637,13331514142884,13331514516503,13331514902674,13331515296364,13331515670125,13331516056117,13331516446191,13331516821255,13331517203934,13331517601259,13331517989312,13331518380935,13331518773254,13331519184979,13331519593489,13331519978441,13331520388540,13331520773214,13331521180780,13331521593210,13331521982810,13331522400256,13331522785404,13331523195103,13331523580331,13331523992350,13331524373403,13331524784795,13331525172614,13331525580483,13331525992165,13331526380301,13331526790955,13331527180956,13331527596467,13331527985232,13331528395381,13331528783781,13331529184834,13331529602523,13331529989912,13331530386766,13331530783745,13331531198337,13331531583452,13331531997620,13331532379570,13331532793515,13331533177279,13331533585605,13331533994863,13331534376565,13331534790641,13331535175676,13331535587459,13331535997283,13331536379323,13331536792627,13331536360185,13331536773828]]\n}"

  val height = 768
  val width = 1024

  it should "parse raw capture JSON" in {
    val json: Option[Any] = JSON.parseFull(rawCapture)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    val originalXsByStroke = map.get("originalX").get.asInstanceOf[List[Any]]
    val originalYsByStroke = map.get("originalY").get.asInstanceOf[List[Any]]
    val timesByStroke = map.get("time").get.asInstanceOf[List[Any]]
    val numStroke = originalXsByStroke.size

    for (i <- 0 until numStroke) {
      originalXsByStroke(i).isInstanceOf[List[Number]] should be(true)
      originalYsByStroke(i).isInstanceOf[List[Number]] should be(true)
      timesByStroke(i).isInstanceOf[List[Number]] should be(true)
    }
  }

  it should "calculate SVG curves through captured points" in {
    val json: Option[Any] = JSON.parseFull(rawCapture)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    val originalXsByStroke = map.get("originalX").get.asInstanceOf[List[List[Double]]]
    val originalYsByStroke = map.get("originalY").get.asInstanceOf[List[List[Double]]]

    var paths: List[String] = Nil
    for (i <- 0 until originalXsByStroke.size) {
      val xs = originalXsByStroke(i).toArray
      val ys = originalYsByStroke(i).toArray
      paths = createSmoothedPathThruCapturedPoints(xs, ys) :: paths
    }

    val pathNodes: List[Elem] = for (path <- paths) yield createPathNode(path, defaultStyle)
    val svg: scala.xml.Elem = createSVG(pathNodes)
    val targetFilename = "test/files/test_thru_captured_points.png"
    saveAsPNG(svg.mkString, targetFilename)
  }

  private def createSmoothedPathThruCapturedPoints(xs: Array[Double], ys: Array[Double]): String = {
    val n = xs.size

    val path = new StringBuilder()

    appendM(path, xs(0), ys(0))

    for (i <- 0 until n - 3) {
      val bezier = BezierCubic(
        xs(i), ys(i),
        xs(i + 1), ys(i + 1),
        xs(i + 2), ys(i + 2),
        xs(i + 3), ys(i + 3))
      val tNearC0: Double = bezier.calculateTClosestToC0()

      val _1_thirds_tNearC0: Double = tNearC0 * 1d / 3d
      val _2_thirds_tNearC0: Double = tNearC0 * 2d / 3d
      val c0x = bezier.calcXCoord(_1_thirds_tNearC0)
      val c0y = bezier.calcYCoord(_1_thirds_tNearC0)
      val c1x = bezier.calcXCoord(_2_thirds_tNearC0)
      val c1y = bezier.calcYCoord(_2_thirds_tNearC0)

      // reflect c0x,c0y and c1x,c1y through P0-C0 using this Linear Algebra equation:
      // http://en.wikipedia.org/wiki/Reflection_(mathematics)#Reflection_across_a_line_in_the_plane
      val c0_reflected = reflectPointAcrossLine(c0x, c0y, xs(i), ys(i), xs(i + 1), ys(i + 1))
      val c1_reflected = reflectPointAcrossLine(c1x, c1y, xs(i), ys(i), xs(i + 1), ys(i + 1))
      val c0x_reflected = c0_reflected._1
      val c0y_reflected = c0_reflected._2
      val c1x_reflected = c1_reflected._1
      val c1y_reflected = c1_reflected._2

      appendC(path,
        c0x_reflected, c0y_reflected,
        c1x_reflected, c1y_reflected,
        xs(i + 1), ys(i + 1))
    }

    path.mkString
  }

  it should "calculate SVG curves smoothed C" in {
    val json: Option[Any] = JSON.parseFull(rawCapture)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    val originalXsByStroke = map.get("originalX").get.asInstanceOf[List[List[Double]]]
    val originalYsByStroke = map.get("originalY").get.asInstanceOf[List[List[Double]]]

    var paths: List[String] = Nil
    for (i <- 0 until originalXsByStroke.size) {
      val xs = originalXsByStroke(i).toArray
      val ys = originalYsByStroke(i).toArray
      paths = createSmoothedPathCStr(xs, ys) :: paths
    }

    val pathNodes: List[Elem] = for (path <- paths) yield createPathNode(path, defaultStyle)
    val svg: scala.xml.Elem = createSVG(pathNodes)
    val targetFilename = "test/files/test_smoothedC.png"
    saveAsPNG(svg.mkString, targetFilename)
    // verify something
  }

  private def createSmoothedPathCStr(xs: Array[Double], ys: Array[Double]): String = {
    val n = xs.size
    val smoothedXs = xs.clone()
    val smoothedYs = ys.clone()

    val path = new StringBuilder()

    appendM(path, xs(0), ys(0))

    for (i <- 0 until n - 3) {
      val bezier = BezierCubic(
        smoothedXs(i), smoothedYs(i),
        xs(i + 1), ys(i + 1),
        xs(i + 2), ys(i + 2),
        xs(i + 3), ys(i + 3))
      val tNearC0: Double = bezier.calculateTClosestToC0()
      smoothedXs(i + 1) = bezier.calcXCoord(tNearC0)
      smoothedYs(i + 1) = bezier.calcYCoord(tNearC0)

      val _1_thirds_tNearC0: Double = tNearC0 * 1d / 3d
      val _2_thirds_tNearC0: Double = tNearC0 * 2d / 3d
      appendC(path,
        bezier.calcXCoord(_1_thirds_tNearC0), bezier.calcYCoord(_1_thirds_tNearC0),
        bezier.calcXCoord(_2_thirds_tNearC0), bezier.calcYCoord(_2_thirds_tNearC0),
        smoothedXs(i + 1), smoothedYs(i + 1))
    }

    path.mkString
  }

  //  it should "calculate SVG curves smoothed S" in {
  //    val json: Option[Any] = JSON.parseFull(rawCapture)
  //    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
  //    val originalXsByStroke = map.get("originalX").get.asInstanceOf[List[List[Double]]]
  //    val originalYsByStroke = map.get("originalY").get.asInstanceOf[List[List[Double]]]
  //
  //    var paths: List[String] = Nil
  //    for (i <- 0 until originalXsByStroke.size) {
  //      val xs = originalXsByStroke(i).toArray
  //      val ys = originalYsByStroke(i).toArray
  //      paths = createSmoothedPathSStr(xs, ys) :: paths
  //    }
  //
  //    val pathNodes: List[Elem] = for (path <- paths) yield createPathNode(path, defaultStyle)
  //    val svg: scala.xml.Elem = createSVG(pathNodes)
  //    val targetFilename = "test/files/test_smoothedS.png"
  //    saveAsPNG(svg.mkString, targetFilename)
  //    // verify something
  //  }
  //
  //  private def createSmoothedPathSStr(xs: Array[Double], ys: Array[Double]): String = {
  //    val n = xs.size
  //    val smoothedXs = xs.clone()
  //    val smoothedYs = ys.clone()
  //
  //    val path = new StringBuilder()
  //
  //    appendM(path, xs(0), ys(0))
  //
  //    for (i <- 0 until n - 3) {
  //      val bezier = BezierCubic(
  //        smoothedXs(i), smoothedYs(i),
  //        xs(i + 1), ys(i + 1),
  //        xs(i + 2), ys(i + 2),
  //        xs(i + 3), ys(i + 3))
  //      val tNearC0: Double = bezier.calculateTClosestToC0()
  //      smoothedXs(i + 1) = bezier.calcXCoord(tNearC0)
  //      smoothedYs(i + 1) = bezier.calcYCoord(tNearC0)
  //
  //      val _1_half_tNearC0: Double = tNearC0 * 1d / 2d
  //      appendS(path,
  //        bezier.calcXCoord(_1_half_tNearC0), bezier.calcYCoord(_1_half_tNearC0),
  //        smoothedXs(i + 1), smoothedYs(i + 1))
  //    }
  //
  //    path.mkString
  //  }
  //
  //  private def appendS(path: StringBuilder, cx: Double, cy: Double, x: Double, y: Double) {
  //    path.append("S " + cx + "," + (height - cy) + " " + x + "," + (height - y) + " ")
  //  }

  it should "calculate SVG curves with asymptotes" in {
    val json: Option[Any] = JSON.parseFull(rawCapture)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    val originalXsByStroke = map.get("originalX").get.asInstanceOf[List[List[Double]]]
    val originalYsByStroke = map.get("originalY").get.asInstanceOf[List[List[Double]]]

    var paths: List[String] = Nil
    for (i <- 0 until originalXsByStroke.size) {
      val xs = originalXsByStroke(i).toArray
      val ys = originalYsByStroke(i).toArray
      paths = createPathStr(xs, ys) :: paths
    }

    val pathNodes: List[Elem] = for (path <- paths) yield createPathNode(path, defaultStyle)
    val svg: scala.xml.Elem = createSVG(pathNodes)
    val targetFilename = "test/files/test_with_asymptotes.png"
    saveAsPNG(svg.mkString, targetFilename)
    // verify something
  }

  private def createPathStr(xs: Array[Double], ys: Array[Double]): String = {
    val n = xs.size
    val z = n / 4
    val path = new StringBuilder()
    appendM(path, xs(0), ys(0))
    for (i <- 0 until z) {
      appendC(path, xs(4 * i + 1), ys(4 * i + 1), xs(4 * i + 2), ys(4 * i + 2), xs(4 * i + 3), ys(4 * i + 3))
    }

    path.mkString
  }

  it should "generate PNG from SVG" in {
    val d0 = "M 24.776081,42.494911 C 11.290676,41.066828 0.95766702,36.020161 1.1577608,26.86514 C 1.622673,11.592479 16.772821,3.6020224 28.133588,3.2468193 C 33.574985,3.0932043 37.935855,5.6331159 38.321883,9.0356234 C 39.192007,16.705029 19.103053,24.665394 19.103053,24.665394 C 19.103053,24.665394 36.961514,18.095102 43.763359,26.86514 C 46.547914,30.455439 31.954198,37.516539 31.954198,37.516539 C 31.954198,37.516539 23.167218,40.872528 24.312977,37.284987 C 26.078562,31.756679 41.10299,27.183575 42.721374,26.980916 C 45.890571,26.584059 49.816762,26.572526 50.709924,27.096692 C 52.046532,27.881102 49.031171,31.235688 49.783715,31.959288 C 50.536259,32.682889 53.604326,29.412214 53.720102,29.991094 C 53.835878,30.569975 53.083333,32.625 54.298982,33.001272 C 55.514631,33.377544 56.614504,31.901399 58.582697,31.496183 C 60.550891,31.090967 60.406171,31.062023 62.171756,31.380407 C 63.937341,31.698791 64.284669,32.017176 65.645038,32.76972 C 67.005407,33.522264 66.513359,34.99841 67.613232,34.390585 C 68.713105,33.782761 69.031488,30.222646 70.044529,30.338422 C 71.057569,30.454198 70.073473,35.72201 71.665394,34.85369 C 73.257315,33.98537 76.412214,26.86514 76.412214,26.86514 C 76.412214,26.86514 75.584515,30.909972 76.412214,30.801527 C 79.540974,30.391596 84.51654,28.225508 90.884224,27.44402 C 97.251908,26.662532 99.548367,27.087063 101.88295,27.675573 C 103.80326,28.159651 104.5458,29.991094 104.5458,29.991094"
    val d1 = "M 113.69212,15.750636 C 113.69212,15.750636 112.59223,12.97201 110.10305,13.666667 C 104.27298,15.293664 93.885182,21.347458 93.66285,29.296437 C 93.203965,45.58701 114.74859,40.436462 116.35496,40.295165 C 129.95442,39.098952 133.73907,30.339871 133.72137,25.823155 C 133.66484,11.515581 114.64727,15.519084 112.65013,15.750636 C 104.75019,16.666573 102.17069,17.323546 102.17069,17.323546"
    val d2 = "M 119.71247,1.0470738 C 117.17369,14.962417 114.58615,28.880628 117.16539,42.494911"
    val d3 = "M 133.6056,27.791349 C 126.98268,27.381298 121.0134,28.169601 116.23919,31.148855"
    val d4 = "M 131.52163,32.76972 C 131.52163,32.76972 135.46693,32.636988 139.85751,31.843511 C 142.25986,31.409351 144.86483,30.106871 147.73028,29.991095 C 150.59574,29.875319 149.46692,31.438295 151.31934,31.380407 C 155.95138,31.235655 159.30788,31.090967 166.25445,30.685751 C 173.20101,30.280535 176.00739,27.921622 179.1056,29.759542 C 180.8133,30.772583 180.49491,32.885496 180.49491,32.885496"
    val d5 = "M 153.51909,31.033079 C 153.51909,31.033079 155.78963,28.285299 152.82443,27.675572 C 149.45556,26.982839 149.11959,30.10687 149.11959,30.10687"
    val pathNodes: List[Elem] = List(createPathNode(d0, defaultStyle), createPathNode(d1, defaultStyle), createPathNode(d2, defaultStyle),
      createPathNode(d3, defaultStyle), createPathNode(d4, defaultStyle), createPathNode(d5, defaultStyle))

    val svg: scala.xml.Elem = createSVG(pathNodes)
    val targetFilename = "test/files/from_svg.png"
    saveAsPNG(svg.mkString, targetFilename)
    new File(targetFilename).length should be(6566)
  }

  it should "overlay PNG images correctly" in {
    val path: File = new File("test/files")

    // load source images
    val image: BufferedImage = ImageIO.read(new File(path, "image.png"))
    val overlay: BufferedImage = ImageIO.read(new File(path, "overlay.png"))

    // create the new image, canvas size is the max. of both image sizes
    val w: Int = scala.math.max(image.getWidth, overlay.getWidth)
    val h: Int = scala.math.max(image.getHeight, overlay.getHeight)
    val combinedImage: BufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

    // paint both images, preserving the alpha channels
    val g: Graphics = combinedImage.getGraphics
    g.drawImage(image, 0, 0, null)
    g.drawImage(overlay, 0, 0, null)

    // Save as new image
    val combined: File = new File(path, "combinedImage.png")
    ImageIO.write(combinedImage, "PNG", combined)
    combined.length should be(149657)
  }

  def reflectPointAcrossLine(x: Double, y: Double, p0x: Double, p0y: Double, p1x: Double, p1y: Double): (Double, Double) = {
    val vx = x - p0x
    val vy = y - p0y
    val lx = p1x - p0x
    val ly = p1y - p0y

    val v_dot_l = vx * lx + vy * ly
    val l_dot_l = lx * lx + ly * ly

    val reflectedX = 2d * (v_dot_l / l_dot_l) * lx - (x - p0x) + p0x
    val reflectedY = 2d * (v_dot_l / l_dot_l) * ly - (y - p0y) + p0y

    (reflectedX, reflectedY)
  }

  it should "reflect point across line in plane" in {
    val pointReflected = reflectPointAcrossLine(1.25, 0d, 0d, 0d, 2d, 1d)
    pointReflected._1 should be(.75)
    pointReflected._2 should be(1d)

    val pointReflectedReversed = reflectPointAcrossLine(.75, 1d, 0d, 0d, 2d, 1d)
    pointReflectedReversed._1 should be(1.25)
    pointReflectedReversed._2 should be(0d)

    val pointReflectedWithOffset = reflectPointAcrossLine(2.25, 1d, 1d, 1d, 3d, 2d)
    pointReflectedWithOffset._1 should be(1.75)
    pointReflectedWithOffset._2 should be(2d)

    val pointReflectedWithOffsetReversed = reflectPointAcrossLine(1.75, 2d, 1d, 1d, 3d, 2d)
    pointReflectedWithOffsetReversed._1 should be(2.25)
    pointReflectedWithOffsetReversed._2 should be(1d)
  }


  private def appendM(path: StringBuilder, x: Double, y: Double) {
    path.append("M " + x + "," + (height - y) + " ")
  }

  private def appendC(path: StringBuilder, c0x: Double, c0y: Double, c1x: Double, c1y: Double, x: Double, y: Double) {
    path.append("C " + c0x + "," + (height - c0y) + " " + c1x + "," + (height - c1y) + " " + x + "," + (height - y) + " ")
  }
}
