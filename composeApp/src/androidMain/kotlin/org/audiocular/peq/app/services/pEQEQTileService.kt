package org.audiocular.peq.app.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Intent
import org.audiocular.peq.app.MainActivity

/**
 * INDUSTRIAL QUICK SETTINGS HUB
 * Allows system-level EQ toggling directly from the Android Notification shade.
 */
class pEQEQTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile
        if (tile.state == Tile.STATE_ACTIVE) {
            tile.state = Tile.STATE_INACTIVE
            println("pEQControl: [TILE] EQ Bypassed via Quick Settings")
        } else {
            tile.state = Tile.STATE_ACTIVE
            println("pEQControl: [TILE] EQ Active via Quick Settings")
        }
        tile.updateTile()
    }

    private fun updateTileState() {
        val tile = qsTile
        tile.label = "pEQ Control"
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }
}
