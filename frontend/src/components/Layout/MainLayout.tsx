import React, { useState } from 'react';
import { Layout, Menu, Avatar, Dropdown, Space, Typography } from 'antd';
import {
  HomeOutlined,
  QuestionCircleOutlined,
  UserOutlined,
  LogoutOutlined,
  DashboardOutlined,
  BarChartOutlined,
  ProjectOutlined,
  BankOutlined,
  DatabaseOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation, Outlet, Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { isAdmin, isSubUser } from '@/utils/auth';
import type { MenuProps } from 'antd';
import './MainLayout.css';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(true);
  const [pinnedExpanded, setPinnedExpanded] = useState(false);

  const handleCollapse = (nextCollapsed: boolean) => {
    if (nextCollapsed) {
      setPinnedExpanded(false);
      setCollapsed(true);
    } else {
      setPinnedExpanded(true);
      setCollapsed(false);
    }
  };

  // 子账号仅显示「项目管理」；其他用户显示完整菜单
  const menuItems: MenuProps['items'] = isSubUser()
    ? [
        {
          key: '/projects',
          icon: <ProjectOutlined />,
          label: '项目管理',
        },
      ]
    : [
        {
          key: '/',
          icon: <HomeOutlined />,
          label: '个人',
        },
        {
          key: '/statistics',
          icon: <BarChartOutlined />,
          label: '数据管理',
        },
        {
          key: '/projects',
          icon: <ProjectOutlined />,
          label: '项目管理',
        },
        {
          key: '/power-plants',
          icon: <BankOutlined />,
          label: '电厂数据',
        },
        {
          key: '/material-library',
          icon: <DatabaseOutlined />,
          label: '材质库',
        },
        ...(isAdmin() ? [{
          key: '/users',
          icon: <UserOutlined />,
          label: '用户管理',
        }] : []),
        {
          key: '/help',
          icon: <QuestionCircleOutlined />,
          label: '帮助',
        },
      ];

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'change-password',
      icon: <KeyOutlined />,
      label: '修改密码',
      onClick: () => {
        navigate('/change-password');
      },
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => {
        logout();
        navigate('/login');
      },
    },
  ];

  const handleMenuClick: MenuProps['onClick'] = (e) => {
    navigate(e.key);
  };

  // 子账号仅允许访问：首页、项目管理、修改密码；其他路径重定向到 /projects
  const subUserAllowedPaths = ['/', '/change-password'];
  const pathname = location.pathname;
  const isProjectPath = pathname === '/projects' || pathname.startsWith('/projects/');
  if (isSubUser() && !subUserAllowedPaths.includes(pathname) && !isProjectPath) {
    return <Navigate to="/projects" replace />;
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={handleCollapse}
        onMouseEnter={() => {
          if (!pinnedExpanded) setCollapsed(false);
        }}
        onMouseLeave={() => {
          if (!pinnedExpanded) setCollapsed(true);
        }}
        width={280}
        style={{
          background: 'linear-gradient(180deg, #667eea 0%, #764ba2 100%)',
        }}
      >
        <div className={`sider-brand${collapsed ? ' sider-brand--collapsed' : ''}`}>
          <DashboardOutlined className="sider-brand-icon" />
          <div className="sider-brand-text">
            <div className="sider-brand-title">数智报告系统</div>
            <div className="sider-brand-subtitle">
              <span className="sider-brand-subtitle-line" aria-hidden />
              <span className="sider-brand-subtitle-text">-by材料技术部-</span>
              <span className="sider-brand-subtitle-line" aria-hidden />
            </div>
          </div>
        </div>
        
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{
            background: 'transparent',
            border: 'none',
          }}
        />
      </Sider>

      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: '#fff',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          }}
        >
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              <div>
                <div style={{ fontSize: 14, fontWeight: 500 }}>{user?.fullName}</div>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {user?.department}
                </Text>
              </div>
            </Space>
          </Dropdown>
        </Header>

        <Content
          style={{
            margin: '24px',
            padding: 24,
            background: '#fff',
            borderRadius: 8,
            minHeight: 280,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
