package com.benson.todocalendar.data

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.time.LocalDate

@Service(Service.Level.PROJECT)
@State(
    name = "TodoCalendarService",
    storages = [Storage("todoCalendar.xml")]
)
class TodoService : PersistentStateComponent<TodoService.State> {

    data class State(
        var todos: MutableList<TodoData> = mutableListOf()
    )

    data class TodoData(
        var id: String = "",
        var taskName: String = "",
        var importance: String = "MEDIUM",
        var priority: Int = 5,
        var description: String = "",
        var startDate: String = "",
        var endDate: String = "",
        var status: String = "WAITING",
        var isCompleted: Boolean = false
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun addTodo(todo: TodoItem) {
        val todoData = TodoData(
            id = todo.id,
            taskName = todo.taskName,
            importance = todo.importance.name,
            priority = todo.priority,
            description = todo.description,
            startDate = todo.startDate.toString(),
            endDate = todo.endDate.toString(),
            status = todo.status.name,
            isCompleted = todo.isCompleted
        )
        // ensure isCompleted mirrors status
        todoData.isCompleted = Status.valueOf(todoData.status) == Status.DONE
        state.todos.add(todoData)
    }

    fun getTodos(): List<TodoItem> {
        return state.todos.map { data ->
            val status = runCatching { Status.valueOf(data.status) }.getOrDefault(
                if (data.isCompleted) Status.DONE else Status.WAITING
            )
            TodoItem(
                id = data.id,
                taskName = data.taskName,
                importance = Importance.valueOf(data.importance),
                priority = data.priority,
                description = data.description,
                startDate = LocalDate.parse(data.startDate),
                endDate = LocalDate.parse(data.endDate),
                status = status,
                isCompleted = (status == Status.DONE)
            )
        }
    }

    fun removeTodo(id: String) {
        state.todos.removeIf { it.id == id }
    }

    fun markCompleted(id: String, completed: Boolean = true) {
        state.todos.find { it.id == id }?.let {
            it.isCompleted = completed
            it.status = if (completed) Status.DONE.name else Status.WAITING.name
        }
    }

    fun setStatus(id: String, status: Status) {
        state.todos.find { it.id == id }?.let {
            it.status = status.name
            it.isCompleted = (status == Status.DONE)
        }
    }

    fun updateTodo(updatedTodo: TodoItem) {
        state.todos.find { it.id == updatedTodo.id }?.let { data ->
            data.taskName = updatedTodo.taskName
            data.importance = updatedTodo.importance.name
            data.priority = updatedTodo.priority
            data.description = updatedTodo.description
            data.startDate = updatedTodo.startDate.toString()
            data.endDate = updatedTodo.endDate.toString()
            data.status = updatedTodo.status.name
            data.isCompleted = updatedTodo.isCompleted
        }
    }

    fun getTodoById(id: String): TodoItem? {
        return getTodos().find { it.id == id }
    }

    fun getOpenTodos(): List<TodoItem> = getTodos().filter { it.status != Status.DONE }

    fun getClosedTodos(): List<TodoItem> = getTodos().filter { it.status == Status.DONE }

    fun getTodosForDate(date: LocalDate): List<TodoItem> =
        getTodos().filter { t -> !date.isBefore(t.startDate) && !date.isAfter(t.endDate) }

    fun getTodayTodos(today: LocalDate = LocalDate.now()): List<TodoItem> = getTodosForDate(today)

    companion object {
        fun getInstance(project: Project): TodoService = project.service()
    }
}
