package dev.fanremote

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import dev.fanremote.ir.FanCommand
import dev.fanremote.ir.NecTransmitter
import dev.fanremote.ui.RemoteScreen
import android.graphics.Color as AColor

/**
 * IR is one-way, so the phone can't read the fan's real state. We keep our best guess
 * (power + speed) and persist it so the dial is right when the app is reopened.
 */
class FanState(context: Context) {
    private val prefs = context.getSharedPreferences("fan", Context.MODE_PRIVATE)

    var on by mutableStateOf(prefs.getBoolean("on", false))
        private set
    var speed by mutableIntStateOf(prefs.getInt("speed", 3))
        private set
    var last by mutableStateOf<FanCommand?>(null)
        private set

    fun record(cmd: FanCommand) {
        when (cmd) {
            FanCommand.OFF -> on = false
            FanCommand.ON -> on = true
            FanCommand.SPEED_UP -> { on = true; speed = (speed + 1).coerceAtMost(5) }
            FanCommand.SPEED_DOWN -> { on = true; speed = (speed - 1).coerceAtLeast(1) }
            else -> { on = true; speed = cmd.ordinal - FanCommand.SPEED_1.ordinal + 1 }
        }
        last = cmd
        prefs.edit().putBoolean("on", on).putInt("speed", speed).apply()
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var ir: NecTransmitter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        ir = NecTransmitter(this)
        val state = FanState(this)

        setContent {
            val view = LocalView.current
            RemoteScreen(
                on = state.on,
                speed = state.speed,
                last = state.last,
                irReady = ir.hasEmitter,
                onCommand = { cmd ->
                    if (!ir.hasEmitter) {
                        Toast.makeText(this, "No IR blaster found on this phone", Toast.LENGTH_SHORT).show()
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        ir.send(cmd) { err ->
                            runOnUiThread {
                                Toast.makeText(this, "IR send failed: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        state.record(cmd)
                    }
                },
            )
        }
    }

    override fun onDestroy() {
        ir.shutdown()
        super.onDestroy()
    }
}
