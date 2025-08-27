package com.benson.todocalendar.panel

import com.benson.todocalendar.data.Importance
import com.benson.todocalendar.data.Status
import com.benson.todocalendar.data.TodoItem
import com.benson.todocalendar.data.TodoService
import com.intellij.ui.JBColor
import java.awt.*
import java.time.LocalDate
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class ManageTodoPanel(private val todoService: TodoService, private val onChanged: () -> Unit) : BaseTablePanel() {
    private var current: List<TodoItem> = emptyList()

    init {
        tableModel.addColumn("업무명")
        tableModel.addColumn("중요도")
        tableModel.addColumn("태그")
        tableModel.addColumn("설명")
        tableModel.addColumn("시작일")
        tableModel.addColumn("종료일")
        tableModel.addColumn("상태")

        // 중요도 색상 렌더러 추가
        val renderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JComponent
                val statusText = (table?.model as DefaultTableModel).getValueAt(row, 6) as? String
                val importanceText = (table?.model as DefaultTableModel).getValueAt(row, 1) as? String
                val isDone = statusText == Status.DONE.displayName

                if (c is JLabel) {
                    if (isDone) {
                        c.foreground = if (isSelected)
                            JBColor(Color(180, 180, 180), Color(120, 120, 120))
                        else
                            JBColor(Color(150, 150, 150), Color(100, 100, 100))
                        val original = c.text
                        c.text = "<html><s>$original</s></html>"
                    } else if (column == 1 && importanceText != null) {
                        // 중요도 컬럼 색상 처리
                        c.foreground = when (importanceText) {
                            "낮음" -> if (isSelected) 
                                JBColor(Color(100, 150, 100), Color(120, 200, 120))
                            else 
                                JBColor(Color(70, 130, 70), Color(100, 180, 100))
                            "보통" -> if (isSelected)
                                JBColor(Color(150, 150, 50), Color(200, 200, 80))
                            else
                                JBColor(Color(130, 130, 30), Color(180, 180, 60))
                            "높음" -> if (isSelected)
                                JBColor(Color(200, 100, 50), Color(255, 140, 80))
                            else
                                JBColor(Color(180, 80, 30), Color(220, 120, 60))
                            "긴급" -> if (isSelected)
                                JBColor(Color(200, 50, 50), Color(255, 80, 80))
                            else
                                JBColor(Color(180, 30, 30), Color(220, 60, 60))
                            else -> if (isSelected) table!!.selectionForeground else JBColor.foreground()
                        }
                    } else {
                        c.foreground = if (isSelected) table!!.selectionForeground else JBColor.foreground()
                    }
                }
                return c
            }
        }

        // 모든 컬럼에 렌더러 적용
        for (i in 0..6) {
            table.columnModel.getColumn(i).cellRenderer = renderer
        }

        // 모든 컬럼을 편집 가능하게 설정
        editableColumns = setOf(0, 1, 2, 3, 4, 5, 6)

        // 각 컬럼에 적절한 에디터 설정
        // 중요도 컬럼 (1)
        val importanceCombo = JComboBox(Importance.values().map { it.displayName }.toTypedArray())
        table.columnModel.getColumn(1).cellEditor = DefaultCellEditor(importanceCombo)

        // 태그 컬럼 (2) - 쉼표로 구분된 태그 입력
        val tagField = JTextField()
        tagField.toolTipText = "쉼표(,)로 태그를 구분하여 입력하세요 (예: frontend, bugfix)"
        table.columnModel.getColumn(2).cellEditor = DefaultCellEditor(tagField)

        // 상태 컬럼 (6)
        val statusCombo = JComboBox(Status.values().map { it.displayName }.toTypedArray())
        table.columnModel.getColumn(6).cellEditor = DefaultCellEditor(statusCombo)

        // 모든 컬럼 변경 감지하여 자동 저장
        tableModel.addTableModelListener { e ->
            if (e.type == TableModelEvent.UPDATE && e.firstRow >= 0 && e.firstRow < current.size) {
                val modelRow = e.firstRow
                val column = e.column
                val todoItem = current[modelRow]

                try {
                    val updatedTodo = when (column) {
                        0 -> todoItem.copy(taskName = tableModel.getValueAt(modelRow, 0) as String)
                        1 -> {
                            val importanceName = tableModel.getValueAt(modelRow, 1) as String
                            val importance = Importance.values().find { it.displayName == importanceName } ?: Importance.MEDIUM
                            todoItem.copy(importance = importance)
                        }
                        2 -> {
                            val tagsText = tableModel.getValueAt(modelRow, 2) as String
                            val tags = tagsText.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .toMutableSet()
                            todoItem.copy(tags = tags)
                        }
                        3 -> todoItem.copy(description = tableModel.getValueAt(modelRow, 3) as String)
                        4 -> {
                            val dateText = tableModel.getValueAt(modelRow, 4) as String
                            val startDate = try { LocalDate.parse(dateText) } catch (e: Exception) { todoItem.startDate }
                            todoItem.copy(startDate = startDate)
                        }
                        5 -> {
                            val dateText = tableModel.getValueAt(modelRow, 5) as String
                            val endDate = try { LocalDate.parse(dateText) } catch (e: Exception) { todoItem.endDate }
                            todoItem.copy(endDate = endDate)
                        }
                        6 -> {
                            val statusName = tableModel.getValueAt(modelRow, 6) as String
                            val status = Status.Companion.fromDisplayName(statusName)
                            todoItem.copy(status = status, isCompleted = status == Status.DONE)
                        }
                        else -> todoItem
                    }

                    // 시작일이 종료일보다 늦은 경우 체크
                    if (updatedTodo.startDate.isAfter(updatedTodo.endDate)) {
                        JOptionPane.showMessageDialog(this@ManageTodoPanel, "시작일이 종료일보다 늦을 수 없습니다.")
                        reload() // 원래 값으로 되돌림
                        return@addTableModelListener
                    }

                    todoService.updateTodo(updatedTodo)
                    reload()
                    onChanged()

                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(this@ManageTodoPanel, "잘못된 입력값입니다: ${e.message}")
                    reload() // 원래 값으로 되돌림
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

    fun reload() {
        allTodos = todoService.getOpenTodos()
        applyFilters()
    }

    override fun updateTableWithFilteredData() {
        current = filteredTodos
        tableModel.rowCount = 0
        current.forEach { todo ->
            tableModel.addRow(arrayOf(
                todo.taskName,
                todo.importance.displayName,
                todo.tags.joinToString(", "),
                todo.description,
                todo.startDate,
                todo.endDate,
                todo.status.displayName
            ))
        }
    }

    private fun showAddTodoDialog() {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(this) as? Frame, "TODO 추가", true)
        dialog.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)

        val taskNameField = JTextField(20)
        val importanceCombo = JComboBox(Importance.values())
        val tagsField = JTextField(20)
        tagsField.toolTipText = "쉼표(,)로 태그를 구분하여 입력하세요 (예: frontend, bugfix)"
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
        dialog.add(JLabel("태그 (쉼표 구분):"), gbc)
        gbc.gridx = 1
        dialog.add(tagsField, gbc)

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
                val tags = tagsField.text.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableSet()

                val todo = TodoItem(
                    taskName = taskNameField.text,
                    importance = importanceCombo.selectedItem as Importance,
                    tags = tags,
                    description = descriptionArea.text,
                    startDate = LocalDate.parse(startDateField.text),
                    endDate = LocalDate.parse(endDateField.text)
                )
                todoService.addTodo(todo)
                reload()
                onChanged()
                dialog.dispose()
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(dialog, "입력 값을 확인해주세요: ${e.message}")
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
}
