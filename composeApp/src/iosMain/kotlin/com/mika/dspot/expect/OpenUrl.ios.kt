package com.mika.dspot.expect

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}

actual fun shareUrl(title: String, url: String) {
    val items = listOf("$title: $url")
    val activityController = UIActivityViewController(activityItems = items, applicationActivities = null)
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        activityController,
        animated = true,
        completion = null,
    )
}

