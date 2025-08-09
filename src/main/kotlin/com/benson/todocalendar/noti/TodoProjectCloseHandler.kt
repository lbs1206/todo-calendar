package com.benson.todocalendar.noti

import com.benson.todocalendar.data.Status
import com.benson.todocalendar.data.TodoService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseHandler
import com.intellij.openapi.ui.Messages
import java.time.LocalDate

class TodoProjectCloseHandler : ProjectCloseHandler {
    override fun canClose(project: Project): Boolean {
        val todoService = TodoService.Companion.getInstance(project)
        val todayTodos = todoService.getTodayTodos(LocalDate.now())
        val openTodos = todayTodos.filter { it.status != Status.DONE }

        if (openTodos.isNotEmpty()) {
            val message = buildString {
                appendLine("오늘 완료되지 않은 할 일이 ${openTodos.size}개 있습니다:")
                appendLine()
                openTodos.take(3).forEach { todo ->
                    val priorityText = if (todo.priority >= 8) " [긴급]" else ""
                    appendLine("• ${todo.taskName}$priorityText")
                }
                if (openTodos.size > 3) {
                    appendLine("... 외 ${openTodos.size - 3}개")
                }
                appendLine()
                append("정말로 프로젝트를 닫으시겠습니까?")
            }

            val result = Messages.showYesNoDialog(
                project,
                message,
                "미완료 할 일 알림",
                "닫기",
                "취소",
                Messages.getWarningIcon()
            )

            return result == Messages.YES
        }

        return true
    }
}
