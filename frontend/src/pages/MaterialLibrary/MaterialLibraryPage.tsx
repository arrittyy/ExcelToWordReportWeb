import React, { useEffect, useMemo, useState } from 'react';
import { Badge, Tabs, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { isSubUser } from '@/utils/auth';
import {
  MATERIAL_CATEGORY_LABELS,
  type MaterialCategory,
} from '@/constants/materialLibraryFields';
import { materialLibraryService } from '@/services/materialLibraryService';
import MaterialCategoryPanel from '@/components/MaterialLibrary/MaterialCategoryPanel';
import MaterialPendingPanel from '@/components/MaterialLibrary/MaterialPendingPanel';
import MaterialMySubmissionsPanel from '@/components/MaterialLibrary/MaterialMySubmissionsPanel';
import './MaterialLibraryPage.css';

const { Title, Paragraph } = Typography;

type MaterialTabKey = MaterialCategory | 'pending' | 'my-submissions';

const CATEGORY_TABS: MaterialCategory[] = ['alloy', 'leeb', 'bolt', 'mechanical', 'hardness'];

const MaterialLibraryPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab');
  const subUser = isSubUser();

  const { data: capabilities } = useQuery({
    queryKey: ['materialLibraryCapabilities'],
    queryFn: materialLibraryService.capabilities,
    enabled: !subUser,
  });

  const canReview = capabilities?.canReview ?? false;
  const rejectedCount = capabilities?.rejectedCount ?? 0;
  const pendingReviewCount = capabilities?.pendingReviewCount ?? 0;

  const resolveTab = (tab: string | null): MaterialTabKey => {
    if (tab === 'pending' && canReview) return 'pending';
    if (tab === 'my-submissions' && !subUser) return 'my-submissions';
    if (CATEGORY_TABS.includes(tab as MaterialCategory)) return tab as MaterialCategory;
    return 'alloy';
  };

  const [activeTab, setActiveTab] = useState<MaterialTabKey>(resolveTab(tabParam));

  useEffect(() => {
    if (tabParam) {
      setActiveTab(resolveTab(tabParam));
    }
  }, [tabParam, canReview, subUser]);

  const tabItems = useMemo(() => {
    const items: { key: string; label: React.ReactNode; children: React.ReactNode }[] =
      CATEGORY_TABS.map((category) => ({
        key: category,
        label: MATERIAL_CATEGORY_LABELS[category],
        children: (
          <MaterialCategoryPanel category={category} active={activeTab === category} />
        ),
      }));

    if (!subUser) {
      items.push({
        key: 'my-submissions',
        label: (
          <Badge count={rejectedCount} size="small" offset={[8, 0]}>
            我的提交
          </Badge>
        ),
        children: <MaterialMySubmissionsPanel active={activeTab === 'my-submissions'} />,
      });
    }

    if (canReview) {
      items.push({
        key: 'pending',
        label: (
          <Badge count={pendingReviewCount} size="small" offset={[8, 0]}>
            待审核
          </Badge>
        ),
        children: <MaterialPendingPanel active={activeTab === 'pending'} />,
      });
    }

    return items;
  }, [activeTab, canReview, subUser, rejectedCount, pendingReviewCount]);

  return (
    <div>
      <div className="material-library-page-header">
        <Title level={3} style={{ marginBottom: 8 }}>
          材质库
        </Title>
        <Paragraph className="material-library-page-desc">
          新增材质需审核通过后，方可用于项目部件选择与报告标准比对。————审核员：杨希锐、李艳军、魏泉泉、高秀娜、胡锋涛、贾新杰、王鹏飞、王红宝
        </Paragraph>
      </div>

      <Tabs
        className="project-detail-section-tabs"
        activeKey={activeTab}
        onChange={(key) => {
          const next = key as MaterialTabKey;
          setActiveTab(next);
          setSearchParams(next === 'alloy' ? {} : { tab: next });
        }}
        destroyInactiveTabPane={false}
        size="large"
        items={tabItems}
      />
    </div>
  );
};

export default MaterialLibraryPage;
