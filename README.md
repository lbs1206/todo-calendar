# Todo Calendar Plugin for IntelliJ IDEA

[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/com.benson.todo-calendar.svg)](https://plugins.jetbrains.com/plugin/com.benson.todo-calendar)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/com.benson.todo-calendar.svg)](https://plugins.jetbrains.com/plugin/com.benson.todo-calendar)

A comprehensive task management plugin that integrates seamlessly with IntelliJ IDEA to help developers organize and track their daily tasks.

## ✨ Features

### 📅 Calendar View
- Visualize your tasks in a monthly calendar format
- Navigate between months with intuitive prev/next buttons
- See task distribution across different dates
- Click on dates to view detailed task information

### 📋 Today Panel
- Quick overview of today's tasks with priority indicators
- Smart filtering to show only relevant tasks
- Visual indicators for urgent tasks (priority 8-10)
- Strike-through formatting for completed tasks

### ✏️ Inline Editing
- Edit tasks directly in the table without dialog popups
- Double-click any cell to start editing
- Auto-save functionality - changes are saved as you type
- Support for different input types (text, dropdown, dates)

### 🎯 Smart Notifications
- Get notified about today's pending tasks at startup
- Customizable notification settings
- Shows task count and urgency indicators
- Non-intrusive balloon notifications

### 📊 Task Management
- Full CRUD operations (Create, Read, Update, Delete)
- Rich task properties:
  - **Task Name** - Brief description
  - **Importance** - Low, Medium, High, Critical
  - **Priority** - Numeric scale from 1-10
  - **Description** - Detailed task information
  - **Date Range** - Start and end dates
  - **Status** - Waiting, In Progress, Done

## 🚀 Installation

### From JetBrains Marketplace
1. Open IntelliJ IDEA
2. Go to `File` → `Settings` → `Plugins`
3. Search for "Todo Calendar"
4. Click `Install` and restart IDE

### Manual Installation
1. Download the latest release from [Releases](https://github.com/benson/todo-calendar/releases)
2. Go to `File` → `Settings` → `Plugins`
3. Click the gear icon and select `Install Plugin from Disk...`
4. Select the downloaded JAR file
5. Restart IntelliJ IDEA

## 📖 How to Use

1. **Open the Tool Window**: Find "Todo Calendar" in the tool window bar (usually on the right side)

2. **Navigate Tabs**:
   - **Today** - View and manage today's tasks
   - **Calendar** - Monthly view with task distribution
   - **Todo** - Complete task management interface
   - **Closed** - View completed tasks

3. **Add Tasks**: Use the "추가" (Add) button in the Todo tab

4. **Edit Tasks**: Double-click any cell to edit inline

5. **Manage Status**: Use dropdown menus to change task status and importance

## 💡 Pro Tips

- **Urgent Tasks**: Tasks with priority 8-10 are marked as "긴급" (urgent) in notifications
- **Visual Feedback**: Completed tasks appear with strike-through formatting
- **Calendar Navigation**: Click on calendar dates to see tasks for specific days
- **Auto-save**: All changes are automatically saved - no need to manually save
- **Project Persistence**: Each project maintains its own set of tasks

## 🛠️ Development

### Prerequisites
- IntelliJ IDEA 2024.2 or later
- Java 17 or later
- Kotlin 2.1.0

### Building from Source
