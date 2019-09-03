package io.heapy.kotbot.bot.rule

import io.heapy.kotbot.bot.*
import kotlinx.io.core.readBytes
import java.io.InputStream
import javax.imageio.ImageIO

suspend fun deleteBannedImagesByHistogram(store: BotStore) = rule { update, queries ->
    val message = update.anyMessage ?: return@rule emptyList()
    message.photo.map {
        val photoInput = queries.getFile(it.fileId)
        val photoBytes = photoInput.readBytes()
        val photoStream = photoBytes.inputStream()

        val (red, green, blue) = getImageHistogram(photoStream)
    }

    listOf()
}

fun getImageHistogram(imageStream: InputStream): Triple<FloatArray, FloatArray, FloatArray> {
    val image = ImageIO.read(imageStream)
    val raster = image.raster
    val width = image.width
    val height = image.height

    val redSamples = raster.getSamples(0, 0, width, height, 0, null as? IntArray)
    val greenSamples = raster.getSamples(0, 0, width, height, 1, null as? IntArray)
    val blueSamples = raster.getSamples(0, 0, width, height, 2, null as? IntArray)

    return Triple(channelHistogram(redSamples), channelHistogram(greenSamples), channelHistogram(blueSamples))
}

fun channelHistogram(channel: IntArray, window: Int = 4): FloatArray {
    val counts = channel.groupBy { it / window }.mapValues { (_, value) -> value.size }
    val maxCount = counts.maxBy { (_, value) -> value }?.value?.toFloat() ?: 1f
    return FloatArray(256 / window) { counts.getOrDefault(it, 0) / channel.size.toFloat() }
}
