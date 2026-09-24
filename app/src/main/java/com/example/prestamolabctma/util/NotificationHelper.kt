package com.example.prestamolabctma.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.prestamolabctma.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "prestamos_reminders_channel"
        const val CHANNEL_NAME = "Recordatorios de Devolución"
    }

    init {
        crearCanalNotificaciones()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones para recordar la fecha y hora de devolución de equipos de laboratorio"
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun tienePermisoNotificaciones(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun mostrarRecordatorioDevolucion(
        solicitudId: Int,
        equipoNombre: String,
        ambiente: String
    ): Boolean {
        if (!tienePermisoNotificaciones()) return false

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recordatorio de Devolución")
            .setContentText("El equipo '$equipoNombre' prestado para '$ambiente' debe ser devuelto pronto.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(solicitudId, builder.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
