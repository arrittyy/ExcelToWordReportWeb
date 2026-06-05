import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { isAdmin } from './utils/auth';
import MainLayout from './components/Layout/MainLayout';
import LoginPage from './pages/Login/LoginPage';
import HomePage from './pages/Home/HomePage';
import HelpPage from './pages/Help/HelpPage';
import StatisticsPage from './pages/Statistics/StatisticsPage';
import ProjectListPage from './pages/Projects/ProjectListPage';
import ProjectFormPage from './pages/Projects/ProjectFormPage';
import ProjectDetailPage from './pages/Projects/ProjectDetailPage';
import PowerPlantListPage from './pages/PowerPlants/PowerPlantListPage';
import PowerPlantFormPage from './pages/PowerPlants/PowerPlantFormPage';
import PowerPlantDetailPage from './pages/PowerPlants/PowerPlantDetailPage';
import UserManagementPage from './pages/Users/UserManagementPage';
import ChangePasswordPage from './pages/Profile/ChangePasswordPage';
import MaterialLibraryPage from './pages/MaterialLibrary/MaterialLibraryPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

// Protected Route component
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

// Admin Route component
const AdminRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  if (!isAdmin()) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
};

const App: React.FC = () => {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#667eea',
          borderRadius: 8,
        },
      }}
    >
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/login" element={<LoginPage />} />

                <Route
                  path="/"
                  element={
                    <ProtectedRoute>
                      <MainLayout />
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<HomePage />} />
                  <Route path="projects" element={<ProjectListPage />} />
                  <Route path="projects/new" element={<ProjectFormPage />} />
                  <Route path="projects/:id" element={<ProjectDetailPage />} />
                  <Route path="projects/:id/edit" element={<ProjectFormPage />} />
                  <Route path="power-plants" element={<PowerPlantListPage />} />
                  <Route path="power-plants/new" element={<PowerPlantFormPage />} />
                  <Route path="power-plants/:id" element={<PowerPlantDetailPage />} />
                  <Route path="power-plants/:id/edit" element={<PowerPlantFormPage />} />
                  <Route path="statistics" element={<StatisticsPage />} />
                  <Route path="material-library" element={<MaterialLibraryPage />} />
                  <Route path="help" element={<HelpPage />} />
                  <Route path="change-password" element={<ChangePasswordPage />} />
                  <Route
                    path="users"
                    element={
                      <AdminRoute>
                        <UserManagementPage />
                      </AdminRoute>
                    }
                  />
                </Route>

                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </BrowserRouter>
          </AuthProvider>
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  );
};

export default App;
