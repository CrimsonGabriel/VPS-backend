import { Routes, Route } from 'react-router-dom';

// Import komponentów
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ProtectedRoute from './components/ProtectedRoute';
import StatusIpPage from './pages/StatusIpPage'; 
import UsersPage from './pages/UsersPage';
import ApiTestPage from './pages/ApiTestPage';
import GlobalLayout from './components/GlobalLayout';
import ForbiddenPage from './pages/ForbiddenPage';
import FileUploadPage from './pages/FileUploadPage.jsx';
import FileListPage from './pages/FileListPage.jsx';
import AccountPage from './pages/AccountPage.jsx';
import ResetPasswordPage from './pages/ResetPasswordPage.jsx';
function App() {
  return (
    <Routes>
      <Route element={<GlobalLayout />}> 
        {/* Trasy publiczne */}
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
		<Route path="/reset-password" element={<ResetPasswordPage />} />
		<Route path="/forbidden" element={<ForbiddenPage />} />

        {/* 1. TRASY CHRONIONE (Wymagany Token - Rola USER lub ADMIN) */}
        <Route element={<ProtectedRoute />}>
          <Route path="/status-ip" element={<StatusIpPage />} />
          <Route path="/api-test" element={<ApiTestPage />} />
	  <Route path="/upload" element={<FileUploadPage />} />
    	  <Route path="/files" element={<FileListPage />} />
		  <Route path="/konto" element={<AccountPage />} />
        </Route>
        
        {/* 2. TRASY CHRONIONE DLA ADMINA (Wymagany Token I Rola ADMIN) */}
        {/* ⭐️ Dodajemy requiredRole="ROLE_ADMIN" ⭐️ */}
        <Route element={<ProtectedRoute requiredRole="ROLE_ADMIN" />}>
          {/* Użytkownicy - panel CRUD */}
          <Route path="/users" element={<UsersPage />} /> 
        </Route>

        {/* Catch-all route */}
        <Route path="*" element={<div>404 - Nie znaleziono</div>} />
        
      </Route>
    </Routes>
  );
}

export default App;