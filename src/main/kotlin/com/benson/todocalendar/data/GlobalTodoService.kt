package com.benson.todocalendar.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@State(
    name = "GlobalTodoCalendarService",
    storages = [Storage("global-todo-calendar.xml")]
)
class GlobalTodoService : PersistentStateComponent<GlobalTodoService.State> {

    data class State(
        var todos: MutableList<TodoData> = mutableListOf()
    )

    data class TodoData(
        var id: String = "",
        var taskName: String = "",
        var importance: String = "MEDIUM",
        var tags: MutableList<String> = mutableListOf(),
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

    // 기존 TodoService와 동일한 기능들
    fun addTodo(todo: TodoItem) {
        val todoData = TodoData(
            id = todo.id,
            taskName = todo.taskName,
            importance = todo.importance.name,
            tags = todo.tags.toMutableList(),
            description = todo.description,
            startDate = todo.startDate.toString(),
            endDate = todo.endDate.toString(),
            status = todo.status.name,
            isCompleted = todo.isCompleted
        )
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
                tags = data.tags.toMutableSet(),
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
            data.tags = updatedTodo.tags.toMutableList()
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

    // JSON Export/Import 기능
    fun exportToJson(filePath: Path): Result<Unit> {
        return try {
            val gson = createGson()
            val jsonData = ExportData(
                exportDate = LocalDate.now(),
                todos = getTodos()
            )
            val json = gson.toJson(jsonData)
            Files.write(filePath, json.toByteArray())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importFromJson(filePath: Path): Result<Int> {
        return try {
            val json = Files.readString(filePath)
            val gson = createGson()
            val importData = gson.fromJson(json, ExportData::class.java)

            var importedCount = 0
            importData.todos.forEach { todo ->
                // 중복 체크 (ID 기준)
                if (state.todos.none { it.id == todo.id }) {
                    addTodo(todo)
                    importedCount++
                }
            }
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllTags(): Set<String> {
        return getTodos().flatMap { it.tags }.toSet()
    }

    private fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
                com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE))
            })
            .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ ->
                LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
            })
            .setPrettyPrinting()
            .create()
    }

    data class ExportData(
        val exportDate: LocalDate,
        val version: String = "1.0",
        val todos: List<TodoItem>
    )

    companion object {
        fun getInstance(): GlobalTodoService = ApplicationManager.getApplication().getService(GlobalTodoService::class.java)
    }
}