package me.odinmain.events.impl

import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

/**
 * Sent when a block has been updated.
 */
@Cancelable
class MotionUpdateEventPost(var x: Double, var y: Double, var z: Double, var yaw: Float, var pitch: Float, var onGround: Boolean, var sneaking: Boolean) : Event()
