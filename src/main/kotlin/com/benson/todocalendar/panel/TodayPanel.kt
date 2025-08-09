package com.benson.todocalendar.panel

import com.benson.todocalendar.data.Status
import com.benson.todocalendar.data.TodoService
import com.intellij.ui.JBColor
import java.awt.*
import java.time.LocalDate
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class TodayPanel(private val todoService: TodoService, private val onChanged: (() -> Unit)? = null) : BaseTablePanel() {
    private var ids: MutableList<String> = mutableListOf()

    init {
        tableModel.addColumn("업무명")
        tableModel.addColumn("중요도")
        tableModel.addColumn("우선순위")
        tableModel.addColumn("설명")
        tableModel.addColumn("시작일")
        tableModel.addColumn("종료일")
        tableModel.addColumn("상태")

        // Strike-through + gray renderer for DONE rows
        val renderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JComponent
                val statusText = (table?.model as DefaultTableModel).getValueAt(row, 6) as? String
                val isDone = statusText == Status.DONE.displayName
                if (c is JLabel) {
                    if (isDone) {
                        c.foreground = if (isSelected) 
                            JBColor(Color(180, 180, 180), Color(120, 120, 120))
                        else 
                            JBColor(Color(150, 150, 150), Color(100, 100, 100))
                        val original = c.text
                        c.text = "<html><s>$original</s></html>"
                    } else {
                        c.foreground = if (isSelected) table!!.selectionForeground else JBColor.foreground()
                    }
                }
                return c
            }
        }
        // apply renderer to all visible columns
        for (i in 0..6) {
            table.columnModel.getColumn(i).cellRenderer = renderer
        }

        // make 상태 column editable only
        editableColumns = setOf(6)
        val statusCombo = JComboBox(Status.values().map { it.displayName }.toTypedArray())
        table.columnModel.getColumn(6).cellEditor = DefaultCellEditor(statusCombo)

        // react to status change
        tableModel.addTableModelListener { e ->
            if (e.type == TableModelEvent.UPDATE && e.column == 6) {
                val modelRow = e.firstRow
                if (modelRow >= 0 && modelRow < ids.size) {
                    val id = ids[modelRow]
                    val statusName = tableModel.getValueAt(modelRow, 6) as String
                    val status = Status.Companion.fromDisplayName(statusName)
                    todoService.setStatus(id, status)
                    // refresh visuals and other tabs
                    reload()
                    onChanged?.invoke()
                }
            }
        }
    }

    fun reload() {
        val today = LocalDate.now()
        allTodos = todoService.getTodayTodos(today)
        applyFilters()
    }

    override fun updateTableWithFilteredData() {
        tableModel.rowCount = 0
        ids.clear()

        filteredTodos.forEach { todo ->
            ids.add(todo.id)
            tableModel.addRow(arrayOf(
                todo.taskName,
                todo.importance.displayName,
                todo.priority,
                todo.description,
                todo.startDate,
                todo.endDate,
                todo.status.displayName
            ))
        }
    }
}
