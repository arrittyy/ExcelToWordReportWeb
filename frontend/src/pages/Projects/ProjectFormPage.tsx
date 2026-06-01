import React, { useEffect, useMemo, useState } from 'react';
import {
  App,
  Form,
  Input,
  DatePicker,
  Button,
  Card,
  Space,
  Typography,
  Select,
  Row,
  Col,
  Tag,
  Modal,
} from 'antd';
import {
  SaveOutlined,
  FileTextOutlined,
  BankOutlined,
  SettingOutlined,
  UserOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectService } from '@/services/projectService';
import { powerPlantService, unitService } from '@/services/powerPlantService';
import type { CreateProject, UpdateProject } from '@/types';
import dayjs from 'dayjs';
import PersonnelSelect from '@/components/PersonnelSelect/PersonnelSelect';
import { RUNDIAN_PERSONNEL_NAMES } from '@/constants/rundianPersonnel';
import { parseStaff, STAFF_DELIMITER } from '@/utils/parseStaff';

const { Title } = Typography;
const { TextArea } = Input;

/** 润电内部人员（项目负责人、工作人员预设） */
const COMMON_PERSONNEL = RUNDIAN_PERSONNEL_NAMES;

const PROJECT_TYPE_OPTIONS = [
  { label: '金属监督', value: '金属监督' },
  { label: '防磨防爆', value: '防磨防爆' },
  { label: '锅炉内检', value: '锅炉内检' },
  { label: '锅炉外检', value: '锅炉外检' },
  { label: '容器定检', value: '容器定检' },
  { label: '容器外检', value: '容器外检' },
];

const ProjectFormPage: React.FC = () => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { id } = useParams();
  const queryClient = useQueryClient();

  const isEditMode = !!id;

  // 生成人员选项列表
  const personnelOptions = useMemo(() => {
    return COMMON_PERSONNEL
      .map(name => ({ label: name, value: name }))
      .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'));
  }, []);

  // 创建 form 实例
  const [form] = Form.useForm();
  const [staffList, setStaffList] = useState<string[]>([]);
  const [staffModalOpen, setStaffModalOpen] = useState(false);
  const [newStaffName, setNewStaffName] = useState('');

  // Load project data if editing
  const { data: projectData } = useQuery({
    queryKey: ['project', id],
    queryFn: () => projectService.getById(Number(id)),
    enabled: isEditMode,
  });

  // Load power plants list
  const { data: powerPlants = [] } = useQuery({
    queryKey: ['powerPlants'],
    queryFn: powerPlantService.getAll,
  });

  const selectedPowerPlantId = Form.useWatch('powerPlantId', form);
  const selectedUnitId = Form.useWatch('unitId', form);
  const selectedProjectType = Form.useWatch('projectType', form);
  
  // 监听所有可选字段的值，用于动态控制 hasFeedback
  const customerContact = Form.useWatch('customerContact', form);
  const endDate = Form.useWatch('endDate', form);
  const responsiblePerson = Form.useWatch('responsiblePerson', form);
  const description = Form.useWatch('description', form);
  // 编辑时用 projectData 电厂 ID 预加载机组（兼容 camelCase / snake_case）
  const projectPowerPlantId = isEditMode && projectData
    ? (projectData.powerPlantId ?? (projectData as any).power_plant_id)
    : undefined;
  const powerPlantIdForUnits = selectedPowerPlantId ?? projectPowerPlantId;

  // Load units for selected power plant (通过客户方选择)
  const { data: units = [] } = useQuery({
    queryKey: ['units', powerPlantIdForUnits],
    queryFn: () => unitService.getByPowerPlantId(powerPlantIdForUnits!),
    enabled: !!powerPlantIdForUnits,
  });

  // 客户方选项：编辑时若 projectData 有电厂 ID，确保当前电厂在列表中以便回显（电厂列表未加载时也能显示）
  const powerPlantOptions = useMemo(() => {
    const base = powerPlants.map((plant) => ({ label: plant.name, value: plant.id }));
    if (isEditMode && projectData && (projectData.powerPlantId != null || (projectData as any).power_plant_id != null)) {
      const powerPlantIdNum = Number(projectData.powerPlantId ?? (projectData as any).power_plant_id);
      const hasCurrent = base.some((o) => o.value === powerPlantIdNum);
      if (!hasCurrent) {
        const label = projectData.customer ?? `电厂 ${powerPlantIdNum}`;
        return [{ label, value: powerPlantIdNum }, ...base];
      }
    }
    return base;
  }, [powerPlants, isEditMode, projectData?.powerPlantId, (projectData as any)?.power_plant_id, projectData?.customer]);

  // 机组选项：编辑时若 projectData 有 unitId，确保当前机组在列表中以便回显（units 未加载时也能显示）
  const unitOptions = useMemo(() => {
    const base = units.map((unit) => ({
      label: unit.installedCapacity
        ? `${unit.unitNumber || unit.unitName} - ${unit.installedCapacity}`
        : unit.unitNumber || unit.unitName,
      value: unit.id,
    }));
    const rawUnitId = projectData?.unitId ?? (projectData as any)?.unit_id;
    if (isEditMode && rawUnitId != null) {
      const unitIdNum = Number(rawUnitId);
      const hasCurrent = base.some((o) => o.value === unitIdNum);
      if (!hasCurrent) {
        const label = projectData?.unitNumber ?? `机组 ${rawUnitId}`;
        return [{ label, value: unitIdNum }, ...base];
      }
    }
    return base;
  }, [units, isEditMode, projectData?.unitId, (projectData as any)?.unit_id, projectData?.unitNumber]);


  // 状态：用于触发 Select 重新渲染
  const [, forceUpdate] = React.useReducer((x) => x + 1, 0);

  useEffect(() => {
    if (projectData) {
      // 编辑模式：强制 reset 后再设置，确保 Select 能正确显示
      const rawPowerPlantId = projectData.powerPlantId ?? (projectData as any).power_plant_id;
      const rawUnitId = projectData.unitId ?? (projectData as any).unit_id;
      const values = {
        projectNumber: projectData.projectNumber,
        thirdPartyProjectNumber: projectData.thirdPartyProjectNumber,
        thirdPartyName: projectData.thirdPartyName,
        projectName: projectData.projectName,
        projectType: projectData.projectType,
        customerContact: projectData.customerContact,
        powerPlantId: rawPowerPlantId != null ? Number(rawPowerPlantId) : undefined,
        unitId: rawUnitId != null ? Number(rawUnitId) : undefined,
        startDate: dayjs(projectData.startDate),
        endDate: projectData.endDate ? dayjs(projectData.endDate) : null,
        status: projectData.status,
        description: projectData.description,
        responsiblePerson: projectData.responsiblePerson,
        staff: projectData.staff,
      };
      console.log('💾 [useEffect] Resetting and setting values:', values.powerPlantId, values.unitId);
      form.resetFields();
      form.setFieldsValue(values);
      setStaffList(parseStaff(projectData.staff));
      console.log('✅ [useEffect] After setFieldsValue:', form.getFieldsValue(['powerPlantId', 'unitId']));
      // 强制组件重新渲染，让 Select 从 form 读取新值
      forceUpdate();
    }
    if (!isEditMode) {
      form.resetFields();
      form.setFieldsValue({ status: 'InProgress' });
      setStaffList([]);
    }
  }, [projectData, form, isEditMode]);

  // 监听所有Select的值变化，动态添加has-value类名
  useEffect(() => {
    const updateSelectClasses = () => {
      const selects = document.querySelectorAll('.ant-select:not(.ant-select-disabled)');
      selects.forEach((select) => {
        const selector = select.querySelector('.ant-select-selector');
        if (!selector) return;
        
        const placeholder = selector.querySelector('.ant-select-selection-placeholder');
        const selectionItem = selector.querySelector('.ant-select-selection-item');
        
        // 检查placeholder是否可见
        let isPlaceholderVisible = false;
        if (placeholder) {
          const placeholderStyle = window.getComputedStyle(placeholder);
          isPlaceholderVisible = placeholderStyle.display !== 'none' && 
                                 placeholderStyle.visibility !== 'hidden' &&
                                 placeholderStyle.opacity !== '0';
        }
        
        // 检查selection-item是否可见
        let isSelectionItemVisible = false;
        if (selectionItem) {
          const itemStyle = window.getComputedStyle(selectionItem);
          isSelectionItemVisible = itemStyle.display !== 'none' && 
                                  itemStyle.visibility !== 'hidden' &&
                                  itemStyle.opacity !== '0';
        }
        
        // 如果有可见的selection-item，或者没有可见的placeholder但有selection-item，则认为有值
        if (isSelectionItemVisible || (selectionItem && !isPlaceholderVisible)) {
          select.classList.add('has-value');
        } else {
          select.classList.remove('has-value');
        }
      });
    };

    // 初始检查 - 延迟执行确保DOM已渲染
    const initialTimeout = setTimeout(updateSelectClasses, 100);

    // 使用MutationObserver监听DOM变化
    const observer = new MutationObserver(() => {
      setTimeout(updateSelectClasses, 50);
    });
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'style'],
    });

    // 定期检查Select状态（因为表单值变化可能不会立即反映到DOM）
    const intervalId = setInterval(updateSelectClasses, 300);

    // 监听表单值变化 - 使用Form.useWatch的值变化来触发更新
    // 这个会在useEffect的依赖中处理

    return () => {
      clearTimeout(initialTimeout);
      observer.disconnect();
      clearInterval(intervalId);
    };
  }, [form, selectedPowerPlantId, selectedUnitId]);

  const createMutation = useMutation({
    mutationFn: projectService.create,
    onSuccess: () => {
      message.success('项目创建成功！');
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      navigate('/projects');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProject }) =>
      projectService.update(id, data),
    onSuccess: () => {
      message.success('项目更新成功！');
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      navigate('/projects');
    },
  });

  const handleSubmit = async (values: any) => {
    try {
      const normalizeText = (value?: string) =>
        value && value.trim() !== '' ? value.trim() : undefined;

      // 提取客户方名字（去掉"-数字"部分）
      const extractCustomerName = (customer?: string): string | undefined => {
        if (!customer) return undefined;
        // 匹配"客户方名字-数字"格式，只返回客户方名字部分
        const match = customer.match(/^(.+?)(-\d+)?$/);
        return match ? match[1].trim() : customer.trim();
      };

      // 如果选择了电厂和机组，自动填充customer字段（只保存电厂名字，不包含机组号）
      let customer: string | undefined = undefined;
      if (values.powerPlantId && values.unitId) {
        try {
          const plant = await powerPlantService.getById(values.powerPlantId);
          customer = plant.name; // 只保存电厂名字，不包含机组号
        } catch (error) {
          console.error('获取电厂信息失败:', error);
          message.warning('无法自动填充客户方，请手动输入');
          // 如果获取失败，使用手动输入的customer（如果有）
          customer = extractCustomerName(normalizeText(values.customer));
        }
      } else {
        // 如果没有选择电厂和机组，使用手动输入的customer，并去掉"-数字"部分
        customer = extractCustomerName(normalizeText(values.customer));
      }

      const data: CreateProject = {
        projectNumber: values.projectNumber,
        thirdPartyProjectNumber: normalizeText(values.thirdPartyProjectNumber),
        thirdPartyName: normalizeText(values.thirdPartyName),
        projectName: values.projectName,
        projectType: values.projectType,
        customer: customer,
        customerContact: normalizeText(values.customerContact),
        powerPlantId: values.powerPlantId,
        unitId: values.unitId,
        startDate: values.startDate.format('YYYY-MM-DD'),
        endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : undefined,
        description: values.description,
        responsiblePerson: normalizeText(values.responsiblePerson),
        staff: normalizeText(values.staff),
      };

      if (isEditMode) {
        const updateData: UpdateProject = {
          ...data,
          status: values.status || 'InProgress',
          selectedExperimentTypeIds: projectData?.selectedExperimentTypeIds,
          ndtSignatureLevels: projectData?.ndtSignatureLevels,
          writerNdt: projectData?.writerNdt,
          writerDateNdt: projectData?.writerDateNdt,
          reviewerNdt: projectData?.reviewerNdt,
          reviewDateNdt: projectData?.reviewDateNdt,
          approverNdt: projectData?.approverNdt,
          approvalDateNdt: projectData?.approvalDateNdt,
          writerChem: projectData?.writerChem,
          writerDateChem: projectData?.writerDateChem,
          reviewerChem: projectData?.reviewerChem,
          reviewDateChem: projectData?.reviewDateChem,
          approverChem: projectData?.approverChem,
          approvalDateChem: projectData?.approvalDateChem,
          thirdPartyApprovalByExperimentType: projectData?.thirdPartyApprovalByExperimentType,
        };
        updateMutation.mutate({ id: Number(id), data: updateData });
      } else {
        createMutation.mutate(data);
      }
    } catch (error) {
      message.error('保存失败，请检查数据');
    }
  };

  return (
    <div>
      <style>{`
        /* 下划线样式输入框 - 只有一条横线，彻底移除所有边框 */
        .ant-input-affix-wrapper {
          border: none !important;
          border-bottom: 1px solid #D1D5DC !important;
          border-top: none !important;
          border-left: none !important;
          border-right: none !important;
          border-radius: 0 !important;
          background-color: transparent !important;
          box-shadow: none !important;
          transition: border-color 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
          padding-left: 0 !important;
          padding-right: 0 !important;
        }
        
        .ant-input-affix-wrapper .ant-input {
          border: none !important;
          border-bottom: none !important;
          border-top: none !important;
          border-left: none !important;
          border-right: none !important;
          background-color: transparent !important;
          box-shadow: none !important;
          padding-left: 0 !important;
          padding-right: 0 !important;
        }
        
        .ant-input {
          border: none !important;
          border-bottom: 1px solid #D1D5DC !important;
          border-top: none !important;
          border-left: none !important;
          border-right: none !important;
          border-radius: 0 !important;
          background-color: transparent !important;
          box-shadow: none !important;
        }
        
        .ant-select-selector {
          border: none !important;
          border-bottom: 1px solid #D1D5DC !important;
          border-top: none !important;
          border-left: none !important;
          border-right: none !important;
          border-radius: 0 !important;
          background-color: transparent !important;
          box-shadow: none !important;
        }
        
        /* DatePicker 下划线样式 */
        .ant-picker {
          border: none !important;
          border-bottom: 1px solid #D1D5DC !important;
          border-top: none !important;
          border-left: none !important;
          border-right: none !important;
          border-radius: 0 !important;
          background-color: transparent !important;
          box-shadow: none !important;
          padding-left: 0 !important;
          padding-right: 0 !important;
        }
        
        .ant-picker:hover {
          border-bottom-color: #8c8c8c !important;
          box-shadow: none !important;
        }
        
        .ant-picker-focused {
          border-bottom-color: #1890ff !important;
          border-bottom-width: 1px !important;
          box-shadow: none !important;
        }
        
        .ant-picker-input > input {
          color: #262626 !important;
          font-size: 15px !important;
        }
        
        .ant-picker-input > input::placeholder {
          color: #bfbfbf !important;
          font-size: 14px !important;
        }
        
        /* 移除所有可能的额外边框和伪元素 */
        .ant-input-affix-wrapper::before,
        .ant-input-affix-wrapper::after,
        .ant-select-selector::before,
        .ant-select-selector::after,
        .ant-picker::before,
        .ant-picker::after {
          display: none !important;
          border: none !important;
          content: none !important;
        }
        
        /* 移除Form.Item的边框 */
        .ant-form-item-control-input,
        .ant-form-item-control-input-content {
          border: none !important;
        }
        
        /* 为验证反馈图标定位做准备 */
        .ant-form-item-control-input {
          position: relative !important;
        }
        
        .ant-input-affix-wrapper:hover,
        .ant-input:hover {
          border-bottom-color: #8c8c8c !important;
          box-shadow: none !important;
        }
        
        /* 默认Select hover状态 - 灰色 */
        .ant-select:hover .ant-select-selector {
          border-bottom-color: #8c8c8c !important;
          box-shadow: none !important;
        }
        
        .ant-input-affix-wrapper-focused,
        .ant-input-focused,
        .ant-select-focused .ant-select-selector {
          border-bottom-color: #1890ff !important;
          border-bottom-width: 1px !important;
          box-shadow: none !important;
        }
        
        /* Select组件有值时保持浅蓝色边框 - 必须在hover规则之后，提高优先级 */
        .ant-select.has-value:not(.ant-select-disabled) .ant-select-selector {
          border-bottom-color: #1890ff !important;
          border-bottom-width: 1px !important;
        }
        
        /* 已选择的Select在hover时保持浅蓝色 - 覆盖默认hover样式 */
        .ant-select.has-value:not(.ant-select-disabled):hover .ant-select-selector {
          border-bottom-color: #1890ff !important;
        }
        
        /* 已选择的Select在focus时保持浅蓝色 */
        .ant-select.has-value:not(.ant-select-disabled).ant-select-focused .ant-select-selector {
          border-bottom-color: #1890ff !important;
        }
        
        /* 验证成功时，已选择的Select保持绿色边框 */
        .ant-form-item-has-success .ant-select.has-value:not(.ant-select-disabled) .ant-select-selector {
          border-bottom-color: #52c41a !important;
        }
        
        /* 输入文本颜色 - 深色 */
        .ant-input-affix-wrapper .ant-input,
        .ant-input,
        .ant-select-selector,
        .ant-select-selection-item {
          color: #262626 !important;
          font-size: 15px !important;
        }
        
        /* Placeholder 颜色 - 浅灰色，字体稍小 */
        .ant-input::placeholder,
        .ant-input-affix-wrapper .ant-input::placeholder {
          color: #bfbfbf !important;
          font-size: 14px !important;
        }
        
        /* Select placeholder 颜色 */
        .ant-select-selection-placeholder {
          color: #bfbfbf !important;
          font-size: 14px !important;
        }
        
        /* 表单项标签样式 - 浅灰色，比placeholder大 */
        .ant-form-item-label > label {
          font-weight: 400;
          color: #8c8c8c !important;
          font-size: 15px !important;
        }
        
        /* 验证成功状态 */
        .ant-form-item-has-success .ant-input-affix-wrapper,
        .ant-form-item-has-success .ant-input,
        .ant-form-item-has-success .ant-select-selector,
        .ant-form-item-has-success .ant-picker {
          border-bottom-color: #52c41a !important;
        }
        
        /* 验证成功时，已选择的Select保持绿色边框 */
        .ant-form-item-has-success .ant-select:not(.ant-select-disabled) .ant-select-selector:not(:has(.ant-select-selection-placeholder)) {
          border-bottom-color: #52c41a !important;
        }
        .ant-form-item-has-success .ant-input-affix-wrapper:hover,
        .ant-form-item-has-success .ant-input:hover,
        .ant-form-item-has-success .ant-select-selector:hover,
        .ant-form-item-has-success .ant-picker:hover {
          border-bottom-color: #73d13d !important;
        }
        .ant-form-item-has-success .ant-input-affix-wrapper-focused,
        .ant-form-item-has-success .ant-input-focused,
        .ant-form-item-has-success .ant-select-focused .ant-select-selector,
        .ant-form-item-has-success .ant-picker-focused {
          border-bottom-color: #52c41a !important;
          border-bottom-width: 1px !important;
        }
        .ant-form-item-has-success .anticon-check-circle {
          color: #52c41a !important;
        }
        
        /* 为所有有 hasFeedback 的 Form.Item 添加默认灰色对号 */
        .ant-form-item-has-feedback .ant-form-item-control-input::after {
          content: '✓';
          position: absolute;
          right: 0;
          top: 50%;
          transform: translateY(-50%);
          color: #bfbfbf !important;
          font-size: 16px;
          line-height: 1;
          pointer-events: none;
          z-index: 1;
          font-weight: bold;
        }
        
        /* 验证成功时显示绿色对号 */
        .ant-form-item-has-success .ant-form-item-control-input::after {
          color: #52c41a !important;
        }
        
        /* 隐藏 Ant Design 默认的验证反馈图标，使用自定义图标 */
        .ant-form-item-has-feedback .ant-form-item-feedback-icon {
          display: none !important;
        }
        
        /* 确保验证成功图标正确显示 */
        .ant-form-item-has-success .ant-form-item-feedback-icon-success {
          display: none !important;
        }
        
        /* 为输入框右侧留出空间给验证图标 */
        .ant-form-item-has-feedback .ant-input-affix-wrapper,
        .ant-form-item-has-feedback .ant-input,
        .ant-form-item-has-feedback .ant-select-selector,
        .ant-form-item-has-feedback .ant-picker {
          padding-right: 24px !important;
        }
        
        /* Select 组件图标间距 - 为左侧图标预留空间 */
        /* Ant Design v5 中需要同时为多个节点设置 padding-left */
        .select-with-icon .ant-select-selector {
          padding-left: 36px !important;
        }
        
        .select-with-icon .ant-select-selection-placeholder,
        .select-with-icon .ant-select-selection-item,
        .select-with-icon .ant-select-selection-search {
          padding-left: 36px !important;
        }
        
        /* 图标样式优化 - 浅灰色 */
        .ant-input-affix-wrapper .anticon {
          color: #bfbfbf !important;
          transition: color 0.3s !important;
          padding-right: 12px !important;
        }
        
        /* Select 图标样式 - 绝对定位在容器左侧居中 */
        .select-with-icon .anticon {
          color: #bfbfbf !important;
          transition: color 0.3s !important;
          pointer-events: none !important;
          z-index: 10 !important;
        }
        
        .ant-input-affix-wrapper-focused .anticon,
        .ant-select-focused ~ .anticon {
          color: #1890ff !important;
        }
        
        .ant-input:focus {
          border-bottom-color: #1890ff !important;
          border-bottom-width: 1px !important;
        }
        
        /* 输入框大小优化 */
        .ant-input-affix-wrapper-lg,
        .ant-input-lg,
        .ant-select-lg .ant-select-selector,
        .ant-picker-lg {
          padding: 6px 0 !important;
          font-size: 15px !important;
          min-height: auto !important;
        }
        
        /* 输入框内部padding，为图标留出空间 */
        .ant-input-affix-wrapper-lg .ant-input {
          padding-left: 0 !important;
          padding-right: 0 !important;
        }
        
        /* 移除输入框的默认背景和边框 */
        .ant-input-affix-wrapper:not(.ant-input-affix-wrapper-disabled):hover {
          border-bottom-color: #8c8c8c !important;
        }
        
        /* Select 下拉箭头位置调整 */
        .ant-select-arrow {
          right: 0 !important;
        }
        
        /* DatePicker 图标位置调整 */
        .ant-picker-suffix {
          right: 0 !important;
        }
        
        /* 表单项间距优化 - 增加上下间距 */
        .ant-form-item {
          margin-bottom: 32px !important;
        }
        
        /* 表单项标签和输入框之间的间距 */
        .ant-form-item-label {
          margin-bottom: 4px !important;
        }
        
        /* 移除Card组件的边框和阴影，但保留内边距 */
        .ant-card {
          border: none !important;
          box-shadow: none !important;
        }
        
        .ant-card-body {
          padding: 24px 32px !important;
        }
        
        /* TextArea 特殊处理 */
        textarea.ant-input {
          border: none !important;
          border-bottom: 1px solid #000000 !important;
          border-top: none !important;
          border-left: none !important;
          border-right: none !important;
          border-radius: 0 !important;
          background-color: transparent !important;
          padding-left: 60px !important;
        }
        
        textarea.ant-input:focus {
          border-bottom-color: #1890ff !important;
          border-bottom-width: 1px !important;
        }
      `}</style>
      <Title level={2}>{isEditMode ? '编辑项目' : '创建新项目'}</Title>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{ status: 'InProgress' }}
        preserve={false}
      >
        <Card>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="项目编号"
                name="projectNumber"
                hasFeedback
                validateTrigger={['onBlur', 'onChange']}
                rules={[
                  { required: true, message: '请输入项目编号' },
                  { max: 50, message: '项目编号不能超过50个字符' },
                ]}
                tooltip="如：CL2025-JCBG0045"
              >
                <Input
                  prefix={<FileTextOutlined style={{ color: '#bfbfbf' }} />}
                  placeholder="请输入项目编号"
                  size="large"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="项目名称"
                name="projectName"
                hasFeedback
                validateTrigger={['onBlur', 'onChange']}
                rules={[
                  { required: true, message: '请输入项目名称' },
                  { max: 200, message: '项目名称不能超过200个字符' },
                ]}
              >
                <Input
                  prefix={<FileTextOutlined style={{ color: '#bfbfbf' }} />}
                  placeholder="请输入项目名称"
                  size="large"
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                label="项目类型"
                name="projectType"
                hasFeedback
                validateTrigger={['onBlur', 'onChange']}
                rules={[
                  { required: true, message: '请选择项目类型' },
                ]}
              >
                <div style={{ position: 'relative' }} className="select-with-icon">
                  <SettingOutlined
                    style={{
                      position: 'absolute',
                      left: '8px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: '#bfbfbf',
                      zIndex: 10,
                      pointerEvents: 'none',
                    }}
                  />
                  <Select
                    value={selectedProjectType}
                    placeholder="请选择项目类型"
                    size="large"
                    showSearch
                    style={{ width: '100%' }}
                    className="ant-select-with-icon"
                    filterOption={(input, option) =>
                      (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                    }
                    options={PROJECT_TYPE_OPTIONS}
                    onChange={(value) => {
                      form.setFieldsValue({ projectType: value });
                    }}
                  />
                </div>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="项目编号（第三方）"
                name="thirdPartyProjectNumber"
                hasFeedback
                validateTrigger={['onBlur', 'onChange']}
                rules={[{ max: 100, message: '第三方项目编号不能超过100个字符' }]}
                tooltip="可选，与内部项目编号独立"
              >
                <Input
                  prefix={<FileTextOutlined style={{ color: '#bfbfbf' }} />}
                  placeholder="请输入第三方项目编号（可选）"
                  size="large"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="第三方名称"
                name="thirdPartyName"
                hasFeedback
                validateTrigger={['onBlur', 'onChange']}
                rules={[{ max: 200, message: '第三方名称不能超过200个字符' }]}
                tooltip="可选"
              >
                <Input
                  prefix={<FileTextOutlined style={{ color: '#bfbfbf' }} />}
                  placeholder="请输入第三方名称（可选）"
                  size="large"
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="客户方"
                name="powerPlantId"
                hasFeedback
                rules={[
                  { required: true, message: '请选择电厂' },
                ]}
                tooltip="选择电厂后，下方会显示该电厂的机组列表"
              >
                <div style={{ position: 'relative' }} className="select-with-icon">
                  <BankOutlined
                    style={{
                      position: 'absolute',
                      left: '8px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: '#bfbfbf',
                      zIndex: 10,
                      pointerEvents: 'none',
                    }}
                  />
                  <Select
                    value={selectedPowerPlantId}
                    placeholder="请选择电厂"
                    size="large"
                    showSearch
                    style={{ width: '100%' }}
                    className="ant-select-with-icon"
                    filterOption={(input, option) =>
                      (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                    }
                    options={powerPlantOptions}
                    onChange={(value) => {
                      console.log('🔴 [客户方] onChange:', value);
                      // 确保 powerPlantId 被正确设置，并清空 unitId
                      form.setFieldsValue({ 
                        powerPlantId: value,
                        unitId: undefined 
                      });
                      if (value) {
                        queryClient.invalidateQueries({ queryKey: ['units', value] });
                      }
                    }}
                  />
                </div>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="机组"
                name="unitId"
                hasFeedback
                validateTrigger={['onChange', 'onBlur']}
                dependencies={['powerPlantId']}
                rules={[
                  ({ getFieldValue }) => ({
                    validator: (_, value) => {
                      const powerPlantId = getFieldValue('powerPlantId');
                      console.log('机组验证执行:', { powerPlantId, value, valueType: typeof value });
                      // 如果选择了电厂，则机组是必填的
                      if (powerPlantId) {
                        // 检查值是否存在（包括数字0，但不包括undefined、null、空字符串）
                        const hasValue = value !== undefined && value !== null && value !== '' && !(typeof value === 'string' && value.trim() === '');
                        if (!hasValue) {
                          console.log('机组验证失败:', { powerPlantId, value, valueType: typeof value });
                          return Promise.reject(new Error('请选择机组'));
                        }
                        console.log('机组验证通过:', { powerPlantId, value, valueType: typeof value });
                      }
                      return Promise.resolve();
                    },
                  }),
                ]}
                tooltip="选择客户方后，必须选择该电厂下的机组"
              >
                <div style={{ position: 'relative' }} className="select-with-icon">
                  <SettingOutlined
                    style={{
                      position: 'absolute',
                      left: '8px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: '#bfbfbf',
                      zIndex: 10,
                      pointerEvents: 'none',
                    }}
                  />
                  <Select
                    value={selectedUnitId}
                    placeholder="请先选择客户方"
                    size="large"
                    showSearch
                    disabled={!selectedPowerPlantId}
                    style={{ width: '100%' }}
                    className="ant-select-with-icon"
                    filterOption={(input, option) =>
                      (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                    }
                    options={unitOptions}
                    onChange={(value) => {
                      console.log('🔵 [机组] onChange:', value);
                      // 手动设置值，确保值被正确设置到表单中
                      form.setFieldsValue({ unitId: value });
                      // 立即触发验证
                      setTimeout(() => {
                        const currentValue = form.getFieldValue('unitId');
                        const powerPlantId = form.getFieldValue('powerPlantId');
                        console.log('准备验证机组:', { currentValue, powerPlantId });
                        form.validateFields(['unitId']).then(() => {
                          console.log('机组验证成功');
                        }).catch((errors) => {
                          console.log('机组验证失败:', errors);
                        });
                      }, 50);
                    }}
                  />
                </div>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="客户方人员"
            name="customerContact"
            hasFeedback={!!(customerContact && customerContact.trim())}
            rules={[
              { max: 100, message: '客户方人员长度不能超过100个字符' },
            ]}
            validateStatus={customerContact && customerContact.trim() ? 'success' : undefined}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="请输入客户方人员"
              size="large"
            />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="开始日期"
                name="startDate"
                hasFeedback
                rules={[{ required: true, message: '请选择开始日期' }]}
              >
                <DatePicker style={{ width: '100%' }} size="large" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item 
                label="结束日期" 
                name="endDate"
                hasFeedback={!!endDate}
                validateStatus={endDate ? 'success' : undefined}
              >
                <DatePicker style={{ width: '100%' }} size="large" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item 
                label="项目负责人" 
                name="responsiblePerson"
                hasFeedback={!!(responsiblePerson && responsiblePerson.trim())}
                validateStatus={responsiblePerson && responsiblePerson.trim() ? 'success' : undefined}
              >
                <PersonnelSelect
                  options={personnelOptions}
                  placeholder="请选择项目负责人"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item 
                label={
                  <span>
                    工作人员
                    <Button
                      type="primary"
                      size="small"
                      icon={<PlusOutlined />}
                      style={{ marginLeft: 8, padding: '0 6px', height: 22, lineHeight: '22px', fontSize: 12 }}
                      onClick={() => {
                        setNewStaffName('');
                        setStaffModalOpen(true);
                      }}
                    >
                      添加
                    </Button>
                  </span>
                }
                name="staff"
                hasFeedback={staffList.length > 0}
                validateStatus={staffList.length > 0 ? 'success' : undefined}
              >
                <div style={{ minHeight: 32, display: 'flex', alignItems: 'center', flexWrap: 'wrap' }}>
                  {staffList.length > 0 &&
                    staffList.map((name, i) => (
                      <Tag
                        key={`${name}-${i}`}
                        closable
                        onClose={() => {
                          const next = staffList.filter((_, j) => j !== i);
                          setStaffList(next);
                          form.setFieldValue('staff', next.join(STAFF_DELIMITER));
                        }}
                        style={{ marginRight: 8, marginBottom: 4, fontSize: 18 ,marginTop: 15}}
                      >
                        {name}
                      </Tag>
                    ))}
                  <Modal
                    title="添加工作人员"
                    open={staffModalOpen}
                    onOk={() => {
                      const name = newStaffName.trim();
                      if (!name) {
                        message.warning('请输入姓名');
                        return;
                      }
                      const next = [...staffList, name];
                      setStaffList(next);
                      form.setFieldValue('staff', next.join(STAFF_DELIMITER));
                      setNewStaffName('');
                      setStaffModalOpen(false);
                    }}
                    onCancel={() => {
                      setStaffModalOpen(false);
                      setNewStaffName('');
                    }}
                    okText="确定"
                    cancelText="取消"
                    destroyOnClose
                  >
                    <div style={{ padding: '8px 0' }}>
                      <Input
                        prefix={<UserOutlined style={{ color: '#bfbfbf' }} />}
                        placeholder="请输入姓名"
                        size="large"
                        value={newStaffName}
                        onChange={(e) => setNewStaffName(e.target.value)}
                        onPressEnter={(e) => {
                          e.preventDefault();
                          const name = newStaffName.trim();
                          if (name) {
                            const next = [...staffList, name];
                            setStaffList(next);
                            form.setFieldValue('staff', next.join(STAFF_DELIMITER));
                            setNewStaffName('');
                            setStaffModalOpen(false);
                          }
                        }}
                        autoFocus
                      />
                    </div>
                  </Modal>
                </div>
              </Form.Item>
            </Col>
          </Row>

          {isEditMode && (
            <Form.Item label="项目状态" name="status" hasFeedback>
              <Select
                options={[
                  { label: '进行中', value: 'InProgress' },
                  { label: '已完成', value: 'Completed' },
                ]}
              />
            </Form.Item>
          )}

          <Form.Item 
            label="项目描述" 
            name="description"
            hasFeedback={!!(description && description.trim())}
            validateStatus={description && description.trim() ? 'success' : undefined}
          >
            <div style={{ position: 'relative' }}>
              <TextArea
                rows={4}
                placeholder="请输入项目描述（可选）"
                maxLength={1000}
                showCount
                style={{ paddingLeft: '60px' }}
              />
              <FileTextOutlined
                style={{
                  position: 'absolute',
                  left: '0px',
                  top: '12px',
                  color: '#bfbfbf',
                  pointerEvents: 'none',
                  width: '16px',
                  height: '16px',
                }}
              />
            </div>
          </Form.Item>
        </Card>

        <Space size="large" style={{ marginTop: 16 }}>
          <Button
            type="primary"
            htmlType="submit"
            size="large"
            icon={<SaveOutlined />}
            loading={createMutation.isPending || updateMutation.isPending}
          >
            {isEditMode ? '保存修改' : '创建项目'}
          </Button>
          <Button size="large" onClick={() => navigate('/projects')}>
            取消
          </Button>
        </Space>
      </Form>
    </div>
  );
};

export default ProjectFormPage;
