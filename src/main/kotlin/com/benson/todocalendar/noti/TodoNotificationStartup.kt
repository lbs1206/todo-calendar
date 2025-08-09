package com.benson.todocalendar.noti

import com.benson.todocalendar.data.Status
import com.benson.todocalendar.data.TodoService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.time.LocalDate

class TodoNotificationStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        val todoService = TodoService.Companion.getInstance(project)
        val todayTodos = todoService.getTodayTodos(LocalDate.now())

        if (todayTodos.isNotEmpty()) {
            val notificationGroup = NotificationGroupManager.getInstance()
                .getNotificationGroup("TodoCalendar")

            val openTodos = todayTodos.filter { it.status != Status.DONE }

            if (openTodos.isNotEmpty()) {
                val title = "오늘 할 일 알림"
                val content = buildString {
                    append("오늘 해야할 일이 ${openTodos.size}개 있습니다:<br/>")
                    openTodos.take(5).forEach { todo ->
                        val priorityText = if (todo.priority >= 8) " [긴급]" else ""
                        append("• ${todo.taskName}$priorityText<br/>")
                    }
                    if (openTodos.size > 5) {
                        append("... 외 ${openTodos.size - 5}개")
                    }
                }

                val notification = notificationGroup.createNotification(
                    title,
                    content,
                    NotificationType.INFORMATION
                )

                notification.notify(project)
            }
        }
    }
}
