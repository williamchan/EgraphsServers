package services

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import java.io._
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import play.Play
import utils.TestConstants

class ImageUtilTests extends UnitFlatSpec
with ShouldMatchers {
  def imageUtil = AppConfig.instance[ImageUtil]

  val rawCapture = "{\n   \"x\": [[67.000000,95.148125,121.414230,151.011597,184.484879,220.291779,255.567886,286.501556,309.481903,323.661255,330.677368,329.015503,314.300873,286.385437,250.558655,217.980347,193.512695,176.263016,165.300140,159.014847,157.115494,160.330505,169.060867,178.425415,184.570480,187.983826,189.809998,191.832565,195.283707,199.417374,204.345871,209.509872,213.854752,217.919907,221.642914,224.634918,227.225143,230.400024,234.414795,239.085846,243.766159,248.264023,253.522644,260.599274,271.696045,288.132111,308.927979,333.386017,359.410614,382.158661,396.454376,402.097565,400.621460,388.776703,364.600494,333.177917,305.941589,287.241943,278.034637,279.602814,290.141541,301.486359,309.847778,315.732635,319.328156,321.800903,323.903931,325.749268,327.481232,329.846252,331.954407,333.949432,335.725739,337.252045,339.370941,342.000000,345.000000],[504.000000,499.518463,491.116547,477.701172,463.022522,451.228851,443.919617,440.198364,438.910583,440.825317,450.096344,469.917389,498.345825,528.880188,553.557007,569.461304,578.432922,581.054138,574.423401,557.310608,534.647583,513.747375,500.665863,495.308350,499.128357,515.371277,534.443298,545.205383,545.208984,532.876709,509.148621,479.599579,451.696136,431.650665,419.896454,415.000000,414.000000],[715.000000,720.444397,727.279724,735.749451,741.888672,740.707703,730.320740,711.687561,690.425903,671.866882,655.812378,644.944336,643.353821,653.697388,675.243591,698.442444,714.353271,718.178711,709.349182,688.547852,662.088196,635.026062,612.007690,598.002258,594.370972,601.850586,622.474182,646.177490,666.533997,679.787781,687.196289,691.724731,695.622070,698.924988,702.311035,706.277283,710.193176,712.723816,714.695862,716.465393,718.841553,720.990051,723.330322,726.468201,730.879395,737.445679,746.613464,757.737244,772.255371,789.520020,808.820679,826.020874,836.932007,842.127930,843.741516,842.441833,836.908630,824.898804,807.192200,786.686523,766.758911,750.298889,737.014404,727.893066,722.523804,720.266235,721.375122,727.851807,740.919006,761.049988,784.681396,809.646240,834.487671,854.181458,864.720337,869.324463,870.096069,865.102478,850.734009,829.439636,803.019104,779.301880,764.941223,759.908447,763.083923,780.432190,813.905762,857.675659,906.000000,950.000000]],\n   \"y\": [[198.000000,208.518494,226.561005,252.721741,287.361938,326.070160,365.131836,398.816772,423.538239,439.381653,447.187103,443.536896,420.714630,382.285797,336.158752,294.639618,263.300629,242.755737,231.335022,226.617767,228.664505,239.937607,256.833344,271.209839,278.395477,281.191223,282.389954,282.152557,280.267395,277.708832,276.506287,276.149994,275.970337,275.398590,273.784729,271.676941,269.200562,267.133484,265.632111,265.261353,265.670013,267.272583,269.784454,274.343506,282.435089,296.202942,317.578613,348.134338,384.076782,414.948608,433.466217,441.323273,439.651306,423.155914,390.194336,349.909424,316.084259,293.839752,281.174713,281.236938,290.070160,296.798523,301.273621,304.155121,305.342224,304.916199,302.641815,299.708649,296.765503,294.967529,293.286652,292.010834,290.447632,288.095581,285.769043,283.000000,281.000000],[393.000000,385.222198,367.991730,344.664215,322.307892,305.535645,295.640167,290.745209,289.961517,296.877441,319.519196,354.264893,393.930267,426.608917,446.328522,455.393585,457.005463,449.964539,430.211700,404.729370,380.327209,361.578400,349.986176,344.144012,342.190796,341.760193,339.447449,332.132568,320.928162,307.571289,297.465546,290.471252,286.658112,286.231995,289.513153,299.000000,301.000000],[356.000000,369.074036,384.244110,405.961151,432.099548,457.399811,479.007294,495.446564,504.317444,505.242157,497.034668,478.454681,456.171722,434.347137,413.028748,391.489990,369.256287,346.964813,325.026581,310.081940,303.135376,302.262299,305.818420,312.057281,317.424347,319.421997,317.754639,312.408752,306.454407,301.542023,298.086487,296.729309,297.141998,299.634644,303.188019,306.981598,310.364899,312.478455,313.549133,313.866394,313.960388,313.988220,314.033508,314.528412,317.008392,323.335785,332.877228,347.185791,368.314270,396.278259,428.934235,459.128601,481.297302,496.273224,505.858673,510.735870,508.921692,494.384186,464.150848,426.526154,390.118835,360.701874,339.615356,327.626740,322.778259,323.675018,329.385162,337.595581,347.880127,362.668152,383.753479,412.260223,444.558533,471.239502,486.811646,494.721924,497.510162,492.743713,474.849976,444.733307,406.809845,371.239929,345.034027,328.454498,318.653168,313.971283,312.361847,310.144226,306.000000,295.000000]],\n   \"t\": [[13331445844472,13331448640856,13331448883353,13331449284887,13331449651436,13331450040379,13331450424036,13331450807652,13331451196301,13331451571027,13331452335120,13331452715422,13331453111580,13331453485879,13331453868402,13331454261722,13331454635498,13331455017782,13331455412529,13331455785207,13331456166898,13331456560894,13331456934011,13331457322500,13331457715549,13331458088385,13331458473840,13331458866709,13331459240098,13331459624481,13331460018091,13331460393174,13331460799934,13331461222643,13331461589850,13331461980530,13331462398425,13331462784410,13331463194394,13331463581897,13331463992719,13331464379978,13331464795079,13331465178617,13331465586223,13331465970468,13331466378919,13331466787303,13331467197008,13331467589763,13331467976468,13331468388426,13331468775448,13331469185619,13331469577402,13331469986701,13331470400979,13331470789692,13331471175473,13331471589816,13331471976246,13331472385216,13331472793954,13331473176490,13331473595481,13331473985074,13331474395116,13331474780376,13331475193705,13331475583284,13331475997763,13331476384461,13331476797005,13331477181443,13331477595114,13331477162456,13331477576030],[13331482295981,13331484203587,13331484579732,13331484963259,13331485355108,13331485732594,13331486113970,13331486505963,13331486879408,13331487268582,13331487656101,13331488030463,13331488418728,13331488808915,13331489181762,13331489569535,13331489962249,13331490333965,13331490718466,13331491111336,13331491487520,13331491869581,13331492262880,13331492636444,13331493022759,13331493414481,13331493788265,13331494173448,13331494565704,13331494939233,13331495325081,13331495717795,13331496091326,13331496476315,13331496860931,13331496457829,13331496842532],[13331501508864,13331503005778,13331503391273,13331503781078,13331504154058,13331504537167,13331504930789,13331505307305,13331505689585,13331506082593,13331506457650,13331506842396,13331507233159,13331507608029,13331507993670,13331508385253,13331508761544,13331509151287,13331509544649,13331509910928,13331510297876,13331510688413,13331511062217,13331511445285,13331511838884,13331512212854,13331512599228,13331512991101,13331513364939,13331513748637,13331514142884,13331514516503,13331514902674,13331515296364,13331515670125,13331516056117,13331516446191,13331516821255,13331517203934,13331517601259,13331517989312,13331518380935,13331518773254,13331519184979,13331519593489,13331519978441,13331520388540,13331520773214,13331521180780,13331521593210,13331521982810,13331522400256,13331522785404,13331523195103,13331523580331,13331523992350,13331524373403,13331524784795,13331525172614,13331525580483,13331525992165,13331526380301,13331526790955,13331527180956,13331527596467,13331527985232,13331528395381,13331528783781,13331529184834,13331529602523,13331529989912,13331530386766,13331530783745,13331531198337,13331531583452,13331531997620,13331532379570,13331532793515,13331533177279,13331533585605,13331533994863,13331534376565,13331534790641,13331535175676,13331535587459,13331535997283,13331536379323,13331536792627,13331536360185,13331536773828]]\n}"

  it should "test parseSignatureRawCaptureJSON" in {

    val strokeData = imageUtil.parseSignatureRawCaptureJSON(TestConstants.signatureStr)
    val xsByStroke = strokeData._1
    val ysByStroke = strokeData._2
    val timesByStroke = strokeData._3
    val numStroke = xsByStroke.size

    for (i <- 0 until numStroke) {
      xsByStroke(i).isInstanceOf[List[Number]] should be(true)
      ysByStroke(i).isInstanceOf[List[Number]] should be(true)
      timesByStroke(i).isInstanceOf[List[Number]] should be(true)
    }
  }

  it should "overlay Andrew's signature on a JPG" in {
    val photoImage: BufferedImage = ImageIO.read(Play.getFile("test/files/longoria/product-2.jpg"))
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(
      TestConstants.signatureStr, Some(TestConstants.messageStr)
    )
    val egraphImage: BufferedImage = imageUtil.createEgraphImage(signatureImage, photoImage, 0, 0)
    val combinedImageFile = new File("test/files/egraph.jpg")
    ImageIO.write(egraphImage, "JPG", combinedImageFile)
    combinedImageFile.length should not be(0)
  }

  "createSignatureImage" should "draw a single-point stroke" in {
    val capture = "{\n   \"x\": [[67.000000]],\n   \"y\": [[198.000000]],\n   \"t\": [[13331445844472]]\n}"
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(capture, None)
    val imageFile = new File("test/files/single-point-stroke.jpg")
    ImageIO.write(signatureImage, "JPG", imageFile)
    imageFile.length should be(52574)
  }

  "createSignatureImage" should "draw a two-point stroke" in {
    val capture = "{\n   \"x\": [[67.000000,95.148125]],\n   \"y\": [[198.000000,208.518494]],\n   \"t\": [[13331445844472,13331448640856]]\n}"
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(capture, None)
    val imageFile = new File("test/files/two-point-stroke.jpg")
    ImageIO.write(signatureImage, "JPG", imageFile)
    imageFile.length should be(53264)
  }

  "createSignatureImage" should "draw a three-point stroke" in {
    val capture = "{\n   \"x\": [[67.000000,95.148125,121.414230]],\n   \"y\": [[198.000000,208.518494,226.561005]],\n   \"t\": [[13331445844472,13331448640856,13331448883353]]\n}"
    val signatureImage: BufferedImage = imageUtil.createSignatureImage(capture, None)
    val imageFile = new File("test/files/three-point-stroke.jpg")
    ImageIO.write(signatureImage, "JPG", imageFile)
    imageFile.length should be(54005)
  }

  it should "overlay PNG images correctly" in {
    val image: BufferedImage = ImageIO.read(new File("test/files/image.png"))
    val overlay: BufferedImage = ImageIO.read(new File("test/files/overlay.png"))
    val combinedImage = imageUtil.createEgraphImage(overlay, image, 0, 0)
    val combinedImageFile: File = new File("test/files/combinedImage.png")
    ImageIO.write(combinedImage, "PNG", combinedImageFile)
    combinedImageFile.length should be(138807)
  }

  it should "resize PNG images correctly" in {
    val image = ImageIO.read(new File("test/files/image.png"))
    val scaled = imageUtil.getScaledInstance(image, 100, 100)
    (scaled.getWidth, scaled.getHeight) should be((100, 100))
  }

}
