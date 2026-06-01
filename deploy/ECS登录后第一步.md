# ECS 登录后第一步（按顺序执行）

你已用 root 登录到阿里云 ECS（如 101.200.184.32），按下面顺序在终端执行即可。

---

## 第一步：安装安全更新（推荐）

系统提示有安全更新，先安装：

```bash
dnf upgrade-minimal --security -y
```

---

## 第二步：安装 Java 17

```bash
dnf install -y java-17-openjdk java-17-openjdk-devel
java -version
```

应显示 `openjdk version "17.x"`。

---

## 第三步：安装 Nginx

```bash
dnf install -y nginx
systemctl start nginx
systemctl enable nginx
```

---

## 第四步：安装 PostgreSQL（本机数据库）

- **数据库放在本机 ECS 上**（没买 RDS）：**必须执行本步**，在你这台服务器上安装 PostgreSQL。
- **若已购买阿里云 RDS**（单独的云数据库）：可跳过本步，后面用环境变量连 RDS。

系统自带源通常没有 PostgreSQL，需先添加官方源再安装（Alibaba Cloud Linux 3 多为 EL-8，若你是 EL-9 把下面 `EL-8` 改成 `EL-9`）：

```bash
# 1. 添加 PostgreSQL 官方源
dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm
dnf -qy module disable postgresql 2>/dev/null || true

# 2. 安装 PostgreSQL 15
dnf install -y postgresql15-server postgresql15

# 3. 初始化并启动
/usr/pgsql-15/bin/postgresql-15-setup initdb
systemctl start postgresql-15
systemctl enable postgresql-15

# 4. 创建数据库和用户（把 你的数据库密码 换成实际密码）
sudo -u postgres psql -c "CREATE USER reportweb WITH PASSWORD '你的数据库密码';"
sudo -u postgres psql -c "CREATE DATABASE reportweb OWNER reportweb;"
```

若 EL-8 源安装失败，可先执行 `cat /etc/os-release` 看版本，若是 `VERSION_ID="9"` 则把上面 URL 里的 `EL-8` 改为 `EL-9` 再执行。

---

## 第五步：开放防火墙端口

```bash
# 若使用 firewalld
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --permanent --add-service=ssh
firewall-cmd --reload
```

**重要**：在阿里云控制台 **安全组** 里也要放行 **入方向 80、443、22**，否则外网无法访问。

---

## 第六步：创建应用目录

```bash
mkdir -p /opt/reportweb/{uploads,signatures,logs,www}
```

- `www`：放前端构建后的静态文件（frontend/dist 内容）
- `uploads`：上传的图片
- `signatures`：签名文件
- `logs`：应用日志

---

## 第七步：上传应用文件（在你自己电脑上做）

在**你的 Windows 电脑**上：

1. **打包后端**（项目根目录，有 pom.xml 的目录）：
   ```powershell
   mvn clean package -DskipTests
   ```

2. **构建前端**：
   ```powershell
   cd frontend
   npm install
   npm run build
   cd ..
   ```

3. **上传到 ECS**（任选一种）：
   - **若本机 SSH 可用**（把 `101.200.184.32` 换成你的 ECS 公网 IP）：
     ```powershell
     scp target/report-web-0.0.1-SNAPSHOT.jar root@101.200.184.32:/opt/reportweb/
     scp -r frontend/dist/* root@101.200.184.32:/opt/reportweb/www/
     ```
   - **若用 Workbench 上传**（只能传文件、不能传文件夹）：
     1. 双击 `deploy/打包前端为zip.bat`，生成 `deploy/dist.zip`。
     2. 在 Workbench 里上传：`target/report-web-0.0.1-SNAPSHOT.jar` → `/opt/reportweb/`；`deploy/dist.zip` → `/opt/reportweb/`。
     3. 在 Workbench 终端执行（若无 unzip 先执行 `dnf install -y unzip`）：`cd /opt/reportweb && rm -rf www/* && unzip -o dist.zip -d www && rm dist.zip`

---

## 第八步：在 ECS 上配置 Nginx

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

## 第九步：配置环境变量并启动后端

在 ECS 上创建环境变量文件（**请把密码和 JWT 换成你自己的**）：

```bash
# systemd 的 EnvironmentFile 要求 KEY=VALUE，不要写 export
cat > /opt/reportweb/env.sh << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DB_USERNAME=reportweb
DB_PASSWORD=你的数据库密码
JWT_SECRET=请使用至少32位的随机字符串作为JWT密钥
EOF
chmod 600 /opt/reportweb/env.sh
```

若使用 **阿里云 RDS**，在 `env.sh` 里增加（把 RDS 地址、用户名、密码换成实际值）：

```bash
export DB_URL=jdbc:postgresql://你的RDS内网地址:5432/reportweb
export DB_USERNAME=你的RDS用户名
export DB_PASSWORD=你的RDS密码
```

启动后端（先前台试跑，确认无报错再改用 systemd）：

```bash
cd /opt/reportweb
source /opt/reportweb/env.sh
nohup java -jar report-web-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &
```

查看是否启动成功：

```bash
sleep 5
curl -s http://127.0.0.1:8080/api/ 2>/dev/null || echo "请检查 logs/app.log"
```

---

## 第十步：用 systemd 做开机自启（推荐）

```bash
cat > /etc/systemd/system/reportweb.service << 'EOF'
[Unit]
Description=Report Web Backend
After=network.target postgresql.service

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

若刚才用 `nohup` 启动了进程，可先停掉再启用 systemd：

```bash
pkill -f report-web-0.0.1-SNAPSHOT.jar
systemctl start reportweb
```

---

## 验证

浏览器访问：**http://你的ECS公网IP**（如 http://101.200.184.32）

应能看到登录页；用默认账号登录测试（首次部署后请修改默认密码）。

---

## 后期更新代码（推荐流程）

### 本地（Windows）

**方式一：一键打包并上传**（推荐）

1. 打开 `deploy/build-and-deploy.bat`，把开头的 `ECS_IP=101.200.184.32` 改成你的 ECS 公网 IP。
2. 双击运行 `deploy/build-and-deploy.bat`，会自动：打包后端 → 构建前端 → 上传到 ECS。
3. 上传完成后，到 ECS 上重启后端（见下方）。

**方式二：只打包，自己上传**

1. 双击 `deploy/仅打包不上传.bat`，只生成 `target/report-web-0.0.1-SNAPSHOT.jar` 和 `frontend/dist/`。
2. 手动用 scp 或 SFTP 上传到 ECS 的 `/opt/reportweb/` 和 `/opt/reportweb/www/`。

**方式三：用 Workbench 上传（只能传文件、不能传文件夹）**

1. 本地打包：双击 `deploy/仅打包不上传.bat`。
2. 打包前端为单个 zip：双击 `deploy/打包前端为zip.bat`，会生成 `deploy/dist.zip`。
3. 在 Workbench 里上传两个**文件**：
   - `target/report-web-0.0.1-SNAPSHOT.jar` → 目标目录 **`/opt/reportweb/`**
   - `deploy/dist.zip` → 目标目录 **`/opt/reportweb/`**
4. 在 Workbench **终端**里执行（解压前端到 www）：
   ```bash
   # 若提示 unzip: command not found，先安装：dnf install -y unzip（或 yum install -y unzip）
   cd /opt/reportweb && rm -rf www/* && unzip -o dist.zip -d www && rm dist.zip
   ```
5. 重启后端：`systemctl restart reportweb`

### ECS 上（上传完成后）

重启后端使新代码生效：

```bash
# 若已配置 systemd（推荐）
systemctl restart reportweb
systemctl status reportweb
```

若未配置 systemd，先停再启：

```bash
pkill -f report-web-0.0.1-SNAPSHOT.jar
cd /opt/reportweb && source env.sh && nohup java -jar report-web-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &
```

**注意**：只改前端时，上传 `frontend/dist/*` 到 `/opt/reportweb/www/` 后刷新浏览器即可，无需重启后端。只改后端时，上传新 jar 后执行上面的重启命令。

---

## 常见问题

| 现象 | 处理 |
|------|------|
| 外网打不开 80 | 阿里云控制台 → ECS → 安全组 → 入方向放行 80、443 |
| 502 Bad Gateway | 后端未启动或 8080 未监听：`systemctl status reportweb`、看 `/opt/reportweb/logs/app.log` |
| 数据库连接失败 | 检查 `env.sh` 里 DB 密码、RDS 白名单是否包含本机内网 IP |
| PostgreSQL 初始化报错 | 不同系统初始化命令可能不同，可查 `postgresql-setup` 或对应版本文档 |

更完整说明见项目根目录 **《阿里云部署详细教程.md》**。
