import type { CSSProperties } from 'react';
import { useEffect, useState } from 'react';
import { Modal, Table, Select, Space } from 'antd';
import { reportService } from '@/services/reportService';

const FIELD_LEEB_CATEGORY = '里氏分类';

/** 管件 / 钢管 / 焊缝 三色标注（与表格行、Tag 一致） */
export function leebCategoryTagColor(category: string): string {
  switch (category) {
    case '管件':
      return 'blue';
    case '钢管':
      return 'green';
    case '焊缝':
      return 'volcano';
    default:
      return 'default';
  }
}

/** 下拉框内勿用 Tag（会与 ant-select 叠加成多重框线），用纯色块即可 */
function categoryPillStyle(category: string): CSSProperties {
  const base: CSSProperties = {
    display: 'inline-block',
    padding: '0 6px',
    fontSize: 12,
    lineHeight: '20px',
    borderRadius: 4,
    verticalAlign: 'middle',
  };
  switch (category) {
    case '管件':
      return { ...base, background: '#e6f4ff', color: '#1677ff' };
    case '钢管':
      return { ...base, background: '#f6ffed', color: '#389e0d' };
    case '焊缝':
      return { ...base, background: '#fff7e6', color: '#d46b08' };
    default:
      return { ...base, background: '#f5f5f5', color: '#666' };
  }
}

function CategoryPill({ category }: { category: string }) {
  return <span style={categoryPillStyle(category)}>{category}</span>;
}

function leebCategoryRowBackground(category: string): string {
  switch (category) {
    case '管件':
      return 'rgba(22, 119, 255, 0.08)';
    case '钢管':
      return 'rgba(82, 196, 26, 0.1)';
    case '焊缝':
      return 'rgba(250, 140, 22, 0.1)';
    default:
      return 'transparent';
  }
}

const CATEGORY_OPTIONS = [
  { value: '管件', label: '管件' },
  { value: '钢管', label: '钢管' },
  { value: '焊缝', label: '焊缝' },
];

export type TableRowRec = Record<string, unknown>;

export interface LeebHardnessCategorySaveModalProps {
  open: boolean;
  sourceRows: TableRowRec[];
  onCancel: () => void;
  /** 用户确认后的完整行数据（已写入里氏分类） */
  onConfirm: (mergedRows: TableRowRec[]) => void;
}

type DraftRow = {
  rowIndex: number;
  number: string;
  suggested: string;
  confirmed: string;
};

export default function LeebHardnessCategorySaveModal({
  open,
  sourceRows,
  onCancel,
  onConfirm,
}: LeebHardnessCategorySaveModalProps) {
  const [loading, setLoading] = useState(false);
  const [draft, setDraft] = useState<DraftRow[]>([]);

  useEffect(() => {
    if (!open) {
      setDraft([]);
      return;
    }

    const indexed: { rowIndex: number; number: string }[] = [];
    sourceRows.forEach((r, rowIndex) => {
      const n = String(r['编号'] ?? '').trim();
      if (n.length > 0 && n !== '/') {
        indexed.push({ rowIndex, number: n });
      }
    });

    if (indexed.length === 0) {
      setDraft([]);
      return;
    }

    let cancelled = false;
    setLoading(true);

    reportService
      .leebClassifySuggestions(indexed.map((x) => x.number))
      .then((resp) => {
        if (cancelled) return;
        setDraft(
          indexed.map((item, j) => {
            const sug = resp[j]?.suggestedCategory ?? '焊缝';
            const existing = String(sourceRows[item.rowIndex][FIELD_LEEB_CATEGORY] ?? '').trim();
            const valid = CATEGORY_OPTIONS.some((o) => o.value === existing);
            return {
              rowIndex: item.rowIndex,
              number: item.number,
              suggested: sug,
              confirmed: valid ? existing : sug,
            };
          }),
        );
      })
      .catch(() => {
        if (cancelled) return;
        setDraft(
          indexed.map((item) => {
            const existing = String(sourceRows[item.rowIndex][FIELD_LEEB_CATEGORY] ?? '').trim();
            const valid = CATEGORY_OPTIONS.some((o) => o.value === existing);
            return {
              rowIndex: item.rowIndex,
              number: item.number,
              suggested: '焊缝',
              confirmed: valid ? existing : '焊缝',
            };
          }),
        );
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [open, sourceRows]);

  const handleOk = () => {
    const merged = sourceRows.map((r) => ({ ...r }));
    for (const d of draft) {
      merged[d.rowIndex] = { ...merged[d.rowIndex], [FIELD_LEEB_CATEGORY]: d.confirmed };
    }
    onConfirm(merged);
  };

  return (
    <Modal
      title="确认里氏硬度检测部位分类"
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      okText="确认并保存"
      cancelText="取消"
      confirmLoading={loading}
      okButtonProps={{ disabled: loading || draft.length === 0 }}
      width={680}
      destroyOnClose
      styles={{ body: { paddingTop: 12, paddingBottom: 12 } }}
    >
      <p style={{ margin: '0 0 6px', color: '#666', fontSize: 12, lineHeight: 1.45 }}>
        请核对每行编号对应的部位类型。确认后将写入「里氏分类」并保存检测数据。
      </p>
      <Space size={[8, 4]} wrap style={{ marginBottom: 8 }}>
        <span style={{ color: '#888', fontSize: 12 }}>颜色：</span>
        <CategoryPill category="管件" />
        <CategoryPill category="钢管" />
        <CategoryPill category="焊缝" />
      </Space>
      <Table<DraftRow>
        size="small"
        loading={loading}
        pagination={false}
        rowKey={(r) => String(r.rowIndex)}
        dataSource={draft}
        onRow={(record) => ({
          style: {
            backgroundColor: leebCategoryRowBackground(record.confirmed),
          },
        })}
        columns={[
          { title: '编号', dataIndex: 'number', ellipsis: true, width: 100 },
          {
            title: '建议分类',
            dataIndex: 'suggested',
            width: 88,
            render: (v: string) => <CategoryPill category={v} />,
          },
          {
            title: '确认分类',
            dataIndex: 'confirmed',
            width: 100,
            render: (_: unknown, record: DraftRow, index: number) => (
              <Select
                size="small"
                style={{ width: '100%', minWidth: 112 }}
                value={record.confirmed}
                options={CATEGORY_OPTIONS}
                labelRender={(props) => (
                  <CategoryPill category={String(props.value)} />
                )}
                optionRender={(ori) => (
                  <CategoryPill category={String(ori.value)} />
                )}
                popupMatchSelectWidth={false}
                listHeight={128}
                onChange={(v) => {
                  setDraft((prev) => {
                    const next = [...prev];
                    next[index] = { ...next[index], confirmed: v };
                    return next;
                  });
                }}
              />
            ),
          },
        ]}
      />
    </Modal>
  );
}
