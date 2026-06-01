# 检测报告 Web - ECS 完整部署步骤

从零到可访问的一整套步骤，包含：环境安装、数据库建库建表、本机数据导出与恢复、应用与 Nginx 配置、前端上传与验证。

---

## 第一部分：本机准备（Windows）

### 1.1 导出本地数据库（表结构 + 83 个账户、电厂等数据）

在**有完整数据的电脑**上，打开 PowerShell 或 CMD，进入任意目录（如项目根目录），执行：

```powershell
set PGPASSWORD=121212
"C:\Program Files\PostgreSQL\15\bin\pg_dump.exe" -h localhost -U postgres -d reportweb -F c -f reportweb.dump
```

- 若本机数据库用户是 `reportweb`，把 `-U postgres` 改为 `-U reportweb`。
- 若 PostgreSQL 版本不是 15，把路径中的 `15` 改为你的版本号。
- 完成后当前目录会生成 **reportweb.dump**。

### 1.2 打包后端和前端

在项目根目录（有 `pom.xml` 的目录）：

1. 双击运行 **`deploy\仅打包不上传.bat`**（生成 `target\report-web-0.0.1-SNAPSHOT.jar` 和 `frontend\dist\`）。
2. 双击运行 **`deploy\打包前端为zip.bat`**（生成 `deploy\dist.zip`）。

准备上传到 ECS 的三个文件：

- `target\report-web-0.0.1-SNAPSHOT.jar`
- `deploy\dist.zip`
- `reportweb.dump`（上一步生成的）

---

## 第二部分：ECS 环境准备（Workbench 终端）

### 2.1 安装 Java 17

```bash
dnf install -y java-17-openjdk java-17-openjdk-devel
java -version
```

### 2.2 安装 Nginx

```bash
dnf install -y nginx
systemctl start nginx
systemctl enable nginx
```

### 2.3 安装 PostgreSQL 15

```bash
dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm
dnf -qy module disable postgresql 2>/dev/null || true
dnf install -y postgresql15-server postgresql15
/usr/pgsql-15/bin/postgresql-15-setup initdb
systemctl start postgresql-15
systemctl enable postgresql-15
```

若系统是 EL-9，把上面 URL 中的 `EL-8` 改为 `EL-9`。

### 2.4 创建数据库和用户（建库，表由恢复步骤建）

**把下面的 `你的数据库密码` 换成你给 reportweb 用户设的密码（记住，后面 env.sh 要用同一密码）：**

```bash
sudo -u postgres psql -c "CREATE USER reportweb WITH PASSWORD '你的数据库密码';"
sudo -u postgres psql -c "CREATE DATABASE reportweb OWNER reportweb;"
```

### 2.5 开放防火墙端口

```bash
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --permanent --add-service=ssh
firewall-cmd --reload
```

**阿里云控制台**：ECS → 安全组 → 入方向 → 放行 **80、443、22**。

### 2.6 创建应用目录

```bash
mkdir -p /opt/reportweb/{uploads,signatures,logs,www}
```

---

## 第三部分：上传文件到 ECS

用 **Workbench 上传**，把下面文件上传到 **/opt/reportweb/**：

| 本机文件 | 上传到 ECS 目标目录 |
|----------|---------------------|
| `target\report-web-0.0.1-SNAPSHOT.jar` | `/opt/reportweb/` |
| `deploy\dist.zip` | `/opt/reportweb/` |
| `reportweb.dump`（或 reportweb.sql） | `/opt/reportweb/` |
| **电子签名（可选）**：运行 `deploy\打包签名为zip.bat` 生成 `deploy\signatures.zip`，上传到 `/opt/reportweb/`，在 ECS 执行：`cd /opt/reportweb && unzip -o signatures.zip -d signatures && rm -f signatures.zip` | `/opt/reportweb/` → 解压到 `signatures/` |

---

## 第四部分：数据库表结构 + 数据恢复（建表并导入 83 个账户、电厂等）

在 **Workbench 终端**执行（**把 `你的数据库密码` 换成 2.4 步设置的 reportweb 密码**）：

```bash
PGPASSWORD=121212 pg_restore -h localhost -U reportweb -d reportweb --no-owner --no-privileges -F c /opt/reportweb/reportweb.dump
```

- 此步骤会**创建所有表**并**导入全部数据**（账户、电厂、检测类型等）。
- 若有少量报错（如已存在的对象），可忽略；若大量报错，检查 dump 是否完整、ECS 上是否已存在 reportweb 库且为空。

恢复完成后可检查表是否生成：

```bash
sudo -u postgres psql -d reportweb -c "\dt"
```

应能看到 `users`、`power_plants`、`reports` 等表。

---

## 第五部分：环境变量与后端服务

### 5.1 创建 env.sh

**把 `你的数据库密码`、`至少32位随机JWT密钥` 换成实际值：**

```bash
# 注意：systemd 的 EnvironmentFile 要求 KEY=VALUE，不要写 export
cat > /opt/reportweb/env.sh << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DB_USERNAME=reportweb
DB_PASSWORD=你的数据库密码
JWT_SECRET=至少32位随机JWT密钥
EOF
chmod 600 /opt/reportweb/env.sh
```

- `DB_PASSWORD` 必须与 2.4 步、第四部分恢复时使用的密码一致。
- `JWT_SECRET` 建议至少 32 位随机字母数字。

### 5.2 创建并启动 reportweb 服务

```bash
cat > /etc/systemd/system/reportweb.service << 'EOF'
[Unit]
Description=Report Web Backend
After=network.target postgresql-15.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/reportweb
EnvironmentFile=/opt/reportweb/env.sh
ExecStart=/usr/bin/java -jar /opt/reportweb/report-web-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable reportweb
systemctl start reportweb
systemctl status reportweb
```

若 PostgreSQL 服务名不是 `postgresql-15`，先执行 `systemctl list-units --type=service | grep -i postgres` 查看，把上面 `After=network.target postgresql-15.service` 中的服务名改成实际名称。

---

## 第六部分：Nginx 配置

```bash
cat > /etc/nginx/conf.d/reportweb.conf << 'EOF'
server {
    listen 80;
    server_name _;
    root /opt/reportweb/www;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

nginx -t && systemctl reload nginx
```

---

## 第七部分：解压前端到 www

若 ECS 没有 unzip，先安装：

```bash
dnf install -y unzip
```

再解压前端：

```bash
cd /opt/reportweb && rm -rf www/* && unzip -o dist.zip -d www && rm dist.zip
```

确认文件在：

```bash
ls -la /opt/reportweb/www/
```

应有 `index.html` 和 `assets/`。

---

## 第八部分：验证

1. **后端**：`systemctl status reportweb` 应为 **active (running)**。
2. **浏览器**：访问 **http://你的ECS公网IP**（如 http://101.200.184.32），应能打开登录页。
3. **登录**：用你本机原有的任意一个账户（或 admin/Admin123!、testuser/Test123!）登录，应能看到 83 个账户、电厂信息等数据。

---

## 步骤总览（便于核对）

| 阶段 | 内容 |
|------|------|
| 本机 | 1.1 导出 reportweb.dump；1.2 打包 jar + dist.zip |
| ECS 环境 | 2.1 Java 17；2.2 Nginx；2.3 PostgreSQL 15；2.4 建库建用户；2.5 防火墙；2.6 应用目录 |
| 上传 | 3. jar、dist.zip、reportweb.dump → /opt/reportweb/ |
| 数据库 | 4. pg_restore 恢复（建表 + 导入数据） |
| 应用 | 5. env.sh；5.2 reportweb.service 并启动 |
| 前端与访问 | 6. Nginx 配置；7. 解压 dist.zip 到 www；8. 浏览器验证 |

更细的说明见 **《ECS登录后第一步.md》**。
