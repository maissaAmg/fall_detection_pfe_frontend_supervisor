package com.example.appfall.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.appfall.R
import com.example.appfall.views.fragments.QRScannerFragment
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

const val channelId = "notification_channel"
const val channelName = "Notification Channel"

class MessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            val title = it.title ?: "No Title"
            val body = it.body ?: "No Message"
            generateNotification(title, body)
        }
    }

    // Called if FCM registration token is updated
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Log the new token or send it to your server
        // This is where you can handle the new token, such as sending it to your server
        // for registration or updating user information.
        println("FCM Token: $token")
        // You may want to send this token to your server for updates or handle it accordingly
        // Example: sendTokenToServer(token)
    }

    private fun getRemoteView(title: String, message: String): RemoteViews {
        val remoteView = RemoteViews(packageName, R.layout.notification)
        remoteView.setTextViewText(R.id.title, title)
        remoteView.setTextViewText(R.id.message, message)
        remoteView.setImageViewResource(R.id.app_logo, R.drawable.ic_launcher_background)
        return remoteView
    }

    private fun generateNotification(title: String, message: String) {
        val intent = Intent(this, QRScannerFragment::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setContentIntent(pendingIntent)
            .setCustomContentView(getRemoteView(title, message))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

}