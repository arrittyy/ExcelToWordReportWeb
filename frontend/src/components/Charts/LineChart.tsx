import React from 'react';
import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';

interface LineChartProps {
  data: Array<{ date: string; count: number }>;
  title?: string;
}

const LineChart: React.FC<LineChartProps> = ({ data, title }) => {
  const dates = data.map((item) => item.date);
  const counts = data.map((item) => item.count);

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
      formatter: '{b}<br/>报告数: {c}',
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        rotate: 45,
      },
    },
    yAxis: {
      type: 'value',
      name: '报告数',
      minInterval: 1,
    },
    series: [
      {
        data: counts,
        type: 'line',
        smooth: true,
        areaStyle: {
          opacity: 0.3,
        },
        itemStyle: {
          color: '#667eea',
        },
      },
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true,
    },
  };

  return <ReactECharts option={option} style={{ height: '400px' }} />;
};

export default LineChart;



