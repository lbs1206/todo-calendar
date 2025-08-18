
package com.benson.todocalendar.panel

import com.benson.todocalendar.data.GlobalTodoService
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class ImportExportPanel : JPanel(BorderLayout()) {
    private val globalService = GlobalTodoService.getInstance()

    init {
        val titleLabel = JLabel("📁 데이터 관리", SwingConstants.CENTER)
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
        add(titleLabel, BorderLayout.NORTH)

        val buttonPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.insets = Insets(10, 10, 10, 10)
        gbc.fill = GridBagConstraints.HORIZONTAL

        // Export 버튼
        val exportButton = JButton("📤 JSON으로 내보내기")
        exportButton.preferredSize = Dimension(200, 40)
        exportButton.toolTipText = "모든 할 일을 JSON 파일로 저장합니다"
        exportButton.addActionListener { exportToJson() }

        // Import 버튼
        val importButton = JButton("📥 JSON에서 가져오기")
        importButton.preferredSize = Dimension(200, 40)
        importButton.toolTipText = "JSON 파일에서 할 일을 가져옵니다 (중복은 건너뜀)"
        importButton.addActionListener { importFromJson() }

        // 태그 통계 패널
        val tagsButton = JButton("🏷️ 태그 통계 보기")
        tagsButton.preferredSize = Dimension(200, 40)
        tagsButton.toolTipText = "사용된 모든 태그와 개수를 확인합니다"
        tagsButton.addActionListener { showTagStatistics() }

        gbc.gridx = 0; gbc.gridy = 0
        buttonPanel.add(exportButton, gbc)

        gbc.gridy = 1
        buttonPanel.add(importButton, gbc)

        gbc.gridy = 2
        buttonPanel.add(tagsButton, gbc)

        add(buttonPanel, BorderLayout.CENTER)

        // 하단에 설명 텍스트
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.border = BorderFactory.createTitledBorder("ℹ️ 정보")

        val info1 = JLabel("• 데이터는 모든 프로젝트에서 공유됩니다")
        val info2 = JLabel("• JSON 형식으로 백업/복원이 가능합니다")
        val info3 = JLabel("• 가져오기 시 중복된 할 일은 건너뜁니다")

        info1.font = info1.font.deriveFont(12f)
        info2.font = info2.font.deriveFont(12f)
        info3.font = info3.font.deriveFont(12f)

        infoPanel.add(Box.createVerticalStrut(5))
        infoPanel.add(info1)
        infoPanel.add(info2)
        infoPanel.add(info3)
        infoPanel.add(Box.createVerticalStrut(5))

        add(infoPanel, BorderLayout.SOUTH)
    }

    private fun exportToJson() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "JSON으로 내보내기"
        fileChooser.fileFilter = FileNameExtensionFilter("JSON Files", "json")
        fileChooser.selectedFile = File("todo-calendar-export.json")

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            val filePath = if (file.name.endsWith(".json")) file.toPath() else File(file.absolutePath + ".json").toPath()

            globalService.exportToJson(filePath).fold(
                onSuccess = {
                    JOptionPane.showMessageDialog(
                        this,
                        "성공적으로 내보냈습니다!\n파일: ${filePath.fileName}",
                        "내보내기 완료",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                },
                onFailure = { e ->
                    JOptionPane.showMessageDialog(
                        this,
                        "내보내기 실패: ${e.message}",
                        "오류",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            )
        }
    }

    private fun importFromJson() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "JSON에서 가져오기"
        fileChooser.fileFilter = FileNameExtensionFilter("JSON Files", "json")

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val filePath = fileChooser.selectedFile.toPath()

            globalService.importFromJson(filePath).fold(
                onSuccess = { count ->
                    JOptionPane.showMessageDialog(
                        this,
                        "성공적으로 가져왔습니다!\n가져온 할 일: ${count}개",
                        "가져오기 완료",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                },
                onFailure = { e ->
                    JOptionPane.showMessageDialog(
                        this,
                        "가져오기 실패: ${e.message}",
                        "오류",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            )
        }
    }

    private fun showTagStatistics() {
        val allTags = globalService.getAllTags()
        val todos = globalService.getTodos()

        val tagStats = allTags.associateWith { tag ->
            todos.count { todo -> todo.tags.contains(tag) }
        }.toList().sortedByDescending { it.second }

        val dialog = JDialog(SwingUtilities.getWindowAncestor(this) as? Frame, "🏷️ 태그 통계", true)
        dialog.layout = BorderLayout()

        val listModel = DefaultListModel<String>()
        if (tagStats.isEmpty()) {
            listModel.addElement("사용된 태그가 없습니다.")
        } else {
            tagStats.forEach { (tag, count) ->
                listModel.addElement("#$tag (${count}개)")
            }
        }

        val list = JList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION

        dialog.add(JLabel("  총 ${tagStats.size}개의 태그가 사용되었습니다  ", SwingConstants.CENTER), BorderLayout.NORTH)
        dialog.add(JScrollPane(list), BorderLayout.CENTER)

        val closeButton = JButton("닫기")
        closeButton.addActionListener { dialog.dispose() }
        val buttonPanel = JPanel()
        buttonPanel.add(closeButton)
        dialog.add(buttonPanel, BorderLayout.SOUTH)

        dialog.size = Dimension(300, 400)
        dialog.setLocationRelativeTo(this)
        dialog.isVisible = true
    }
}