package com.domanskii.homealarmbot.clients

import mu.KotlinLogging
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import java.io.File
import java.nio.file.Files


private val log = KotlinLogging.logger {}

class RTSPVideoClient {
    companion object {
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