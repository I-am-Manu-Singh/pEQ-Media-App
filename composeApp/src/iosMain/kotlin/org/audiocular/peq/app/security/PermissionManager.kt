package org.audiocular.peq.app.security

class IosPermissionManager : PermissionManager {
    override fun requestMediaPermissions() {}
    override fun hasMediaPermissions(): Boolean = true
    override fun requestMicrophonePermission() {}
    override fun hasMicrophonePermission(): Boolean = true
    override fun requestNotificationPermission() {}
    override fun hasNotificationPermission(): Boolean = true
}

actual fun getPermissionManager(): PermissionManager = IosPermissionManager()
