package com.mika.dspot.expect

import platform.UIKit.UIPasteboard

actual fun copyToClipboard(label: String, text: String) {
    UIPasteboard.generalPasteboard.string = text
}

