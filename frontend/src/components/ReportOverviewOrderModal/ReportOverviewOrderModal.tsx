import React, { useCallback, useEffect, useState } from 'react';
import { Modal, Button, Typography, message, Row, Col } from 'antd';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { HolderOutlined } from '@ant-design/icons';
import type { ReportList } from '@/types';
import type { ProjectComponent } from '@/services/componentService';
import { projectService } from '@/services/projectService';
import { formatMultiComponentDisplay } from '@/utils/reportComponentDisplay';
import {
  mergeSavedOrder,
  buildCategoryStructureFromMergedOrder,
  componentGroupKey,
  compareReportsForAggregateWithCustomOrder,
  buildDefaultExperimentTypeOrderFromReports,
  buildProjectScopedExperimentTypeOrder,
} from '@/utils/aggregateDetectionLogOrder';

const { Text, Paragraph } = Typography;

const CAT_PREFIX = 'cat:';
const CK_PREFIX = 'ck:';
const ETN_PREFIX = 'etn:';

function etDndId(name: string): string {
  return `${ETN_PREFIX}${encodeURIComponent(name)}`;
}
function parseEtDndName(id: string): string | null {
  if (!id.startsWith(ETN_PREFIX)) return null;
  try {
    return decodeURIComponent(id.slice(ETN_PREFIX.length));
  } catch {
    return null;
  }
}
function parseCatDndId(id: string): string | null {
  if (!id.startsWith(CAT_PREFIX)) return null;
  return id.slice(CAT_PREFIX.length);
}
function catDndId(category: string): string {
  return `${CAT_PREFIX}${category}`;
}
function parseCkDndId(id: string): string | null {
  if (!id.startsWith(CK_PREFIX)) return null;
  try {
    return decodeURIComponent(id.slice(CK_PREFIX.length));
  } catch {
    return null;
  }
}
function ckDndId(groupKey: string): string {
  return `${CK_PREFIX}${encodeURIComponent(groupKey)}`;
}

function SortableRow({
  id,
  children,
  style,
}: {
  id: string;
  children: React.ReactNode;
  style?: React.CSSProperties;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id });
  const rowStyle: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.75 : 1,
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    padding: '8px 10px',
    marginBottom: 6,
    background: '#fafafa',
    borderRadius: 8,
    border: '1px solid #f0f0f0',
    ...style,
  };
  return (
    <div ref={setNodeRef} style={rowStyle}>
      <span
        {...attributes}
        {...listeners}
        style={{ cursor: 'grab', color: '#8c8c8c', touchAction: 'none' }}
        aria-label="拖拽排序"
      >
        <HolderOutlined />
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>{children}</div>
    </div>
  );
}

function CategoryKeysSortBlock({
  cat,
  keys,
  groupTitle,
  onReorder,
}: {
  cat: string;
  keys: string[];
  groupTitle: (gk: string) => string;
  onReorder: (category: string, nextKeys: string[]) => void;
}) {
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );
  const ckIds = keys.map(ckDndId);
  const onDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const aid = String(active.id);
    const oid = String(over.id);
    if (!aid.startsWith(CK_PREFIX) || !oid.startsWith(CK_PREFIX)) return;
    const keyA = parseCkDndId(aid);
    const keyB = parseCkDndId(oid);
    if (keyA == null || keyB == null) return;
    if (!keys.includes(keyA) || !keys.includes(keyB)) return;
    const oldIndex = keys.indexOf(keyA);
    const newIndex = keys.indexOf(keyB);
    if (oldIndex < 0 || newIndex < 0) return;
    onReorder(cat, arrayMove([...keys], oldIndex, newIndex));
  };
  return (
    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
      <SortableContext items={ckIds} strategy={verticalListSortingStrategy}>
        {keys.map((gk) => (
          <SortableRow key={ckDndId(gk)} id={ckDndId(gk)}>
            <Text>{groupTitle(gk)}</Text>
          </SortableRow>
        ))}
      </SortableContext>
    </DndContext>
  );
}

export interface ReportOverviewOrderModalProps {
  open: boolean;
  onClose: () => void;
  projectId: number;
  reports: ReportList[];
  components?: ProjectComponent[];
  aggregateDetectionLogOrderJson?: string | null;
  onSaved?: () => void;
}

const ReportOverviewOrderModal: React.FC<ReportOverviewOrderModalProps> = ({
  open,
  onClose,
  projectId,
  reports,
  components = [],
  aggregateDetectionLogOrderJson,
  onSaved,
}) => {
  const [categoryOrder, setCategoryOrder] = useState<string[]>([]);
  const [keysByCategory, setKeysByCategory] = useState<Record<string, string[]>>({});
  const [experimentTypeOrder, setExperimentTypeOrder] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  const sensorsCategory = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );
  const sensorsType = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const resetFromProps = useCallback(() => {
    const merged = mergeSavedOrder(reports || [], aggregateDetectionLogOrderJson, components);
    const { categoryOrder: co, componentKeysByCategory } = buildCategoryStructureFromMergedOrder(
      merged,
      components,
    );
    setCategoryOrder(co);
    setKeysByCategory({ ...componentKeysByCategory });
    const fullTypeOrder = merged.experimentTypeOrder?.length
      ? [...merged.experimentTypeOrder]
      : buildDefaultExperimentTypeOrderFromReports(reports || []);
    setExperimentTypeOrder(buildProjectScopedExperimentTypeOrder(fullTypeOrder, reports || []));
  }, [reports, aggregateDetectionLogOrderJson, components]);

  useEffect(() => {
    if (open) resetFromProps();
  }, [open, resetFromProps]);

  const groupTitle = useCallback(
    (groupKey: string): string => {
      const rep = (reports || []).find((r) => r.id != null && componentGroupKey(r) === groupKey);
      if (!rep) return groupKey === 'none' ? '未指定部件' : groupKey;
      if (!rep.projectComponentIds?.length && rep.projectComponentId == null) {
        return '未指定部件';
      }
      return formatMultiComponentDisplay(rep.projectComponentIds, rep.projectComponentId, components);
    },
    [reports, components],
  );

  const onCategoryDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const aid = String(active.id);
    const oid = String(over.id);
    if (!aid.startsWith(CAT_PREFIX) || !oid.startsWith(CAT_PREFIX)) return;
    const a = parseCatDndId(aid);
    const b = parseCatDndId(oid);
    if (a == null || b == null) return;
    const oldIndex = categoryOrder.indexOf(a);
    const newIndex = categoryOrder.indexOf(b);
    if (oldIndex < 0 || newIndex < 0) return;
    setCategoryOrder(arrayMove(categoryOrder, oldIndex, newIndex));
  };

  const reorderKeysInCategory = (category: string, nextKeys: string[]) => {
    setKeysByCategory((prev) => ({ ...prev, [category]: nextKeys }));
  };

  const onTypeDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const aid = String(active.id);
    const oid = String(over.id);
    if (!aid.startsWith(ETN_PREFIX) || !oid.startsWith(ETN_PREFIX)) return;
    const nameA = parseEtDndName(aid);
    const nameB = parseEtDndName(oid);
    if (nameA == null || nameB == null) return;
    setExperimentTypeOrder((prev) => {
      const oldIndex = prev.indexOf(nameA);
      const newIndex = prev.indexOf(nameB);
      if (oldIndex < 0 || newIndex < 0) return prev;
      return arrayMove(prev, oldIndex, newIndex);
    });
  };

  const handleSave = async () => {
    const componentKeys = categoryOrder.flatMap((c) => keysByCategory[c] || []);
    const withId = (reports || []).filter((r) => r.id != null);
    const reportIdsByComponent: Record<string, number[]> = {};
    const eto = [...experimentTypeOrder];
    for (const key of componentKeys) {
      const rows = withId.filter((r) => componentGroupKey(r) === key);
      rows.sort((a, b) => compareReportsForAggregateWithCustomOrder(a, b, eto));
      reportIdsByComponent[key] = rows.map((r) => r.id as number);
    }
    setSaving(true);
    try {
      await projectService.saveAggregateDetectionLogOrder(projectId, {
        version: 4,
        componentKeys,
        reportIdsByComponent,
        experimentTypeOrder: eto,
      });
      message.success('报告顺序已保存');
      onSaved?.();
      onClose();
    } catch (e: unknown) {
      const msg =
        (e as { response?: { data?: { message?: string } } })?.response?.data?.message || '保存失败';
      message.error(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title="调整报告顺序"
      open={open}
      onCancel={onClose}
      width={920}
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={onClose}>
          取消
        </Button>,
        <Button key="save" type="primary" loading={saving} onClick={handleSave}>
          保存
        </Button>,
      ]}
    >
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        顶部左侧调整「类别」顺序，右侧调整「检测类型顺序」（仅列出本项目报告中已出现的检测类型）；下方按类别调整「部件组」顺序。保存后与总报告、报告编号及总检测日志导出顺序一致。
      </Paragraph>

      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col xs={24} md={12}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>
            类别顺序
          </Text>
          <DndContext sensors={sensorsCategory} collisionDetection={closestCenter} onDragEnd={onCategoryDragEnd}>
            <SortableContext items={categoryOrder.map(catDndId)} strategy={verticalListSortingStrategy}>
              <div>
                {categoryOrder.map((cat) => (
                  <SortableRow key={catDndId(cat)} id={catDndId(cat)}>
                    <Text strong>{cat}</Text>
                  </SortableRow>
                ))}
              </div>
            </SortableContext>
          </DndContext>
        </Col>
        <Col xs={24} md={12}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>
            检测类型顺序（全局）
          </Text>
          {experimentTypeOrder.length === 0 ? (
            <Text type="secondary">暂无带检测类型名称的报告</Text>
          ) : (
            <DndContext sensors={sensorsType} collisionDetection={closestCenter} onDragEnd={onTypeDragEnd}>
              <SortableContext items={experimentTypeOrder.map(etDndId)} strategy={verticalListSortingStrategy}>
                {experimentTypeOrder.map((name) => (
                  <SortableRow key={etDndId(name)} id={etDndId(name)}>
                    <Text>{name}</Text>
                  </SortableRow>
                ))}
              </SortableContext>
            </DndContext>
          )}
        </Col>
      </Row>

      <Text strong style={{ display: 'block', marginBottom: 10 }}>
        部件组顺序（按类别）
      </Text>
      {categoryOrder.map((cat) => {
        const keys = keysByCategory[cat] || [];
        return (
          <div key={cat} style={{ marginBottom: 16 }}>
            <Text strong style={{ display: 'block', marginBottom: 6 }}>
              {cat} — 部件组顺序
            </Text>
            <CategoryKeysSortBlock cat={cat} keys={keys} groupTitle={groupTitle} onReorder={reorderKeysInCategory} />
          </div>
        );
      })}
    </Modal>
  );
};

export default ReportOverviewOrderModal;
