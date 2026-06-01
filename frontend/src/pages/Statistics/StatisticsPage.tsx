import React, { useState, useMemo, useEffect } from 'react';
import { Card, Row, Col, Typography, List } from 'antd';
import { ProjectOutlined, UserOutlined, WarningOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import ReactEcharts from 'echarts-for-react';
import * as echarts from 'echarts';
import { projectService } from '@/services/projectService';
import { powerPlantService } from '@/services/powerPlantService';
import { reportService } from '@/services/reportService';
import { getCityCoordinates } from '@/utils/cityCoordinates';
import StatCard from '@/components/Charts/StatCard';
import type { ProjectList } from '@/types';

let chinaMapLoaded = false;
let chinaMapLoadFailed = false;
const loadChinaMap = async (): Promise<boolean> => {
  if (chinaMapLoaded) return true;
  if (chinaMapLoadFailed) return false;
  const mapSources = [
    'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json',
    'https://raw.githubusercontent.com/apache/echarts/master/map/json/china.json',
    'https://fastly.jsdelivr.net/npm/echarts@latest/map/json/china.json',
  ];
  for (const source of mapSources) {
    try {
      const response = await fetch(source, { method: 'GET', headers: { Accept: 'application/json' } });
      if (!response.ok) continue;
      const geoJson = await response.json();
      if (geoJson?.features) {
        echarts.registerMap('china', geoJson);
        chinaMapLoaded = true;
        return true;
      }
    } catch {
      continue;
    }
  }
  chinaMapLoadFailed = true;
  return false;
};

const StatisticsPage: React.FC = () => {
  const navigate = useNavigate();
  const [mapReady, setMapReady] = useState(false);
  const [mapLoadFailed, setMapLoadFailed] = useState(false);

  useEffect(() => {
    loadChinaMap()
      .then((ok) => {
        setMapReady(ok);
        setMapLoadFailed(!ok);
      })
      .catch(() => {
        setMapReady(false);
        setMapLoadFailed(true);
      });
  }, []);

  const { data: projects = [] } = useQuery({
    queryKey: ['projects'],
    queryFn: projectService.getAll,
  });
  const { data: powerPlants = [] } = useQuery({
    queryKey: ['powerPlants'],
    queryFn: powerPlantService.getAll,
  });
  const { data: reports = [] } = useQuery({
    queryKey: ['reports'],
    queryFn: reportService.getAll,
  });

  const activeProjects = useMemo(
    () => projects.filter((p) => p.status === 'InProgress'),
    [projects]
  );
  const projectPlantMap = useMemo(() => {
    const map = new Map<number, number>();
    activeProjects.forEach((p) => {
      if (p.powerPlantId) map.set(p.id, p.powerPlantId);
    });
    return map;
  }, [activeProjects]);
  const activePlantIds = useMemo(
    () => new Set(projectPlantMap.values()),
    [projectPlantMap]
  );
  const plantDefectCounts = useMemo(() => {
    const counts = new Map<number, number>();
    const projToPlant = new Map<number, number>();
    projects.forEach((p) => {
      if (p.powerPlantId) projToPlant.set(p.id, p.powerPlantId);
    });
    reports.forEach((r) => {
      if (r.hasDefect === '是') {
        const plantId = projToPlant.get(r.projectId);
        if (plantId) counts.set(plantId, (counts.get(plantId) || 0) + 1);
      }
    });
    return counts;
  }, [reports, projects]);
  const currentYear = new Date().getFullYear();
  const annualProjectCount = useMemo(
    () => projects.filter((p) => p.startDate?.startsWith(String(currentYear))).length,
    [projects, currentYear]
  );
  const ongoingPersonCount = useMemo(
    () => new Set(activeProjects.map((p) => p.responsiblePerson).filter(Boolean)).size,
    [activeProjects]
  );
  const totalDefectCount = useMemo(
    () => reports.filter((r) => r.hasDefect === '是').length,
    [reports]
  );
  const mapData = useMemo(
    () =>
      powerPlants.map((plant) => {
        const [lng, lat] = getCityCoordinates(plant.province, plant.city);
        return {
          name: plant.name,
          value: [lng, lat, plant.id],
          plantId: plant.id,
          hasActiveProject: activePlantIds.has(plant.id),
          plant,
        };
      }),
    [powerPlants, activePlantIds]
  );
  const mapOption = useMemo(() => {
    if (!mapReady || mapLoadFailed) {
      return {
        backgroundColor: 'transparent',
        graphic: {
          type: 'text',
          left: 'center',
          top: 'middle',
          style: {
            text: '地图数据加载失败，请检查网络连接',
            fontSize: 16,
            fill: '#999',
          },
        },
      };
    }
    return {
      backgroundColor: 'transparent',
      geo: {
        map: 'china',
        roam: false,
        zoom: 1.5,
        center: [104.0, 35.0],
        itemStyle: {
          areaColor: '#1e3a8a',
          borderColor: '#3b82f6',
          borderWidth: 1,
        },
        emphasis: { itemStyle: { areaColor: '#3b82f6' } },
      },
      legend: {
        show: true,
        orient: 'horizontal',
        left: 20,
        bottom: 20,
        data: ['普通电厂', '有正在开展项目'],
        textStyle: { color: '#333', fontSize: 12 },
        icon: 'circle',
      },
      tooltip: {
        trigger: 'item',
        formatter: (params: any) =>
          params.seriesType === 'scatter'
            ? `${params.data.name}<br/>${params.data.plant?.province} ${params.data.plant?.city}`
            : params.name,
      },
      series: [
        {
          name: '普通电厂',
          type: 'scatter',
          coordinateSystem: 'geo',
          data: mapData.filter((d) => !d.hasActiveProject),
          symbolSize: 8,
          itemStyle: { color: '#C0C0C0', shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' },
          emphasis: { itemStyle: { color: '#fbbf24', borderColor: '#fff', borderWidth: 2 } },
        },
        {
          name: '有正在开展项目',
          type: 'scatter',
          coordinateSystem: 'geo',
          data: mapData.filter((d) => d.hasActiveProject),
          symbolSize: 10,
          itemStyle: { color: '#ef4444', shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' },
          emphasis: { itemStyle: { color: '#fbbf24', borderColor: '#fff', borderWidth: 2 } },
        },
      ],
    };
  }, [mapData, mapReady, mapLoadFailed]);

  return (
    <div>
      <Typography.Title level={2}>数据管理</Typography.Title>

      {/* 地图与电厂/缺陷统计 */}
      <Row gutter={[16, 16]} align="stretch" style={{ marginBottom: 40 ,marginTop: 20}}>
        <Col xs={24} lg={16}>
          <Card
            title="支持电厂"
            headStyle={{ borderBottom: 'none', fontSize: 20 }}
            style={{
              borderRadius: 20,
              boxShadow: '0 1px 4px rgba(0,21,41,.08)',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
            }}
            styles={{ body: { padding: 8, flex: 1, minHeight: 0, overflow: 'hidden' } }}
          >
            <div style={{ height: '100%', minHeight: 0 }}>
              {mapReady && !mapLoadFailed ? (
                <ReactEcharts option={mapOption} style={{ height: '100%', width: '100%' }} />
              ) : mapLoadFailed ? (
                <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
                  地图数据加载失败
                </div>
              ) : (
                <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
                  正在加载地图...
                </div>
              )}
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card
            title="电厂统计"
            headStyle={{ borderBottom: 'none' }}
            style={{ borderRadius: 20, boxShadow: '0 1px 4px rgba(0,21,41,.08)', marginBottom: 16 }}
            bodyStyle={{ padding: 16 }}
          >
            <ReactEcharts
              option={{
                tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
                legend: { orient: 'vertical', left: 'left', top: 'center', textStyle: { color: '#333', fontSize: 12 } },
                series: [{
                  name: '电厂统计',
                  type: 'pie',
                  radius: ['40%', '70%'],
                  center: ['60%', '50%'],
                  itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
                  label: { show: true, formatter: '{b}\n{c} ({d}%)', fontSize: 12 },
                  data: [
                    { value: activePlantIds.size, name: '有正在开展项目', itemStyle: { color: '#ef4444' } },
                    { value: Math.max(0, powerPlants.length - activePlantIds.size), name: '普通电厂', itemStyle: { color: '#3b82f6' } },
                  ],
                }],
              }}
              style={{ height: 180, width: '100%' }}
            />
          </Card>
          <Card
            title="缺陷统计（按电厂）"
            headStyle={{ borderBottom: 'none' }}
            style={{ borderRadius: 20, boxShadow: '0 1px 4px rgba(0,21,41,.08)' }}
            bodyStyle={{ padding: 16 }}
          >
            <ReactEcharts
              option={{
                tooltip: { trigger: 'item', formatter: '{b}: {c}个缺陷' },
                series: [{
                  name: '缺陷统计',
                  type: 'pie',
                  radius: '60%',
                  center: ['50%', '50%'],
                  itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
                  label: { show: true, formatter: '{b}\n{c}个', fontSize: 12 },
                  data: Array.from(plantDefectCounts.entries())
                    .map(([plantId, count]) => ({
                      value: count,
                      name: powerPlants.find((p) => p.id === plantId)?.name || `电厂${plantId}`,
                    }))
                    .sort((a, b) => b.value - a.value)
                    .slice(0, 10),
                }],
              }}
              style={{ height: 180, width: '100%' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 4 个概览卡片：年度累计项目、正在进行项目、正在开展项目人员、累计发现缺陷 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="年度累计项目"
            value={annualProjectCount}
            prefix={<ProjectOutlined />}
            valueStyle={{ color: '#1890ff' }}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="正在进行项目"
            value={activeProjects.length}
            prefix={<ProjectOutlined />}
            valueStyle={{ color: '#52c41a' }}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="正在开展项目人员"
            value={ongoingPersonCount}
            prefix={<UserOutlined />}
            valueStyle={{ color: '#722ed1' }}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="累计发现缺陷"
            value={totalDefectCount}
            prefix={<WarningOutlined />}
            valueStyle={{ color: '#faad14' }}
          />
        </Col>
      </Row>

      {/* 进行中项目列表 */}
      <Card
        title={<><ProjectOutlined /> 进行中项目</>}
        style={{ marginBottom: 16, borderRadius: 30, boxShadow: '0 1px 4px rgba(0,21,41,.08)' }}
        bodyStyle={{ padding: activeProjects.length ? '16px 24px' : 24 }}
      >
        {activeProjects.length === 0 ? (
          <div style={{ color: '#999', padding: '16px 0' }}>暂无进行中的项目</div>
        ) : (
          <List
            size="small"
            itemLayout="horizontal"
            dataSource={activeProjects.slice(0, 20)}
            renderItem={(item: ProjectList) => (
              <List.Item
                style={{ cursor: 'pointer' }}
                onClick={() => navigate(`/projects/${item.id}`)}
                actions={[<span key="go">查看</span>]}
              >
                <List.Item.Meta
                  title={item.projectName}
                  description={`${item.projectNumber}${item.reportCount != null ? ` · ${item.reportCount} 份报告` : ''}`}
                />
              </List.Item>
            )}
          />
        )}
      </Card>
    </div>
  );
};

export default StatisticsPage;



