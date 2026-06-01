import React, { useState } from 'react';
import { Form, Input, Button, Card, message, Typography, Space } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { authService } from '@/services/authService';
import { ChangePasswordRequest } from '@/types';

const { Title } = Typography;

const ChangePasswordPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [form] = Form.useForm();

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      // 只发送 currentPassword 和 newPassword，不发送 confirmPassword
      const changePasswordRequest: ChangePasswordRequest = {
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      };
      await authService.changePassword(changePasswordRequest);
      message.success('密码修改成功！');
      form.resetFields();
      // 可以选择返回上一页或留在当前页
      navigate(-1);
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || error?.message || '密码修改失败，请检查网络连接';
      console.error('❌ [ChangePassword] 密码修改失败:', {
        error,
        message: errorMessage,
        response: error?.response?.data,
        status: error?.response?.status,
      });
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 500, margin: '0 auto' }}>
      <Card>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div>
            <Title level={3}>修改密码</Title>
          </div>

          <Form
            form={form}
            name="changePassword"
            onFinish={onFinish}
            size="large"
            autoComplete="off"
            layout="vertical"
          >
            <Form.Item
              name="currentPassword"
              label="当前密码"
              rules={[{ required: true, message: '请输入当前密码！' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="请输入当前密码"
              />
            </Form.Item>

            <Form.Item
              name="newPassword"
              label="新密码"
              rules={[
                { required: true, message: '请输入新密码！' },
                { min: 6, message: '密码长度至少6个字符！' },
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="请输入新密码（至少6个字符）"
              />
            </Form.Item>

            <Form.Item
              name="confirmPassword"
              label="确认新密码"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: '请确认新密码！' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('两次输入的密码不一致！'));
                  },
                }),
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="请再次输入新密码"
              />
            </Form.Item>

            <Form.Item>
              <Space>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                >
                  修改密码
                </Button>
                <Button onClick={() => navigate(-1)}>
                  取消
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Space>
      </Card>
    </div>
  );
};

export default ChangePasswordPage;
