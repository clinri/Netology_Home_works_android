package ru.netology.nmedia.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.PushMessage
import kotlin.random.Random


class FCMService : FirebaseMessagingService() {
    private val content = "content"
    private val recipientId = "recipientId"
    private val channelId = "remote"
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        println(gson.toJson(message))
        val pushMessage = message.data[content]
        val recipientIdInPushMessage = gson.fromJson(pushMessage, PushMessage::class.java).recipientId
        val contentInPushMessage = gson.fromJson(pushMessage, PushMessage::class.java).content
        println("idInPushMessage = $recipientIdInPushMessage")
        println("contentInPushMessage = $contentInPushMessage")
        val currentId = AppAuth.getInstance().authStateFlow.value?.id

        //если recipientId = null, то это массовая рассылка, показываете Notification.
        if (recipientIdInPushMessage == null) {
            handleNotificationForAll(contentInPushMessage)

            // если recipientId = 0 (и не равен вашему), сервер считает, что у вас анонимная
            // аутентификация и вам нужно переотправить свой push token
        } else if (recipientIdInPushMessage == 0L && recipientIdInPushMessage != currentId) {
            AppAuth.getInstance().sendPushToken()

            // если recipientId != 0 (и не равен вашему), значит сервер считает, что на вашем
            // устройстве другая аутентификация и вам нужно переотправить свой push token;
        } else if (recipientIdInPushMessage != 0L && recipientIdInPushMessage != currentId) {
            AppAuth.getInstance().sendPushToken()

            // если recipientId = тому, что в AppAuth, то всё ok, показываете Notification
        } else if (recipientIdInPushMessage == currentId) {
            handleNotificationForRecipient(currentId, contentInPushMessage)
        }
    }

    private fun handleNotificationForRecipient(id: Long, contentNotify: String) {
        val notificationMessage = getString(
            R.string.notification_for_recipient_login_user,
            id
        )
        notify(notificationMessage, contentNotify)
    }

    private fun handleNotificationForAll(contentNotify: String) {
        val notificationMessage = getString(
            R.string.notification_for_all,
        )
        notify(notificationMessage, contentNotify)
    }

    private fun notify(notificationMessage: String, contentNotify: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationMessage)
            .setContentText(contentNotify)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(100_000), notification)
    }

    override fun onNewToken(tokenFirebase: String) {
        println("tokenFirebase: $tokenFirebase")
        AppAuth.getInstance().sendPushToken(tokenFirebase)
    }
}
