package com.domanskii.homealarmbot.clients

import mu.KotlinLogging
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Java2DFrameConverter
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam


private val log = KotlinLogging.logger {}

class RTSPClient {
    companion object {
        fun getImage(rtspUrl: String): ByteArray {
            log.debug { "Taking photo from RTSP stream..." }

            val grabber = FFmpegFrameGrabber(rtspUrl)
            val converter = Java2DFrameConverter()
            val byteArrayOutputStream = ByteArrayOutputStream()

            grabber.audioStream = Int.MAX_VALUE
            grabber.start()

            // Grab the frame
            val frame = grabber.grab()

            // Convert the frame to BufferedImage
            val image = converter.getBufferedImage(frame)

            // Write the BufferedImage to JPG with desired quality
            val jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next()
            val jpgWriteParam = jpgWriter.defaultWriteParam
            jpgWriteParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
            jpgWriteParam.compressionQuality = 0.9f  // 90% quality

            val imageOutput = ImageIO.createImageOutputStream(byteArrayOutputStream)

            jpgWriter.output = imageOutput
            jpgWriter.write(null, javax.imageio.IIOImage(image, null, null), jpgWriteParam)
            jpgWriter.dispose()

            grabber.stop()

            return byteArrayOutputStream.toByteArray()
        }

        fun getVideo(rtspUrl: String, recordingTime: Int): ByteArray {
            log.debug { "Recording $recordingTime seconds video..." }

            val grabber = FFmpegFrameGrabber(rtspUrl)
            val intermediateFile = "temp_output.mp4"

            grabber.audioStream = Int.MAX_VALUE
            grabber.start()

            // Use intermediate file for recording
            val recorder = FFmpegFrameRecorder(intermediateFile, grabber.imageWidth, grabber.imageHeight)
            recorder.videoCodec = avcodec.AV_CODEC_ID_H264
            recorder.format = "mp4"
            recorder.frameRate = grabber.frameRate
            recorder.start()

            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < recordingTime * 1000) {
                val frame = grabber.grabFrame() ?: break
                recorder.record(frame)
            }

            recorder.stop()
            grabber.stop()

            // Read the intermediate file into a ByteArray
            val byteArray = Files.readAllBytes(File(intermediateFile).toPath())

            // Delete the intermediate file
            File(intermediateFile).delete()

            return byteArray
        }
    }
}