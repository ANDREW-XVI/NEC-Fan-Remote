package dev.fanremote.ir

/** Captured 32-bit NEC codes for the fan (address 0x02, MSB-first as captured). */
enum class FanCommand(val label: String, val code: Long) {
    OFF("Power off", 0x02FDD12EL),
    ON("Power on", 0x02FD31CEL),
    SPEED_1("Speed 1", 0x02FD41BEL),
    SPEED_2("Speed 2", 0x02FDC13EL),
    SPEED_3("Speed 3", 0x02FD21DEL),
    SPEED_4("Speed 4", 0x02FDA15EL),
    SPEED_5("Speed 5", 0x02FD619EL),
    SPEED_DOWN("Speed −", 0x02FD39C6L),
    SPEED_UP("Speed +", 0x02FD9966L);

    val hex: String get() = "0x%08X".format(code)

    companion object {
        fun speed(n: Int): FanCommand = when (n) {
            1 -> SPEED_1
            2 -> SPEED_2
            3 -> SPEED_3
            4 -> SPEED_4
            else -> SPEED_5
        }
    }
}
