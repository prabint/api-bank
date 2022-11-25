package api.bank.notification

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

fun notifyException(
    project: Project?,
    content: String
) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("API Bank (Exception)")
        .createNotification(
            content = content,
            type = NotificationType.ERROR,
        )
        .notify(project)
}

fun notifyWarning(
    project: Project?,
    content: String
) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("API Bank (Warning)")
        .createNotification(
            content = content,
            type = NotificationType.WARNING,
        )
        .notify(project)
}

fun notifySuccess(
    project: Project?,
    content: String
) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("API Bank (Success)")
        .createNotification(
            content = content,
            type = NotificationType.INFORMATION,
        )
        .notify(project)
}
