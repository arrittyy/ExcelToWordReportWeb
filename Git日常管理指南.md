# Git 日常管理指南

本项目 Git 仓库：https://github.com/arrittyy/ExcelToWordReportWeb

本地路径：

```text
c:\Users\admin\Desktop\ReportProject\ReportProject\ExcelToWordReport\ExcelToWordReportWeb
```

---

## 一、分支说明

| 分支 | 用途 | 何时 push |
|------|------|-----------|
| `main` | **稳定版**，可部署、对外使用 | 仅在 `dev` 测试通过后合并发布 |
| `dev` | **日常开发**，小更新、试验、未完成功能 | 每次改完一批就 push |

```text
main   ← 稳定版（生产/部署用）
  ↑
dev    ← 日常开发（默认在这里改代码）
```

**原则：**

- 日常开发在 `dev`，不要直接在 `main` 上改。
- 服务器部署只拉 `main`：`git pull origin main`。

---

## 二、进入项目

PowerShell：

```powershell
cd "c:\Users\admin\Desktop\ReportProject\ReportProject\ExcelToWordReport\ExcelToWordReportWeb"
```

---

## 三、日常开发（最常用）

每次开工、改代码、收工，按下面做即可。

### 1. 切到 dev 并拉最新（换电脑或多人协作时）

```powershell
git checkout dev
git pull origin dev
```

若远程还没有 `dev`，本地已有 `dev` 时，可跳过 `pull`。

### 2. 改代码

修改前端、后端、文档等。

### 3. 查看改动

```powershell
git status
git diff
```

只看某个文件：

```powershell
git diff frontend/src/pages/Projects/ProjectDetailPage.tsx
```

### 4. 提交并推送到 dev

```powershell
git add .
git status
git commit -m "fix: 序号合并修复"
git push origin dev
```

第一次 push `dev` 若提示未设置上游：

```powershell
git push -u origin dev
```

---

## 四、发布稳定版（dev → main）

`dev` 上功能/修复测试通过后，合并到 `main`：

```powershell
git checkout main
git pull origin main

git merge dev

git push origin main
```

可选：打版本标签：

```powershell
git tag -a v1.0.1 -m "稳定版说明"
git push origin v1.0.1
```

合并后继续开发：

```powershell
git checkout dev
```

---

## 五、提交信息规范

格式：`类型: 说明`

| 类型 | 含义 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 报告支持批量导出 Word` |
| `fix` | 修 bug | `fix: 检测人员下拉仅显示项目工作人员` |
| `docs` | 文档 | `docs: 更新部署指南` |
| `refactor` | 重构 | `refactor: 抽取 parseStaff 工具函数` |
| `chore` | 配置/杂项 | `chore: 更新 .gitignore` |
| `test` | 测试 | `test: 添加对接焊缝参数单元测试` |

进行中未完成：

```powershell
git commit -m "wip: 对接焊缝模板调整中"
```

---

## 六、常用命令速查

```powershell
git status              # 当前状态
git branch -a           # 所有分支（本地+远程）
git log --oneline -10   # 最近 10 条提交
git diff                # 未暂存的改动
git add .               # 暂存所有改动
git add 某文件           # 只暂存某个文件
git commit -m "说明"    # 本地提交
git push origin dev     # 推到 dev
git push origin main    # 推到 main
git pull origin dev     # 拉 dev 最新
git pull origin main    # 拉 main 最新
git checkout dev        # 切换到 dev
git checkout main       # 切换到 main
```

---

## 七、不要提交的内容

以下内容应写在 `.gitignore` 中，**不要** `git add`：

| 路径/文件 | 原因 |
|-----------|------|
| `target/` | Maven 编译产物 |
| `frontend/node_modules/` | 前端依赖 |
| `frontend/dist/` | 前端构建产物 |
| `uploads/` | 用户上传的业务数据 |
| `tmp_test_images/` | 临时测试文件 |
| `.idea/` `.vscode/` `.cursor/` | IDE 个人配置 |
| `*.class` | 编译文件 |
| 密码、Token、`.env` | 安全风险 |

误加入暂存后移除跟踪（不删本地文件）：

```powershell
git rm -r --cached uploads/
git commit -m "chore: stop tracking uploads"
git push origin dev
```

---

## 八、常见问题

### 8.1 push 失败：连不上 GitHub

错误示例：

```text
Failed to connect to github.com port 443
Recv failure: Connection was reset
```

**处理：**

1. 浏览器能否打开 https://github.com
2. 若使用 VPN/代理，为 Git 配置代理（端口按实际修改）：

```powershell
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890
```

3. 网络恢复后重试：

```powershell
git push origin dev
```

取消代理：

```powershell
git config --global --unset http.proxy
git config --global --unset https.proxy
```

本地可先 `commit`，网络好了再 `push`，提交不会丢。

### 8.2 改错了，还没 commit

丢弃单个文件改动：

```powershell
git checkout -- 文件路径
```

### 8.3 已经 commit，还没 push

撤销最后一次提交，保留代码：

```powershell
git reset --soft HEAD~1
```

### 8.4 已经 push 了

用新提交修复，不要改历史：

```powershell
git add .
git commit -m "fix: 修正上一版问题"
git push origin dev
```

### 8.5 push 被拒绝（远程有新提交）

```powershell
git pull origin dev
# 若有冲突，解决后：
git add .
git commit -m "merge: resolve conflicts"
git push origin dev
```

### 8.6 查看某次提交内容

```powershell
git log --oneline
git show 提交哈希
```

---

## 九、紧急修复（hotfix）

线上 `main` 有严重 bug，可从 `main` 拉分支修完再合并：

```powershell
git checkout main
git pull origin main
git checkout -b hotfix/问题简述

# 修 bug...
git add .
git commit -m "fix: 紧急修复 xxx"
git push -u origin hotfix/问题简述

# 合并到 main
git checkout main
git merge hotfix/问题简述
git push origin main

# 同步到 dev
git checkout dev
git merge main
git push origin dev
```

---

## 十、换电脑 / 新环境

第一次克隆：

```powershell
git clone https://github.com/arrittyy/ExcelToWordReportWeb.git
cd ExcelToWordReportWeb
git checkout dev
```

配置身份（每台电脑一次）：

```powershell
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"
```

---

## 十一、服务器部署（只拉 main）

```bash
cd /path/to/ExcelToWordReportWeb
git fetch origin
git checkout main
git pull origin main
# 然后按 deploy/应用更新指南.md 构建、重启
```

---

## 十二、推荐工作节奏

**每天：**

1. `git checkout dev`
2. 改完一个完整小功能或 bug → `add` → `commit` → `push origin dev`
3. 提交信息写清楚

**准备部署 / 对外发布：**

1. 在 `dev` 上自测通过
2. `merge dev` → `main` → `push origin main`
3. 可选打 tag：`v1.0.x`
4. 服务器 `git pull origin main`

**一句话：**

- 小更新 → `dev`
- 稳定版 → `main`
- 改代码 → `add` → `commit` → `push`

---

## 十三、首次创建 dev 分支（已完成可跳过）

若远程还没有 `dev`，在本地执行：

```powershell
git checkout main
git pull origin main
git checkout -b dev
git push -u origin dev
```

本地已有 `dev` 但远程没有时，只需：

```powershell
git checkout dev
git push -u origin dev
```
