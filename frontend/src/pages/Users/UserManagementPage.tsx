import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Typography,
  Tag,
  message,
  Modal,
  Form,
  Input,
  Select,
  Statistic,
  Row,
  Col,
  Popconfirm,
  Alert,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userService } from '@/services/userService';
import { UserList, CreateUserRequest, UpdateUserRequest, UserRole } from '@/types';
import dayjs from 'dayjs';

const { Title } = Typography;
const { Option } = Select;

const UserManagementPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<UserList | null>(null);
  const [form] = Form.useForm();

  // 获取用户列表
  const { data: users = [], isLoading, error: usersError } = useQuery({
    queryKey: ['users'],
    queryFn: userService.getAllUsers,
    retry: 2, // 失败时重试2次
    retryDelay: 1000, // 重试延迟1秒
  });

  // 获取用户统计
  const { data: stats, error: statsError } = useQuery({
    queryKey: ['userStats'],
    queryFn: userService.getUserStats,
    retry: 2,
    retryDelay: 1000,
  });

  // 处理用户列表加载错误
  useEffect(() => {
    if (usersError) {
      const error = usersError as any;
      const status = error?.response?.status;
      if (status === 403) {
        message.error('没有权限访问用户管理，请联系管理员');
      } else {
        message.error('加载用户列表失败，请稍后重试');
      }
    }
  }, [usersError]);

  // 处理用户统计加载错误
  useEffect(() => {
    if (statsError) {
      console.error('Failed to load user stats:', statsError);
      // 统计信息失败不影响主要功能，只记录错误
    }
  }, [statsError]);

  // 创建用户
  const createMutation = useMutation({
    mutationFn: userService.createUser,
    onSuccess: () => {
      message.success('用户创建成功');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['userStats'] });
      setIsModalVisible(false);
      form.resetFields();
      setEditingUser(null);
    },
    onError: (error: any) => {
      const errorMessage = error?.response?.data?.message || '创建失败，请重试';
      message.error(errorMessage);
    },
  });

  // 更新用户
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      userService.updateUser(id, data),
    onSuccess: () => {
      message.success('用户更新成功');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['userStats'] });
      setIsModalVisible(false);
      form.resetFields();
      setEditingUser(null);
    },
    onError: (error: any) => {
      const errorMessage = error?.response?.data?.message || '更新失败，请重试';
      message.error(errorMessage);
    },
  });

  // 删除用户
  const deleteMutation = useMutation({
    mutationFn: userService.deleteUser,
    onSuccess: () => {
      message.success('用户删除成功');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['userStats'] });
    },
    onError: (error: any) => {
      const errorMessage = error?.response?.data?.message || '删除失败，请重试';
      message.error(errorMessage);
    },
  });

  // 主账号列表（用于子账号选择主账号）：非子账号用户
  const mainUsers = users.filter((u) => u.role !== 'SUB_USER' && !u.parentUserId);

  const handleCreate = () => {
    setEditingUser(null);
    form.resetFields();
    form.setFieldsValue({ role: 'USER' });
    setIsModalVisible(true);
  };

  const handleEdit = (user: UserList) => {
    setEditingUser(user);
    form.setFieldsValue({
      fullName: user.fullName,
      email: user.email,
      department: user.department,
      role: user.role,
      parentUserId: user.parentUserId,
    });
    setIsModalVisible(true);
  };

  const handleDelete = (id: string) => {
    deleteMutation.mutate(id);
  };

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      if (editingUser) {
        // 更新用户
        const updateData: UpdateUserRequest = {
          fullName: values.fullName,
          email: values.email,
          department: values.department,
          role: values.role,
        };
        updateMutation.mutate({ id: editingUser.id, data: updateData });
      } else {
        // 创建用户
        const createData: CreateUserRequest = {
          username: values.username,
          password: values.password,
          email: values.email,
          fullName: values.fullName,
          department: values.department,
          role: values.role,
        };
        if (values.role === 'SUB_USER' && values.parentUserId) {
          createData.parentUserId = values.parentUserId;
        }
        createMutation.mutate(createData);
      }
    });
  };

  const columns: ColumnsType<UserList> = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 150,
    },
    {
      title: '姓名',
      dataIndex: 'fullName',
      key: 'fullName',
      width: 150,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 200,
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 100,
      render: (role: UserRole) => {
        const label = role === 'ADMIN' ? '管理员' : role === 'SUB_USER' ? '子账号' : '普通用户';
        const color = role === 'ADMIN' ? 'red' : role === 'SUB_USER' ? 'green' : 'blue';
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: '主账号',
      dataIndex: 'parentFullName',
      key: 'parentFullName',
      width: 120,
      render: (_: unknown, record: UserList) => record.parentFullName ?? (record.parentUserId ? '-' : null),
    },
    {
      title: '部门',
      dataIndex: 'department',
      key: 'department',
      width: 150,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个用户吗？"
            description="删除后无法恢复，如果该用户有项目，将无法删除。"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];


  return (
    <div style={{ padding: '24px' }}>
      <Title level={2}>用户管理</Title>

      {/* 错误提示 */}
      {usersError && (
        <Alert
          message="加载失败"
          description={
            (usersError as any)?.response?.status === 403
              ? '您没有权限访问用户管理功能，请联系管理员'
              : '无法加载用户列表，请检查网络连接或稍后重试'
          }
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {/* 用户统计 */}
      {stats && (
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={6}>
            <Card>
              <Statistic title="总用户数" value={stats.totalCount} prefix={<UserOutlined />} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="管理员" value={stats.adminCount} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="普通用户" value={stats.userCount} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="子账号" value={stats.subUserCount ?? 0} />
            </Card>
          </Col>
        </Row>
      )}

      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            创建用户
          </Button>
        </Space>

        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={isLoading}
          onRow={(record) => ({
            onClick: (e) => {
              // 如果点击的是操作按钮，不触发跳转
              const target = e.target as HTMLElement;
              if (target.closest('button') || target.closest('.ant-popconfirm')) {
                return;
              }
              navigate(`/projects?userId=${record.id}`);
            },
            style: { cursor: 'pointer' },
          })}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
        />
      </Card>

      {/* 创建/编辑用户模态框 */}
      <Modal
        title={editingUser ? '编辑用户' : '创建用户'}
        open={isModalVisible}
        onOk={handleSubmit}
        onCancel={() => {
          setIsModalVisible(false);
          form.resetFields();
          setEditingUser(null);
        }}
        okText="确定"
        cancelText="取消"
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ role: 'USER' }}
        >
          {!editingUser && (
            <>
              <Form.Item
                label="用户名"
                name="username"
                rules={[
                  { required: true, message: '请输入用户名' },
                  { min: 3, max: 50, message: '用户名长度必须在3-50个字符之间' },
                ]}
              >
                <Input placeholder="请输入用户名" />
              </Form.Item>
              <Form.Item
                label="密码"
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, max: 100, message: '密码长度必须在6-100个字符之间' },
                ]}
              >
                <Input.Password placeholder="请输入密码" />
              </Form.Item>
            </>
          )}

          <Form.Item
            label="姓名"
            name="fullName"
            rules={[
              { required: true, message: '请输入姓名' },
              { max: 200, message: '姓名长度不能超过200个字符' },
            ]}
          >
            <Input placeholder="请输入姓名" />
          </Form.Item>

          <Form.Item
            label="邮箱"
            name="email"
            rules={[
              { type: 'email', message: '邮箱格式不正确' },
            ]}
          >
            <Input placeholder="请输入邮箱（可选）" />
          </Form.Item>

          <Form.Item
            label="部门"
            name="department"
            rules={[{ max: 100, message: '部门长度不能超过100个字符' }]}
          >
            <Input placeholder="请输入部门" />
          </Form.Item>

          <Form.Item
            label="角色"
            name="role"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select
              placeholder="请选择角色"
              onChange={() => form.setFieldValue('parentUserId', undefined)}
            >
              <Option value="ADMIN">管理员</Option>
              <Option value="USER">普通用户</Option>
              <Option value="SUB_USER">子账号（录入账号）</Option>
            </Select>
          </Form.Item>

          {!editingUser && Form.useWatch('role', form) === 'SUB_USER' && (
            <Form.Item
              label="主账号"
              name="parentUserId"
              rules={[{ required: true, message: '请选择主账号' }]}
            >
              <Select
                placeholder="请选择主账号（子账号将只能看到该主账号的进行中项目）"
                showSearch
                optionFilterProp="label"
                options={mainUsers.map((u) => ({
                  value: u.id,
                  label: `${u.fullName || u.username} (${u.username})`,
                }))}
              />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default UserManagementPage;
