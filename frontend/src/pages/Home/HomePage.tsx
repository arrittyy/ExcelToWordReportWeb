import React from 'react';
import { Row, Col, Card, List, Typography, Tag, Button, Space, Empty, Badge, Divider } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ProjectOutlined, FileTextOutlined, RightOutlined } from '@ant-design/icons';
import { projectService } from '@/services/projectService';
import type { ProjectList, TodoItem } from '@/types';

const { Title, Text } = Typography;

const roleLabel = (r: string) =>
  ({ writer: '编写人', reviewer: '审核人', approver: '批准人' }[r] || r);

const HomePage: React.FC = () => {
  const navigate = useNavigate();

  const { data: projects = [], isLoading: projectsLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: projectService.getAll,
  });

  const { data: todos = [], isLoading: todosLoading } = useQuery({
    queryKey: ['my-todos'],
    queryFn: projectService.getMyTodos,
  });

  const myOngoingProjects = projects.filter((p) => p.status === 'InProgress');

  const trackLabel = (t: string) => (t === 'ndt' ? '无损' : '理化');
  const roleColor = (role: string) =>
    role === 'writer' ? 'blue' : role === 'reviewer' ? 'orange' : 'green';

  return (
    <div style={{ minHeight: '100%' }}>
      <style>{`.home-list-item:hover { background: #fafafa; }`}</style>
      <Title level={2} style={{ color: '#333', marginBottom: 32 }}>
        个人事项
      </Title>
      <Divider style={{ margin: '0 0 24px 0' }} />

      <Row gutter={[24, 24]}>
        {/* 左侧：进行中项目 */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <ProjectOutlined />
                <span>进行中项目</span>
              </Space>
            }
            loading={projectsLoading}
            style={{
              borderRadius: 20,
              boxShadow: '0 2px 8px rgba(0,0,0,.06)',
            }}
            headStyle={{ borderBottom: '1px solid #f0f0f0', fontWeight: 600 }}
            bodyStyle={{ padding: '20px 24px' }}
          >
            {myOngoingProjects.length === 0 ? (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无进行中的项目"
                style={{ padding: '24px 0' }}
              >
                <Button type="primary" onClick={() => navigate('/projects/new')}>
                  新建项目
                </Button>
              </Empty>
            ) : (
              <List
                split
                itemLayout="horizontal"
                dataSource={myOngoingProjects}
                renderItem={(item: ProjectList) => (
                  <List.Item
                    className="home-list-item"
                    style={{ cursor: 'pointer', paddingTop: 12, paddingBottom: 12 }}
                    onClick={() => navigate(`/projects/${item.id}`)}
                    actions={[
                      <Button
                        type="link"
                        size="small"
                        icon={<RightOutlined />}
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/projects/${item.id}`);
                        }}
                      >
                        查看
                      </Button>,
                    ]}
                  >
                    <List.Item.Meta
                      avatar={<ProjectOutlined style={{ color: '#1890ff', fontSize: 18 }} />}
                      title={
                        <Space>
                          <span>{item.projectName}</span>
                          <Tag color="processing">{item.status === 'InProgress' ? '进行中' : item.status}</Tag>
                        </Space>
                      }
                      description={
                        <Text type="secondary">
                          {item.projectNumber}
                          {item.reportCount != null && ` · ${item.reportCount} 份报告`}
                        </Text>
                      }
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>

        {/* 右侧：待处理事项 */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <FileTextOutlined />
                <span>待处理事项</span>
                {todos.length > 0 && (
                  <Badge count={todos.length} size="small" />
                )}
              </Space>
            }
            loading={todosLoading}
            style={{
              borderRadius: 20,
              boxShadow: '0 2px 8px rgba(0,0,0,.06)',
            }}
            headStyle={{ borderBottom: '1px solid #f0f0f0', fontWeight: 600 }}
            bodyStyle={{ padding: '20px 24px' }}
          >
            {todos.length === 0 ? (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无待办事项"
                style={{ padding: '24px 0' }}
              />
            ) : (
              <List
                split
                itemLayout="horizontal"
                dataSource={todos}
                renderItem={(item: TodoItem) => (
                  <List.Item
                    className="home-list-item"
                    style={{ cursor: 'pointer', paddingTop: 12, paddingBottom: 12 }}
                    onClick={() => navigate(`/projects/${item.projectId}`)}
                    actions={[
                      <Button
                        type="primary"
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/projects/${item.projectId}`);
                        }}
                      >
                        去处理
                      </Button>,
                    ]}
                  >
                    <List.Item.Meta
                      title={
                        <Space wrap>
                          <span>{item.projectName}</span>
                          <Tag color={roleColor(item.role)}>{item.stepLabel}</Tag>
                        </Space>
                      }
                      description={
                        <Text type="secondary">
                          {item.projectNumber} · {trackLabel(item.track)} · {roleLabel(item.role)}
                        </Text>
                      }
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default HomePage;
