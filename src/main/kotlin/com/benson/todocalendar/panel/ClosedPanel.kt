package com.benson.todocalendar.panel

import com.benson.todocalendar.data.Status
import com.benson.todocalendar.data.TodoItem
import com.benson.todocalendar.data.TodoService
import javax.swing.DefaultCellEditor
import javax.swing.JComboBox
import javax.swing.event.TableModelEvent

class ClosedPanel(private val todoService: TodoService, private val onChanged: (() -> Unit)? = null) : BaseTablePanel() {
    private var current: List<TodoItem> = emptyList()

    init {
        tableModel.addColumn("업무명")
        tableModel.addColumn("중요도")
        tableModel.addColumn("태그")
        tableModel.addColumn("설명")
        tableModel.addColumn("시작일")
        tableModel.addColumn("종료일")
        tableModel.addColumn("상태")

        editableColumns = setOf(6)
        val statusCombo = JComboBox(Status.values().map { it.displayName }.toTypedArray())
        table.columnModel.getColumn(6).cellEditor = DefaultCellEditor(statusCombo)

        tableModel.addTableModelListener { e ->
            if (e.type == TableModelEvent.UPDATE && e.column == 6) {
                val modelRow = e.firstRow
                if (modelRow >= 0 && modelRow < current.size) {
                    val id = current[modelRow].id
                    val statusName = tableModel.getValueAt(modelRow, 6) as String
                    val status = Status.Companion.fromDisplayName(statusName)
                    todoService.setStatus(id, status)
                    reload()
                    onChanged?.invoke()
                }
            }
        }
    }

    fun reload() {
        allTodos = todoService.getClosedTodos()
        applyFilters()
    }

    override fun updateTableWithFilteredData() {
        current = filteredTodos
        tableModel.rowCount = 0
        current.forEach { todo ->
            tableModel.addRow(arrayOf(
                todo.taskName,
                todo.importance.displayName,
                todo.tags.joinToString(", ") { "#$it" },
                todo.description,
                todo.startDate,
                todo.endDate,
                todo.status.displayName
            ))
        }
    }
}