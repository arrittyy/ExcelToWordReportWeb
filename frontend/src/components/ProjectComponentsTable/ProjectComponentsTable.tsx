import React, { useState, useMemo, useRef, useEffect } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Checkbox, AutoComplete, theme, Tag } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ImportOutlined,
  DownloadOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as XLSX from 'xlsx';
import {
  componentService,
  ProjectComponent,
  CreateComponentRequest,
  UpdateComponentRequest,
  ComponentSpecPrefix,
} from '@/services/componentService';
import { getComponentDisplaySpec } from '@/utils/projectComponentDisplaySpec';
import { COMPONENT_CATEGORY_ORDER, sortProjectComponents } from '@/utils/sortProjectComponents';
import { MATERIAL_KEYS_FROM_LIBRARY } from '@/constants/materialKeysFromLibrary';
import { materialLibraryService } from '@/services/materialLibraryService';
import type { ReportList } from '@/types';
import { buildDetectionTypesByComponentId } from '@/utils/componentDetectionTypes';
import { abbreviateDetectionTypeName, detectionTypeTagColor } from '@/utils/detectionTypeLabel';
import { sortCategoriesForOverview } from '@/utils/aggregateDetectionLogOrder';

// 预定义的常见材质列表（保留历史项；与 MATERIAL_KEYS_FROM_LIBRARY 合并去重）
const COMMON_MATERIALS = [
  "07Cr18Ni11Nb", "07Cr2MoW2VNbB", "07Cr25Ni21NbN", "08Cr18Ni11NbFG", "10Cr18Ni9NbCu3BN",
  "10Cr9Mo1VNbN", "10Cr9MoW2VNbBN", "12Cr1MoV", "12Cr1MoVG", "12CrMoMoWVTiB",
  "15CrMoG", "1Cr11MoNiW1VNbN", "20Cr12NiMoWV(C422)", "20Cr1Mo1V1", "20Cr1Mo1VNbTiB",
  "20Cr1Mo1VTiB", "20CrMo", "20G", "25Cr2MoV",
  "2Cr11Mo1NiWVNbN", "2Cr11Mo1VNbN", "2Cr11NiMoNbVN", "2Cr12NiW1Mo1V", "35CrMo",
  "42CrMo", "45Cr1MoV", "G102", "GH445", "P122",
  "P22", "P91", "P92", "R-26(Ni-Cr-Co合金）", "S30432/SUPER304H",
  "SA-210C", "T12", "T23", "T91", "T92",
  "TP310HCbN/HR3C", "TP347H", "TP347HFG"
];

interface ProjectComponentsTableProps {
  projectId: number;
  reports?: ReportList[];
  onComponentChange?: () => void;
}

const COMPONENT_EXCEL_HEADERS = ['部件名称', '类别', '材质', '管径', '壁厚', '规格前缀', '牙距', '备注'] as const;

function componentNameImpliesMetricThread(name: string): boolean {
  return name.includes('螺栓') || name.includes('螺帽');
}

/** 类别筛选键：空类别与表格排序一致，归入「其他」 */
function categoryFilterLabel(category: string | null | undefined): string {
  const cat = (category ?? '').trim();
  return cat || '其他';
}

/** Excel 单元格 → 合法前缀或 undefined（省略则后端按名称自动） */
function specPrefixFromExcelCell(raw: string): ComponentSpecPrefix | undefined {
  const s = raw.trim();
  if (!s || s === '自动') return undefined;
  const u = s.toUpperCase();
  if (u === 'PHI' || s === 'Φ' || s === 'φ') return 'PHI';
  if (u === 'M') return 'M';
  if (u === 'NONE' || s === '无前缀') return 'NONE';
  return undefined;
}

type ExcelMappedComponentKey = Exclude<keyof CreateComponentRequest, 'specPrefix' | 'threadPitch'>;

const COMPONENT_HEADER_TO_KEY: Record<string, ExcelMappedComponentKey> = {
  部件名称: 'componentName',
  类别: 'category',
  材质: 'material',
  管径: 'pipeDiameter',
  壁厚: 'wallThickness',
  备注: 'remark',
};

const ProjectComponentsTable: React.FC<ProjectComponentsTableProps> = ({
  projectId,
  reports = [],
  onComponentChange,
}) => {
  const { token } = theme.useToken();
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isImportModalVisible, setIsImportModalVisible] = useState(false);
  const [editingComponent, setEditingComponent] = useState<ProjectComponent | null>(null);
  const [selectedUnitComponentIds, setSelectedUnitComponentIds] = useState<number[]>([]);
  const [componentSearchKeyword, setComponentSearchKeyword] = useState('');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string | null>(null);
  const excelFileInputRef = useRef<HTMLInputElement>(null);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();
  const specPrefixTouchedRef = useRef(false);
  const prevComponentNameRef = useRef<string | undefined>(undefined);
  const componentNameWatched = Form.useWatch('componentName', form);

  const { data: components = [], isLoading } = useQuery({
    queryKey: ['projectComponents', projectId],
    queryFn: () => componentService.getProjectComponents(projectId),
    enabled: !!projectId,
  });

  // 获取可用部件列表（包含项目部件和机组部件）
  const { data: availableComponents } = useQuery({
    queryKey: ['availableComponents', projectId],
    queryFn: () => componentService.getAvailableComponents(projectId),
    enabled: !!projectId,
  });

  const unitComponents = availableComponents?.unitComponents || [];

  const { data: materialKeysFromApi } = useQuery({
    queryKey: ['materialKeys'],
    queryFn: materialLibraryService.listKeys,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  // 生成材质选项列表（用于 AutoComplete 的建议）：常见材质 + 静态库 + API 动态键，去重后排序
  const materialOptions = useMemo(() => {
    const merged = [
      ...new Set([
        ...COMMON_MATERIALS,
        ...MATERIAL_KEYS_FROM_LIBRARY,
        ...(materialKeysFromApi ?? []),
      ]),
    ];
    merged.sort((a, b) => a.localeCompare(b, 'zh-CN'));
    return merged.map(m => ({ value: m }));
  }, [materialKeysFromApi]);

  const categoryOptions = useMemo(() => {
    const standardSet = new Set<string>(COMPONENT_CATEGORY_ORDER);
    const custom = new Set<string>();
    for (const c of components) {
      const cat = c.category?.trim();
      if (cat && !standardSet.has(cat)) custom.add(cat);
    }
    const customSorted = [...custom].sort((a, b) => a.localeCompare(b, 'zh-CN'));
    const merged = [...COMPONENT_CATEGORY_ORDER, ...customSorted];
    return merged.map(value => ({ value }));
  }, [components]);

  const specPrefixFormOptions = [
    { value: '', label: '自动（按名称）' },
    { value: 'PHI', label: 'Φ' },
    { value: 'M', label: 'M' },
    { value: 'NONE', label: '无前缀' },
  ];

  useEffect(() => {
    if (!isModalVisible) {
      prevComponentNameRef.current = undefined;
      return;
    }
    if (specPrefixTouchedRef.current) return;
    const name = String(componentNameWatched ?? '');
    if (prevComponentNameRef.current === undefined) {
      prevComponentNameRef.current = name;
      return;
    }
    if (prevComponentNameRef.current === name) return;
    prevComponentNameRef.current = name;
    const implies = componentNameImpliesMetricThread(name);
    form.setFieldsValue({ specPrefix: implies ? 'M' : 'PHI' });
  }, [componentNameWatched, isModalVisible, form]);

  const createMutation = useMutation({
    mutationFn: (data: CreateComponentRequest) => componentService.createComponent(projectId, data),
    onSuccess: () => {
      message.success('部件创建成功');
      setIsModalVisible(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['projectComponents', projectId] });
      onComponentChange?.();
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || '创建失败';
      message.error(msg);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateComponentRequest }) =>
      componentService.updateComponent(id, data),
    onSuccess: () => {
      message.success('部件更新成功');
      setIsModalVisible(false);
      form.resetFields();
      setEditingComponent(null);
      queryClient.invalidateQueries({ queryKey: ['projectComponents', projectId] });
      onComponentChange?.();
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || '更新失败';
      message.error(msg);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: componentService.deleteComponent,
    onSuccess: () => {
      message.success('部件删除成功');
      queryClient.invalidateQueries({ queryKey: ['projectComponents', projectId] });
      onComponentChange?.();
    },
    onError: () => {
      message.error('删除失败');
    },
  });

  const importMutation = useMutation({
    mutationFn: (unitComponentIds: number[]) => componentService.importComponentsFromUnit(projectId, unitComponentIds),
    onSuccess: (data) => {
      message.success(`成功导入 ${data.count} 个部件`);
      setIsImportModalVisible(false);
      setSelectedUnitComponentIds([]);
      queryClient.invalidateQueries({ queryKey: ['projectComponents', projectId] });
      onComponentChange?.();
    },
    onError: (error: any) => {
      const errorMessage = error?.response?.data?.message || '导入失败';
      message.error(errorMessage);
    },
  });

  const handleAdd = () => {
    setEditingComponent(null);
    specPrefixTouchedRef.current = false;
    form.resetFields();
    form.setFieldsValue({ specPrefix: 'PHI' });
    setIsModalVisible(true);
  };

  const handleImport = () => {
    if (unitComponents.length === 0) {
      message.warning('该项目未关联机组或机组中没有部件');
      return;
    }
    setIsImportModalVisible(true);
    setSelectedUnitComponentIds([]);
  };

  const handleImportSubmit = () => {
    if (selectedUnitComponentIds.length === 0) {
      message.warning('请至少选择一个部件');
      return;
    }
    importMutation.mutate(selectedUnitComponentIds);
  };

  /** 下载检测部件列表 Excel 模板 */
  const handleDownloadTemplate = () => {
    const templateRow: Record<string, string> = {};
    COMPONENT_EXCEL_HEADERS.forEach(h => { templateRow[h] = ''; });
    const ws = XLSX.utils.json_to_sheet([templateRow]);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, '检测部件列表');
    XLSX.writeFile(wb, '检测部件列表_模板.xlsx');
  };

  /** 上传 Excel：解析后逐条 createComponent 追加 */
  const handleUploadExcel = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';
    const reader = new FileReader();
    reader.onload = async (ev) => {
      try {
        const data = ev.target?.result;
        if (!data) return;
        const workbook = XLSX.read(data, { type: 'binary' });
        const firstSheetName = workbook.SheetNames[0];
        const sheet = workbook.Sheets[firstSheetName];
        const raw = XLSX.utils.sheet_to_json(sheet, { header: 1 }) as (string | number)[][];
        if (raw.length < 2) {
          message.warning('Excel 中无数据行');
          return;
        }
        const headerRow = raw[0].map(c => String(c ?? '').trim());
        const dataRows = raw.slice(1);
        let successCount = 0;
        let failCount = 0;
        for (const row of dataRows) {
          const obj: Record<string, unknown> = {};
          let hasAny = false;
          headerRow.forEach((h, i) => {
            const val = row[i];
            const v = val === null || val === undefined ? '' : String(val).trim();
            if (h === '规格前缀') {
              const p = specPrefixFromExcelCell(v);
              if (p) {
                obj.specPrefix = p;
                hasAny = true;
              }
              return;
            }
            if (h === '牙距') {
              if (v) {
                obj.threadPitch = v;
                hasAny = true;
              }
              return;
            }
            const key = COMPONENT_HEADER_TO_KEY[h];
            if (!key) return;
            if (v) hasAny = true;
            obj[key] = v || undefined;
          });
          if (!hasAny) continue;
          if (!String(obj.componentName ?? '').trim()) {
            failCount += 1;
            continue;
          }
          try {
            await componentService.createComponent(projectId, obj as unknown as CreateComponentRequest);
            successCount += 1;
          } catch {
            failCount += 1;
          }
        }
        queryClient.invalidateQueries({ queryKey: ['projectComponents', projectId] });
        onComponentChange?.();
        if (failCount === 0) {
          message.success(`成功导入 ${successCount} 个部件`);
        } else {
          message.warning(`成功 ${successCount} 条，失败 ${failCount} 条`);
        }
      } catch (err) {
        console.error('Excel 解析失败', err);
        message.error('Excel 解析失败，请确认文件格式与表头与模板一致');
      }
    };
    reader.readAsBinaryString(file);
  };

  const handleEdit = (component: ProjectComponent) => {
    setEditingComponent(component);
    specPrefixTouchedRef.current = component.specPrefix != null;
    form.setFieldsValue({
      componentName: component.componentName,
      material: component.material,
      category: component.category,
      pipeDiameter: component.pipeDiameter,
      wallThickness: component.wallThickness,
      specPrefix: component.specPrefix ?? '',
      threadPitch: component.threadPitch ?? '',
      remark: component.remark,
    });
    setIsModalVisible(true);
  };

  const handleDelete = (id: number) => {
    deleteMutation.mutate(id);
  };

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      const specPrefix =
        values.specPrefix === '' || values.specPrefix == null ? null : (values.specPrefix as ComponentSpecPrefix);
      const threadPitch =
        values.threadPitch == null || String(values.threadPitch).trim() === ''
          ? null
          : String(values.threadPitch).trim();
      const category = String(values.category ?? '').trim();
      const data: CreateComponentRequest = {
        ...values,
        category,
        specPrefix,
        threadPitch,
      };
      if (editingComponent) {
        updateMutation.mutate({ id: editingComponent.id, data });
        return;
      }

      const name = (data.componentName ?? '').trim();
      const nameExists =
        name.length > 0 &&
        components.some(c => (c.componentName ?? '').trim() === name);

      if (nameExists) {
        Modal.confirm({
          title: '提示',
          content: '该部件已存在，是否为添加新的材质或规格？',
          okText: '是',
          cancelText: '否',
          onOk: () => {
            createMutation.mutate(data);
          },
        });
      } else {
        createMutation.mutate(data);
      }
    });
  };

  const sortedComponents = useMemo(() => sortProjectComponents(components), [components]);

  const filterCategoryLabels = useMemo(() => {
    const set = new Set<string>();
    for (const c of sortedComponents) {
      set.add(categoryFilterLabel(c.category));
    }
    return sortCategoriesForOverview(set);
  }, [sortedComponents]);

  useEffect(() => {
    if (
      selectedCategoryFilter != null &&
      !filterCategoryLabels.includes(selectedCategoryFilter)
    ) {
      setSelectedCategoryFilter(null);
    }
  }, [filterCategoryLabels, selectedCategoryFilter]);

  const detectionTypesByComponentId = useMemo(
    () => buildDetectionTypesByComponentId(reports),
    [reports],
  );

  const columns = [
    {
      title: '序号',
      key: 'serialNumber',
      width: 60,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: '部件名称',
      dataIndex: 'componentName',
      width: 270,
      key: 'componentName',
    },
    {
      title: '类别',
      dataIndex: 'category',
      width: 140,
      key: 'category',
    },
    {
      title: '材质',
      dataIndex: 'material',
      key: 'material',
      width: 180,
      ellipsis: true,
      render: (material: string | undefined) => (material?.trim() ? material.trim() : '—'),
    },
    {
      title: '规格',
      key: 'displaySpec',
      width: 180,
      ellipsis: true,
      render: (_: unknown, record: ProjectComponent) => getComponentDisplaySpec(record) || '—',
    },
    {
      title: '检测类型',
      key: 'detectionTypes',
      //width: 260,
      render: (_: unknown, record: ProjectComponent) => {
        const types = detectionTypesByComponentId.get(record.id) ?? [];
        if (types.length === 0) return '—';
        return (
          <Space size={[4, 4]} wrap>
            {types.map(typeName => (
              <Tag
                key={typeName}
                color={detectionTypeTagColor(typeName)}
                style={{ borderRadius: 18, marginInlineEnd: 0 }}
              >
                {abbreviateDetectionTypeName(typeName)}
              </Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: ProjectComponent) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个部件吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const displayedComponents = useMemo(() => {
    let list = sortedComponents;
    if (selectedCategoryFilter != null) {
      list = list.filter(
        c => categoryFilterLabel(c.category) === selectedCategoryFilter,
      );
    }
    const q = componentSearchKeyword.trim().toLowerCase();
    if (!q) return list;
    return list.filter(c => (c.componentName ?? '').toLowerCase().includes(q));
  }, [sortedComponents, selectedCategoryFilter, componentSearchKeyword]);

  return (
    <>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 8,
          width: '100%',
          paddingRight: 8,
          marginBottom: 12,
        }}
      >
        <Space wrap align="center" size="middle">
          <h3
            style={{
              margin: 0,
              color: token.colorTextHeading,
              fontSize: token.fontSizeLG,
              fontWeight: token.fontWeightStrong,
            }}
          >
            检测部件列表
            <span style={{ marginLeft: 8, fontSize: token.fontSizeLG, fontWeight: token.fontWeightStrong }}>
              （共 {displayedComponents.length} 条）
            </span>
          </h3>
          <Input.Search
            allowClear
            placeholder="搜索部件名称"
            style={{ width: 260 }}
            value={componentSearchKeyword}
            onChange={e => setComponentSearchKeyword(e.target.value)}
          />
          {filterCategoryLabels.map(label => {
            const selected = selectedCategoryFilter === label;
            return (
              <Button
                key={label}
                type="primary"
                size="small"
                style={
                  selected
                    ? {
                        opacity: 1,
                        background: token.colorPrimary,
                        border: '2px solid #ffffff',
                        boxShadow: `0 0 0 3px ${token.colorPrimary}, 0 4px 14px rgba(22, 119, 255, 0.65)`,
                        fontWeight: 700,
                      }
                    : {
                        opacity: 0.55,
                        boxShadow: 'none',
                      }
                }
                onClick={() =>
                  setSelectedCategoryFilter(prev => (prev === label ? null : label))
                }
              >
                {label}
              </Button>
            );
          })}
        </Space>
        <Space wrap>
          <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
            下载 Excel 模板
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => excelFileInputRef.current?.click()}>
            上传 Excel
          </Button>
          <input
            ref={excelFileInputRef}
            type="file"
            accept=".xlsx,.xls"
            style={{ display: 'none' }}
            onChange={handleUploadExcel}
          />
          <Button icon={<ImportOutlined />} onClick={handleImport} disabled={unitComponents.length === 0}>
            从机组导入
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加部件
          </Button>
        </Space>
      </div>
      <Table
        columns={columns}
        dataSource={displayedComponents}
        rowKey="id"
        loading={isLoading}
        pagination={false}
      />
      <Modal
        title={editingComponent ? '编辑部件' : '添加部件'}
        open={isModalVisible}
        onOk={handleSubmit}
        onCancel={() => {
          setIsModalVisible(false);
          form.resetFields();
          setEditingComponent(null);
          specPrefixTouchedRef.current = false;
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="componentName"
            label="部件名称"
            rules={[{ required: true, message: '请输入部件名称' }]}
          >
            <Input placeholder="请输入部件名称" />
          </Form.Item>
          <Form.Item
            name="category"
            label="类别"
            rules={[{ required: true, whitespace: true, message: '请输入或选择类别' }]}
          >
            <AutoComplete
              options={categoryOptions}
              placeholder="请输入或选择类别"
              filterOption={(inputValue, option) =>
                (option?.value as string)
                  .toUpperCase()
                  .includes(inputValue.toUpperCase())
              }
              allowClear
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item name="material" label="材质">
            <AutoComplete
              options={materialOptions}
              placeholder="请输入或选择材质"
              filterOption={(inputValue, option) =>
                (option?.value as string)
                  .toUpperCase()
                  .includes(inputValue.toUpperCase())
              }
              allowClear
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item name="pipeDiameter" label="管径">
            <Input placeholder="请输入管径" />
          </Form.Item>
          <Form.Item name="wallThickness" label="壁厚">
            <Input placeholder="请输入壁厚" />
          </Form.Item>
          <Form.Item name="specPrefix" label="规格前缀">
            <Select
              options={specPrefixFormOptions}
              placeholder="自动（按名称）"
              style={{ width: '100%' }}
              onChange={() => {
                specPrefixTouchedRef.current = true;
              }}
            />
          </Form.Item>
          <Form.Item name="threadPitch" label="牙距">
            <Input placeholder="一般为数字，报告中会自动补 mm（若未写 mm）" allowClear />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="从机组导入部件"
        open={isImportModalVisible}
        onOk={handleImportSubmit}
        onCancel={() => {
          setIsImportModalVisible(false);
          setSelectedUnitComponentIds([]);
        }}
        confirmLoading={importMutation.isPending}
        width={800}
      >
        {unitComponents.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            该项目未关联机组或机组中没有部件
          </div>
        ) : (
          <div>
            <div style={{ marginBottom: 16 }}>
              <Checkbox
                indeterminate={
                  selectedUnitComponentIds.length > 0 && selectedUnitComponentIds.length < unitComponents.length
                }
                checked={selectedUnitComponentIds.length === unitComponents.length && unitComponents.length > 0}
                onChange={(e) => {
                  if (e.target.checked) {
                    setSelectedUnitComponentIds(unitComponents.map((uc) => uc.id));
                  } else {
                    setSelectedUnitComponentIds([]);
                  }
                }}
              >
                全选
              </Checkbox>
              <span style={{ marginLeft: 16, color: '#666' }}>
                已选择 {selectedUnitComponentIds.length} / {unitComponents.length} 个部件
              </span>
            </div>
            <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
              <Table
                columns={[
                  {
                    title: '部件名称',
                    dataIndex: 'componentName',
                    key: 'componentName',
                  },
                  {
                    title: '类别',
                    dataIndex: 'category',
                    key: 'category',
                  },
                  {
                    title: '材质',
                    dataIndex: 'material',
                    key: 'material',
                  },
                  {
                    title: '管径',
                    dataIndex: 'pipeDiameter',
                    key: 'pipeDiameter',
                  },
                  {
                    title: '壁厚',
                    dataIndex: 'wallThickness',
                    key: 'wallThickness',
                  },
                ]}
                dataSource={unitComponents}
                rowKey="id"
                pagination={false}
                rowSelection={{
                  selectedRowKeys: selectedUnitComponentIds,
                  onChange: (selectedRowKeys) => {
                    setSelectedUnitComponentIds(selectedRowKeys as number[]);
                  },
                }}
              />
            </div>
          </div>
        )}
      </Modal>
    </>
  );
};

export default ProjectComponentsTable;
