package com.domanskii.homealarmbot.clients

import mu.KotlinLogging
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import java.io.ByteArrayOutputStream


private val log = KotlinLogging.logger {}

class RTSPVideoClient {
    companion object {
        fun getVideo(rtspUrl: String, recordingTime: Int): ByteArray {
            val frameRate = 30.0

            log.debug { "Recording $recordingTime seconds video..." }

            val grabber = FFmpegFrameGrabber(rtspUrl)
            grabber.start()

            val outputStream = ByteArrayOutputStream()
            val recorder = FFmpegFrameRecorder(outputStream, grabber.imageWidth, grabber.imageHeight)

            recorder.videoCodec = grabber.videoCodec
            recorder.format = "mp4"
            recorder.frameRate = frameRate
            recorder.start()

            val start = System.currentTimeMillis()

            while (System.currentTimeMillis() - start < recordingTime * 1000) {
                val frame: Frame = grabber.grab() ?: break
                recorder.record(frame)
            }

            recorder.stop()
            grabber.stop()

            return outputStream.toByteArray()
        }
    }
}