package com.benson.todocalendar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.DefaultTableCellRenderer
import java.awt.*
import java.time.LocalDate

class ToDoWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = MainTabbedPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

private class MainTabbedPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val todoService = TodoService.getInstance(project)
    private val tabs = JTabbedPane()

    // Child panels
    private val todayPanel = TodayPanel(todoService) { refreshAll() }
    private val calendarPanel = CalendarPanel(todoService)
    private val todoPanel = ManageTodoPanel(todoService) { refreshAll() }
    private val closedPanel = ClosedPanel(todoService) { refreshAll() }

    init {
        tabs.addTab("Today ToDo List", todayPanel)
        tabs.addTab("Calendar", calendarPanel)
        tabs.addTab("Todo", todoPanel)
        tabs.addTab("Closed", closedPanel)
        add(tabs, BorderLayout.CENTER)
        refreshAll()
    }

    private fun refreshAll() {
        todayPanel.reload()
        calendarPanel.reload()
        todoPanel.reload()
        closedPanel.reload()
    }
}

private open class BaseTablePanel : JPanel(BorderLayout()) {
    protected val tableModel = DefaultTableModel()
    protected var editableColumns: Set<Int> = emptySet()
    protected val table = object : JTable(tableModel) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return editableColumns.contains(column)
        }
    }

    init {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.putClientProperty("terminateEditOnFocusLost", true)
        add(JScrollPane(table), BorderLayout.CENTER)
    }
}

private class TodayPanel(private val todoService: TodoService, private val onChanged: (() -> Unit)? = null) : BaseTablePanel() {
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
                        c.foreground = if (isSelected) Color(200, 200, 200) else Color(150, 150, 150)
                        val original = c.text
                        c.text = "<html><s>" + original + "</s></html>"
                    } else {
                        c.foreground = if (isSelected) table.foreground else Color(0, 0, 0)
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
            if (e.type == javax.swing.event.TableModelEvent.UPDATE && e.column == 6) {
                val modelRow = e.firstRow
                if (modelRow >= 0 && modelRow < ids.size) {
                    val id = ids[modelRow]
                    val statusName = tableModel.getValueAt(modelRow, 6) as String
                    val status = Status.fromDisplayName(statusName)
                    todoService.setStatus(id, status)
                    // refresh visuals and other tabs
                    reload()
                    onChanged?.invoke()
                }
            }
        }
    }

    private var ids: MutableList<String> = mutableListOf()

    fun reload() {
        ids.clear()
        tableModel.rowCount = 0
        val today = LocalDate.now()
        todoService.getTodayTodos(today).forEach { todo ->
            tableModel.addRow(
                arrayOf(
                    todo.taskName,
                    todo.importance.displayName,
                    todo.priority,
                    todo.description,
                    todo.startDate,
                    todo.endDate,
                    todo.status.displayName
                )
            )
            ids.add(todo.id)
        }
    }
}

private class ManageTodoPanel(private val todoService: TodoService, private val onChanged: () -> Unit) : BaseTablePanel() {
    init {
        tableModel.addColumn("업무명")
        tableModel.addColumn("중요도")
        tableModel.addColumn("우선순위")
        tableModel.addColumn("설명")
        tableModel.addColumn("시작일")
        tableModel.addColumn("종료일")
        tableModel.addColumn("상태")

        // 상태 컬럼만 편집 가능
        editableColumns = setOf(6)
        val statusCombo = JComboBox(Status.values().map { it.displayName }.toTypedArray())
        table.columnModel.getColumn(6).cellEditor = DefaultCellEditor(statusCombo)

        // 상태 변경 반영
        tableModel.addTableModelListener { e ->
            if (e.type == javax.swing.event.TableModelEvent.UPDATE && e.column == 6) {
                val modelRow = e.firstRow
                if (modelRow >= 0 && modelRow < current.size) {
                    val id = current[modelRow].id
                    val statusName = tableModel.getValueAt(modelRow, 6) as String
                    val status = Status.fromDisplayName(statusName)
                    todoService.setStatus(id, status)
                    reload()
                    onChanged()
                }
            }
        }

        val buttonPanel = JPanel(FlowLayout())
        val addButton = JButton("추가")
        val deleteButton = JButton("삭제")
        addButton.addActionListener { showAddTodoDialog() }
        deleteButton.addActionListener { deleteSelected() }

        buttonPanel.add(addButton)
        buttonPanel.add(deleteButton)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    private var current: List<TodoItem> = emptyList()

    fun reload() {
        tableModel.rowCount = 0
        current = todoService.getOpenTodos()
        current.forEach { todo ->
            tableModel.addRow(
                arrayOf(
                    todo.taskName,
                    todo.importance.displayName,
                    todo.priority,
                    todo.description,
                    todo.startDate,
                    todo.endDate,
                    todo.status.displayName
                )
            )
        }
    }

    private fun showAddTodoDialog() {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(this) as? Frame, "TODO 추가", true)
        dialog.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)

        val taskNameField = JTextField(20)
        val importanceCombo = JComboBox(Importance.values())
        val prioritySpinner = JSpinner(SpinnerNumberModel(5, 1, 10, 1))
        val descriptionArea = JTextArea(3, 20)
        val startDateField = JTextField(LocalDate.now().toString(), 10)
        val endDateField = JTextField(LocalDate.now().plusDays(1).toString(), 10)

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST
        dialog.add(JLabel("업무명:"), gbc)
        gbc.gridx = 1
        dialog.add(taskNameField, gbc)

        gbc.gridx = 0; gbc.gridy = 1
        dialog.add(JLabel("중요도:"), gbc)
        gbc.gridx = 1
        dialog.add(importanceCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 2
        dialog.add(JLabel("우선순위 (1-10):"), gbc)
        gbc.gridx = 1
        dialog.add(prioritySpinner, gbc)

        gbc.gridx = 0; gbc.gridy = 3
        dialog.add(JLabel("설명:"), gbc)
        gbc.gridx = 1
        dialog.add(JScrollPane(descriptionArea), gbc)

        gbc.gridx = 0; gbc.gridy = 4
        dialog.add(JLabel("시작일 (YYYY-MM-DD):"), gbc)
        gbc.gridx = 1
        dialog.add(startDateField, gbc)

        gbc.gridx = 0; gbc.gridy = 5
        dialog.add(JLabel("종료일 (YYYY-MM-DD):"), gbc)
        gbc.gridx = 1
        dialog.add(endDateField, gbc)

        val buttonPanel = JPanel()
        val okButton = JButton("확인")
        val cancelButton = JButton("취소")

        okButton.addActionListener {
            try {
                val todo = TodoItem(
                    taskName = taskNameField.text,
                    importance = importanceCombo.selectedItem as Importance,
                    priority = prioritySpinner.value as Int,
                    description = descriptionArea.text,
                    startDate = LocalDate.parse(startDateField.text),
                    endDate = LocalDate.parse(endDateField.text)
                )
                todoService.addTodo(todo)
                reload()
                onChanged()
                dialog.dispose()
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(dialog, "입력 값을 확인해주세요: ${'$'}{e.message}")
            }
        }

        cancelButton.addActionListener { dialog.dispose() }
        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2
        dialog.add(buttonPanel, gbc)

        dialog.pack()
        dialog.setLocationRelativeTo(this)
        dialog.isVisible = true
    }

    private fun deleteSelected() {
        val idx = table.selectedRow
        if (idx >= 0) {
            val modelIdx = table.convertRowIndexToModel(idx)
            if (modelIdx < current.size) {
                todoService.removeTodo(current[modelIdx].id)
                reload()
                onChanged()
            }
        } else {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택해주세요.")
        }
    }

    private fun completeSelected() {
        val idx = table.selectedRow
        if (idx >= 0) {
            val modelIdx = table.convertRowIndexToModel(idx)
            if (modelIdx < current.size) {
                todoService.markCompleted(current[modelIdx].id, true)
                reload()
                onChanged() // to update Closed and Today
            }
        } else {
            JOptionPane.showMessageDialog(this, "완료할 항목을 선택해주세요.")
        }
    }
}

private class ClosedPanel(private val todoService: TodoService, private val onChanged: (() -> Unit)? = null) : BaseTablePanel() {
    private var current: List<TodoItem> = emptyList()

    init {
        tableModel.addColumn("업무명")
        tableModel.addColumn("중요도")
        tableModel.addColumn("우선순위")
        tableModel.addColumn("설명")
        tableModel.addColumn("시작일")
        tableModel.addColumn("종료일")
        tableModel.addColumn("상태")

        editableColumns = setOf(6)
        val statusCombo = JComboBox(Status.values().map { it.displayName }.toTypedArray())
        table.columnModel.getColumn(6).cellEditor = DefaultCellEditor(statusCombo)

        tableModel.addTableModelListener { e ->
            if (e.type == javax.swing.event.TableModelEvent.UPDATE && e.column == 6) {
                val modelRow = e.firstRow
                if (modelRow >= 0 && modelRow < current.size) {
                    val id = current[modelRow].id
                    val statusName = tableModel.getValueAt(modelRow, 6) as String
                    val status = Status.fromDisplayName(statusName)
                    todoService.setStatus(id, status)
                    reload()
                    onChanged?.invoke()
                }
            }
        }
    }

    fun reload() {
        tableModel.rowCount = 0
        current = todoService.getClosedTodos()
        current.forEach { todo ->
            tableModel.addRow(
                arrayOf(
                    todo.taskName,
                    todo.importance.displayName,
                    todo.priority,
                    todo.description,
                    todo.startDate,
                    todo.endDate,
                    todo.status.displayName
                )
            )
        }
    }
}

private class CalendarPanel(private val todoService: TodoService) : JPanel(BorderLayout()) {
    private val monthLabel = JLabel("", SwingConstants.CENTER)
    private val calendarTable = JTable()
    private val detailsModel = DefaultListModel<String>()
    private val detailsList = JList(detailsModel).apply {
        visibleRowCount = 6
    }

    private var currentMonth: LocalDate = LocalDate.now().withDayOfMonth(1)

    init {
        // Top bar with prev/next
        val top = JPanel(BorderLayout())
        val prev = JButton("<")
        val next = JButton(">")
        top.add(prev, BorderLayout.WEST)
        top.add(monthLabel, BorderLayout.CENTER)
        top.add(next, BorderLayout.EAST)
        add(top, BorderLayout.NORTH)

        // Calendar grid
        val headers = arrayOf("일", "월", "화", "수", "목", "금", "토")
        calendarTable.model = object : DefaultTableModel(6, 7) {
            override fun isCellEditable(row: Int, column: Int) = false
            override fun getColumnName(column: Int) = headers[column]
        }

        calendarTable.rowHeight = 24
        calendarTable.cellSelectionEnabled = true
        calendarTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        calendarTable.setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
                c.horizontalAlignment = SwingConstants.LEFT
                val cellDate = value as? LocalDate
                if (cellDate != null) {
                    val todos = todoService.getTodosForDate(cellDate)
                    val dayText = cellDate.dayOfMonth.toString()
                    c.text = if (todos.isEmpty()) dayText else "$dayText (" + todos.size + ")"
                    c.font = c.font.deriveFont(if (todos.isEmpty()) Font.PLAIN else Font.BOLD)
                    if (cellDate.month == currentMonth.month) {
                        c.foreground = if (isSelected) table!!.selectionForeground else Color.BLACK
                    } else {
                        c.foreground = Color.GRAY
                    }
                } else {
                    c.text = ""
                }
                return c
            }
        })

        calendarTable.selectionModel.addListSelectionListener {
            val selRow = calendarTable.selectedRow
            val selCol = calendarTable.selectedColumn
            if (selRow >= 0 && selCol >= 0) {
                val date = calendarTable.getValueAt(selRow, selCol) as? LocalDate
                if (date != null) updateDetails(date) else clearDetails()
            }
        }

        add(JScrollPane(calendarTable), BorderLayout.CENTER)
        val detailsScroll = JScrollPane(detailsList)
        detailsScroll.preferredSize = Dimension(10, 150)
        add(detailsScroll, BorderLayout.SOUTH)

        prev.addActionListener { currentMonth = currentMonth.minusMonths(1); reload() }
        next.addActionListener { currentMonth = currentMonth.plusMonths(1); reload() }

        reload()
    }

    fun reload() {
        // Set month label
        monthLabel.text = currentMonth.year.toString() + "-" + String.format("%02d", currentMonth.monthValue)
        // Fill dates
        val firstDay = currentMonth
        val startCol = (firstDay.dayOfWeek.value % 7) // convert Monday(1)..Sunday(7) -> Monday 1..Sunday 0
        val model = calendarTable.model as DefaultTableModel
        for (r in 0 until 6) for (c in 0 until 7) model.setValueAt(null, r, c)

        var d = firstDay.minusDays(((startCol + 6) % 7).toLong()) // start from Sunday of the first week
        for (r in 0 until 6) {
            for (c in 0 until 7) {
                model.setValueAt(d, r, c)
                d = d.plusDays(1)
            }
        }

        // Repaint and refresh details for today if in view
        val today = LocalDate.now()
        if (today.month == currentMonth.month && today.year == currentMonth.year) {
            updateDetails(today)
        } else {
            clearDetails()
        }
    }

    private fun updateDetails(date: LocalDate) {
        detailsModel.clear()
        val items = todoService.getTodosForDate(date)
        items.forEach { t ->
            val status = if (t.isCompleted) "[완료] " else ""
            detailsModel.addElement(status + t.taskName + " - " + t.importance.displayName)
        }
    }

    private fun clearDetails() {
        detailsModel.clear()
    }
}
