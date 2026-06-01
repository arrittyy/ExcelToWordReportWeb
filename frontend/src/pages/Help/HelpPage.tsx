import React from 'react';
import { Typography, Card } from 'antd';
import { CheckCircleOutlined } from '@ant-design/icons';

const { Title, Paragraph } = Typography;

const HelpPage: React.FC = () => {
  return (
    <div>
      <Title level={2}>帮助文档</Title>
      <Paragraph type="secondary">
        欢迎使用自动报告系统，以下是常见问题和使用说明。
      </Paragraph>

      <Card
        title={
          <span>
            <CheckCircleOutlined style={{ marginRight: 8, color: '#52c41a' }} />
            快速开始
          </span>
        }
        style={{ marginBottom: 16 }}
      >
        <Title level={4}>创建报告的步骤</Title>
        <ol>
          <li>点击"报告管理"或首页的"创建新报告"按钮</li>
          <li>填写报告基本信息（报告编号、项目名称、检测日期、人员）</li>
          <li>添加部件信息、设备信息</li>
          <li>点击"添加检测项"选择实验类型（如磁粉检测、渗透检测等）</li>
          <li>在动态表格中填写检测数据</li>
          <li>上传相关图片并关联到对应的检测记录</li>
          <li>保存报告（草稿或完成状态）</li>
          <li>点击"生成Word"下载报告文档</li>
        </ol>
      </Card>

     
      
    </div>
  );
};

export default HelpPage;


