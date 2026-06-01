import React, { useState } from 'react';
import { Form, Input, Button, Card, message, Typography, Space } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { authService } from '@/services/authService';
import { useAuth } from '@/contexts/AuthContext';
import { LoginRequest } from '@/types';

const { Title, Text } = Typography;

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const onFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      console.log('🔐 [Login] 开始登录请求，用户名:', values.username);
      const response = await authService.login(values);
      console.log('✅ [Login] 登录成功，用户:', response.username);
      login(response.token, response);
      message.success('登录成功！');
      navigate('/');
    } catch (error: any) {
      const data = error?.response?.data;
      const errorMessage =
        (typeof data?.message === 'string' && data.message.trim()
          ? data.message
          : null) ||
        error?.message ||
        '登录失败，请检查网络连接';
      console.error('❌ [Login] 登录失败:', {
        error,
        message: errorMessage,
        response: data,
        status: error?.response?.status,
      });
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      <Card
        style={{
          width: 400,
          boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
        }}
      >
        <Space direction="vertical" size="large" style={{ width: '100%', textAlign: 'center' }}>
          <div>
            <Title level={2}>自动报告系统</Title>
            <Text type="secondary">材料技术部</Text>
          </div>

          <Form
            name="login"
            onFinish={onFinish}
            size="large"
            autoComplete="off"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名！' }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="用户名"
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码！' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="密码"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                style={{ height: 40 }}
              >
                登录
              </Button>
            </Form.Item>
          </Form>
        </Space>
      </Card>
    </div>
  );
};

export default LoginPage;


