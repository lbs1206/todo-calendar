package com.benson.todocalendar

import com.benson.todocalendar.data.TodoService
import com.benson.todocalendar.panel.CalendarPanel
import com.benson.todocalendar.panel.ClosedPanel
import com.benson.todocalendar.panel.ManageTodoPanel
import com.benson.todocalendar.panel.TodayPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane

class ToDoWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = MainTabbedPanel(project)
        val content = ContentFactory.getInstance().createContent(mainPanel, "", false)
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
        tabs.addTab("Today", todayPanel)
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
