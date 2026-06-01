import React from 'react';
import { Modal, Typography, Spin, Empty } from 'antd';
import type { ProjectOverviewPreview } from '@/types';

const { Title, Text, Paragraph } = Typography;

interface OverviewPreviewModalProps {
  open: boolean;
  loading: boolean;
  data: ProjectOverviewPreview | null;
  onClose: () => void;
}

const OverviewPreviewModal: React.FC<OverviewPreviewModalProps> = ({
  open,
  loading,
  data,
  onClose,
}) => (
  <Modal
    title="概述预览"
    open={open}
    onCancel={onClose}
    footer={null}
    width={960}
    destroyOnClose
    styles={{ body: { maxHeight: '70vh', overflow: 'auto', paddingTop: 8 } }}
  >
    {loading ? (
      <div style={{ textAlign: 'center', padding: 48 }}>
        <Spin tip="正在加载概述…" />
      </div>
    ) : !data ? (
      <Empty description="暂无概述数据" />
    ) : (
      <Typography>
        {data.abstractParagraph?.trim() && (
          <Paragraph style={{ textIndent: '2em', marginBottom: 24 }}>{data.abstractParagraph}</Paragraph>
        )}

        <Title level={4} style={{ marginTop: 0 }}>1 概述</Title>
        <Paragraph style={{ textIndent: '2em' }}>
          {data.section1Body?.trim() || '（无项目描述）'}
        </Paragraph>

        {data.showChapter2 && (
          <>
            <Title level={4}>2 发现问题及处理情况</Title>
            {data.categories.map(cat =>
              cat.chapter2Components.length > 0 ? (
                <div key={`ch2-${cat.category}`} style={{ marginBottom: 16 }}>
                  <Title level={5} style={{ marginBottom: 8 }}>
                    2.{cat.chapter2CategoryIndex || cat.categoryIndex} {cat.category}
                  </Title>
                  {cat.chapter2Components.map(comp => (
                    <div key={`ch2-${cat.category}-${comp.componentName}`} style={{ marginBottom: 12, paddingLeft: 16 }}>
                      <Text strong>
                        2.{cat.chapter2CategoryIndex || cat.categoryIndex}.{comp.componentIndex} {comp.componentName}
                      </Text>
                      {comp.items.map(item => (
                        <Paragraph key={item.number} style={{ textIndent: '2em', marginBottom: 8, marginTop: 8 }}>
                          <Text strong>{item.number}</Text> {item.text}
                        </Paragraph>
                      ))}
                    </div>
                  ))}
                </div>
              ) : null,
            )}
          </>
        )}

        <Title level={4}>3 工作内容</Title>
        {data.categories.every(c => c.chapter3Components.length === 0) ? (
          <Paragraph type="secondary">（无工作内容条目）</Paragraph>
        ) : (
          data.categories.map(cat =>
            cat.chapter3Components.length > 0 ? (
              <div key={`ch3-${cat.category}`} style={{ marginBottom: 16 }}>
                <Title level={5} style={{ marginBottom: 8 }}>
                  3.{cat.chapter3CategoryIndex || cat.categoryIndex} {cat.category}
                </Title>
                {cat.chapter3Components.map(comp => (
                  <div key={`ch3-${cat.category}-${comp.componentName}`} style={{ marginBottom: 12, paddingLeft: 16 }}>
                    <Text strong>
                      3.{cat.chapter3CategoryIndex || cat.categoryIndex}.{comp.componentIndex} {comp.componentName}
                    </Text>
                    {comp.items.map(item => (
                      <Paragraph key={item.number} style={{ textIndent: '2em', marginBottom: 8, marginTop: 8 }}>
                        <Text strong>{item.number}</Text> {item.text}
                      </Paragraph>
                    ))}
                  </div>
                ))}
              </div>
            ) : null,
          )
        )}
      </Typography>
    )}
  </Modal>
);

export default OverviewPreviewModal;
