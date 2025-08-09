package com.benson.todocalendar.panel

import com.benson.todocalendar.data.TodoService
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.LocalDate
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class CalendarPanel(private val todoService: TodoService) : JPanel(BorderLayout()) {
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
                c.horizontalAlignment = LEFT
                val cellDate = value as? LocalDate
                if (cellDate != null) {
                    val todos = todoService.getTodosForDate(cellDate)
                    val dayText = cellDate.dayOfMonth.toString()
                    c.text = if (todos.isEmpty()) dayText else "$dayText (${todos.size})"
                    c.font = c.font.deriveFont(if (todos.isEmpty()) Font.PLAIN else Font.BOLD)
                    if (cellDate.month == currentMonth.month) {
                        c.foreground = if (isSelected) table!!.selectionForeground else JBColor.foreground()
                    } else {
                        c.foreground = JBColor.GRAY
                    }
                } else {
                    c.text = ""
                }
                return c
            }
        })

        calendarTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = calendarTable.rowAtPoint(e.point)
                val col = calendarTable.columnAtPoint(e.point)
                if (row >= 0 && col >= 0) {
                    val date = calendarTable.getValueAt(row, col) as? LocalDate
                    if (date != null) updateDetails(date) else clearDetails()
                }
            }
        })

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
