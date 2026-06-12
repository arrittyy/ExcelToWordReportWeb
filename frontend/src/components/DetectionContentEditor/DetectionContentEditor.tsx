import React, { useState } from 'react';
import { Table, Button, Select, Input, Popconfirm, Space, Tooltip } from 'antd';
import { PlusOutlined, DeleteOutlined, SaveOutlined, EditOutlined, CheckOutlined } from '@ant-design/icons';
import { resolveDetectionContentRowComponentId } from '@/utils/detectionContentRowComponent';
import {
  DetectionContentPayload,
  DetectionContentTablePayload,
  DetectionContentTableRow,
  DetectionContentDualTextareaPayload,
  DetectionContentTextareaPayload,
  DetectionContentSingleFieldPayload,
  DetectionContentVisualGroupsPayload,
  DetectionContentSodPayload,
} from '@/types';
import ImageUpload from '@/components/ImageUpload/ImageUpload';

const { TextArea } = Input;

/** 与 ProjectDetailPage CUSTOM_TYPE_OPTION_LABEL 一致 */
const CUSTOM_TYPE_LABEL = '自定义';
const CUSTOM_SELECT_VALUE = '__CUSTOM__';

export type DetectionContentMode = DetectionContentPayload['mode'];

export type DetectionContentReadOnlyField = 'type' | 'locationNumber' | 'total';

/** dual-textarea 附加下拉（如金相浸蚀剂） */
export interface DetectionContentExtraSelectConfig {
  key: 'etchant';
  label: string;
  options: string[];
  allowCustom?: boolean;
}

export interface DetectionContentConfig {
  mode: DetectionContentMode;
  typeOptions?: string[];
  extraSelect?: DetectionContentExtraSelectConfig;
  /** 自动导入只读字段：类型、位置编号、总计在保存检测数据时由后端填充，不可修改 */
  readOnlyFields?: DetectionContentReadOnlyField[];
  /** 超声波测厚：检测内容中必填整份「最小需要厚度」，位于类型与检测数据之间 */
  requireMinRequiredThickness?: boolean;
  tableOptions?: {
    showType?: boolean;
    showLocationDesc?: boolean;
    showLocationNumber?: boolean;
    showTotal?: boolean;
    showResult?: boolean;
    /** 在「检测位置」与「检测结果」之间显示检测方式列（大段 TextArea） */
    showDetectionMethod?: boolean;
    methodLabel?: string;
    locationDescLabel?: string;
    resultLabel?: string;
    /** 检测内容表「类型」列宽（px），默认 220；内窥镜等可收窄 */
    typeColumnWidth?: number;
  };
  labels?: {
    position?: string;
    conclusion?: string;
    single?: string;
  };
  placeholders?: {
    position?: string;
    conclusion?: string;
    single?: string;
  };
}

interface DetectionContentEditorProps {
  value: DetectionContentPayload;
  config: DetectionContentConfig;
  onChange: (value: DetectionContentPayload) => void;
  onSave?: (value: DetectionContentPayload) => void;
  disabled?: boolean;
  /** 标题栏「文本预览」：常驻显示，点击时调用（未传则无操作） */
  onTextPreview?: () => void;
  /** 每行首列「检测数据」入口（仅 table 模式） */
  detectionDataButton?: {
    onOpen: (contentRowIndex: number) => void;
  };
  /** 多选部件：每行从已选部件下拉绑定 projectComponentId */
  multiRowComponentSelect?: {
    selectedIds: number[];
    options: { label: string; value: number }[];
  };
  /** 只读编号/总计时，从检测数据实时推算的预览值（保存前展示） */
  autoFillPreview?: (rowIndex: number) => { locationNumber?: string; total?: string };
}

const ensureValueMatchesMode = (
  value: DetectionContentPayload,
  config: DetectionContentConfig,
): DetectionContentPayload => {
  if (value?.mode === config.mode) {
    return value;
  }

  switch (config.mode) {
    case 'table':
      return { mode: 'table', rows: [{ type: '', locationDesc: '', method: '', result: '', locationNumber: '', total: '', minRequiredThickness: '' }] };
    case 'dual-textarea':
      return { mode: 'dual-textarea', position: '', conclusion: '', etchant: '' };
    case 'textarea':
      return { mode: 'textarea', conclusion: '' };
    case 'single':
      return { mode: 'single', value: '' };
    case 'visual-groups':
      return { mode: 'visual-groups', numberingRule: '', groups: [] };
    case 'sod':
      return { mode: 'sod', probeSpec: '', tubeSample: '', sensitivityCalibration: '', rows: [] };
    default:
      return value;
  }
};

const DetectionContentEditor: React.FC<DetectionContentEditorProps> = ({
  value,
  config,
  onChange,
  onSave,
  disabled = false,
  onTextPreview,
  detectionDataButton,
  multiRowComponentSelect,
  autoFillPreview,
}) => {
  const normalizedValue = ensureValueMatchesMode(value, config);
  const [editingRows, setEditingRows] = useState<Set<number>>(new Set());
  /** 类型为空时，仅当用户在下拉中点了「自定义」后才展示自定义输入框（避免一进编辑就等同选了自定义） */
  const [typeCustomExplicitRows, setTypeCustomExplicitRows] = useState<Set<number>>(new Set());
  /** 金相等 dual-textarea 附加下拉：用户点「自定义」后才展示输入框 */
  const [extraSelectCustomExplicit, setExtraSelectCustomExplicit] = useState(false);

  const shiftTypeCustomExplicitAfterDelete = (deletedIndex: number) => {
    setTypeCustomExplicitRows((prev) => {
      const next = new Set<number>();
      prev.forEach((i) => {
        if (i < deletedIndex) next.add(i);
        else if (i > deletedIndex) next.add(i - 1);
      });
      return next;
    });
  };

  const handleSave = () => {
    if (onSave) {
      onSave(normalizedValue);
    }
  };

  const renderTableEditor = (
    tableValue: DetectionContentTablePayload,
    overrideConfig?: DetectionContentConfig,
    overrideOnChange?: (value: DetectionContentPayload) => void,
    overrideOnSave?: (value: DetectionContentPayload) => void,
  ) => {
    const effectiveConfig = overrideConfig ?? config;
    const effectiveOnChange = overrideOnChange ?? onChange;
    const rows = tableValue.rows || [];
    const requireMinThickness = effectiveConfig.requireMinRequiredThickness ?? false;

    const updateRow = (index: number, field: keyof DetectionContentTableRow, fieldValue: string) => {
      const newRows = rows.map((row, idx) =>
        idx === index ? { ...row, [field]: fieldValue } : row
      );
      effectiveOnChange({ ...tableValue, rows: newRows });
    };

    const updateRowComponentId = (index: number, componentId: number | undefined) => {
      const newRows = rows.map((row, idx) => {
        if (idx !== index) return row;
        if (componentId == null || componentId <= 0) {
          const { projectComponentId: _removed, ...rest } = row;
          return rest;
        }
        return { ...row, projectComponentId: componentId };
      });
      effectiveOnChange({ ...tableValue, rows: newRows });
    };

    const emptyContentRow = (): DetectionContentTableRow => ({
      type: '',
      locationDesc: '',
      method: '',
      result: '',
      locationNumber: '',
      total: '',
      ...(requireMinThickness ? { minRequiredThickness: '' } : {}),
    });

    const handleAddRow = () => {
      const newIndex = rows.length;
      let added = emptyContentRow();
      if (
        multiRowComponentSelect &&
        newIndex < multiRowComponentSelect.selectedIds.length
      ) {
        added = { ...added, projectComponentId: multiRowComponentSelect.selectedIds[newIndex] };
      }
      const newRows = [...rows, added];
      effectiveOnChange({ ...tableValue, rows: newRows });
      setEditingRows(new Set([...editingRows, newIndex]));
    };

    const handleDeleteRow = (index: number) => {
      const newRows = rows.filter((_, idx) => idx !== index);
      effectiveOnChange({ ...tableValue, rows: newRows });
      shiftTypeCustomExplicitAfterDelete(index);
      // 删除后更新编辑状态
      const newEditingRows = new Set<number>();
      editingRows.forEach(rowIndex => {
        if (rowIndex < index) {
          newEditingRows.add(rowIndex);
        } else if (rowIndex > index) {
          newEditingRows.add(rowIndex - 1);
        }
      });
      setEditingRows(newEditingRows);
    };

    const handleEdit = (index: number) => {
      setEditingRows(new Set([...editingRows, index]));
    };

    const handleSaveRow = (index: number) => {
      // 退出编辑状态
      const newEditingRows = new Set(editingRows);
      newEditingRows.delete(index);
      setEditingRows(newEditingRows);
      // 与卡片右上角「保存」一致：把当前检测内容提交给父级（含持久化到后端）
      const nextValue: DetectionContentTablePayload = {
        mode: 'table',
        rows: [...rows],
      };
      if (overrideOnSave) {
        overrideOnSave(nextValue);
      } else if (onSave) {
        onSave(nextValue);
      }
    };

    const isEditing = (index: number) => editingRows.has(index);
    const readOnlyFields = effectiveConfig.readOnlyFields ?? [];
    const isLocationNumberReadOnly = readOnlyFields.includes('locationNumber');
    const isTotalReadOnly = readOnlyFields.includes('total');
    const isAutoFillReadOnly = readOnlyFields.length > 0;
    const tableOptions = effectiveConfig.tableOptions ?? {};
    const showType = tableOptions.showType ?? true;
    const showLocationDesc = tableOptions.showLocationDesc ?? true;
    const showLocationNumber = tableOptions.showLocationNumber ?? true;
    const showTotal = tableOptions.showTotal ?? true;
    const showResult = tableOptions.showResult ?? false;
    const showDetectionMethod = tableOptions.showDetectionMethod ?? false;
    const methodLabel = tableOptions.methodLabel || '检测方式';
    const locationDescLabel = tableOptions.locationDescLabel || '位置描述';
    const resultLabel = tableOptions.resultLabel || '检测结果';
    const typeColumnWidth = tableOptions.typeColumnWidth ?? 220;

    const typePresets = (effectiveConfig.typeOptions || []).filter((o) => o !== CUSTOM_TYPE_LABEL);

    const minRequiredColumn =
      requireMinThickness
        ? [
            {
              title: '最小需要厚度（必填）',
              key: 'minRequiredThickness',
              width: 148,
              render: (_: unknown, record: DetectionContentTableRow, index: number) => {
                const minStr = String(record.minRequiredThickness ?? '').trim();
                if (isEditing(index)) {
                  return (
                    <Input
                      size="small"
                      style={{ width: '100%' }}
                      inputMode="decimal"
                      placeholder="必填"
                      value={record.minRequiredThickness ?? ''}
                      disabled={disabled}
                      onChange={(e) => updateRow(index, 'minRequiredThickness', e.target.value)}
                    />
                  );
                }
                return minStr || '-';
              },
            },
          ]
        : [];

    const dataButtonColumn = detectionDataButton
      ? [
          {
            title: '检测数据',
            key: 'detectionDataEntry',
            width: 88,
            render: (_: any, record: DetectionContentTableRow, index: number) => {
              const typeReady = String(record.type ?? '').trim().length > 0;
              const btn = (
                <Button
                  type="link"
                  size="small"
                  disabled={disabled || !typeReady}
                  onClick={() => detectionDataButton.onOpen(index)}
                >
                  查看
                </Button>
              );
              if (!typeReady) {
                return (
                  <Tooltip title="请先在本行选择「类型」后再查看检测数据">
                    <span>{btn}</span>
                  </Tooltip>
                );
              }
              return btn;
            },
          },
        ]
      : [];

    const summaryColumn =
      multiRowComponentSelect && multiRowComponentSelect.selectedIds.length > 1
        ? [
            {
              title: '部件信息',
              key: 'componentSummary',
              width: 280,
              render: (_: unknown, record: DetectionContentTableRow, index: number) => {
                const resolvedId = resolveDetectionContentRowComponentId(
                  record,
                  index,
                  multiRowComponentSelect.selectedIds,
                );
                if (!isEditing(index)) {
                  const label = multiRowComponentSelect.options.find((o) => o.value === resolvedId)?.label;
                  return label || '-';
                }
                return (
                  <Select
                    size="small"
                    style={{ width: '100%' }}
                    allowClear
                    placeholder="请选择部件"
                    disabled={disabled}
                    value={resolvedId}
                    options={multiRowComponentSelect.options}
                    onChange={(v) => updateRowComponentId(index, v ?? undefined)}
                  />
                );
              },
            },
          ]
        : [];

    /** 列顺序：部件信息（如有）→ 类型 → 最小需要厚度 → 检测数据 → 其余字段 */
    const columns: any[] = [...summaryColumn];

    if (showType) {
      columns.push({
        title: '类型（必填项）',
        dataIndex: 'type',
        key: 'type',
        width: typeColumnWidth,
        render: (_: any, record: DetectionContentTableRow, index: number) => {
          if (isEditing(index)) {
            const rawType = record.type ?? '';
            const isPreset = typePresets.includes(rawType);
            const hasSavedCustom = !isPreset && rawType !== '';
            const explicitCustom = typeCustomExplicitRows.has(index);
            const showCustomInput = hasSavedCustom || explicitCustom;
            let selectValue: string | undefined;
            if (isPreset) {
              selectValue = rawType;
            } else if (hasSavedCustom) {
              selectValue = CUSTOM_SELECT_VALUE;
            } else if (explicitCustom) {
              selectValue = CUSTOM_SELECT_VALUE;
            } else {
              selectValue = undefined;
            }
            return (
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Select
                  size="small"
                  style={{ width: '100%' }}
                  placeholder="选择类型"
                  allowClear
                  options={[
                    ...typePresets.map((p) => ({ label: p, value: p })),
                    { label: CUSTOM_TYPE_LABEL, value: CUSTOM_SELECT_VALUE },
                  ]}
                  value={selectValue}
                  onChange={(val) => {
                    if (val === undefined || val === null) {
                      setTypeCustomExplicitRows((prev) => {
                        const n = new Set(prev);
                        n.delete(index);
                        return n;
                      });
                      updateRow(index, 'type', '');
                      return;
                    }
                    if (val === CUSTOM_SELECT_VALUE) {
                      setTypeCustomExplicitRows((prev) => new Set(prev).add(index));
                      updateRow(index, 'type', '');
                    } else {
                      setTypeCustomExplicitRows((prev) => {
                        const n = new Set(prev);
                        n.delete(index);
                        return n;
                      });
                      updateRow(index, 'type', val);
                    }
                  }}
                  dropdownMatchSelectWidth={false}
                  disabled={disabled}
                />
                {showCustomInput && (
                  <Input
                    size="small"
                    placeholder="请输入自定义类型"
                    value={rawType}
                    onChange={(e) => updateRow(index, 'type', e.target.value)}
                    disabled={disabled}
                  />
                )}
              </Space>
            );
          }
          return record.type || '-';
        },
      });
    }

    columns.push(...minRequiredColumn);
    columns.push(...dataButtonColumn);

    if (showLocationDesc) {
      columns.push({
        title: locationDescLabel,
        dataIndex: 'locationDesc',
        key: 'locationDesc',
        width: 220,
        render: (_: any, record: DetectionContentTableRow, index: number) => {
          if (isEditing(index)) {
            return (
              <TextArea
                size="small"
                placeholder={`请输入${locationDescLabel}`}
                value={record.locationDesc}
                onChange={(e) => updateRow(index, 'locationDesc', e.target.value)}
                disabled={disabled}
                autoSize={{ minRows: 2, maxRows: 16 }}
                style={{
                  width: '100%',
                  maxWidth: '100%',
                  boxSizing: 'border-box',
                  overflowWrap: 'anywhere',
                }}
              />
            );
          }
          if (!record.locationDesc?.trim()) {
            return '-';
          }
          return (
            <span
              style={{
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                overflowWrap: 'anywhere',
              }}
            >
              {record.locationDesc}
            </span>
          );
        },
      });
    }
    if (showDetectionMethod) {
      columns.push({
        title: methodLabel,
        dataIndex: 'method',
        key: 'method',
        width: 220,
        render: (_: any, record: DetectionContentTableRow, index: number) => {
          if (isEditing(index)) {
            return (
              <TextArea
                size="small"
                placeholder={`请输入${methodLabel}`}
                value={record.method}
                onChange={(e) => updateRow(index, 'method', e.target.value)}
                disabled={disabled}
                autoSize={{ minRows: 2, maxRows: 16 }}
                style={{
                  width: '100%',
                  maxWidth: '100%',
                  boxSizing: 'border-box',
                  overflowWrap: 'anywhere',
                }}
              />
            );
          }
          if (!record.method?.trim()) {
            return '-';
          }
          return (
            <span
              style={{
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                overflowWrap: 'anywhere',
              }}
            >
              {record.method}
            </span>
          );
        },
      });
    }
    if (showResult) {
      columns.push({
        title: resultLabel,
        dataIndex: 'result',
        key: 'result',
        width: 220,
        render: (_: any, record: DetectionContentTableRow, index: number) => {
          if (isEditing(index)) {
            return (
              <TextArea
                size="small"
                placeholder={`请输入${resultLabel}`}
                value={record.result}
                onChange={(e) => updateRow(index, 'result', e.target.value)}
                disabled={disabled}
                autoSize={{ minRows: 2, maxRows: 16 }}
                style={{
                  width: '100%',
                  maxWidth: '100%',
                  boxSizing: 'border-box',
                  overflowWrap: 'anywhere',
                }}
              />
            );
          }
          if (!record.result?.trim()) {
            return '-';
          }
          return (
            <span
              style={{
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                overflowWrap: 'anywhere',
              }}
            >
              {record.result}
            </span>
          );
        },
      });
    }
    if (showLocationNumber) {
      columns.push({
        title: '位置编号',
        dataIndex: 'locationNumber',
        key: 'locationNumber',
        width: 160,
        render: (_: any, record: DetectionContentTableRow, index: number) => {
          const preview = autoFillPreview?.(index);
          if (isLocationNumberReadOnly) {
            return record.locationNumber || preview?.locationNumber || '-';
          }
          if (isEditing(index)) {
            return (
              <Input
                size="small"
                placeholder="请输入编号"
                value={record.locationNumber}
                onChange={(e) => updateRow(index, 'locationNumber', e.target.value)}
                disabled={disabled}
              />
            );
          }
          return record.locationNumber || '-';
        },
      });
    }
    if (showTotal) {
      columns.push({
        title: '总计',
        dataIndex: 'total',
        key: 'total',
        width: 140,
        render: (_: any, record: DetectionContentTableRow, index: number) => {
          const preview = autoFillPreview?.(index);
          if (isTotalReadOnly) {
            return record.total || preview?.total || '-';
          }
          if (isEditing(index)) {
            return (
              <Input
                size="small"
                placeholder="请输入总计"
                value={record.total}
                onChange={(e) => updateRow(index, 'total', e.target.value)}
                disabled={disabled}
              />
            );
          }
          return record.total || '-';
        },
      });
    }
    columns.push({
        title: '操作',
        key: 'operation',
        width: 150,
        render: (_: any, __: DetectionContentTableRow, index: number) =>
          isAutoFillReadOnly ? (
            isEditing(index) ? (
              <Space size="small">
                <Button
                  type="link"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => handleSaveRow(index)}
                  disabled={disabled}
                >
                  保存
                </Button>
              </Space>
            ) : (
              <Space size="small">
                <Button
                  type="link"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => handleEdit(index)}
                  disabled={disabled}
                >
                  编辑
                </Button>
                {rows.length > 1 && (
                  <Popconfirm
                    title="确认删除该行？"
                    onConfirm={() => handleDeleteRow(index)}
                    disabled={disabled}
                  >
                    <Button
                      type="link"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      disabled={disabled}
                    >
                      删除
                    </Button>
                  </Popconfirm>
                )}
              </Space>
            )
          ) : (
            <Space size="small">
              {isEditing(index) ? (
                <Button
                  type="link"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => handleSaveRow(index)}
                  disabled={disabled}
                >
                  保存
                </Button>
              ) : (
                <Button
                  type="link"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => handleEdit(index)}
                  disabled={disabled}
                >
                  编辑
                </Button>
              )}
              <Popconfirm
                title="确认删除该行？"
                onConfirm={() => handleDeleteRow(index)}
                disabled={disabled}
              >
                <Button
                  type="link"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  disabled={disabled}
                >
                  删除
                </Button>
              </Popconfirm>
            </Space>
          ),
      });

    return (
      <div className="report-subcard-table">
        <Table
          size="small"
          tableLayout="fixed"
          scroll={{ x: detectionDataButton ? 968 : 880 }}
          pagination={false}
          dataSource={rows.map((row, index) => ({ ...row, key: index }))}
          columns={columns}
          bordered={false}
          locale={{
            emptyText: (
              <div style={{ padding: '16px 0', fontSize: '14px', color: '#999' }}>
                暂无数据
              </div>
            ),
          }}
          footer={() => (
            <div className="report-subcard-footer">
              <Button
                type="dashed"
                icon={<PlusOutlined />}
                onClick={handleAddRow}
                disabled={disabled}
                block
              >
                添加数据行
              </Button>
            </div>
          )}
        />
      </div>
    );
  };

  const renderDualTextarea = (dualValue: DetectionContentDualTextareaPayload) => {
    const positionLabel = config.labels?.position || '检测部位';
    const conclusionLabel = config.labels?.conclusion || '检测结论';
    const positionPlaceholder = config.placeholders?.position || `请输入${positionLabel}`;
    const conclusionPlaceholder = config.placeholders?.conclusion || `请输入${conclusionLabel}`;
    const extraSelect = config.extraSelect;

    const renderExtraSelect = () => {
      if (!extraSelect) return null;
      const presets = extraSelect.options;
      const raw =
        extraSelect.key === 'etchant' ? (dualValue.etchant ?? '').trim() : '';
      const isPreset = presets.includes(raw);
      const hasSavedCustom = !isPreset && raw !== '';
      const showCustomInput =
        extraSelect.allowCustom !== false && (hasSavedCustom || extraSelectCustomExplicit);
      let selectValue: string | undefined;
      if (isPreset) {
        selectValue = raw;
      } else if (hasSavedCustom || extraSelectCustomExplicit) {
        selectValue = CUSTOM_SELECT_VALUE;
      } else {
        selectValue = undefined;
      }

      return (
        <div>
          <div style={{ marginBottom: 4 }}>{extraSelect.label}</div>
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <Select
              size="small"
              style={{ width: '100%' }}
              placeholder={`请选择${extraSelect.label}`}
              allowClear
              disabled={disabled}
              options={[
                ...presets.map((p) => ({ label: p, value: p })),
                ...(extraSelect.allowCustom !== false
                  ? [{ label: CUSTOM_TYPE_LABEL, value: CUSTOM_SELECT_VALUE }]
                  : []),
              ]}
              value={selectValue}
              onChange={(val) => {
                if (val === undefined || val === null) {
                  setExtraSelectCustomExplicit(false);
                  onChange({ ...dualValue, etchant: '', mode: 'dual-textarea' });
                  return;
                }
                if (val === CUSTOM_SELECT_VALUE) {
                  setExtraSelectCustomExplicit(true);
                  onChange({ ...dualValue, etchant: '', mode: 'dual-textarea' });
                } else {
                  setExtraSelectCustomExplicit(false);
                  onChange({ ...dualValue, etchant: val, mode: 'dual-textarea' });
                }
              }}
            />
            {showCustomInput && (
              <Input
                size="small"
                placeholder={`请输入自定义${extraSelect.label}`}
                value={raw}
                disabled={disabled}
                onChange={(e) =>
                  onChange({ ...dualValue, etchant: e.target.value, mode: 'dual-textarea' })
                }
              />
            )}
          </Space>
        </div>
      );
    };

    return (
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        {renderExtraSelect()}
        <div>
          <div style={{ marginBottom: 4 }}>{positionLabel}</div>
          <TextArea
            rows={3}
            placeholder={positionPlaceholder}
            value={dualValue.position}
            onChange={(e) =>
              onChange({ ...dualValue, position: e.target.value, mode: 'dual-textarea' })
            }
          />
        </div>
        <div>
          <div style={{ marginBottom: 4 }}>{conclusionLabel}</div>
          <TextArea
            rows={3}
            placeholder={conclusionPlaceholder}
            value={dualValue.conclusion}
            onChange={(e) =>
              onChange({ ...dualValue, conclusion: e.target.value, mode: 'dual-textarea' })
            }
          />
        </div>
      </Space>
    );
  };

  const renderTextarea = (textareaValue: DetectionContentTextareaPayload) => {
    const conclusionLabel = config.labels?.conclusion || '检测结论';
    const conclusionPlaceholder = config.placeholders?.conclusion || `请输入${conclusionLabel}`;

    return (
      <div style={{ width: '100%' }}>
        <div style={{ marginBottom: 4 }}>{conclusionLabel}</div>
        <TextArea
          rows={4}
          placeholder={conclusionPlaceholder}
          value={textareaValue.conclusion}
          onChange={(e) => onChange({ ...textareaValue, conclusion: e.target.value, mode: 'textarea' })}
        />
      </div>
    );
  };

  const renderSingleField = (singleValue: DetectionContentSingleFieldPayload) => {
    const label = config.labels?.single || '检测数据';
    const placeholder = config.placeholders?.single || `请输入${label}`;

    return (
      <div style={{ width: '100%' }}>
        <div style={{ marginBottom: 4 }}>{label}</div>
        <Input
          placeholder={placeholder}
          value={singleValue.value}
          onChange={(e) => onChange({ ...singleValue, value: e.target.value, mode: 'single' })}
        />
      </div>
    );
  };

  const renderVisualGroups = (visualValue: DetectionContentVisualGroupsPayload) => {
    const groups = visualValue.groups || [];

    const updateVisualValue = (next: DetectionContentVisualGroupsPayload) => {
      onChange({ ...next, mode: 'visual-groups' });
    };

    const updateGroup = (groupIndex: number, updater: (group: DetectionContentVisualGroupsPayload['groups'][number]) => DetectionContentVisualGroupsPayload['groups'][number]) => {
      const nextGroups = groups.map((group, index) => (index === groupIndex ? updater(group) : group));
      updateVisualValue({ ...visualValue, groups: nextGroups });
    };

    const addGroup = () => {
      updateVisualValue({
        ...visualValue,
        groups: [...groups, { locationDesc: '', items: [{ resultDesc: '', imageIds: [] }] }],
      });
    };

    const removeGroup = (groupIndex: number) => {
      updateVisualValue({
        ...visualValue,
        groups: groups.filter((_, index) => index !== groupIndex),
      });
    };

    const addItem = (groupIndex: number) => {
      updateGroup(groupIndex, (group) => ({
        ...group,
        items: [...(group.items || []), { resultDesc: '', imageIds: [] }],
      }));
    };

    const removeItem = (groupIndex: number, itemIndex: number) => {
      updateGroup(groupIndex, (group) => ({
        ...group,
        items: (group.items || []).filter((_, index) => index !== itemIndex),
      }));
    };

    const updateItem = (
      groupIndex: number,
      itemIndex: number,
      updater: (item: DetectionContentVisualGroupsPayload['groups'][number]['items'][number]) => DetectionContentVisualGroupsPayload['groups'][number]['items'][number],
    ) => {
      updateGroup(groupIndex, (group) => ({
        ...group,
        items: (group.items || []).map((item, index) => (index === itemIndex ? updater(item) : item)),
      }));
    };

    return (
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <div>
          <div style={{ marginBottom: 4 }}>编号规则</div>
          <TextArea
            rows={2}
            placeholder="请输入编号规则"
            value={visualValue.numberingRule}
            onChange={(e) => updateVisualValue({ ...visualValue, numberingRule: e.target.value })}
            disabled={disabled}
          />
        </div>

        <Space direction="vertical" style={{ width: '100%' }} size={16}>
          {groups.map((group, groupIndex) => (
            <div
              key={`group-${groupIndex}`}
              style={{
                border: '1px solid #f0f0f0',
                borderRadius: 12,
                padding: 16,
                background: '#fafafa',
              }}
            >
              <Space direction="vertical" style={{ width: '100%' }} size={12}>
                <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
                  <div style={{ flex: 1 }}>
                    <div style={{ marginBottom: 4, fontWeight: 600 }}>位置描述 {groupIndex + 1}</div>
                    <TextArea
                      placeholder="请输入位置描述"
                      value={group.locationDesc}
                      onChange={(e) =>
                        updateGroup(groupIndex, (currentGroup) => ({ ...currentGroup, locationDesc: e.target.value }))
                      }
                      disabled={disabled}
                      autoSize={{ minRows: 2, maxRows: 16 }}
                      style={{
                        width: '100%',
                        maxWidth: '100%',
                        boxSizing: 'border-box',
                        overflowWrap: 'anywhere',
                      }}
                    />
                  </div>
                  <Popconfirm title="确认删除这个位置分组？" onConfirm={() => removeGroup(groupIndex)} disabled={disabled}>
                    <Button danger icon={<DeleteOutlined />} disabled={disabled}>
                      删除分组
                    </Button>
                  </Popconfirm>
                </Space>

                <Space direction="vertical" style={{ width: '100%' }} size={12}>
                  {(group.items || []).map((item, itemIndex) => (
                    <div
                      key={`group-${groupIndex}-item-${itemIndex}`}
                      style={{
                        border: '1px solid #e8e8e8',
                        borderRadius: 10,
                        padding: 12,
                        background: '#fff',
                      }}
                    >
                      <Space direction="vertical" style={{ width: '100%' }} size={10}>
                        <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
                          <div style={{ fontWeight: 500 }}>检测结果 {itemIndex + 1}</div>
                          <Popconfirm title="确认删除这条检测结果？" onConfirm={() => removeItem(groupIndex, itemIndex)} disabled={disabled}>
                            <Button type="link" danger icon={<DeleteOutlined />} disabled={disabled}>
                              删除
                            </Button>
                          </Popconfirm>
                        </Space>

                        <TextArea
                          rows={3}
                          placeholder="请输入检测结果描述"
                          value={item.resultDesc}
                          onChange={(e) =>
                            updateItem(groupIndex, itemIndex, (currentItem) => ({
                              ...currentItem,
                              resultDesc: e.target.value,
                            }))
                          }
                          disabled={disabled}
                        />

                        <div>
                          <div style={{ marginBottom: 6 }}>附图</div>
                          <ImageUpload
                            value={item.imageIds || []}
                            onChange={(imageIds) =>
                              updateItem(groupIndex, itemIndex, (currentItem) => ({
                                ...currentItem,
                                imageIds,
                              }))
                            }
                            maxCount={10}
                          />
                        </div>
                      </Space>
                    </div>
                  ))}
                </Space>

                <Button type="dashed" icon={<PlusOutlined />} onClick={() => addItem(groupIndex)} block disabled={disabled}>
                  添加检测结果
                </Button>
              </Space>
            </div>
          ))}

          <Button type="dashed" icon={<PlusOutlined />} onClick={addGroup} block disabled={disabled}>
            添加位置分组
          </Button>
        </Space>
      </Space>
    );
  };

  const renderSodEditor = (sodValue: DetectionContentSodPayload) => {
    const tableValue: DetectionContentTablePayload = { mode: 'table', rows: sodValue.rows || [] };
    const sodConfig: DetectionContentConfig = {
      mode: 'table',
      typeOptions: ['母管'],
      readOnlyFields: ['locationNumber', 'total'],
    };
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <div>
          <div style={{ marginBottom: 4 }}>探头规格</div>
          <Input
            placeholder="请输入探头规格"
            value={sodValue.probeSpec}
            onChange={(e) => onChange({ ...sodValue, probeSpec: e.target.value })}
            disabled={disabled}
          />
        </div>
        <div>
          <div style={{ marginBottom: 4 }}>管样</div>
          <Input
            placeholder="请输入管样"
            value={sodValue.tubeSample}
            onChange={(e) => onChange({ ...sodValue, tubeSample: e.target.value })}
            disabled={disabled}
          />
        </div>
        <div>
          <div style={{ marginBottom: 4 }}>检测灵敏度标定</div>
          <TextArea
            placeholder="请输入检测灵敏度标定内容"
            value={sodValue.sensitivityCalibration}
            onChange={(e) => onChange({ ...sodValue, sensitivityCalibration: e.target.value })}
            disabled={disabled}
            autoSize={{ minRows: 5 }}
            style={{ width: '100%' }}
          />
        </div>
        <div>
          <div style={{ marginBottom: 4 }}>检测部位</div>
          {renderTableEditor(tableValue, sodConfig, (updatedTableValue) => {
            onChange({ ...sodValue, rows: (updatedTableValue as DetectionContentTablePayload).rows });
          }, (updatedTableValue) => {
            onSave?.({ ...sodValue, rows: (updatedTableValue as DetectionContentTablePayload).rows });
          })}
        </div>
      </Space>
    );
  };

  const renderContent = () => {
    switch (config.mode) {
      case 'table':
        return renderTableEditor(normalizedValue as DetectionContentTablePayload);
      case 'dual-textarea':
        return renderDualTextarea(normalizedValue as DetectionContentDualTextareaPayload);
      case 'textarea':
        return renderTextarea(normalizedValue as DetectionContentTextareaPayload);
      case 'single':
        return renderSingleField(normalizedValue as DetectionContentSingleFieldPayload);
      case 'visual-groups':
        return renderVisualGroups(normalizedValue as DetectionContentVisualGroupsPayload);
      case 'sod':
        return renderSodEditor(normalizedValue as DetectionContentSodPayload);
      default:
        return null;
    }
  };

  return (
    <div className="report-subcard-section-body">
      <div className="report-subcard-header">
        <h4 className="report-subcard-title">检测内容</h4>
        <Space size="small">
          <Button
            size="small"
            type="default"
            onClick={() => onTextPreview?.()}
            disabled={disabled}
            style={{
              borderColor: '#fa8c16',
              color: '#fa8c16',
              fontWeight: 500,
            }}
          >
            文本预览
          </Button>
          <Button
            type="primary"
            size="small"
            icon={<SaveOutlined />}
            onClick={handleSave}
            disabled={disabled}
          >
            保存
          </Button>
        </Space>
      </div>
      <div className="report-subcard-content" style={{ padding: config.mode === 'table' ? 0 : '0 12px 12px' }}>
        {renderContent()}
      </div>
    </div>
  );
};

export default DetectionContentEditor;

