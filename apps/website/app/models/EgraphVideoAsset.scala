package models

import com.google.inject.Inject
import enums.{Mp4, VideoType}
import java.io.File
import services.{Utils, TempFile, Time, AppConfig}
import services.audio.AudioConverter
import services.blobs.{AccessPolicy, Blobs}
import services.logging.Logging
import services.video.VideoEncoder
import Blobs.Conversions._

/**
 * The video representation of an Egraph.
 *
 * @param blobPath Root blobstore path where permutations of the video should be stored. For example,
 *                 the first Egraph will probably pass usually be "egraphs/1/video"
 * @param videoType type of video (mp4, webm, etc.)
 * @param width width in pixels of video
 * @param services The functionality necessary to manipulate the EgraphVideoAsset's data.
 */
case class EgraphVideoAsset(blobPath: String,
                            egraph: Egraph,
                            videoType: VideoType = Mp4,
                            width: Int = VideoEncoder.canvasWidth,
                            services: EgraphVideoAssetServices = AppConfig.instance[EgraphVideoAssetServices]
                           ) {

  import EgraphVideoAsset._

  /* TODO(egraph-exploration): Work in progress. Not finalized. */
  protected[models] def encodeVideo: Array[Byte] = {

    /* Xuggle uses intermediate files. So, prep a bunch of temp files. */
    val wavTempFile = TempFile.named(blobPath + "/temp.wav")
    val sourceMp3TempFile = TempFile.named(blobPath + "/source.mp3")
    val finalAacTempFile = TempFile.named(blobPath + "/final.aac")
    val egraphImageTempFile = TempFile.named(blobPath + "/egraph.jpg")
    val videoNoAudioFile = TempFile.named(blobPath + "/no-audio.mp4")
    val videoWithAudioFile = TempFile.named(blobPath + "/with-audio.mp4")
    val finalMp4TempFile = TempFile.named(blobPath + "/final.mp4")
    try {
      /* Get audio duration in seconds */
      Utils.saveToFile(egraph.assets.audioWav.asByteArray, wavTempFile)
      val audioDuration = AudioConverter.getDurationOfWavInSeconds(wavTempFile)

      /**
       * Generate aac audio file. This should not have to go through an intermediate step of generating an mp3,
       * but Xuggle seems to complain when converting a wav to an aac.
       */
      Utils.saveToFile(egraph.assets.audioMp3.asByteArray, sourceMp3TempFile)
      Utils.convertMediaFile(sourceMp3TempFile, finalAacTempFile)
      /**
       * Enable the following code to generate an aac appropriate with a video with preamble:
       * val sourceAacTempFile = TempFile.named(blobPath + "/source.aac")
       * Utils.convertMediaFile(sourceMp3TempFile, sourceAacTempFile)
       * VideoEncoder.generateFinalAudio(sourceAacTempFile, finalAacTempFile)
       */

      /* Get egraph image as a jpg */
      val egraphImage = egraph.getEgraphImage(width).asJpg
      egraphImage.getSavedUrl(AccessPolicy.Public)
      Utils.saveToFile(egraphImage.transformAndRender.graphicsSource.asByteArray, egraphImageTempFile)

      /* Generate an mp4 without sound */
      val thisOrder = egraph.order
      val videoNoAudioFileName = videoNoAudioFile.getPath
      VideoEncoder.generateMp4SansAudio(
        targetFilePath = videoNoAudioFileName,
        egraphImageFile = egraphImageTempFile,
        recipientName = thisOrder.recipientName,
        celebrityName = thisOrder.product.celebrity.publicName,
        audioDuration = audioDuration
      )

      /* Mux the soundless mp4 with the aac audio to create an mp4 with sound */
      VideoEncoder.muxVideoWithAudio(
        videoFile = new File(videoNoAudioFileName),
        audioFile = finalAacTempFile,
        targetFile = videoWithAudioFile
      )

      /* This step may be needed to create a correctly formatted mp4. Xuggle is weird. */
      Utils.convertMediaFile(videoWithAudioFile, finalMp4TempFile)
      val mp4Bytes = Blobs.Conversions.fileToByteArray(finalMp4TempFile)
      mp4Bytes

    } finally {
      wavTempFile.delete()
      sourceMp3TempFile.delete()
      finalAacTempFile.delete()
      egraphImageTempFile.delete()
      videoNoAudioFile.delete()
      videoWithAudioFile.delete()
      finalMp4TempFile.delete()
      //      sourceAacTempFile.delete()
    }

  }

  /**
   * The key by which this EgraphVideoAsset will be known in the blobstore.
   **/
  private val blobKey: String = {
    val idString = "width-" + width + "px"
    blobPath + "/" + idString + "-v" + EgraphVideoAsset.Version + "." + videoType.extension
  }

  /* Looks just like EgraphImage. Can be refactored. */
  def getSavedUrl(accessPolicy: AccessPolicy, overwrite: Boolean = false): String = {
    val blobs = services.blobs
    val ((url, alreadyCached), durationSecs) = Time.stopwatch {
      (overwrite, blobs.getUrlOption(blobKey)) match {
        case (false, Some(alreadySavedUrl)) => (alreadySavedUrl, true)
        case (true, _) | (_, None) => (saveAndGetUrl(accessPolicy), false)
      }
    }
    if (alreadyCached) {
      log("Retrieving cached EgraphVideoAsset with key \"" + blobKey + "\" in " + durationSecs + "s")
    } else {
      log("Rendering and returning EgraphVideoAsset with key \"" + blobKey + "\" in " + durationSecs + "s")
    }
    url
  }

  protected[models] def saveAndGetUrl(accessPolicy: AccessPolicy): String = {
    log("Rendering EgraphVideoAsset with key \"" + blobKey + "\"")
    val (url, durationSecs) = Time.stopwatch {
      val encodedBytes = encodeVideo
      val blobs = services.blobs
      blobs.put(blobKey, encodedBytes, accessPolicy)
      blobs.getUrlOption(blobKey).get
    }
    log("Rendered the EgraphVideoAsset in " + durationSecs + "s")
    url
  }
}

object EgraphVideoAsset extends Logging {
  // Increment this every time we improve EgraphVideoAsset rendering to avoid
  // Caches serving up old versions.
  val Version = 0
}

/**
 * Services necessary for EgraphVideoAsset to perform its full functionality.
 * @param blobs the application blobstore
 */
case class EgraphVideoAssetServices @Inject()(blobs: Blobs)
