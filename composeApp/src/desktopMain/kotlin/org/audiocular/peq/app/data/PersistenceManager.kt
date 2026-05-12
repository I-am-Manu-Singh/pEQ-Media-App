package org.audiocular.peq.app.data

import org.audiocular.peq.app.DesktopPersistenceManager

actual fun getPersistenceManager(): PersistenceManager = DesktopPersistenceManager()
