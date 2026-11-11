package com.rr.numio.control.service

import android.service.notification.NotificationListenerService

/**
 * Android only shares "what's playing" (media sessions) with an app that has
 * notification access, and that access is granted to a listener service like this one.
 * It is empty on purpose: Control by Numio never reads, keeps or sends notifications.
 * It only uses the access to show the song and its controls in the music card.
 */
class MediaListener : NotificationListenerService()
