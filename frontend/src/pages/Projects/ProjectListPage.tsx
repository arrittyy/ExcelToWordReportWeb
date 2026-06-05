import React, { useEffect, useMemo, useState } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Typography,
  Tag,
  message,
  Input,
  Dropdown,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  FileWordOutlined,
  SearchOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectService, type WordExportJob } from '@/services/projectService';
import { userService } from '@/services/userService';
import type { ProjectList } from '@/types';
import dayjs from 'dayjs';
import { waitForWordExportJob } from '@/utils/wordExportJob';

const { Title } = Typography;

/** 列表页拉取各项目 SUMMARY 最新任务时限制并发，避免瞬时请求过多（弱网/ECS 上尤为重要） */
const SUMMARY_LATEST_FETCH_CHUNK = 6;

/** 项目列表缓存时间：避免从详情返回时反复 refetch 导致同一批 ID 重复拉「最新导出任务」 */
const PROJECTS_LIST_STALE_MS = 120_000;

const ProjectListPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const [searchText, setSearchText] = useState('');
  const [summaryJobs, setSummaryJobs] = useState<Record<number, WordExportJob>>({});
  const [summaryGenerating, setSummaryGenerating] = useState<Record<number, boolean>>({});
  const userId = searchParams.get('userId');

  // 获取用户列表（用于显示用户名）
  const { data: users = [] } = useQuery({
    queryKey: ['users'],
    queryFn: userService.getAllUsers,
    enabled: !!userId, // 只在有userId时获取用户列表
  });

  // 根据是否有userId决定调用哪个API
  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['projects', userId],
    queryFn: () => {
      if (userId) {
        return projectService.getByUserId(userId);
      }
      return projectService.getAll();
    },
    staleTime: PROJECTS_LIST_STALE_MS,
  });

  // 获取当前查看的用户信息
  const currentUser = userId ? users.find(u => u.id === userId) : null;

  /** 仅用「项目 ID 集合」驱动拉取：React Query 常返回新数组引用，避免同一批 ID 反复触发 N 次 latest 请求（本地快、ECS 上易卡死） */
  const projectIdsKey = useMemo(
    () =>
      [...projects.map((p) => p.id)]
        .sort((a, b) => a - b)
        .join(','),
    [projects]
  );

  useEffect(() => {
    let cancelled = false;
    const ids =
      projectIdsKey.length === 0 ? [] : projectIdsKey.split(',').map((s) => Number(s));

    (async () => {
      const entries: (readonly [number, WordExportJob | null])[] = [];
      for (let i = 0; i < ids.length; i += SUMMARY_LATEST_FETCH_CHUNK) {
        if (cancelled) return;
        const chunk = ids.slice(i, i + SUMMARY_LATEST_FETCH_CHUNK);
        const part = await Promise.all(
          chunk.map(async (pid) => {
            try {
              const latest = await projectService.getLatestWordExportJob(pid, 'SUMMARY');
              return [pid, latest] as const;
            } catch {
              return [pid, null] as const;
            }
          })
        );
        entries.push(...part);
        // 让浏览器有机会重绘，减轻 ECS 弱网下长时间并发导致的界面假死感
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
      }
      if (cancelled) return;
      const mapped: Record<number, WordExportJob> = {};
      entries.forEach(([id, job]) => {
        if (job) mapped[id] = job;
      });
      setSummaryJobs(mapped);
    })();
    return () => {
      cancelled = true;
    };
  }, [projectIdsKey]);

  const deleteMutation = useMutation({
    mutationFn: projectService.delete,
    onSuccess: () => {
      message.success('项目删除成功');
      queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
    onError: () => {
      message.error('删除失败');
    },
  });

  const handlePrepareSummaryWord = async (project: ProjectList) => {
    const key = `word-summary-project-${project.id}`;
    setSummaryGenerating((prev) => ({ ...prev, [project.id]: true }));
    try {
      message.loading({ content: '正在创建导出任务...', key, duration: 0 });
      const created = await projectService.createWordExportJob(project.id, { type: 'SUMMARY' });
      setSummaryJobs((prev) => ({ ...prev, [project.id]: created }));
      message.loading({ content: '后台正在生成项目总报告，请稍候...', key, duration: 0 });
      const finalJob = await waitForWordExportJob(project.id, created.jobId);
      setSummaryJobs((prev) => ({ ...prev, [project.id]: finalJob }));
      if (finalJob.status === 'SUCCEEDED') {
        message.success({ content: '项目总报告已生成，可点击下载', key });
      } else {
        throw new Error(finalJob.errorMessage || '项目总报告生成失败');
      }
    } catch (error: any) {
      message.error({ content: error?.message || '生成失败，请重试', key });
    } finally {
      setSummaryGenerating((prev) => ({ ...prev, [project.id]: false }));
    }
  };

  const isSummaryRunning = (projectId: number) => {
    const status = summaryJobs[projectId]?.status;
    return status === 'PENDING' || status === 'RUNNING';
  };

  const handleDownloadSummaryWord = async (project: ProjectList) => {
    const key = `word-summary-project-download-${project.id}`;
    const job = summaryJobs[project.id];
    if (!job || job.status !== 'SUCCEEDED') {
      message.warning('请先预生成并等待完成');
      return;
    }
    try {
      message.loading({ content: '正在下载项目总报告...', key, duration: 0 });
      const blob = await projectService.downloadWordExportJob(project.id, job.jobId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = job.suggestedFileName || `${project.projectNumber}_${dayjs().format('YYYY-MM-DD_HHmmss')}_总报告.docx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      message.success({ content: '项目总报告下载成功', key });
    } catch (error: any) {
      message.error({ content: error?.message || '下载失败，请重试', key });
    }
  };

  const columns: ColumnsType<ProjectList> = [
    {
      title: '客户方',
      width: 270,
      dataIndex: 'customer',
      key: 'customer',
      ellipsis: true,
      filteredValue: [searchText],
      onFilter: (value: any, record: ProjectList) => {
        const search = value.toString().toLowerCase();
        return (
          record.projectNumber.toLowerCase().includes(search) ||
          record.projectName.toLowerCase().includes(search) ||
          (record.customer || '').toLowerCase().includes(search) ||
          (record.responsiblePerson || '').toLowerCase().includes(search)
        );
      },
      render: (text: string | undefined) => (text && text.trim() !== '' ? text : '—'),
    },
    {
      title: '项目名称',
  
      dataIndex: 'projectName',
      key: 'projectName',
      ellipsis: true,
    },
    {
      title: '项目负责人',
      dataIndex: 'responsiblePerson',
      key: 'responsiblePerson',
      width: 120,
      ellipsis: true,
      render: (text: string | undefined) => (text && text.trim() !== '' ? text : '—'),
    },
    {
      title: '项目编号',
      dataIndex: 'projectNumber',
      key: 'projectNumber',
      width: 180,
    },
    {
      title: '开始日期',
      dataIndex: 'startDate',
      key: 'startDate',
      width: 110,
      render: (date: string) => dayjs(date).format('YYYY-MM-DD'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (status: string) => (
        <Tag color={status === 'Completed' ? 'success' : 'processing'}>
          {status === 'Completed' ? '已完成' : '进行中'}
        </Tag>
      ),
    },
    {
      title: '报告数',
      dataIndex: 'reportCount',
      key: 'reportCount',
      width: 80,
      align: 'center' as const,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right' as const,
      render: (_: any, record: ProjectList) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/projects/${record.id}`)}
          >
            查看
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => navigate(`/projects/${record.id}/edit`)}
          >
            编辑
          </Button>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'prepare-generate',
                  label: summaryGenerating[record.id] || isSummaryRunning(record.id) ? '总报告生成中...' : '预生成总报告',
                  icon: <FileWordOutlined />,
                  disabled: summaryGenerating[record.id] || isSummaryRunning(record.id),
                  onClick: () => handlePrepareSummaryWord(record),
                },
                {
                  key: 'download-generate',
                  label: summaryJobs[record.id]?.status === 'SUCCEEDED' ? '下载总报告' : '下载总报告（未就绪）',
                  icon: <FileWordOutlined />,
                  disabled: summaryJobs[record.id]?.status !== 'SUCCEEDED',
                  onClick: () => handleDownloadSummaryWord(record),
                },
                {
                  key: 'delete',
                  label: '删除项目',
                  icon: <DeleteOutlined />,
                  danger: true,
                  onClick: () => {
                    const confirmed = window.confirm('删除项目会同时删除该项目下的所有报告，确定要删除吗？');
                    if (confirmed) {
                      deleteMutation.mutate(record.id);
                    }
                  },
                },
              ],
            }}
          >
            <Button type="link" size="small" icon={<MoreOutlined />}>
              更多
            </Button>
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Title level={2}>
          {currentUser ? `${currentUser.fullName || currentUser.username}的项目` : '项目管理'}
        </Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          size="large"
          onClick={() => navigate('/projects/new')}
        >
          创建新项目
        </Button>
      </div>

      <Card style={{ borderRadius: 20 }}>
        <div style={{ marginBottom: 16 }}>
          <Input
            placeholder="搜索项目编号、项目名称、客户方或项目负责人"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            allowClear
            style={{ width: 300 }}
          />
        </div>

        <Table
          columns={columns}
          dataSource={projects}
          rowKey="id"
          loading={isLoading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 个项目`,
          }}
        />
      </Card>
    </div>
  );
};

export default ProjectListPage;