# Java IDE 红叉与语言服务器（与 `mvn compile` 不一致时）

当 **`mvn compile` 成功** 但 Cursor/VS Code 仍对 `WordGeneratorService.java`、`WordGeneratorServiceImpl.java` 报大量语法错误（如「重复方法」、行号超出文件实际行数）时，多为 **Java Language Server 缓存** 或 **未保存的重复内容**，而非源码错误。

## 按顺序操作

1. 打开 `src/main/java/com/reportweb/service/WordGeneratorService.java`，滚到文件末尾，应 **只有一份** `public interface WordGeneratorService` 与结尾 `}`（约 46 行）。若末尾有第二段接口或重复粘贴，删除重复或 **从磁盘还原**（Revert）。
2. **保存全部**（Ctrl+K S）。
3. 命令面板执行：**Java: Clean Java Language Server Workspace**，按提示重启；或 **Developer: Reload Window**。
4. 项目根目录执行：`mvn -q compile`。通过则说明编译无误，红叉可忽略或待缓存刷新后消失。
5. 若仍异常：关闭该文件标签再打开；确认文件编码为 **UTF-8**。

## 参考

接口应仅包含：`generateReportAsync`、`generateProjectSummaryAsync`、`generateThirdPartyOnlyProjectWordAsync`、`generateTechnicalSupervisionNotifications(Project, List<Integer>)`。
