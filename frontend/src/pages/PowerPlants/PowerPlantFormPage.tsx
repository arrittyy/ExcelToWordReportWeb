import React, { useEffect, useState } from 'react';
import {
  Form,
  Input,
  Button,
  Card,
  Space,
  message,
  Typography,
  Row,
  Col,
  Select,
  Spin,
} from 'antd';
import {
  SaveOutlined,
  ArrowLeftOutlined,
  BankOutlined,
  GlobalOutlined,
  EnvironmentOutlined,
  HomeOutlined,
  PhoneOutlined,
  PrinterOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { powerPlantService, CreatePowerPlant, UpdatePowerPlant } from '@/services/powerPlantService';
import { normalizeProvince } from '@/utils/cityCoordinates';

const { Title } = Typography;
const { TextArea } = Input;

// 中国省市数据（简化版，实际应该从API或配置文件加载）
const PROVINCES = [
  '北京市', '天津市', '河北省', '山西省', '内蒙古自治区', '辽宁省', '吉林省', '黑龙江省',
  '上海市', '江苏省', '浙江省', '安徽省', '福建省', '江西省', '山东省', '河南省',
  '湖北省', '湖南省', '广东省', '广西壮族自治区', '海南省', '重庆市', '四川省', '贵州省',
  '云南省', '西藏自治区', '陕西省', '甘肃省', '青海省', '宁夏回族自治区', '新疆维吾尔自治区'
];

const CITIES: Record<string, string[]> = {
  '北京市': ['东城区', '西城区', '朝阳区', '丰台区', '石景山区', '海淀区', '门头沟区', '房山区', '通州区', '顺义区', '昌平区', '大兴区', '怀柔区', '平谷区', '密云区', '延庆区'],
  '天津市': ['和平区', '河东区', '河西区', '南开区', '河北区', '红桥区', '东丽区', '西青区', '津南区', '北辰区', '武清区', '宝坻区', '滨海新区', '宁河区', '静海区', '蓟州区'],
  '河北省': ['石家庄市', '唐山市', '秦皇岛市', '邯郸市', '邢台市', '保定市', '张家口市', '承德市', '沧州市', '廊坊市', '衡水市'],
  '山西省': ['太原市', '大同市', '阳泉市', '长治市', '晋城市', '朔州市', '晋中市', '运城市', '忻州市', '临汾市', '吕梁市'],
  '内蒙古自治区': ['呼和浩特市', '包头市', '乌海市', '赤峰市', '通辽市', '鄂尔多斯市', '呼伦贝尔市', '巴彦淖尔市', '乌兰察布市', '兴安盟', '锡林郭勒盟', '阿拉善盟'],
  '辽宁省': ['沈阳市', '大连市', '鞍山市', '抚顺市', '本溪市', '丹东市', '锦州市', '营口市', '阜新市', '辽阳市', '盘锦市', '铁岭市', '朝阳市', '葫芦岛市'],
  '吉林省': ['长春市', '吉林市', '四平市', '辽源市', '通化市', '白山市', '松原市', '白城市', '延边朝鲜族自治州'],
  '黑龙江省': ['哈尔滨市', '齐齐哈尔市', '鸡西市', '鹤岗市', '双鸭山市', '大庆市', '伊春市', '佳木斯市', '七台河市', '牡丹江市', '黑河市', '绥化市', '大兴安岭地区'],
  '上海市': ['黄浦区', '徐汇区', '长宁区', '静安区', '普陀区', '虹口区', '杨浦区', '闵行区', '宝山区', '嘉定区', '浦东新区', '金山区', '松江区', '青浦区', '奉贤区', '崇明区'],
  '江苏省': ['南京市', '无锡市', '徐州市', '常州市', '苏州市', '南通市', '连云港市', '淮安市', '盐城市', '扬州市', '镇江市', '泰州市', '宿迁市', '常熟市'],
  '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市', '丽水市'],
  '安徽省': ['合肥市', '芜湖市', '蚌埠市', '淮南市', '马鞍山市', '淮北市', '铜陵市', '安庆市', '黄山市', '滁州市', '阜阳市', '宿州市', '六安市', '亳州市', '池州市', '宣城市'],
  '福建省': ['福州市', '厦门市', '莆田市', '三明市', '泉州市', '漳州市', '南平市', '龙岩市', '宁德市'],
  '江西省': ['南昌市', '景德镇市', '萍乡市', '九江市', '新余市', '鹰潭市', '赣州市', '吉安市', '宜春市', '抚州市', '上饶市'],
  '山东省': ['济南市', '青岛市', '淄博市', '枣庄市', '东营市', '烟台市', '潍坊市', '济宁市', '泰安市', '威海市', '日照市', '临沂市', '德州市', '聊城市', '滨州市', '菏泽市'],
  '河南省': ['郑州市', '开封市', '洛阳市', '平顶山市', '安阳市', '鹤壁市', '新乡市', '焦作市', '濮阳市', '许昌市', '漯河市', '三门峡市', '南阳市', '商丘市', '信阳市', '周口市', '驻马店市', '济源市', '登封市'],
  '湖北省': ['武汉市', '黄石市', '十堰市', '宜昌市', '襄阳市', '鄂州市', '荆门市', '孝感市', '荆州市', '黄冈市', '咸宁市', '随州市', '恩施土家族苗族自治州', '赤壁市'],
  '湖南省': ['长沙市', '株洲市', '湘潭市', '衡阳市', '邵阳市', '岳阳市', '常德市', '张家界市', '益阳市', '郴州市', '永州市', '怀化市', '娄底市', '湘西土家族苗族自治州'],
  '广东省': ['广州市', '韶关市', '深圳市', '珠海市', '汕头市', '佛山市', '江门市', '湛江市', '茂名市', '肇庆市', '惠州市', '梅州市', '汕尾市', '河源市', '阳江市', '清远市', '东莞市', '中山市', '潮州市', '揭阳市', '云浮市'],
  '广西壮族自治区': ['南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市', '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市'],
  '海南省': ['海口市', '三亚市', '三沙市', '儋州市', '五指山市', '琼海市', '文昌市', '万宁市', '东方市', '定安县', '屯昌县', '澄迈县', '临高县', '白沙黎族自治县', '昌江黎族自治县', '乐东黎族自治县', '陵水黎族自治县', '保亭黎族苗族自治县', '琼中黎族苗族自治县'],
  '重庆市': ['万州区', '涪陵区', '渝中区', '大渡口区', '江北区', '沙坪坝区', '九龙坡区', '南岸区', '北碚区', '綦江区', '大足区', '渝北区', '巴南区', '黔江区', '长寿区', '江津区', '合川区', '永川区', '南川区', '璧山区', '铜梁区', '潼南区', '荣昌区', '开州区', '梁平区', '武隆区', '城口县', '丰都县', '垫江县', '忠县', '云阳县', '奉节县', '巫山县', '巫溪县', '石柱土家族自治县', '秀山土家族苗族自治县', '酉阳土家族苗族自治县', '彭水苗族土家族自治县'],
  '四川省': ['成都市', '自贡市', '攀枝花市', '泸州市', '德阳市', '绵阳市', '广元市', '遂宁市', '内江市', '乐山市', '南充市', '眉山市', '宜宾市', '广安市', '达州市', '雅安市', '巴中市', '资阳市', '阿坝藏族羌族自治州', '甘孜藏族自治州', '凉山彝族自治州'],
  '贵州省': ['贵阳市', '六盘水市', '遵义市', '安顺市', '毕节市', '铜仁市', '黔西南布依族苗族自治州', '黔东南苗族侗族自治州', '黔南布依族苗族自治州'],
  '云南省': ['昆明市', '曲靖市', '玉溪市', '保山市', '昭通市', '丽江市', '普洱市', '临沧市', '楚雄彝族自治州', '红河哈尼族彝族自治州', '文山壮族苗族自治州', '西双版纳傣族自治州', '大理白族自治州', '德宏傣族景颇族自治州', '怒江傈僳族自治州', '迪庆藏族自治州'],
  '西藏自治区': ['拉萨市', '日喀则市', '昌都市', '林芝市', '山南市', '那曲市', '阿里地区'],
  '陕西省': ['西安市', '铜川市', '宝鸡市', '咸阳市', '渭南市', '延安市', '汉中市', '榆林市', '安康市', '商洛市'],
  '甘肃省': ['兰州市', '嘉峪关市', '金昌市', '白银市', '天水市', '武威市', '张掖市', '平凉市', '酒泉市', '庆阳市', '定西市', '陇南市', '临夏回族自治州', '甘南藏族自治州'],
  '青海省': ['西宁市', '海东市', '海北藏族自治州', '黄南藏族自治州', '海南藏族自治州', '果洛藏族自治州', '玉树藏族自治州', '海西蒙古族藏族自治州'],
  '宁夏回族自治区': ['银川市', '石嘴山市', '吴忠市', '固原市', '中卫市'],
  '新疆维吾尔自治区': ['乌鲁木齐市', '克拉玛依市', '吐鲁番市', '哈密市', '昌吉回族自治州', '博尔塔拉蒙古自治州', '巴音郭楞蒙古自治州', '阿克苏地区', '克孜勒苏柯尔克孜自治州', '喀什地区', '和田地区', '伊犁哈萨克自治州', '塔城地区', '阿勒泰地区', '石河子市', '阿拉尔市', '图木舒克市', '五家渠市', '北屯市', '铁门关市', '双河市', '可克达拉市', '昆玉市', '胡杨河市', '新星市', '白杨市'],
};

// 大区选项常量
const REGIONS = [
  '华北大区',
  '华中大区',
  '华东大区',
  '华南大区',
  '北方大区',
  '东北大区',
  '中西大区',
  '外部客户'
];

/** 规范化大区：确保返回值在 REGIONS 中，否则按前缀匹配 */
function normalizeRegion(region: string): string {
  const s = (region ?? '').trim();
  if (!s) return '';
  if (REGIONS.includes(s)) return s;
  const found = REGIONS.find(r => r.startsWith(s) || s.startsWith(r));
  return found ?? s;
}

/** 从接口数据取字段，兼容 camelCase / snake_case */
function getApiField(obj: Record<string, unknown> | null | undefined, camelKey: string): string {
  if (!obj) return '';
  const camel = obj[camelKey];
  if (camel !== undefined && camel !== null) return String(camel);
  const snake = camelKey.replace(/[A-Z]/g, (c) => '_' + c.toLowerCase());
  const snakeVal = obj[snake];
  if (snakeVal !== undefined && snakeVal !== null) return String(snakeVal);
  return '';
}

const iconStyle = {
  position: 'absolute' as const,
  left: '8px',
  top: '50%',
  transform: 'translateY(-50%)',
  color: '#bfbfbf',
  zIndex: 10,
  pointerEvents: 'none' as const,
};

/** 带左侧图标的 Select，作为 Form.Item 直接子组件以正确接收 value/onChange */
const SelectWithIcon: React.FC<{
  value?: string;
  onChange?: (val: string) => void;
  placeholder?: string;
  options?: { label: string; value: string }[];
  disabled?: boolean;
  icon: React.ReactNode;
  extraOnChange?: (val: string) => void;
  [key: string]: unknown;
}> = ({ value, onChange, placeholder, options = [], disabled, icon, extraOnChange, ...rest }) => (
  <div style={{ position: 'relative' }} className="select-with-icon">
    {icon}
    <Select
      value={value}
      onChange={(val) => {
        onChange?.(val as string);
        extraOnChange?.(val as string);
      }}
      placeholder={placeholder}
      options={options}
      disabled={disabled}
      size="large"
      showSearch
      style={{ width: '100%' }}
      className="ant-select-with-icon"
      filterOption={(input, option) =>
        (option?.label ?? '').toLowerCase().includes((input ?? '').toLowerCase())
      }
      {...rest}
    />
  </div>
);

const PowerPlantFormPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [form] = Form.useForm();

  const isEditMode = !!id;
  // 用 state 存当前选的省，保证选省后市的选项和 enabled 立即更新（不依赖 useWatch 时机）
  const [provinceForCity, setProvinceForCity] = useState<string>('');

  const { data: powerPlantData, isLoading: isLoadingPlant } = useQuery({
    queryKey: ['powerPlant', id],
    queryFn: () => powerPlantService.getById(Number(id)),
    enabled: isEditMode,
  });

  // 编辑回显：兼容接口 camelCase/snake_case，规范化省/大区以匹配下拉选项
  const editFormValues = React.useMemo(() => {
    if (!powerPlantData) return undefined;
    const raw = powerPlantData as unknown as Record<string, unknown>;
    return {
      name: getApiField(raw, 'name'),
      region: normalizeRegion(getApiField(raw, 'region')),
      province: normalizeProvince(getApiField(raw, 'province')),
      city: getApiField(raw, 'city'),
      address: getApiField(raw, 'address'),
      phone: getApiField(raw, 'phone'),
      fax: getApiField(raw, 'fax'),
      remark: getApiField(raw, 'remark'),
    };
  }, [powerPlantData]);

  // 编辑时数据加载后写入表单，并同步“当前省”供市下拉用
  useEffect(() => {
    if (!isEditMode || !editFormValues) return;
    form.setFieldsValue(editFormValues);
    setProvinceForCity(editFormValues.province ?? '');
    const id = setTimeout(() => {
      form.setFieldsValue(editFormValues);
      setProvinceForCity(editFormValues.province ?? '');
    }, 80);
    return () => clearTimeout(id);
  }, [isEditMode, editFormValues, form]);

  const createMutation = useMutation({
    mutationFn: powerPlantService.create,
    onSuccess: () => {
      message.success('电厂创建成功！');
      queryClient.invalidateQueries({ queryKey: ['powerPlants'] });
      navigate('/power-plants');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: number; data: UpdatePowerPlant }) =>
      powerPlantService.update(data.id, data.data),
    onSuccess: () => {
      message.success('电厂更新成功！');
      queryClient.invalidateQueries({ queryKey: ['powerPlants'] });
      queryClient.invalidateQueries({ queryKey: ['powerPlant', id] });
      navigate('/power-plants');
    },
  });

  const handleSubmit = async (values: any) => {
    try {
      const data: CreatePowerPlant = {
        name: values.name,
        region: values.region,
        province: values.province,
        city: values.city,
        address: values.address,
        phone: values.phone,
        fax: values.fax,
        remark: values.remark,
      };

      if (isEditMode) {
        updateMutation.mutate({ id: Number(id), data });
      } else {
        createMutation.mutate(data);
      }
    } catch (error) {
      message.error('保存失败，请检查数据');
    }
  };

  // 市下拉：用 provinceForCity（省 Select onChange 同步）查 CITIES，规范化省名以兼容 "山东"/"山东省"
  const provinceKey = provinceForCity ? normalizeProvince(provinceForCity.trim()) : '';
  const cityList = provinceKey ? (CITIES[provinceKey] ?? []) : [];
  const cityOptions = cityList.map((city) => ({ label: city, value: city }));

  return (
    <div>
      <style>{`
        /* 下划线样式输入框 - 只有一条横线，彻底移除所有边框 */
        .ant-input-affix-wrapper {
          border: none !important;
          border-bottom: 1px solid #d9d9d9 !important;
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
          border-bottom: 1px solid #d9d9d9 !important;
          border-top: none !important;
          border-left: none !important;
          border-right: none !important;
          border-radius: 0 !important;
          background-color: transparent !important;
          box-shadow: none !important;
        }
        
        .ant-select-selector {
          border: none !important;
          border-bottom: 1px solid #d9d9d9 !important;
          border-top: none !important;
          border-left: none !important;
          border-right: none !important;
          border-radius: 0 !important;
          background-color: transparent !important;
          box-shadow: none !important;
        }
        
        /* 移除所有可能的额外边框和伪元素 */
        .ant-input-affix-wrapper::before,
        .ant-input-affix-wrapper::after,
        .ant-select-selector::before,
        .ant-select-selector::after {
          display: none !important;
          border: none !important;
          content: none !important;
        }
        
        /* 移除Form.Item的边框 */
        .ant-form-item-control-input,
        .ant-form-item-control-input-content {
          border: none !important;
        }
        
        .ant-input-affix-wrapper:hover,
        .ant-input:hover,
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
        .ant-form-item-has-success .ant-select-selector {
          border-bottom-color: #52c41a !important;
        }
        .ant-form-item-has-success .ant-input-affix-wrapper:hover,
        .ant-form-item-has-success .ant-input:hover,
        .ant-form-item-has-success .ant-select-selector:hover {
          border-bottom-color: #73d13d !important;
        }
        .ant-form-item-has-success .ant-input-affix-wrapper-focused,
        .ant-form-item-has-success .ant-input-focused,
        .ant-form-item-has-success .ant-select-focused .ant-select-selector {
          border-bottom-color: #52c41a !important;
          border-bottom-width: 1px !important;
        }
        .ant-form-item-has-success .anticon-check-circle {
          color: #52c41a !important;
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
        
        /* 图标样式优化 - 浅灰色，增加间距 */
        .ant-input-affix-wrapper .anticon {
          color: #bfbfbf !important;
          transition: color 0.3s !important;
          padding-right: 12px !important;
        }
        
        .select-with-icon .anticon {
          color: #bfbfbf !important;
          transition: color 0.3s !important;
          left: 0px !important;
          width: 16px !important;
          height: 16px !important;
          margin-right: 8px !important;
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
        .ant-select-lg .ant-select-selector {
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
          border-bottom: 1px solid #d9d9d9 !important;
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
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/power-plants')}>
          返回电厂列表
        </Button>
      </Space>

      <Title level={2}>{isEditMode ? '编辑电厂' : '创建新电厂'}</Title>

      {isEditMode && (isLoadingPlant || !powerPlantData) ? (
        <Card>
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Spin size="large" />
            <div style={{ marginTop: 16 }}>加载电厂信息中...</div>
          </div>
        </Card>
      ) : (
      <Card>
        <Form
          key={isEditMode ? `edit-${id}` : 'create'}
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={isEditMode && editFormValues ? editFormValues : undefined}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="电厂名称"
                name="name"
                hasFeedback
                rules={[
                  { required: true, message: '请输入电厂名称' },
                  { max: 200, message: '电厂名称不能超过200个字符' },
                ]}
              >
                <Input
                  prefix={<BankOutlined style={{ color: '#bfbfbf' }} />}
                  placeholder="请输入电厂名称"
                  size="large"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="大区"
                name="region"
                hasFeedback
                rules={[
                  { required: true, message: '请选择大区' },
                ]}
              >
                <SelectWithIcon
                  icon={<GlobalOutlined style={iconStyle} />}
                  placeholder="请选择大区"
                  options={REGIONS.map((region) => ({ label: region, value: region }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="省"
                name="province"
                hasFeedback
                rules={[
                  { required: true, message: '请选择省' },
                ]}
              >
                <SelectWithIcon
                  icon={<EnvironmentOutlined style={iconStyle} />}
                  placeholder="请选择省"
                  options={PROVINCES.map((p) => ({ label: p, value: p }))}
                  extraOnChange={(val) => {
                    setProvinceForCity(val ?? '');
                    form.setFieldValue('city', undefined);
                  }}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="市"
                name="city"
                hasFeedback
                rules={[
                  { required: true, message: '请选择市' },
                ]}
              >
                <SelectWithIcon
                  icon={<EnvironmentOutlined style={iconStyle} />}
                  placeholder="请选择市"
                  options={cityOptions}
                  disabled={!provinceKey}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="详细地址"
            name="address"
            hasFeedback
            rules={[
              { required: true, message: '请输入详细地址' },
              { max: 500, message: '详细地址长度不能超过500个字符' },
            ]}
          >
            <Input
              prefix={<HomeOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="请输入详细地址"
              size="large"
            />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="电话"
                name="phone"
                hasFeedback
                rules={[{ max: 50, message: '电话长度不能超过50个字符' }]}
              >
                <Input
                  prefix={<PhoneOutlined style={{ color: '#bfbfbf' }} />}
                  placeholder="请输入电话"
                  size="large"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="传真"
                name="fax"
                hasFeedback
                rules={[{ max: 50, message: '传真长度不能超过50个字符' }]}
              >
                <Input
                  prefix={<PrinterOutlined style={{ color: '#bfbfbf' }} />}
                  placeholder="请输入传真"
                  size="large"
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="备注"
            name="remark"
            hasFeedback
            rules={[{ max: 1000, message: '备注长度不能超过1000个字符' }]}
          >
            <div style={{ position: 'relative' }}>
              <TextArea
                placeholder="请输入备注"
                rows={4}
                showCount
                maxLength={1000}
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

          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                icon={<SaveOutlined />}
                loading={createMutation.isPending || updateMutation.isPending}
                size="large"
              >
                {isEditMode ? '更新' : '创建'}
              </Button>
              <Button size="large" onClick={() => navigate('/power-plants')}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
      )}
    </div>
  );
};

export default PowerPlantFormPage;

