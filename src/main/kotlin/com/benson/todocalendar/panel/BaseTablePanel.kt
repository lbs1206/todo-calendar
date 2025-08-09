package com.benson.todocalendar.panel

import com.benson.todocalendar.data.Importance
import com.benson.todocalendar.data.Status
import com.benson.todocalendar.data.TodoItem
import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel

open class BaseTablePanel : JPanel(BorderLayout()) {
    protected val tableModel = DefaultTableModel()
    protected var editableColumns: Set<Int> = emptySet()
    protected val table = object : JTable(tableModel) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return editableColumns.contains(column)
        }
    }

    // 필터링 관련 필드
    protected val searchField = JTextField(15)
    protected var allTodos: List<TodoItem> = emptyList()
    protected var filteredTodos: List<TodoItem> = emptyList()
    protected var currentFilters = FilterState()
    protected var resultCountLabel: JLabel? = null

    data class FilterState(
        var searchText: String = "",
        var importanceFilter: Importance? = null,
        var statusFilter: Status? = null,
        var priorityRange: IntRange = 1..10
    )

    init {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.putClientProperty("terminateEditOnFocusLost", true)

        // 검색/필터 패널 생성
        val filterPanel = createFilterPanel()
        add(filterPanel, BorderLayout.NORTH)
        add(JScrollPane(table), BorderLayout.CENTER)
    }

    protected open fun createFilterPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        // 검색 실시간 반영
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = applyFilters()
            override fun removeUpdate(e: DocumentEvent?) = applyFilters()
            override fun changedUpdate(e: DocumentEvent?) = applyFilters()
        })

        // 반응형 필터링 패널 생성 (두 줄로 배치)
        val filtersPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.insets = Insets(2, 2, 2, 8)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.NONE

        // 첫 번째 줄: 검색창, 중요도와 상태 필터
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        filtersPanel.add(JLabel("🔍"), gbc)

        gbc.gridx = 1; gbc.insets = Insets(2, 0, 2, 15)
        searchField.preferredSize = Dimension(120, searchField.preferredSize.height)
        filtersPanel.add(searchField, gbc)

        gbc.gridx = 2; gbc.insets = Insets(2, 0, 2, 8)
        filtersPanel.add(JLabel("중요도:"), gbc)

        gbc.gridx = 3; gbc.insets = Insets(2, 0, 2, 15)
        val importanceCombo = JComboBox(arrayOf("전체", "낮음", "보통", "높음", "긴급"))
        importanceCombo.preferredSize = Dimension(90, importanceCombo.preferredSize.height)
        importanceCombo.addActionListener { 
            currentFilters.importanceFilter = when (importanceCombo.selectedIndex) {
                0 -> null
                1 -> Importance.LOW
                2 -> Importance.MEDIUM
                3 -> Importance.HIGH
                4 -> Importance.CRITICAL
                else -> null
            }
            applyFilters()
        }
        filtersPanel.add(importanceCombo, gbc)

        gbc.gridx = 4; gbc.insets = Insets(2, 0, 2, 8)
        filtersPanel.add(JLabel("상태:"), gbc)

        gbc.gridx = 5; gbc.insets = Insets(2, 0, 2, 15)
        val statusCombo = JComboBox(arrayOf("전체", "대기", "진행중", "완료"))
        statusCombo.preferredSize = Dimension(80, statusCombo.preferredSize.height)
        statusCombo.addActionListener { 
            currentFilters.statusFilter = when (statusCombo.selectedIndex) {
                0 -> null
                1 -> Status.WAITING
                2 -> Status.IN_PROGRESS
                3 -> Status.DONE
                else -> null
            }
            applyFilters()
        }
        filtersPanel.add(statusCombo, gbc)

        // 여유 공간을 채우기 위한 빈 공간
        gbc.gridx = 6; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL
        filtersPanel.add(Box.createHorizontalGlue(), gbc)

        // 두 번째 줄: 우선순위 필터와 리셋 버튼
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE
        gbc.insets = Insets(2, 2, 2, 8)
        filtersPanel.add(JLabel("우선순위:"), gbc)

        gbc.gridx = 1; gbc.insets = Insets(2, 0, 2, 15)
        val priorityCombo = JComboBox(arrayOf("전체", "높음 (8-10)", "보통 (4-7)", "낮음 (1-3)"))
        priorityCombo.preferredSize = Dimension(110, priorityCombo.preferredSize.height)
        priorityCombo.addActionListener { 
            currentFilters.priorityRange = when (priorityCombo.selectedIndex) {
                0 -> 1..10
                1 -> 8..10
                2 -> 4..7
                3 -> 1..3
                else -> 1..10
            }
            applyFilters()
        }
        filtersPanel.add(priorityCombo, gbc)

        // 필터 초기화 버튼
        gbc.gridx = 2; gbc.insets = Insets(2, 0, 2, 8)
        val resetFiltersBtn = JButton("🔄 초기화")
        resetFiltersBtn.toolTipText = "모든 필터 초기화"
        resetFiltersBtn.preferredSize = Dimension(80, resetFiltersBtn.preferredSize.height)
        resetFiltersBtn.addActionListener {
            // 모든 필터 초기화
            searchField.text = ""
            importanceCombo.selectedIndex = 0
            statusCombo.selectedIndex = 0
            priorityCombo.selectedIndex = 0
            currentFilters = FilterState()
            applyFilters()
        }
        filtersPanel.add(resetFiltersBtn, gbc)

        // 결과 개수 표시 레이블
        gbc.gridx = 3; gbc.insets = Insets(2, 0, 2, 15)
        val resultLabel = JLabel("")
        resultLabel.foreground = JBColor.GRAY
        filtersPanel.add(resultLabel, gbc)

        // 필터링 결과 개수 업데이트를 위한 참조 저장
        this.resultCountLabel = resultLabel

        panel.add(filtersPanel, BorderLayout.CENTER)

        return panel
    }

    protected fun applyFilters() {
        currentFilters.searchText = searchField.text.trim()

        filteredTodos = allTodos.filter { todo ->
            // 검색어 필터
            val searchMatch = if (currentFilters.searchText.isEmpty()) true else {
                todo.taskName.contains(currentFilters.searchText, ignoreCase = true) ||
                todo.description.contains(currentFilters.searchText, ignoreCase = true)
            }

            // 중요도 필터
            val importanceMatch = currentFilters.importanceFilter?.let { it == todo.importance } ?: true

            // 상태 필터
            val statusMatch = currentFilters.statusFilter?.let { it == todo.status } ?: true

            // 우선순위 필터
            val priorityMatch = todo.priority in currentFilters.priorityRange

            searchMatch && importanceMatch && statusMatch && priorityMatch
        }

        // 결과 개수 업데이트
        resultCountLabel?.text = "${filteredTodos.size}개 / ${allTodos.size}개"

        updateTableWithFilteredData()
    }

    protected open fun updateTableWithFilteredData() {
        // 각 하위 클래스에서 구현
    }
}
