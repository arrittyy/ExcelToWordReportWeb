# Frontend - React Application

## 技术栈

- React 18 + TypeScript
- Ant Design 5 (UI组件库)
- React Router (路由管理)
- Axios (HTTP客户端)
- React Query (数据状态管理)
- Vite (构建工具)

## 开发

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

构建输出在 `dist` 目录

## 项目结构

```
src/
├── components/          # 通用组件
│   ├── Layout/         # 布局组件
│   ├── DynamicTable/   # 动态表格
│   └── ImageUpload/    # 图片上传
├── contexts/           # React Context
│   └── AuthContext.tsx # 认证上下文
├── pages/              # 页面组件
│   ├── Login/          # 登录页
│   ├── Home/           # 首页
│   ├── Reports/        # 报告管理
│   └── Help/           # 帮助页
├── services/           # API服务
├── types/              # TypeScript类型定义
├── utils/              # 工具函数
├── App.tsx             # 根组件
└── main.tsx            # 入口文件
```

## 主要功能

### 1. 用户认证
- JWT Token认证
- 自动刷新token
- 路由守卫

### 2. 报告管理
- 报告列表（查看、编辑、删除）
- 创建/编辑报告
- Word文档生成

### 3. 动态表格
- 根据实验类型配置渲染
- 支持文本、数字、下拉选择等字段类型
- 图片上传和关联

### 4. 图片管理
- 拖拽上传
- 预览和删除
- 自动关联到表格数据

## 环境变量

开发环境下，API请求会自动代理到 http://localhost:5000

生产环境需要配置正确的API地址。

## 注意事项

1. 确保后端API服务已启动
2. 默认API端口为5000
3. 上传图片大小限制为10MB
4. 支持的图片格式：JPG、PNG、GIF、BMP


