---
name: update-ut-bolt-template
overview: Align the UT bolt ultrasonic report template with the provided fixed-parameter Word layout, using bolt-specific info and a single-probe parameter block.
todos:
  - id: ut-bolt-1
    content: 实现 addUTBoltInfoTable，按螺栓模板生成专用信息表（不含动态计算逻辑）
    status: completed
  - id: ut-bolt-2
    content: 实现 addUTBoltProbeAndSensitivityTable，按螺栓模板生成单探头参数/检测灵敏度/综合补偿区域，全部使用固定默认文案
    status: completed
  - id: ut-bolt-3
    content: 重构 generateUTBolt 流程，接入 addUTBoltInfoTable、addUTBoltProbeAndSensitivityTable 及检测内容说明/检测结果区域，并保留统一签字区
    status: completed
isProject: false
---

### 目标

- **根据你提供的螺栓超声检测模板，重做后端 UT 螺栓报告版式**：
- 信息表结构与截图一致（项目名称、单项编号、记录编号、设备信息、焊接/热处理/表面状态等）。
- 中部“探头/参数/检测灵敏度/综合补偿”区域按模板固定文案，**不做任何按壁厚或管径的自动计算**，只允许自定义字段覆盖。
- 下方“检测内容说明”、“检测结果”等区域文字、布局按模板固定。

### 关键文件

- [`c:\Users\admin\Desktop\ReportProject\ExcelToWordReport\ExcelToWordReportWeb\src\main\java\com\reportweb\service\WordGeneratorServiceImpl.java`](c:\Users\admin\Desktop\ReportProject\ExcelToWordReport\ExcelToWordReportWeb\src\main\java\com\reportweb\service\WordGeneratorServiceImpl.java)
- `generateUTBolt`：螺栓 UT 报告入口，目前调用通用 `addUTMainInfoTable`、`addUTDetectionContentRow`、`addUTDefectAndResultAndSign`。
- 已有对接焊缝、弯头、角焊缝等模板的专用 info/probe/defect 方法，可借鉴表格生成代码结构。

### 实施步骤

- **1. 螺栓专用信息表方法**
- 新增 `addUTBoltInfoTable(XWPFDocument document, Report report, Project project)`：
- 参考截图和 `addUTMainInfoTable`，创建 4 列主表，行内容按螺栓模板布置：项目名称、单项报告编号、记录编号、设备名称/编号、检测部位（或螺栓位置）、检测标准、检测仪器、仪器编号、扫查位置、耦合剂、检测人员、检测日期等。
- 文本默认值全部采用固定字符串或现有字段（如 `project.projectName`、`report.instrumentModel` 等），不加入任何自动推导逻辑；仅通过 `getFieldValue`/`getCustomField` 允许你在前端自定义覆盖。
- `generateUTBolt` 改为调用 `addUTBoltInfoTable`，不再使用通用 `addUTMainInfoTable`。

- **2. 单探头参数/灵敏度/补偿表格**
- 在螺栓模板附近新增 `addUTBoltProbeAndSensitivityTable(XWPFDocument document, Report report, Project project)`：
- 创建一个 3 行左右的小表：
- 行 1：标题“探头 / 频率 / 参数”等（按截图中的具体列名排版）。
- 行 2：探头参数行：默认值使用截图中的固定文案，例如 `"1号探头（5MHz φ5×12）"` 或你模板上的精确表述，通过 `getCustomField(report, "探头参数", default)` 保持可覆盖。
- 行 3：检测灵敏度行：默认值为模板中的固定 `草状波调整至80%高度` 或类似描述；
- 行 4：综合补偿行：默认值 `0dB` 或截图里的数值。
- 所有字段都采用“固定默认值 + 自定义字段覆盖”的模式，不读取任何部件信息。

- **3. 检测内容说明与检测结果文本**
- 在 `generateUTBolt` 中，在探头/灵敏度表之后，新增两个区域：
- **检测内容说明区域**：
- 使用一个跨 4 列的行或单独表，标题“检测内容说明”或与模板一致的文字。
- 内容默认写死为截图中的完整说明句子（例如“被测XX部位……共X条（需点焊密封的螺栓……）”等），通过 `getCustomField(report, "检测内容说明", default)` 允许覆盖。
- **检测结果区域**：
- 同样使用跨列单元格，标题“检测结果”。
- 默认文案如模板中的“在上述检测条件下，未发现裂纹信号。”，允许通过自定义字段覆盖。

- **4. 签字区**
- 保持当前 `addUTDefectAndResultAndSign` 在 `hasDefectTable=false, hasRemarkArea=true` 下生成的签字区域结构（编制/审核/批准 + 日期），只针对螺栓：
- 在 `generateUTBolt` 的最后继续调用 `addUTDefectAndResultAndSign(document, report, project, false, true, UT_JC10_BG01)`，这样签字/日期区域与其他 UT 报告保持统一。
- 如需与截图签字行文字完全一致，可在将来再单独拆出一个 `addUTBoltSignArea` 方法细调文案。

- **5. 整体接线调整**
- 更新 `generateUTBolt` 流程为：
- `addUTReportHeader(..., UT_JC10_BG01)`
- `addUTBoltInfoTable(...)`
- `addUTBoltProbeAndSensitivityTable(...)`
- （可选）保留 `addUTDetectionContentRow` 或直接用“检测内容说明”替代，按你现在使用的数据结构选择一种；
- “检测内容说明”区域；
- “检测结果”区域；
- `addUTDefectAndResultAndSign(..., false, true, UT_JC10_BG01)` 生成签字区。

- **6. 验证**
- 用现有的一份螺栓 UT 报告数据生成 Word：
- 核对表格布局、行顺序与截图一致。
- 确认探头参数、检测灵敏度、综合补偿、检测内容说明、检测结果等文字全部为预期固定值，且通过自定义字段能被覆盖。
- 确认没有缺陷表（只有说明 + 结果 + 签名）。