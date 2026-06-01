import React from 'react';
import { Select } from 'antd';
import { UserOutlined } from '@ant-design/icons';

export interface PersonnelSelectOption {
  label: string;
  value: string;
}

export interface PersonnelSelectProps {
  value?: string;
  onChange?: (value: string | undefined) => void;
  options: PersonnelSelectOption[];
  placeholder: string;
  showSearch?: boolean;
  allowClear?: boolean;
  disabled?: boolean;
  size?: 'large' | 'middle' | 'small';
  style?: React.CSSProperties;
  className?: string;
}

/**
 * Select with left icon for use inside Form.Item.
 * Forwards value/onChange so form state updates correctly when wrapped in a div.
 */
const PersonnelSelect: React.FC<PersonnelSelectProps> = ({
  value,
  onChange,
  options,
  placeholder,
  showSearch = true,
  allowClear = true,
  disabled,
  size = 'large',
  style = { width: '100%' },
  className = 'ant-select-with-icon',
}) => (
  <div style={{ position: 'relative' }} className="select-with-icon">
    <UserOutlined
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
      value={value}
      onChange={onChange}
      options={options}
      showSearch={showSearch}
      disabled={disabled}
      filterOption={(inputValue, option) =>
        (option?.label ?? '').toString().indexOf(inputValue) !== -1
      }
      allowClear={allowClear}
      placeholder={placeholder}
      size={size}
      style={style}
      className={className}
    />
  </div>
);

export default PersonnelSelect;
