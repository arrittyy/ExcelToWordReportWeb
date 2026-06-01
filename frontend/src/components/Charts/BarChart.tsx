import React from 'react';
import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';

interface BarChartData {
  experimentTypeName: string;
  qualificationRate: number;
  totalItems: number;
}

interface BarChartProps {
  data: BarChartData[];
  title?: string;
}

const BarChart: React.FC<BarChartProps> = ({ data, title }) => {
  const names = data.map((item) => item.experimentTypeName);
  const rates = data.map((item) => item.qualificationRate);

  const option: EChartsOption = {
    title: {
      text: title,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal',
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        const param = params[0];
        const index = param.dataIndex;
        const item = data[index];
        return `${param.name}<br/>合格率: ${param.value}%<br/>总检测数: ${item.totalItems}`;
      },
    },
    xAxis: {
      type: 'category',
      data: names,
    },
    yAxis: {
      type: 'value',
      name: '合格率 (%)',
      max: 100,
    },
    series: [
      {
        data: rates,
        type: 'bar',
        itemStyle: {
          color: (params) => {
            const value = params.value as number;
            if (value >= 95) return '#52c41a';
            if (value >= 80) return '#faad14';
            return '#f5222d';
          },
          borderRadius: [5, 5, 0, 0],
        },
        label: {
          show: true,
          position: 'top',
          formatter: '{c}%',
        },
      },
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
  };

  return <ReactECharts option={option} style={{ height: '400px' }} />;
};

export default BarChart;



