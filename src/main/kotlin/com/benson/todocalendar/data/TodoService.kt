package com.benson.todocalendar.data

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.time.LocalDate

/**
 * 프로젝트별 TodoService - 이제 글로벌 서비스로 위임
 */
@Service(Service.Level.PROJECT)
class TodoService {

    private val globalService = GlobalTodoService.getInstance()

    // 모든 메서드를 글로벌 서비스로 위임
    fun addTodo(todo: TodoItem) = globalService.addTodo(todo)
    fun getTodos(): List<TodoItem> = globalService.getTodos()
    fun removeTodo(id: String) = globalService.removeTodo(id)
    fun markCompleted(id: String, completed: Boolean = true) = globalService.markCompleted(id, completed)
    fun setStatus(id: String, status: Status) = globalService.setStatus(id, status)
    fun updateTodo(updatedTodo: TodoItem) = globalService.updateTodo(updatedTodo)
    fun getTodoById(id: String): TodoItem? = globalService.getTodoById(id)
    fun getOpenTodos(): List<TodoItem> = globalService.getOpenTodos()
    fun getClosedTodos(): List<TodoItem> = globalService.getClosedTodos()
    fun getTodosForDate(date: LocalDate): List<TodoItem> = globalService.getTodosForDate(date)
    fun getTodayTodos(today: LocalDate = LocalDate.now()): List<TodoItem> = globalService.getTodayTodos(today)

    companion object {
        fun getInstance(project: Project): TodoService = project.service()
    }
}