package dev.fanremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import java.util.concurrent.Executors

/**
 * Sends NEC frames through the phone's IR blaster.
 *
 * NEC frame (timings in microseconds, 38 kHz carrier):
 *   leader 9000 mark + 4500 space
 *   32 bits, each a 560 mark followed by 560 space (0) or 1690 space (1)
 *   560 stop mark
 *
 * The codes were captured as a 32-bit value whose most significant bit is the
 * first bit received, so the bits are sent from bit 31 down to bit 0.
 */
class NecTransmitter(context: Context) {

    private val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    // transmit() blocks for the length of the frame (~68 ms), so keep it off the UI thread
    // and send one frame at a time.
    private val worker = Executors.newSingleThreadExecutor()

    val hasEmitter: Boolean get() = ir?.hasIrEmitter() == true

    fun send(command: FanCommand, onError: (Throwable) -> Unit = {}) {
        val manager = ir ?: return
        val pattern = pattern(command.code)
        worker.execute {
            try {
                manager.transmit(CARRIER_HZ, pattern)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun shutdown() = worker.shutdown()

    companion object {
        const val CARRIER_HZ = 38_000
        private const val HDR_MARK = 9000
        private const val HDR_SPACE = 4500
        private const val BIT_MARK = 560
        private const val ONE_SPACE = 1690
        private const val ZERO_SPACE = 560

        fun pattern(code: Long): IntArray {
            val out = IntArray(2 + 32 * 2 + 1)
            var i = 0
            out[i++] = HDR_MARK
            out[i++] = HDR_SPACE
            for (bit in 31 downTo 0) {
                out[i++] = BIT_MARK
                out[i++] = if ((code shr bit) and 1L == 1L) ONE_SPACE else ZERO_SPACE
            }
            out[i] = BIT_MARK
            return out
        }
    }
}
